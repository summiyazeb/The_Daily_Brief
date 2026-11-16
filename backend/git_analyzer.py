import os
import re
from collections import Counter
from datetime import datetime, timezone
import requests
from dotenv import load_dotenv
from google import genai

load_dotenv()

GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

NOISE_PREFIXES = (
    "chore:", "style:", "docs:", "ci:", "build:", "lint:", "format:", "merge "
)
TRIVIAL_EXACT = {
    "initial commit", "wip", "fix typo", "typo", "update readme",
    "update readme.md", "cleanup", "minor fix", "test", "."
}


def _github_headers() -> dict[str, str]:
    headers = {"Accept": "application/vnd.github+json"}
    if GITHUB_TOKEN:
        headers["Authorization"] = f"Bearer {GITHUB_TOKEN}"
    return headers


def is_meaningful_commit(commit_item: dict) -> bool:
    """Filters out merge commits, bot commits, and trivial one-liners."""
    if len(commit_item.get("parents", [])) > 1:
        return False

    author_login = (commit_item.get("author") or {}).get("login", "").lower()
    if "[bot]" in author_login or author_login in {"dependabot", "renovate"}:
        return False

    msg = commit_item["commit"]["message"].strip()
    if not msg:
        return False

    first_line = msg.splitlines()[0].strip()
    lowered = first_line.lower()

    if lowered in TRIVIAL_EXACT or lowered.startswith(NOISE_PREFIXES):
        return False
    if re.match(r"^merge (pull request|branch) ", lowered):
        return False
    if len(first_line.split()) < 2 and len(first_line) < 10:
        return False

    return True


def _generate_ai_summary(repo_name: str, meaningful_commits: list[dict], contributors: list[dict]) -> str:
    """Uses Gemini to summarize meaningful changes (replaces AiSummaryService.java)."""
    if not GEMINI_API_KEY or not meaningful_commits:
        return "No AI summary available."

    commit_lines = "\n".join(
        f"- [{c['timestamp'][:10]}] {c['actor']}: {c['title']}"
        for c in meaningful_commits[:20]
    )
    top_contribs = ", ".join(f"{c['username']} ({c['contributions']})" for c in contributors[:5])

    prompt = (
        f"Summarize the recent engineering progress for repository '{repo_name}' in 3 concise bullet points. "
        f"Focus only on meaningful feature additions, bug fixes, and architectural changes.\n\n"
        f"Top contributors: {top_contribs}\n"
        f"Recent meaningful commits:\n{commit_lines}"
    )

    client = genai.Client(api_key=GEMINI_API_KEY)
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=prompt,
    )
    return response.text.strip()


def analyze_repository(owner: str, repo: str, include_ai: bool = True) -> dict:
    """Fetches repo stats, filters meaningful commits, computes insights, and generates AI summary."""
    headers = _github_headers()
    base_url = f"https://api.github.com/repos/{owner}/{repo}"

    # 1. Fetch repository metadata, recent commits, and contributors
    repo_resp = requests.get(base_url, headers=headers, timeout=15)
    repo_resp.raise_for_status()
    repo_info = repo_resp.json()

    commits_resp = requests.get(f"{base_url}/commits", headers=headers, params={"per_page": 50}, timeout=15)
    commits_resp.raise_for_status()
    raw_commits = commits_resp.json()

    contrib_resp = requests.get(f"{base_url}/contributors", headers=headers, params={"per_page": 20}, timeout=15)
    contributors = [
        {"username": c["login"], "contributions": c["contributions"]}
        for c in (contrib_resp.json() if contrib_resp.ok else [])
    ]

    # 2. Filter meaningful commits into unified dashboard event format
    meaningful_events = []
    author_counter = Counter()

    for item in raw_commits:
        if not is_meaningful_commit(item):
            continue

        commit_data = item["commit"]
        author_name = commit_data["author"]["name"]
        author_counter[author_name] += 1
        msg = commit_data["message"].strip()

        meaningful_events.append({
            "source": "git",
            "repo": f"{owner}/{repo}",
            "actor": author_name,
            "timestamp": commit_data["author"]["date"],
            "title": msg.splitlines()[0].strip(),
            "details": msg,
            "url": item["html_url"],
        })

    # 3. Compute analytics (replaces RepositoryAnalyzer.java)
    total_meaningful = len(meaningful_events)
    dominant_author, dominant_count = author_counter.most_common(1)[0] if author_counter else ("None", 0)
    bus_driver_risk = (dominant_count / total_meaningful > 0.7) if total_meaningful > 0 else False

    pushed_at = repo_info.get("pushed_at")
    days_since_push = 999
    if pushed_at:
        last_push_dt = datetime.fromisoformat(pushed_at.replace("Z", "+00:00"))
        days_since_push = (datetime.now(timezone.utc) - last_push_dt).days

    health = "ACTIVE" if days_since_push <= 14 else ("MODERATE" if days_since_push <= 60 else "INACTIVE")

    # 4. Generate Gemini AI summary
    ai_summary = _generate_ai_summary(f"{owner}/{repo}", meaningful_events, contributors) if include_ai else ""

    return {
        "repo": f"{owner}/{repo}",
        "stars": repo_info.get("stargazers_count", 0),
        "forks": repo_info.get("forks_count", 0),
        "health": health,
        "dominant_contributor": dominant_author,
        "bus_driver_risk": bus_driver_risk,
        "total_raw_commits_checked": len(raw_commits),
        "meaningful_commits_count": total_meaningful,
        "contributors": contributors,
        "ai_summary": ai_summary,
        "events": meaningful_events,
    }
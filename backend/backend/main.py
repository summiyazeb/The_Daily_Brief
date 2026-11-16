from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from git_analyzer import analyze_repository

app = FastAPI(title="The Daily Brief API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/dashboard")
def get_dashboard_data(owner: str, repo: str, ai: bool = True):
    try:
        git_data = analyze_repository(owner, repo, include_ai=ai)
        # Later: slack_events = fetch_meaningful_slack_messages(...)
        # Combined feed sorted newest-first:
        unified_feed = sorted(
            git_data["events"],  # + slack_events
            key=lambda x: x["timestamp"],
            reverse=True,
        )
        return {
            "git_insights": {k: v for k, v in git_data.items() if k != "events"},
            "feed": unified_feed,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
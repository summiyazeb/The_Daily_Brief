import sys
from pathlib import Path
from datetime import datetime, timezone
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from git_analyzer import analyze_repository
from dotenv import load_dotenv
PROJECT_ROOT = Path(__file__).resolve().parents[2]
OURSLACK_PATH = PROJECT_ROOT / "OurSlack"
load_dotenv(OURSLACK_PATH / ".env")

sys.path.insert(0, str(PROJECT_ROOT))
sys.path.insert(0, str(OURSLACK_PATH))

from OurSlack.main import run_ourslack

app = FastAPI(title="The Daily Brief API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

def convert_slack_updates(slack_data):
    events = []

    for update in slack_data.get("updates", []):
        references = update.get("source_references", [])

        timestamps = [
            float(ref["message_ts"])
            for ref in references
            if ref.get("message_ts")
        ]

        if not timestamps:
            continue

        timestamp = datetime.fromtimestamp(
            max(timestamps),
            tz=timezone.utc
        ).isoformat()

        responsible = update.get("responsible_people", [])
        involved = update.get("involved_people", [])

        actor = (
            responsible[0] if responsible
            else involved[0] if involved
            else None
        )

        events.append({
            "source": "slack",
            "actor": actor,
            "timestamp": timestamp,
            "title": update.get("summary", ""),
            "details": update.get("details", ""),
            "category": update.get("category"),
            "reason": update.get("reason"),
            "responsible_people": responsible,
            "involved_people": involved,
            "source_references": references
        })

    return events

@app.get("/api/dashboard")
def get_dashboard_data(owner: str, repo: str, ai: bool = True):
    try:
        git_data = analyze_repository(owner, repo, include_ai=ai)
        slack_data = run_ourslack()
        slack_events = convert_slack_updates(slack_data)
       
        unified_feed = sorted(
            git_data["events"] + slack_events,
            key=lambda x: datetime.fromisoformat(
            x["timestamp"].replace("Z", "+00:00")
            ),
            reverse=True,
        )
        return {
            "git_insights": {k: v for k, v in git_data.items() if k != "events"},
            "feed": unified_feed,
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/slack")
def get_slack_data():
    try:
        slack_data = run_ourslack()
        return slack_data
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/slack-feed")
def get_slack_feed():
    try:
        slack_data = run_ourslack()
        slack_events = convert_slack_updates(slack_data)

        return {
            "total_events": len(slack_events),
            "feed": slack_events
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
import os

from dotenv import load_dotenv
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError


load_dotenv()


def get_slack_client():
    token = os.getenv("SLACK_BOT_TOKEN")

    if not token:
        raise ValueError(
            "SLACK_BOT_TOKEN is missing. Check your .env file."
        )

    return WebClient(token=token)


def verify_slack_connection(client):
    try:
        response = client.auth_test()

        return {
            "authenticated": True,
            "team": response.get("team"),
            "team_id": response.get("team_id"),
            "bot_user_id": response.get("user_id"),
            "bot_name": response.get("user"),
        }

    except SlackApiError as error:
        error_code = error.response.get("error", "unknown_error")

        raise RuntimeError(
            f"Slack authentication failed: {error_code}"
        ) from error
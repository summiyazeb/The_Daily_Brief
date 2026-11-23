import pytest
from unittest.mock import Mock, patch

from slack_sdk.errors import SlackApiError

from ourslack.authentication import (
    get_slack_client,
    verify_slack_connection,
)


def test_missing_slack_token():
    with patch.dict("os.environ", {}, clear=True):
        with pytest.raises(ValueError, match="SLACK_BOT_TOKEN is missing"):
            get_slack_client()


def test_create_slack_client():
    with patch(
        "ourslack.authentication.WebClient"
    ) as mock_client:
        with patch.dict(
            "os.environ",
            {"SLACK_BOT_TOKEN": "xoxb-test-token"}
        ):
            get_slack_client()

        mock_client.assert_called_once_with(
            token="xoxb-test-token"
        )


def test_successful_authentication():
    client = Mock()
    client.auth_test.return_value = {
        "team": "Test Workspace",
        "team_id": "T123",
        "user_id": "U123",
        "user": "test-bot",
    }

    result = verify_slack_connection(client)

    assert result["authenticated"] is True
    assert result["team"] == "Test Workspace"
    assert result["team_id"] == "T123"
    assert result["bot_user_id"] == "U123"
    assert result["bot_name"] == "test-bot"


def test_failed_authentication():
    client = Mock()

    response = Mock()
    response.get.return_value = "invalid_auth"

    client.auth_test.side_effect = SlackApiError(
        message="Authentication failed",
        response=response,
    )

    with pytest.raises(
        RuntimeError,
        match="Slack authentication failed: invalid_auth"
    ):
        verify_slack_connection(client)
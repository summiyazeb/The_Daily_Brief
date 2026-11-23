import pytest
from unittest.mock import Mock

from slack_sdk.errors import SlackApiError

from ourslack.messages import get_channel_messages


def test_get_channel_messages():
    client = Mock()
    client.conversations_history.return_value = {
        "messages": [
            {"ts": "123.456", "user": "U123", "text": "Task completed"},
            {"ts": "123.457", "user": "U456", "text": "Testing started"},
        ],
        "response_metadata": {"next_cursor": ""},
    }

    result = get_channel_messages(client, "C123")

    assert len(result) == 2
    assert result[0]["text"] == "Task completed"
    assert result[0]["channel_id"] == "C123"
    assert result[0]["ts"] == "123.456"


def test_message_pagination():
    client = Mock()
    client.conversations_history.side_effect = [
        {
            "messages": [{"ts": "123.456", "text": "First message"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
        {
            "messages": [{"ts": "123.457", "text": "Second message"}],
            "response_metadata": {"next_cursor": ""},
        },
    ]

    result = get_channel_messages(client, "C123")

    assert len(result) == 2
    assert client.conversations_history.call_count == 2


def test_date_filtering():
    client = Mock()
    client.conversations_history.return_value = {
        "messages": [],
        "response_metadata": {"next_cursor": ""},
    }

    get_channel_messages(
        client,
        "C123",
        oldest="100.000",
        latest="200.000",
        inclusive=True,
    )

    client.conversations_history.assert_called_once_with(
        channel="C123",
        limit=200,
        cursor=None,
        oldest="100.000",
        latest="200.000",
        inclusive=True,
    )


def test_empty_channel():
    client = Mock()
    client.conversations_history.return_value = {
        "messages": [],
        "response_metadata": {"next_cursor": ""},
    }

    result = get_channel_messages(client, "C123")

    assert result == []


def test_missing_channel_id():
    client = Mock()

    with pytest.raises(ValueError, match="Channel ID is required"):
        get_channel_messages(client, "")

    client.conversations_history.assert_not_called()


def test_invalid_limit():
    client = Mock()

    with pytest.raises(ValueError, match="Limit must be an integer"):
        get_channel_messages(client, "C123", limit=0)

    client.conversations_history.assert_not_called()


def test_slack_api_error():
    client = Mock()

    response = Mock()
    response.get.return_value = "missing_scope"

    client.conversations_history.side_effect = SlackApiError(
        message="Missing scope",
        response=response,
    )

    with pytest.raises(
        RuntimeError,
        match="Failed to retrieve messages from channel C123: missing_scope",
    ):
        get_channel_messages(client, "C123")
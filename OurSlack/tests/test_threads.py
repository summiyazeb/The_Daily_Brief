
import pytest
from unittest.mock import Mock

from slack_sdk.errors import SlackApiError

from ourslack.threads import get_channel_threads


def test_get_channel_threads():
    client = Mock()
    client.conversations_replies.return_value = {
        "messages": [
            {"ts": "123.456", "user": "U123", "text": "Task is blocked"},
            {"ts": "123.457", "user": "U456", "text": "I will fix it"},
        ],
        "response_metadata": {"next_cursor": ""},
    }

    messages = [
        {"ts": "123.456", "reply_count": 1}
    ]

    result = get_channel_threads(client, "C123", messages)

    assert len(result) == 1
    assert result[0]["channel_id"] == "C123"
    assert result[0]["thread_ts"] == "123.456"
    assert len(result[0]["messages"]) == 2
    assert result[0]["messages"][1]["text"] == "I will fix it"


def test_thread_pagination():
    client = Mock()
    client.conversations_replies.side_effect = [
        {
            "messages": [{"ts": "123.456", "text": "Original"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
        {
            "messages": [{"ts": "123.457", "text": "Reply"}],
            "response_metadata": {"next_cursor": ""},
        },
    ]

    messages = [{"ts": "123.456", "reply_count": 1}]

    result = get_channel_threads(client, "C123", messages)

    assert len(result[0]["messages"]) == 2
    assert client.conversations_replies.call_count == 2


def test_duplicate_replies_are_removed():
    client = Mock()
    client.conversations_replies.return_value = {
        "messages": [
            {"ts": "123.456", "text": "Original"},
            {"ts": "123.457", "text": "Reply"},
            {"ts": "123.457", "text": "Reply"},
        ],
        "response_metadata": {"next_cursor": ""},
    }

    messages = [{"ts": "123.456", "reply_count": 2}]

    result = get_channel_threads(client, "C123", messages)

    assert len(result[0]["messages"]) == 2


def test_messages_without_replies_are_skipped():
    client = Mock()
    messages = [
        {"ts": "123.456", "reply_count": 0},
        {"ts": "123.457"},
    ]

    result = get_channel_threads(client, "C123", messages)

    assert result == []
    client.conversations_replies.assert_not_called()


def test_message_without_timestamp_is_skipped():
    client = Mock()
    messages = [{"reply_count": 2}]

    result = get_channel_threads(client, "C123", messages)

    assert result == []
    client.conversations_replies.assert_not_called()


def test_invalid_channel_id():
    client = Mock()

    with pytest.raises(ValueError, match="Channel ID is required"):
        get_channel_threads(client, "", [])

    client.conversations_replies.assert_not_called()


def test_slack_api_error():
    client = Mock()
    response = Mock()
    response.get.return_value = "missing_scope"

    client.conversations_replies.side_effect = SlackApiError(
        message="Missing scope",
        response=response,
    )

    messages = [{"ts": "123.456", "reply_count": 1}]

    with pytest.raises(
        RuntimeError,
        match="Failed to retrieve threads from channel C123: missing_scope",
    ):
        get_channel_threads(client, "C123", messages)


def test_repeated_pagination_cursor():
    client = Mock()
    client.conversations_replies.side_effect = [
        {
            "messages": [{"ts": "123.456", "text": "Original"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
        {
            "messages": [{"ts": "123.457", "text": "Reply"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
    ]

    messages = [{"ts": "123.456", "reply_count": 1}]

    with pytest.raises(
        RuntimeError,
        match="Slack returned a repeated pagination cursor",
    ):
        get_channel_threads(client, "C123", messages)

    assert client.conversations_replies.call_count == 2

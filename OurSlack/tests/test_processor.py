import pytest

from ourslack.processor import process_messages


def test_process_messages():
    messages = [
        {
            "ts": "123.457",
            "channel_id": "C123",
            "user": "U456",
            "text": "Second message",
        },
        {
            "ts": "123.456",
            "channel_id": "C123",
            "user": "U123",
            "text": "First message",
        },
    ]

    result = process_messages(messages)

    assert len(result) == 2
    assert result[0]["text"] == "First message"
    assert result[1]["text"] == "Second message"


def test_user_information():
    messages = [
        {
            "ts": "123.456",
            "channel_id": "C123",
            "user": "U123",
            "text": "Task completed",
        }
    ]

    users = {
        "U123": {"name": "Sarah"}
    }

    result = process_messages(messages, users=users)

    assert result[0]["user_id"] == "U123"
    assert result[0]["user_name"] == "Sarah"


def test_user_name_as_string():
    messages = [
        {
            "ts": "123.456",
            "user": "U123",
            "text": "Hello",
        }
    ]

    result = process_messages(
        messages,
        users={"U123": "Sarah"},
    )

    assert result[0]["user_name"] == "Sarah"


def test_thread_messages_are_combined():
    messages = [
        {
            "ts": "123.456",
            "channel_id": "C123",
            "user": "U123",
            "text": "Task is blocked",
        }
    ]

    threads = [
        {
            "channel_id": "C123",
            "thread_ts": "123.456",
            "messages": [
                {
                    "ts": "123.456",
                    "user": "U123",
                    "text": "Task is blocked",
                },
                {
                    "ts": "123.457",
                    "user": "U456",
                    "text": "I will fix it",
                },
            ],
        }
    ]

    result = process_messages(messages, threads)

    assert len(result) == 2
    assert result[1]["text"] == "I will fix it"
    assert result[1]["is_thread_reply"] is True
    assert result[1]["thread_ts"] == "123.456"


def test_duplicate_messages_are_removed():
    messages = [
        {
            "ts": "123.456",
            "channel_id": "C123",
            "text": "Original",
        },
        {
            "ts": "123.456",
            "channel_id": "C123",
            "text": "Original",
        },
    ]

    result = process_messages(messages)

    assert len(result) == 1


def test_empty_messages():
    result = process_messages([])

    assert result == []


def test_invalid_messages_input():
    with pytest.raises(ValueError, match="Messages must be provided as a list"):
        process_messages("invalid")


def test_invalid_threads_input():
    with pytest.raises(ValueError, match="Threads must be provided as a list"):
        process_messages([], threads="invalid")


def test_messages_without_timestamps_are_skipped():
    messages = [
        {"user": "U123", "text": "No timestamp"},
        {"ts": "123.456", "text": "Valid message"},
    ]

    result = process_messages(messages)

    assert len(result) == 1
    assert result[0]["text"] == "Valid message"
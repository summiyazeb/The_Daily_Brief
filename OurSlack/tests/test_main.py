import json

import pytest
from pydantic import BaseModel

import main


class SampleUpdate(BaseModel):
    summary: str


def test_get_channel_ids_from_string():
    result = main.get_channel_ids(" C01, C02, C03 ")

    assert result == ["C01", "C02", "C03"]


def test_get_channel_ids_removes_duplicates():
    result = main.get_channel_ids(["C01", "C02", "C01"])

    assert result == ["C01", "C02"]


def test_get_channel_ids_from_environment(monkeypatch):
    monkeypatch.setenv("SLACK_CHANNEL_IDS", "C01, C02")

    assert main.get_channel_ids() == ["C01", "C02"]


def test_get_channel_ids_rejects_empty_input():
    with pytest.raises(ValueError):
        main.get_channel_ids([])


def test_get_channel_ids_rejects_invalid_input():
    with pytest.raises(ValueError):
        main.get_channel_ids(123)


def test_get_batch_limit_default(monkeypatch):
    monkeypatch.delenv("SLACK_MAX_BATCH_CHARS", raising=False)

    assert main.get_batch_limit() == 100000


def test_get_batch_limit_from_environment(monkeypatch):
    monkeypatch.setenv("SLACK_MAX_BATCH_CHARS", "50000")

    assert main.get_batch_limit() == 50000


def test_get_batch_limit_rejects_invalid_value():
    with pytest.raises(ValueError):
        main.get_batch_limit(-10)


def test_build_batches_preserves_conversations():
    messages = [
        {
            "ts": "1",
            "channel_id": "C01",
            "conversation_id": "C01:1"
        },
        {
            "ts": "2",
            "channel_id": "C01",
            "conversation_id": "C01:1"
        },
        {
            "ts": "3",
            "channel_id": "C01",
            "conversation_id": "C01:3"
        }
    ]

    first_size = len(json.dumps(messages[:2]))
    second_size = len(json.dumps(messages[2:]))

    batches = main.build_batches(
        messages,
        max(first_size, second_size)
    )

    assert len(batches) == 2
    assert len(batches[0]) == 2
    assert len(batches[1]) == 1

    assert batches[0][0]["conversation_id"] == "C01:1"
    assert batches[0][1]["conversation_id"] == "C01:1"


def test_build_batches_rejects_oversized_conversation():
    messages = [
        {
            "ts": "1",
            "channel_id": "C01",
            "conversation_id": "C01:1",
            "text": "A" * 500
        }
    ]

    with pytest.raises(ValueError):
        main.build_batches(messages, 100)


def test_build_batches_rejects_missing_timestamp():
    messages = [
        {
            "channel_id": "C01",
            "conversation_id": "C01:1"
        }
    ]

    with pytest.raises(ValueError):
        main.build_batches(messages, 1000)


def test_normalize_updates_from_dictionary():
    result = main.normalize_updates({
        "updates": [{"summary": "Task completed"}]
    })

    assert result == [{"summary": "Task completed"}]


def test_normalize_updates_from_pydantic_model():
    result = main.normalize_updates({
        "updates": [SampleUpdate(summary="Task completed")]
    })

    assert result == [{"summary": "Task completed"}]


def test_normalize_updates_rejects_invalid_result():
    with pytest.raises(ValueError):
        main.normalize_updates({"updates": "invalid"})


def test_run_ourslack_with_empty_channel(monkeypatch):
    monkeypatch.setattr(
        main,
        "get_channels",
        lambda client: [{"id": "C01"}]
    )

    monkeypatch.setattr(
        main,
        "select_channels",
        lambda channels, ids: channels
    )

    monkeypatch.setattr(
        main,
        "get_users",
        lambda client: {}
    )

    monkeypatch.setattr(
        main,
        "get_channel_messages",
        lambda *args, **kwargs: []
    )

    monkeypatch.setattr(
        main,
        "get_channel_threads",
        lambda *args, **kwargs: []
    )

    monkeypatch.setattr(
        main,
        "process_messages",
        lambda *args, **kwargs: []
    )

    monkeypatch.setattr(
        main,
        "extract_project_updates",
        lambda *args, **kwargs: pytest.fail(
            "Extraction should not run for an empty channel."
        )
    )

    result = main.run_ourslack(
        channel_ids=["C01"],
        client=object(),
        connection_info={"team_id": "T01"}
    )

    assert result["source"] == "slack"
    assert result["workspace_id"] == "T01"
    assert result["total_updates"] == 0
    assert result["updates"] == []


def test_run_ourslack_extracts_and_formats_updates(monkeypatch):
    messages = [
        {
            "ts": "1",
            "channel_id": "C01",
            "conversation_id": "C01:1",
            "text": "The task is complete."
        }
    ]

    monkeypatch.setattr(
        main,
        "get_channels",
        lambda client: [{"id": "C01"}]
    )

    monkeypatch.setattr(
        main,
        "select_channels",
        lambda channels, ids: channels
    )

    monkeypatch.setattr(
        main,
        "get_users",
        lambda client: {}
    )

    monkeypatch.setattr(
        main,
        "get_channel_messages",
        lambda *args, **kwargs: messages
    )

    monkeypatch.setattr(
        main,
        "get_channel_threads",
        lambda *args, **kwargs: []
    )

    monkeypatch.setattr(
        main,
        "process_messages",
        lambda *args, **kwargs: messages
    )

    monkeypatch.setattr(
        main,
        "extract_project_updates",
        lambda *args, **kwargs: {
            "updates": [
                {"summary": "Task completed"}
            ]
        }
    )

    result = main.run_ourslack(
        channel_ids=["C01"],
        client=object(),
        connection_info={"team_id": "T01"}
    )

    assert result["total_updates"] == 1
    assert result["updates"][0]["summary"] == "Task completed"


def test_run_ourslack_reports_channel_errors(monkeypatch):
    monkeypatch.setattr(
        main,
        "get_channels",
        lambda client: [{"id": "C01"}]
    )

    monkeypatch.setattr(
        main,
        "select_channels",
        lambda channels, ids: channels
    )

    monkeypatch.setattr(
        main,
        "get_users",
        lambda client: {}
    )

    def fail_retrieval(*args, **kwargs):
        raise RuntimeError("Slack retrieval failed.")

    monkeypatch.setattr(
        main,
        "get_channel_messages",
        fail_retrieval
    )

    with pytest.raises(
        RuntimeError,
        match="Failed to process Slack channel C01"
    ):
        main.run_ourslack(
            channel_ids=["C01"],
            client=object(),
            connection_info={"team_id": "T01"}
        )


def test_run_ourslack_saves_output(monkeypatch, tmp_path):
    messages = [
        {
            "ts": "1",
            "channel_id": "C01",
            "conversation_id": "C01:1"
        }
    ]

    monkeypatch.setattr(
        main,
        "get_channels",
        lambda client: [{"id": "C01"}]
    )

    monkeypatch.setattr(
        main,
        "select_channels",
        lambda channels, ids: channels
    )

    monkeypatch.setattr(
        main,
        "get_users",
        lambda client: {}
    )

    monkeypatch.setattr(
        main,
        "get_channel_messages",
        lambda *args, **kwargs: messages
    )

    monkeypatch.setattr(
        main,
        "get_channel_threads",
        lambda *args, **kwargs: []
    )

    monkeypatch.setattr(
        main,
        "process_messages",
        lambda *args, **kwargs: messages
    )

    monkeypatch.setattr(
        main,
        "extract_project_updates",
        lambda *args, **kwargs: {
            "updates": [{"summary": "Completed"}]
        }
    )

    output_file = tmp_path / "slack_output.json"

    main.run_ourslack(
        channel_ids=["C01"],
        client=object(),
        connection_info={"team_id": "T01"},
        output_path=str(output_file)
    )

    assert output_file.exists()

    saved_data = json.loads(
        output_file.read_text(encoding="utf-8")
    )

    assert saved_data["source"] == "slack"
    assert saved_data["total_updates"] == 1
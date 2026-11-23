import pytest
from unittest.mock import Mock

from openai import OpenAIError

from ourslack.extractor import (
    ExtractionResult,
    ProjectUpdate,
    SourceReference,
    extract_project_updates,
)


def test_empty_messages():
    result = extract_project_updates([])

    assert isinstance(result, ExtractionResult)
    assert result.updates == []


def test_invalid_messages_input():
    with pytest.raises(ValueError, match="Messages must be provided as a list"):
        extract_project_updates("invalid")


def test_missing_api_key():
    from unittest.mock import patch

    with patch.dict("os.environ", {}, clear=True):
        with pytest.raises(ValueError, match="OPENAI_API_KEY is missing"):
            extract_project_updates([{"text": "Task completed"}])


def test_successful_extraction():
    expected_result = ExtractionResult(
        updates=[
            ProjectUpdate(
                category="completed_task",
                summary="Authentication completed",
                details="Sarah completed Slack authentication.",
                responsible_people=["Sarah"],
                involved_people=[],
                reason=None,
                source_references=[
                    SourceReference(
                        channel_id="C123",
                        message_ts="123.456",
                        user_id="U123",
                        thread_ts=None,
                    )
                ],
            )
        ]
    )

    client = Mock()
    client.responses.parse.return_value.output_parsed = expected_result

    result = extract_project_updates(
        [{"channel_id": "C123", "ts": "123.456", "text": "Authentication completed"}],
        client=client,
    )

    assert result == expected_result
    assert result.updates[0].category == "completed_task"
    assert result.updates[0].responsible_people == ["Sarah"]

    client.responses.parse.assert_called_once()


def test_extraction_returns_no_result():
    client = Mock()
    client.responses.parse.return_value.output_parsed = None

    with pytest.raises(RuntimeError, match="no structured result"):
        extract_project_updates(
            [{"text": "Some update"}],
            client=client,
        )


def test_openai_api_error():
    client = Mock()
    client.responses.parse.side_effect = OpenAIError(
        "API request failed"
    )

    with pytest.raises(RuntimeError, match="AI extraction failed"):
        extract_project_updates(
            [{"text": "Some update"}],
            client=client,
        )


def test_source_reference_validation():
    reference = SourceReference(
        channel_id="C123",
        message_ts="123.456",
        user_id="U123",
        thread_ts="123.400",
    )

    assert reference.channel_id == "C123"
    assert reference.message_ts == "123.456"
    assert reference.thread_ts == "123.400"


def test_invalid_update_category():
    with pytest.raises(ValueError):
        ProjectUpdate(
            category="invalid_category",
            summary="Test",
            details="Test details",
            responsible_people=[],
            involved_people=[],
            reason=None,
            source_references=[],
        )
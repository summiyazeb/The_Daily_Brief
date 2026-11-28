
import pytest
from unittest.mock import Mock, patch

from groq import APIError

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
    with patch.dict("os.environ", {}, clear=True):
        with pytest.raises(ValueError, match="GROQ_API_KEY is missing"):
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
    client.chat.completions.create.return_value.choices = [
        Mock(
            message=Mock(
                content=expected_result.model_dump_json()
            )
        )
    ]

    result = extract_project_updates(
        [{"channel_id": "C123", "ts": "123.456", "text": "Authentication completed"}],
        client=client,
    )

    assert result == expected_result
    assert result.updates[0].category == "completed_task"
    assert result.updates[0].responsible_people == ["Sarah"]

    client.chat.completions.create.assert_called_once()


def test_extraction_returns_no_result():
    client = Mock()
    client.chat.completions.create.return_value.choices = [
        Mock(message=Mock(content=""))
    ]

    with pytest.raises(RuntimeError, match="empty response"):
        extract_project_updates(
            [{"text": "Some update"}],
            client=client,
        )


def test_groq_api_error():
    client = Mock()
    client.chat.completions.create.side_effect = APIError(
    message="API request failed",
    request=Mock(),
    body=None,
)

    with pytest.raises(RuntimeError, match="Groq extraction failed"):
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

def test_completed_task_and_blocker_are_separate():
    client = Mock()

    client.chat.completions.create.return_value.choices = [
        Mock(
            message=Mock(
                content=ExtractionResult(
                    updates=[
                        ProjectUpdate(
                            category="completed_task",
                            summary="Login page completed.",
                            details="Sarah completed the login page.",
                            responsible_people=["Sarah"],
                            involved_people=["Sarah"],
                            reason=None,
                            source_references=[],
                        ),
                        ProjectUpdate(
                            category="blocker",
                            summary="Authentication bug prevents testing.",
                            details="Testing is blocked by an authentication bug.",
                            responsible_people=[],
                            involved_people=["Sarah"],
                            reason="Authentication bug",
                            source_references=[],
                        ),
                    ]
                ).model_dump_json()
            )
        )
    ]

    result = extract_project_updates(
        [{"text": "Login page completed, but testing is blocked."}],
        client=client,
    )

    assert len(result.updates) == 2

    completed = result.updates[0]
    blocker = result.updates[1]

    assert completed.category == "completed_task"
    assert blocker.category == "blocker"

    assert "authentication bug" not in completed.details.lower()
    assert "testing" not in completed.details.lower()

def test_separate_tasks_from_same_message():
    client = Mock()

    client.chat.completions.create.return_value.choices = [
        Mock(
            message=Mock(
                content=ExtractionResult(
                    updates=[
                        ProjectUpdate(
                            category="completed_task",
                            summary="Dashboard UI completed.",
                            details="Sarah completed the dashboard UI.",
                            responsible_people=["Sarah"],
                            involved_people=["Sarah"],
                            reason=None,
                            source_references=[],
                        ),
                        ProjectUpdate(
                            category="ongoing_task",
                            summary="Backend API integration started.",
                            details="Sarah started integrating the backend API.",
                            responsible_people=["Sarah"],
                            involved_people=["Sarah"],
                            reason=None,
                            source_references=[],
                        ),
                    ]
                ).model_dump_json()
            )
        )
    ]

    result = extract_project_updates(
        [
            {
                "text": (
                    "The dashboard UI is complete. "
                    "I have started integrating the backend API."
                )
            }
        ],
        client=client,
    )

    assert len(result.updates) == 2

    assert result.updates[0].category == "completed_task"
    assert result.updates[1].category == "ongoing_task"

    assert "backend api" not in result.updates[0].details.lower()

def test_reporting_blocker_does_not_assign_responsibility():
    client = Mock()

    client.chat.completions.create.return_value.choices = [
        Mock(
            message=Mock(
                content=ExtractionResult(
                    updates=[
                        ProjectUpdate(
                            category="blocker",
                            summary="Authentication bug blocks testing.",
                            details="Sarah reported an authentication bug.",
                            responsible_people=[],
                            involved_people=["Sarah"],
                            reason="Authentication bug",
                            source_references=[],
                        )
                    ]
                ).model_dump_json()
            )
        )
    ]

    result = extract_project_updates(
        [{"text": "I found an authentication bug that blocks testing."}],
        client=client,
    )

    blocker = result.updates[0]

    assert blocker.category == "blocker"
    assert blocker.responsible_people == []
    assert blocker.involved_people == ["Sarah"]
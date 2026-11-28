import pytest
from unittest.mock import Mock

from slack_sdk.errors import SlackApiError

from ourslack.users import get_users


def test_get_users_success():
    client = Mock()

    client.users_list.return_value = {
        "members": [
            {
                "id": "U123",
                "name": "summiya",
                "real_name": "Summiya Aurangzeb",
                "profile": {
                    "display_name": "Summiya"
                },
                "is_bot": False,
                "deleted": False,
            }
        ],
        "response_metadata": {"next_cursor": ""},
    }

    users = get_users(client)

    assert "U123" in users
    assert users["U123"]["name"] == "Summiya"
    assert users["U123"]["real_name"] == "Summiya Aurangzeb"


def test_get_users_pagination():
    client = Mock()

    client.users_list.side_effect = [
        {
            "members": [{"id": "U1", "name": "Ali"}],
            "response_metadata": {"next_cursor": "cursor_1"},
        },
        {
            "members": [{"id": "U2", "name": "Sara"}],
            "response_metadata": {"next_cursor": ""},
        },
    ]

    users = get_users(client)

    assert len(users) == 2
    assert "U1" in users
    assert "U2" in users
    assert client.users_list.call_count == 2


def test_get_users_empty():
    client = Mock()

    client.users_list.return_value = {
        "members": [],
        "response_metadata": {"next_cursor": ""},
    }

    assert get_users(client) == {}


def test_get_users_skips_missing_ids():
    client = Mock()

    client.users_list.return_value = {
        "members": [
            {"name": "Ali"},
            {"id": "U2", "name": "Sara"},
        ],
        "response_metadata": {"next_cursor": ""},
    }

    users = get_users(client)

    assert len(users) == 1
    assert "U2" in users


def test_get_users_name_fallback():
    client = Mock()

    client.users_list.return_value = {
        "members": [
            {
                "id": "U1",
                "real_name": "Ali Khan",
                "profile": {},
            },
            {
                "id": "U2",
                "name": "sara",
                "profile": {},
            },
        ],
        "response_metadata": {"next_cursor": ""},
    }

    users = get_users(client)

    assert users["U1"]["name"] == "Ali Khan"
    assert users["U2"]["name"] == "sara"


def test_get_users_invalid_limit():
    client = Mock()

    with pytest.raises(ValueError):
        get_users(client, limit=0)


def test_get_users_repeated_cursor():
    client = Mock()

    client.users_list.return_value = {
        "members": [],
        "response_metadata": {"next_cursor": "cursor_1"},
    }

    with pytest.raises(RuntimeError, match="repeated pagination cursor"):
        get_users(client)


def test_get_users_api_error():
    client = Mock()

    client.users_list.side_effect = SlackApiError(
        message="API error",
        response={"error": "missing_scope"},
    )

    with pytest.raises(RuntimeError, match="missing_scope"):
        get_users(client)
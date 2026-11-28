import pytest
from unittest.mock import Mock

from slack_sdk.errors import SlackApiError

from ourslack.channels import get_channels, select_channels


def test_get_public_channels():
    client = Mock()
    client.conversations_list.return_value = {
        "channels": [
            {"id": "C123", "name": "general"},
            {"id": "C456", "name": "development"},
        ],
        "response_metadata": {"next_cursor": ""},
    }

    result = get_channels(client, include_private=False)

    assert len(result) == 2
    assert result[0]["name"] == "general"

    client.conversations_list.assert_called_once_with(
        types="public_channel",
        exclude_archived=True,
        limit=200,
        cursor=None,
    )


def test_channel_pagination():
    client = Mock()

    client.conversations_list.side_effect = [
        {
            "channels": [{"id": "C123", "name": "general"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
        {
            "channels": [{"id": "C456", "name": "development"}],
            "response_metadata": {"next_cursor": ""},
        },
    ]

    result = get_channels(client)

    assert len(result) == 2
    assert client.conversations_list.call_count == 2


def test_empty_channels():
    client = Mock()
    client.conversations_list.return_value = {
        "channels": [],
        "response_metadata": {"next_cursor": ""},
    }

    result = get_channels(client)

    assert result == []


def test_select_channels():
    channels = [
        {"id": "C123", "name": "general"},
        {"id": "C456", "name": "development"},
    ]

    result = select_channels(channels, ["C456"])

    assert len(result) == 1
    assert result[0]["name"] == "development"


def test_invalid_channel_selection():
    channels = [
        {"id": "C123", "name": "general"},
    ]

    with pytest.raises(ValueError, match="not found or inaccessible"):
        select_channels(channels, ["C999"])


def test_slack_api_error():
    client = Mock()

    response = Mock()
    response.get.return_value = "missing_scope"

    client.conversations_list.side_effect = SlackApiError(
        message="Missing scope",
        response=response,
    )

    with pytest.raises(
        RuntimeError,
        match="Failed to retrieve Slack channels: missing_scope",
    ):
        get_channels(client)

def test_repeated_pagination_cursor():
    client = Mock()
    client.conversations_list.side_effect = [
        {
            "channels": [{"id": "C123", "name": "general"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
        {
            "channels": [{"id": "C456", "name": "development"}],
            "response_metadata": {"next_cursor": "next-page"},
        },
    ]

    with pytest.raises(
        RuntimeError,
        match="Slack returned a repeated pagination cursor",
    ):
        get_channels(client)

    assert client.conversations_list.call_count == 2
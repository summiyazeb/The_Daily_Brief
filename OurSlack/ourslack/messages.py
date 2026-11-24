from slack_sdk.errors import SlackApiError


def get_channel_messages(
    client,
    channel_id,
    oldest=None,
    latest=None,
    inclusive=False,
    limit=200,
):
    if not channel_id:
        raise ValueError("Channel ID is required.")

    if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= 1000:
        raise ValueError("Limit must be an integer between 1 and 1000.")

    messages = []
    cursor = None
    seen_cursors = set()

    try:
        while True:
            request_params = {
                "channel": channel_id,
                "limit": limit,
                "cursor": cursor,
            }

            if oldest is not None:
                request_params["oldest"] = oldest

            if latest is not None:
                request_params["latest"] = latest

            if inclusive:
                request_params["inclusive"] = True

            response = client.conversations_history(**request_params)

            channel_messages = response.get("messages", [])

            for message in channel_messages:
                message_data = dict(message)
                message_data["channel_id"] = channel_id
                messages.append(message_data)

            next_cursor = response.get(
                "response_metadata", {}
            ).get("next_cursor")

            if not next_cursor:
                break

            if next_cursor in seen_cursors:
                raise RuntimeError(
                    "Slack returned a repeated pagination cursor."
                )

            seen_cursors.add(next_cursor)
            cursor = next_cursor

        return messages

    except SlackApiError as error:
        error_code = error.response.get("error", "unknown_error")

        raise RuntimeError(
            f"Failed to retrieve messages from channel "
            f"{channel_id}: {error_code}"
        ) from error
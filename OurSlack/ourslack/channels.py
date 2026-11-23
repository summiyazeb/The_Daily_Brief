from slack_sdk.errors import SlackApiError


def get_channels(client, include_private=True):
    channels = []
    cursor = None

    channel_types = ["public_channel"]

    if include_private:
        channel_types.append("private_channel")

    try:
        while True:
            response = client.conversations_list(
                types=",".join(channel_types),
                exclude_archived=True,
                limit=200,
                cursor=cursor,
            )

            channels.extend(response.get("channels", []))

            cursor = response.get("response_metadata", {}).get(
                "next_cursor"
            )

            if not cursor:
                break

        return channels

    except SlackApiError as error:
        error_code = error.response.get("error", "unknown_error")
        raise RuntimeError(
            f"Failed to retrieve Slack channels: {error_code}"
        ) from error


def select_channels(channels, selected_ids):
    channel_map = {
        channel["id"]: channel
        for channel in channels
    }

    invalid_ids = [
        channel_id
        for channel_id in selected_ids
        if channel_id not in channel_map
    ]

    if invalid_ids:
        raise ValueError(
            f"Selected channels not found or inaccessible: {invalid_ids}"
        )

    return [
        channel_map[channel_id]
        for channel_id in selected_ids
    ]
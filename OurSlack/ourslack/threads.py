
from slack_sdk.errors import SlackApiError


def get_channel_threads(client, channel_id, messages, limit=200):
    if not channel_id:
        raise ValueError("Channel ID is required.")

    if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= 1000:
        raise ValueError("Limit must be an integer between 1 and 1000.")

    threads = []

    try:
        for message in messages:
            reply_count = message.get("reply_count", 0)

            if not reply_count:
                continue

            thread_ts = message.get("ts")

            if not thread_ts:
                continue

            thread_messages = []
            cursor = None
            seen_cursors = set()
            seen_timestamps = set()

            while True:
                response = client.conversations_replies(
                    channel=channel_id,
                    ts=thread_ts,
                    limit=limit,
                    cursor=cursor,
                )

                for reply in response.get("messages", []):
                    reply_data = dict(reply)
                    reply_data["channel_id"] = channel_id
                    reply_data["thread_ts"] = thread_ts

                    timestamp = reply_data.get("ts")

                    if timestamp not in seen_timestamps:
                        thread_messages.append(reply_data)
                        seen_timestamps.add(timestamp)

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

            threads.append({
                "channel_id": channel_id,
                "thread_ts": thread_ts,
                "messages": thread_messages,
            })

        return threads

    except SlackApiError as error:
        error_code = error.response.get("error", "unknown_error")

        raise RuntimeError(
            f"Failed to retrieve threads from channel "
            f"{channel_id}: {error_code}"
        ) from error

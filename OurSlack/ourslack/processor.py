def process_messages(messages, threads=None, users=None):
    if not isinstance(messages, list):
        raise ValueError("Messages must be provided as a list.")

    if threads is None:
        threads = []

    if users is None:
        users = {}

    if not isinstance(threads, list):
        raise ValueError("Threads must be provided as a list.")

    processed = {}
    
    def add_message(message, channel_id=None, thread_ts=None):
        if not isinstance(message, dict):
            return

        timestamp = message.get("ts")

        if not timestamp:
            return

        message_data = dict(message)

        if channel_id:
            message_data["channel_id"] = channel_id

        if thread_ts:
            message_data["thread_ts"] = thread_ts

        user_id = message_data.get("user")

        message_data["user_id"] = user_id
        message_data["user_name"] = None

        if user_id and user_id in users:
            user_info = users[user_id]

            if isinstance(user_info, dict):
                message_data["user_name"] = user_info.get("name")
            elif isinstance(user_info, str):
                message_data["user_name"] = user_info

        message_data["is_thread_reply"] = (
            bool(thread_ts) and timestamp != thread_ts
        )

        message_data["conversation_id"] = (
            f"{message_data.get('channel_id', 'unknown')}:"
            f"{thread_ts or timestamp}"
        )

        key = (
            message_data.get("channel_id"),
            timestamp,
        )

        processed[key] = message_data

    for message in messages:
        add_message(message)

    for thread in threads:
        if not isinstance(thread, dict):
            continue

        channel_id = thread.get("channel_id")
        thread_ts = thread.get("thread_ts")

        for message in thread.get("messages", []):
            add_message(message, channel_id, thread_ts)

    result = list(processed.values())

    result.sort(
        key=lambda message: float(message["ts"])
    )

    return result
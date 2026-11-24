from slack_sdk.errors import SlackApiError


def get_users(client, limit=200):
    if not isinstance(limit, int) or isinstance(limit, bool) or not 1 <= limit <= 200:
        raise ValueError("Limit must be an integer between 1 and 200.")

    users = {}
    cursor = None
    seen_cursors = set()

    try:
        while True:
            response = client.users_list(
                limit=limit,
                cursor=cursor,
            )

            for user in response.get("members", []):
                user_id = user.get("id")

                if not user_id:
                    continue

                profile = user.get("profile", {})

                if not isinstance(profile, dict):
                    profile = {}

                display_name = profile.get("display_name")
                real_name = user.get("real_name")
                username = user.get("name")

                users[user_id] = {
                    "name": (
                        display_name
                        or real_name
                        or username
                    ),
                    "display_name": display_name,
                    "real_name": real_name,
                    "username": username,
                    "is_bot": user.get("is_bot", False),
                    "deleted": user.get("deleted", False),
                }

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

        return users

    except SlackApiError as error:
        error_code = error.response.get("error", "unknown_error")

        raise RuntimeError(
            f"Failed to retrieve Slack users: {error_code}"
        ) from error
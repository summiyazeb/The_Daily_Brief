import json
import os

from dotenv import load_dotenv
from pydantic import BaseModel

from ourslack.client import create_client
from ourslack.authentication import verify_slack_connection
from ourslack.channels import get_channels, select_channels
from ourslack.messages import get_channel_messages
from ourslack.threads import get_channel_threads
from ourslack.users import get_users
from ourslack.processor import process_messages
from ourslack.extractor import extract_project_updates
from ourslack.output import format_output, save_output


load_dotenv()


def get_channel_ids(channel_ids=None):
    if channel_ids is None:
        configured_ids = os.getenv("SLACK_CHANNEL_IDS", "")
        selected_ids = configured_ids.split(",")

    elif isinstance(channel_ids, str):
        selected_ids = channel_ids.split(",")

    elif isinstance(channel_ids, (list, tuple)):
        selected_ids = channel_ids

    else:
        raise ValueError(
            "Channel IDs must be provided as a string, list, or tuple."
        )

    cleaned_ids = []

    for channel_id in selected_ids:
        if not isinstance(channel_id, str):
            raise ValueError("Every channel ID must be a string.")

        channel_id = channel_id.strip()

        if channel_id and channel_id not in cleaned_ids:
            cleaned_ids.append(channel_id)

    if not cleaned_ids:
        raise ValueError(
            "No Slack channels selected. Set SLACK_CHANNEL_IDS "
            "or provide channel_ids."
        )

    return cleaned_ids


def get_batch_limit(max_batch_chars=None):
    if max_batch_chars is None:
        configured_limit = os.getenv(
            "SLACK_MAX_BATCH_CHARS",
            "100000"
        )

        try:
            max_batch_chars = int(configured_limit)

        except ValueError as error:
            raise ValueError(
                "SLACK_MAX_BATCH_CHARS must be a positive integer."
            ) from error

    if (
        not isinstance(max_batch_chars, int)
        or isinstance(max_batch_chars, bool)
        or max_batch_chars <= 0
    ):
        raise ValueError(
            "Maximum batch size must be a positive integer."
        )

    return max_batch_chars


def build_batches(messages, max_batch_chars):
    conversations = {}

    for message in messages:
        if not isinstance(message, dict):
            raise ValueError(
                "Every processed message must be a dictionary."
            )

        timestamp = message.get("ts")

        if not timestamp:
            raise ValueError(
                "Every processed message must have a timestamp."
            )

        channel_id = message.get("channel_id", "unknown")

        conversation_id = message.get("conversation_id")

        if not conversation_id:
            conversation_id = (
                f"{channel_id}:"
                f"{message.get('thread_ts') or timestamp}"
            )

        conversations.setdefault(
            conversation_id, []
        ).append(message)

    batches = []
    current_batch = []
    current_size = 0

    for conversation_messages in conversations.values():

        conversation_size = len(
            json.dumps(
                conversation_messages,
                ensure_ascii=False,
                default=str
            )
        )

        if conversation_size > max_batch_chars:
            raise ValueError(
                "A single Slack conversation exceeds the maximum "
                "batch size. Increase SLACK_MAX_BATCH_CHARS "
                "to process it without losing context."
            )

        if not current_batch:
            current_batch = list(conversation_messages)
            current_size = conversation_size
            continue

        combined_size = current_size + conversation_size - 1

        if combined_size > max_batch_chars:
            batches.append(current_batch)

            current_batch = list(conversation_messages)
            current_size = conversation_size

        else:
            current_batch.extend(conversation_messages)
            current_size = combined_size

    if current_batch:
        batches.append(current_batch)

    return batches


def normalize_updates(extraction_result):
    if isinstance(extraction_result, dict):
        updates = extraction_result.get("updates")

    else:
        updates = getattr(
            extraction_result,
            "updates",
            None
        )

    if not isinstance(updates, list):
        raise ValueError(
            "AI extraction must return a list of updates."
        )

    normalized_updates = []

    for update in updates:
        if isinstance(update, BaseModel):
            update = update.model_dump(mode="json")

        elif not isinstance(update, dict):
            raise ValueError(
                "Every extracted update must be a dictionary "
                "or Pydantic model."
            )

        normalized_updates.append(update)

    return normalized_updates


def run_ourslack(
    channel_ids=None,
    oldest=None,
    latest=None,
    output_path=None,
    client=None,
    connection_info=None,
    extraction_client=None,
    max_batch_chars=None
):
    if client is None:
        client, connection_info = create_client()

    elif connection_info is None:
        connection_info = verify_slack_connection(client)

    if not isinstance(connection_info, dict):
        raise ValueError(
            "Valid Slack connection information is required."
        )

    selected_ids = get_channel_ids(channel_ids)

    if oldest is None:
        oldest = os.getenv("SLACK_OLDEST") or None

    if latest is None:
        latest = os.getenv("SLACK_LATEST") or None

    batch_limit = get_batch_limit(max_batch_chars)

    channels = get_channels(client)

    selected_channels = select_channels(
        channels,
        selected_ids
    )

    users = get_users(client)

    all_updates = []

    for channel in selected_channels:
        channel_id = channel["id"]

        try:
            messages = get_channel_messages(
                client,
                channel_id,
                oldest=oldest,
                latest=latest
            )

            threads = get_channel_threads(
                client,
                channel_id,
                messages
            )

            processed_messages = process_messages(
                messages,
                threads,
                users
            )

            if not processed_messages:
                continue

            batches = build_batches(
                processed_messages,
                batch_limit
            )

            for batch in batches:
                extraction_result = extract_project_updates(
                    batch,
                    client=extraction_client
                )

                all_updates.extend(
                    normalize_updates(extraction_result)
                )

        except Exception as error:
            raise RuntimeError(
                f"Failed to process Slack channel "
                f"{channel_id}: {error}"
            ) from error

    output_data = format_output(
        {"updates": all_updates},
        workspace_id=connection_info.get("team_id")
    )

    if output_path is None:
        output_path = os.getenv("SLACK_OUTPUT_PATH")

    if output_path:
        save_output(output_data, output_path)

    return output_data


if __name__ == "__main__":
    result = run_ourslack()

    print(
        "Successfully processed Slack data. "
        f"Extracted {result['total_updates']} updates."
    )
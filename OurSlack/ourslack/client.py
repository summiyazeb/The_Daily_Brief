from ourslack.authentication import (
    get_slack_client,
    verify_slack_connection
)


def create_client():
    client = get_slack_client()
    connection_info = verify_slack_connection(client)

    return client, connection_info

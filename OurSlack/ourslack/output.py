from datetime import datetime, timezone

from pydantic import BaseModel


def format_output(extraction_result, workspace_id=None):
    if isinstance(extraction_result, BaseModel):
        updates = extraction_result.model_dump(mode="json")["updates"]

    elif isinstance(extraction_result, dict):
        updates = extraction_result.get("updates", [])

    else:
        raise ValueError(
            "Extraction result must be a Pydantic model or dictionary."
        )

    if not isinstance(updates, list):
        raise ValueError("Updates must be provided as a list.")

    return {
        "source": "slack",
        "workspace_id": workspace_id,
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "total_updates": len(updates),
        "updates": updates,
    }


def save_output(data, file_path):
    import json

    if not isinstance(data, dict):
        raise ValueError("Output data must be a dictionary.")

    with open(file_path, "w", encoding="utf-8") as file:
        json.dump(
            data,
            file,
            indent=4,
            ensure_ascii=False,
        )
import json
import pytest

from ourslack.output import format_output, save_output
from ourslack.extractor import ExtractionResult


def test_format_output_with_pydantic_model():
    result = ExtractionResult(updates=[])

    output = format_output(result, workspace_id="T123")

    assert output["source"] == "slack"
    assert output["workspace_id"] == "T123"
    assert output["total_updates"] == 0
    assert output["updates"] == []


def test_format_output_with_dictionary():
    result = {
        "updates": [
            {"category": "completed_task", "summary": "Task completed"}
        ]
    }

    output = format_output(result)

    assert output["total_updates"] == 1
    assert output["updates"] == result["updates"]


def test_format_output_includes_generated_timestamp():
    result = {"updates": []}

    output = format_output(result)

    assert "generated_at" in output
    assert output["generated_at"].endswith("+00:00")


def test_format_output_invalid_type():
    with pytest.raises(ValueError):
        format_output("invalid")


def test_format_output_updates_not_list():
    result = {"updates": "invalid"}

    with pytest.raises(ValueError):
        format_output(result)


def test_save_output_creates_json_file(tmp_path):
    data = {"source": "slack", "updates": []}
    file_path = tmp_path / "output.json"

    save_output(data, file_path)

    assert file_path.exists()

    with open(file_path, "r", encoding="utf-8") as file:
        saved_data = json.load(file)

    assert saved_data == data


def test_save_output_invalid_data():
    with pytest.raises(ValueError):
        save_output([], "output.json")
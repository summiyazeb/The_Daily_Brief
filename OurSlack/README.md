# OurSlack

OurSlack is a Python module developed for The Daily Brief project.

It connects to Slack, retrieves messages from selected channels, collects conversation threads, and extracts project-related updates.

## Features

- Slack API authentication
- Channel retrieval and selection
- Message retrieval with pagination
- Thread and reply retrieval
- Message processing and normalisation
- Extraction of tasks, progress, blockers, and delays
- Structured output for integration with The Daily Brief

## Technologies

- Python 3.14
- Slack SDK
- Groq API for AI-powered project update extraction
- Pydantic
- Pytest

## Project Structure

- `ourslack/`: Main Python package
- `tests/`: Automated tests
- `main.py`: Application entry point

## Configuration

Environment variables are managed through a `.env` file.

See `.env.example` for the required configuration variables.
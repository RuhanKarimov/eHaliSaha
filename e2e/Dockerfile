FROM python:3.12-slim

WORKDIR /e2e

# basic tools (optional) for debugging / waiting
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY _helpers.py .

COPY tests ./tests

# default command (jenkins stages override this with specific test file)
CMD ["pytest", "-q"]

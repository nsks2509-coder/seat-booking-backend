#!/usr/bin/env python3
"""Race many independent HTTP booking requests for one held seat."""

import concurrent.futures
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request
import uuid

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080").rstrip("/")
CONTENDERS = int(os.getenv("CONTENDERS", "100"))
ADMIN_USERNAME = os.environ["ADMIN_USERNAME"]
ADMIN_PASSWORD = os.environ["ADMIN_PASSWORD"]
USER_USERNAME = os.getenv("TEST_USERNAME", f"load-{uuid.uuid4().hex[:10]}")
USER_PASSWORD = os.getenv("TEST_PASSWORD", "local-test-password-change-me")


def request(method, path, *, token=None, body=None, headers=None):
    merged = {"Content-Type": "application/json", **(headers or {})}
    if token:
        merged["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE_URL + path, data=data, headers=merged, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            text = response.read().decode()
            return response.status, json.loads(text) if text else None
    except urllib.error.HTTPError as error:
        text = error.read().decode()
        return error.code, json.loads(text) if text else None


def token_for(username, password):
    status, response = request("POST", "/api/auth/token", body={"username": username, "password": password})
    if status != 200:
        raise RuntimeError(f"Login failed ({status}): {response}")
    return response["accessToken"]


def main():
    admin_token = token_for(ADMIN_USERNAME, ADMIN_PASSWORD)
    status, user_response = request("POST", "/api/auth/register", body={"username": USER_USERNAME, "password": USER_PASSWORD})
    if status == 409:
        user_token = token_for(USER_USERNAME, USER_PASSWORD)
    elif status == 201:
        user_token = user_response["accessToken"]
    else:
        raise RuntimeError(f"Test user registration failed ({status}): {user_response}")

    starts_at = (dt.datetime.now(dt.timezone.utc) + dt.timedelta(days=1)).isoformat()
    status, event = request("POST", "/api/admin/events", token=admin_token,
                            body={"name": f"Concurrency demo {uuid.uuid4().hex[:8]}", "startsAt": starts_at, "seatCount": 1})
    if status != 201:
        raise RuntimeError(f"Event setup failed ({status}): {event}")
    event_id = event["id"]
    status, catalog = request("GET", f"/api/events/{event_id}/seats")
    if status != 200 or len(catalog) != 1:
        raise RuntimeError(f"Seat setup failed ({status}): {catalog}")
    seat_id = catalog[0]["id"]
    status, hold = request("POST", f"/api/events/{event_id}/seats/{seat_id}/holds", token=user_token)
    if status != 200:
        raise RuntimeError(f"Seat hold failed ({status}): {hold}")

    def contend(index):
        key = f"load-{index}-{uuid.uuid4().hex}"
        return request("POST", f"/api/events/{event_id}/seats/{seat_id}/bookings?holdToken={hold['holdToken']}",
                       token=user_token, headers={"Idempotency-Key": key})

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONTENDERS) as pool:
        results = list(pool.map(contend, range(CONTENDERS)))

    statuses = [status for status, _ in results]
    winners = statuses.count(201)
    conflicts = statuses.count(409)
    rate_limited = statuses.count(429)
    print(f"contenders={CONTENDERS} winners={winners} conflicts={conflicts} rate_limited={rate_limited}")
    if winners != 1 or conflicts + rate_limited != CONTENDERS - 1:
        for status, body in results:
            if status not in (201, 409, 429):
                print(status, body, file=sys.stderr)
        raise SystemExit("FAIL: expected exactly one confirmed booking for the seat")
    print(f"PASS: exactly one booking won for event={event_id} seat={seat_id}")


if __name__ == "__main__":
    main()

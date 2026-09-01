import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

import pytest


API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8080")


def api_request(method, path, body=None, token=None):
    url = f"{API_BASE_URL}{path}"
    data = None
    headers = {"Content-Type": "application/json"}

    if body is not None:
        data = json.dumps(body).encode("utf-8")

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, data=data, headers=headers, method=method)

    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            response_body = response.read().decode("utf-8")
            return response.status, parse_response(response_body)
    except urllib.error.HTTPError as error:
        response_body = error.read().decode("utf-8")
        return error.code, parse_response(response_body)


def parse_response(response_body):
    if not response_body:
        return {}

    try:
        return json.loads(response_body)
    except json.JSONDecodeError:
        return {"raw": response_body}


def unique_driver(prefix="driver"):
    timestamp_letters = base26_letters(int(time.time()))
    pid_letters = base26_letters(os.getpid())
    run_id = f"{timestamp_letters}_{pid_letters}"
    username = f"{prefix}_{run_id}"
    return {
        "username": username,
        "email": f"{username}@example.com",
        "password": "driverpass123",
        "role": "driver",
    }


def base26_letters(number):
    letters = []

    while number:
        number, remainder = divmod(number, 26)
        letters.append(chr(ord("a") + remainder))

    return "".join(reversed(letters)) or "a"


@pytest.fixture(scope="module", autouse=True)
def backend_is_running():
    try:
        status, _ = api_request(
            "POST",
            "/auth/login",
            {"username": "health_check", "password": "wrong-password"},
        )
    except urllib.error.URLError as error:
        pytest.fail(f"Backend is not reachable at {API_BASE_URL}: {error}")

    assert status in {400, 401, 403, 404}, (
        "Backend should answer auth requests, even when credentials are invalid"
    )


def test_register_and_login_as_driver_allows_dashboard_access():
    """This proves the main success scenario for driver registration and dashboard access."""
    driver = unique_driver()

    register_status, register_body = api_request("POST", "/auth/register", driver)
    assert register_status == 200, register_body

    login_status, login_body = api_request(
        "POST",
        "/auth/login",
        {"username": driver["username"], "password": driver["password"]},
    )
    assert login_status == 200, login_body
    assert login_body["accessToken"]

    profile_status, profile_body = api_request(
        "GET",
        "/api/users/me",
        token=login_body["accessToken"],
    )
    assert profile_status == 200, profile_body
    assert profile_body["username"] == driver["username"]
    assert profile_body["role"] == "ROLE_DRIVER"

    stats_path = "/api/driverStats?" + urllib.parse.urlencode(
        {"username": driver["username"]}
    )
    stats_status, stats_body = api_request("GET", stats_path)
    assert stats_status == 200, stats_body
    assert stats_body["username"] == driver["username"]
    assert "totalDeliveries" in stats_body
    assert "totalEarning" in stats_body


def test_rejects_incomplete_driver_registration_information():
    """This proves extension 3a: incomplete registration information is rejected."""
    driver = unique_driver("incomplete_driver")
    del driver["email"]

    status, body = api_request("POST", "/auth/register", driver)

    assert status >= 400, body


def test_rejects_driver_login_with_invalid_credentials():
    """This proves extension 6a: invalid credentials do not allow login."""
    driver = unique_driver("bad_login_driver")
    register_status, register_body = api_request("POST", "/auth/register", driver)
    assert register_status == 200, register_body

    login_status, login_body = api_request(
        "POST",
        "/auth/login",
        {"username": driver["username"], "password": "wrong-password"},
    )

    assert login_status in {400, 401, 403}, login_body


def test_rejects_duplicate_driver_email_address():
    """This proves extension 5a: a second account cannot reuse an existing email."""
    first_driver = unique_driver("first_email_driver")
    first_status, first_body = api_request("POST", "/auth/register", first_driver)
    assert first_status == 200, first_body

    second_driver = unique_driver("second_email_driver")
    second_driver["email"] = first_driver["email"]
    second_status, second_body = api_request("POST", "/auth/register", second_driver)

    assert second_status == 400, second_body

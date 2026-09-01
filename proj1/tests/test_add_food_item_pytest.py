"""Use Case #8 -- Add a new food item.

Needs the backend running. Overlaps substantially with Use Case #12's
authorization suite (test_inventory_menu_authorization_pytest.py), which
independently found the same FoodController gap from the inventory-management
angle; this file documents it from the "add a food item" use case's own
extension instead. Creates and cleans up its own throwaway food rows.
"""

import pytest

from api_helpers import (
    api_request,
    admin_token,
    register_and_login,
    require_backend,
    unique_name,
)


@pytest.fixture(scope="module", autouse=True)
def backend():
    require_backend()


@pytest.fixture
def cleanup_food():
    """Deletes any food ids appended to this list after the test finishes."""
    created_ids = []
    yield created_ids
    admin = admin_token()
    for food_id in created_ids:
        api_request("DELETE", f"/api/foods/{food_id}", token=admin)


def test_staff_can_add_a_food_item(cleanup_food):
    """This proves the main success scenario: staff can add a food item to the catalog."""
    _, staff = register_and_login("staff", "add_food_staff")
    food = {
        "foodName": unique_name("staff_added_item"),
        "amount": 5,
        "price": 3,
        "allergies": ["NONE"],
    }

    status, body = api_request("POST", "/api/foods", food, token=staff)

    assert status == 200, body
    cleanup_food.append(body["id"])


def test_customer_should_not_be_able_to_add_a_food_item(cleanup_food):
    """This asserts the correct behavior for extension 2a and is expected to FAIL
    today: FoodController has no @PreAuthorize on createFood(), so a plain
    customer can create catalog items exactly like staff can."""
    _, customer = register_and_login("customer", "sneaky_food_customer")
    food = {
        "foodName": unique_name("should_be_blocked_item"),
        "amount": 5,
        "price": 3,
        "allergies": ["NONE"],
    }

    status, body = api_request("POST", "/api/foods", food, token=customer)

    if status == 200:
        cleanup_food.append(body["id"])

    assert status == 403, f"a CUSTOMER account was able to create a food item: {status} {body}"

"""Use Case #6 -- Fulfilled Order.

Needs the backend running and the seeded admin account (admin / admin123).
"""

import pytest

from api_helpers import (
    api_request,
    admin_token,
    register_and_login,
    require_backend,
)


@pytest.fixture(scope="module", autouse=True)
def backend():
    require_backend()


@pytest.fixture(scope="module")
def admin():
    return admin_token()


def get_in_stock_food(token):
    status, foods = api_request("GET", "/api/foods", token=token)
    assert status == 200, foods
    in_stock = [f for f in foods if isinstance(f.get("amount"), int) and f["amount"] > 0]
    assert in_stock, "no in-stock foods available for this test"
    return in_stock[0]


def create_order(token, food, name):
    payload = {
        "name": name,
        "foods": [{"id": food["id"]}],
        "isFulfilled": False,
        "cost": round(food["price"] * 1.3, 2),
        "status": "Placed",
        "deliveryCost": round(food["price"] * 0.3, 2),
    }
    status, body = api_request("POST", "/api/orders", payload, token=token)
    assert status == 200, body
    return body


def test_fulfilling_an_order_updates_status_and_both_order_lists(admin):
    """This proves the main success scenario and both stated postconditions:
    fulfilling an order marks it fulfilled, and it then appears in both
    /api/orders/fulfilledOrders and the owning customer's /my-orders/fulfilled."""
    customer, customer_token = register_and_login("customer", "fulfill_customer")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC6 Pytest Order")

    fulfill_status, fulfill_body = api_request(
        "POST", "/api/orders/fulfillOrder", order, token=admin
    )
    assert fulfill_status == 200, fulfill_body
    assert fulfill_body["isFulfilled"] is True, fulfill_body

    all_fulfilled_status, all_fulfilled = api_request(
        "GET", "/api/orders/fulfilledOrders", token=admin
    )
    assert all_fulfilled_status == 200, all_fulfilled
    assert any(o["id"] == order["id"] for o in all_fulfilled), all_fulfilled

    my_fulfilled_status, my_fulfilled = api_request(
        "GET", "/api/orders/my-orders/fulfilled", token=customer_token
    )
    assert my_fulfilled_status == 200, my_fulfilled
    assert any(o["id"] == order["id"] for o in my_fulfilled), my_fulfilled


def test_fulfilling_a_nonexistent_order_returns_412():
    """This proves extension 2a: an unknown order id is rejected with 412, not 200."""
    _, staff = register_and_login("staff", "fulfill_staff_notfound")

    status, body = api_request(
        "POST", "/api/orders/fulfillOrder", {"id": 999999999}, token=staff
    )

    assert status == 412, body


def test_fulfilling_an_already_fulfilled_order_returns_410(admin):
    """This proves extension 2b: fulfilling an already-fulfilled order is rejected with 410."""
    customer, customer_token = register_and_login("customer", "fulfill_customer_two")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC6 Already Fulfilled Order")

    first_status, first_body = api_request(
        "POST", "/api/orders/fulfillOrder", order, token=admin
    )
    assert first_status == 200, first_body

    second_status, second_body = api_request(
        "POST", "/api/orders/fulfillOrder", order, token=admin
    )
    assert second_status == 410, second_body

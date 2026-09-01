"""Use Case #10 -- Update order status (pick up / deliver).

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


def create_order(token, food, name, delivery_cost=5):
    payload = {
        "name": name,
        "foods": [{"id": food["id"]}],
        "isFulfilled": False,
        "cost": food["price"] + delivery_cost,
        "status": "Placed",
        "deliveryCost": delivery_cost,
    }
    status, body = api_request("POST", "/api/orders", payload, token=token)
    assert status == 200, body
    return body


def update_status(order_id, status, username, token):
    return api_request(
        "POST", f"/api/orders/{order_id}",
        {"status": status, "username": username},
        token=token,
    )


def driver_earnings(username):
    status, body = api_request("GET", f"/api/driverStats?username={username}")
    assert status == 200, body
    return body["totalEarning"]


def test_a_driver_can_pick_up_and_deliver_an_order():
    """This proves the main success scenario: a driver picks up then delivers
    an order, and their earnings are credited exactly once."""
    customer, customer_token = register_and_login("customer", "main_scenario_customer")
    driver, driver_token = register_and_login("driver", "main_scenario_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Main Scenario Order")

    pickup_status, pickup_body = update_status(
        order["id"], "Picked Up", driver["username"], driver_token
    )
    assert pickup_status == 200, pickup_body

    earnings_before = driver_earnings(driver["username"])

    deliver_status, deliver_body = update_status(
        order["id"], "Delivered", driver["username"], driver_token
    )
    assert deliver_status == 200, deliver_body
    assert deliver_body["isFulfilled"] is True, deliver_body

    earnings_after = driver_earnings(driver["username"])
    assert earnings_after == earnings_before + order["deliveryCost"], (
        earnings_before, earnings_after, order["deliveryCost"]
    )


def test_any_driver_can_pick_up_an_order_already_assigned_to_another_driver():
    """This documents extension 2b: no guard exists against reassigning an
    order already picked up by another driver."""
    customer, customer_token = register_and_login("customer", "reassign_customer")
    driver_a, token_a = register_and_login("driver", "reassign_driver_a")
    driver_b, token_b = register_and_login("driver", "reassign_driver_b")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Reassignment Order")

    first_status, _ = update_status(order["id"], "Picked Up", driver_a["username"], token_a)
    assert first_status == 200

    second_status, second_body = update_status(
        order["id"], "Picked Up", driver_b["username"], token_b
    )

    assert second_status == 200, (
        f"expected the reassignment to silently succeed (no guard exists): {second_status} {second_body}"
    )


def test_delivered_by_the_wrong_driver_leaves_the_order_unfulfilled():
    """This documents extension 5a: "Delivered" from a driver who is not the
    order's assigned driver silently leaves the order unfulfilled instead of
    being rejected."""
    customer, customer_token = register_and_login("customer", "wrong_driver_customer")
    assigned_driver, assigned_token = register_and_login("driver", "assigned_driver")
    other_driver, other_token = register_and_login("driver", "other_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Wrong Driver Order")

    pickup_status, _ = update_status(
        order["id"], "Picked Up", assigned_driver["username"], assigned_token
    )
    assert pickup_status == 200

    deliver_status, deliver_body = update_status(
        order["id"], "Delivered", other_driver["username"], other_token
    )

    assert deliver_status == 200, deliver_body
    assert deliver_body["isFulfilled"] is False, (
        "an order delivered by the wrong driver should not be silently marked fulfilled"
    )


def test_delivering_an_order_with_no_assigned_driver_crashes():
    """This documents extension 5b: an unhandled NullPointerException (surfaced
    as an unhandled 500) when "Delivered" arrives for an order that was never
    picked up, so it has no assigned driver."""
    customer, customer_token = register_and_login("customer", "unassigned_customer")
    driver, driver_token = register_and_login("driver", "phantom_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Unassigned Driver Order")

    status, body = update_status(order["id"], "Delivered", driver["username"], driver_token)

    assert status == 500, body


def test_a_missing_status_value_crashes():
    """This documents extension 5c: an unhandled NullPointerException (surfaced
    as an unhandled 500) when the status field is missing entirely. Needs a
    real, existing order -- a nonexistent order id returns early before the
    status field is ever touched."""
    customer, customer_token = register_and_login("customer", "missing_status_customer")
    driver, driver_token = register_and_login("driver", "missing_status_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Missing Status Order")

    status, body = api_request(
        "POST", f"/api/orders/{order['id']}",
        {"username": driver["username"]},
        token=driver_token,
    )

    assert status == 500, body


def test_an_unrecognized_status_value_is_accepted():
    """This documents extension 5d: no validation against an allowed status
    set exists -- an arbitrary string is accepted and persisted as-is."""
    customer, customer_token = register_and_login("customer", "garbage_status_customer")
    driver, driver_token = register_and_login("driver", "garbage_status_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Garbage Status Order")

    status, body = update_status(order["id"], "Bananas", driver["username"], driver_token)

    assert status == 200, body
    assert body["status"] == "Bananas", body


def test_a_second_delivered_call_double_credits_earnings():
    """This documents extension 5e: reprocessing "Delivered" on an
    already-fulfilled order credits the driver's earnings a second time."""
    customer, customer_token = register_and_login("customer", "double_credit_customer")
    driver, driver_token = register_and_login("driver", "double_credit_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Double Credit Order")

    update_status(order["id"], "Picked Up", driver["username"], driver_token)
    update_status(order["id"], "Delivered", driver["username"], driver_token)
    earnings_after_first = driver_earnings(driver["username"])

    second_status, second_body = update_status(
        order["id"], "Delivered", driver["username"], driver_token
    )
    assert second_status == 200, second_body

    earnings_after_second = driver_earnings(driver["username"])

    assert earnings_after_second == earnings_after_first + order["deliveryCost"], (
        f"expected earnings to be credited again (the bug): "
        f"{earnings_after_first} -> {earnings_after_second}, delivery cost {order['deliveryCost']}"
    )


def test_a_second_delivered_call_should_not_double_credit_earnings():
    """This asserts the correct behavior for extension 5e and is expected to
    FAIL today: reprocessing "Delivered" should not credit earnings again."""
    customer, customer_token = register_and_login("customer", "should_not_double_credit_customer")
    driver, driver_token = register_and_login("driver", "should_not_double_credit_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Should Not Double Credit Order")

    update_status(order["id"], "Picked Up", driver["username"], driver_token)
    update_status(order["id"], "Delivered", driver["username"], driver_token)
    earnings_after_first = driver_earnings(driver["username"])

    update_status(order["id"], "Delivered", driver["username"], driver_token)
    earnings_after_second = driver_earnings(driver["username"])

    assert earnings_after_second == earnings_after_first, (
        f"earnings should not change on a reprocessed 'Delivered' call: "
        f"{earnings_after_first} -> {earnings_after_second}"
    )


def test_a_stale_pickup_reverts_an_already_delivered_order():
    """This documents extension 5f: a stale "Pick Up" click on an
    already-delivered order silently reverts it to unfulfilled, while the
    earnings already credited for it are never clawed back."""
    customer, customer_token = register_and_login("customer", "stale_pickup_customer")
    driver, driver_token = register_and_login("driver", "stale_pickup_driver")
    food = get_in_stock_food(customer_token)
    order = create_order(customer_token, food, "UC10 Stale Pickup Order")

    update_status(order["id"], "Picked Up", driver["username"], driver_token)
    update_status(order["id"], "Delivered", driver["username"], driver_token)
    earnings_after_delivery = driver_earnings(driver["username"])

    status, body = update_status(order["id"], "Picked Up", driver["username"], driver_token)

    assert status == 200, body
    assert body["isFulfilled"] is False, (
        "a stale Pick Up should silently revert a delivered order to unfulfilled"
    )

    earnings_after_stale_pickup = driver_earnings(driver["username"])
    assert earnings_after_stale_pickup == earnings_after_delivery, (
        "earnings already credited for the original delivery should not be clawed back"
    )

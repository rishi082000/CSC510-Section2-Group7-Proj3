"""Use Case #9 -- Remove a food item from the menu.

Needs the backend running and the seeded admin account (admin / admin123).
Overlaps with Use Case #12's authorization suite for the missing-role-check
angle; this file adds the historical-order-consistency and dangling-rating
findings that are specific to this use case's own extensions.

Note: against the live, persistent database, DELETE /api/foods/{id} currently
fails with an unhandled 500 for EVERY food that was ever added through the
normal creation path, for any caller, admin included -- FoodServiceImpl
.deleteFood() never removes the food from Inventory.foods before deleting the
row, so MySQL's FK constraint on inventory_foods blocks it. This is the same
root cause UC12's suite already documents for the unauthorized-customer case
(test_a_customer_cannot_delete_a_menu_item), confirmed here to also block the
legitimate admin path. Our JUnit suite demonstrates the deeper order-mutation
and dangling-rating defects (extensions 5a/5b) successfully, because it wipes
the Inventory row entirely before each test, avoiding this blocker -- see
test-suite-evaluation.md Findings 7-8 for the isolated-vs-live distinction
this produces.
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


@pytest.fixture(scope="module")
def admin():
    return admin_token()


def add_food(admin, price=5, amount=5):
    food = {
        "foodName": unique_name("removable_item"),
        "amount": amount,
        "price": price,
        "allergies": [],
    }
    status, body = api_request("POST", "/api/foods", food, token=admin)
    assert status == 200, body
    return body


def place_order(token, food, name):
    payload = {
        "name": name,
        "foods": [{"id": food["id"]}],
        "isFulfilled": False,
        "cost": food["price"],
        "status": "Placed",
        "deliveryCost": 0,
    }
    status, body = api_request("POST", "/api/orders", payload, token=token)
    assert status == 200, body
    return body


def fulfill_order(admin, order):
    status, body = api_request("POST", "/api/orders/fulfillOrder", order, token=admin)
    assert status == 200, body
    return body


def test_customer_should_not_be_able_to_delete_a_food_item(admin):
    """This asserts the correct behavior for extension 1a and is expected to FAIL
    today: FoodController has no @PreAuthorize on deleteFood(), so a customer's
    request is never stopped at 403. Today it actually 500s (see module
    docstring) rather than succeeding outright, but either way the request
    reaches the database, which is the extension 1a claim: nothing stops it
    at the authorization layer."""
    food = add_food(admin)
    _, customer = register_and_login("customer", "sneaky_delete_customer")

    status, body = api_request("DELETE", f"/api/foods/{food['id']}", token=customer)

    assert status == 403, f"a CUSTOMER account's delete request was not rejected at the authorization layer: {status} {body}"


def test_deletion_is_blocked_while_an_unfulfilled_order_contains_the_item(admin):
    """This proves extension 4a: an item in at least one unfulfilled order is
    refused with 409, naming the blocking count. This is the one case in this
    file where deletion is correctly and deliberately blocked, so the
    Inventory FK issue never comes into play -- the request never reaches
    foodRepository.delete() at all."""
    food = add_food(admin)
    _, customer = register_and_login("customer", "blocking_order_customer")
    place_order(customer, food, "Blocking Order")

    status, body = api_request("DELETE", f"/api/foods/{food['id']}", token=admin)

    assert status == 409, body


def test_deleting_a_food_in_a_fulfilled_order_fails_before_reaching_extension_5a(admin):
    """This documents the live-environment reality described in the module
    docstring: even the legitimate admin path to delete a food that is only in
    a fulfilled order fails with an unhandled 500 from the Inventory FK
    constraint, before the order-mutation behavior extension 5a describes can
    ever be observed over HTTP. Our JUnit suite's
    testDeleteFood_removesItemFromFulfilledOrder_priceStaysStale demonstrates
    the actual extension 5a defect (order silently mutated, cost never
    recomputed) in an isolated database where this blocker does not apply."""
    food = add_food(admin, price=7)
    _, customer = register_and_login("customer", "stale_price_customer")
    order = place_order(customer, food, "Fulfilled Order To Be Mutated")
    fulfill_order(admin, order)

    delete_status, delete_body = api_request(
        "DELETE", f"/api/foods/{food['id']}", token=admin
    )

    assert delete_status == 500, (
        f"expected the known Inventory FK-constraint failure; got {delete_status} {delete_body}"
    )


def test_deleting_a_rated_food_also_fails_before_reaching_extension_5b(admin):
    """This documents the same live-environment blocker for extension 5b: even
    a rated food's deletion fails with the Inventory FK-constraint 500 before
    any question of ratedFoodIds cleanup can be observed over HTTP. Our JUnit
    suite's testDeleteFood_ratingRecordStillReferencesDeletedItem demonstrates
    the actual dangling-reference defect in an isolated database."""
    food = add_food(admin, price=4)
    _, customer = register_and_login("customer", "dangling_rating_customer")
    order = place_order(customer, food, "Order To Be Rated Then Orphaned")
    fulfill_order(admin, order)

    rate_status, rate_body = api_request(
        "POST", f"/api/foods/orders/{order['id']}/{food['id']}/rate?rating=4.5", token=customer
    )
    assert rate_status == 200, rate_body

    delete_status, delete_body = api_request(
        "DELETE", f"/api/foods/{food['id']}", token=admin
    )

    assert delete_status == 500, (
        f"expected the known Inventory FK-constraint failure; got {delete_status} {delete_body}"
    )

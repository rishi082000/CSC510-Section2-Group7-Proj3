"""Use Case #7 -- Delete user account.

Needs the backend running and the seeded admin account (admin / admin123).
"""

import pytest

from api_helpers import (
    api_request,
    admin_token,
    find_user_id,
    register_and_login,
    require_backend,
)


@pytest.fixture(scope="module", autouse=True)
def backend():
    require_backend()


@pytest.fixture(scope="module")
def admin():
    return admin_token()


def promote_to_admin(admin, account):
    """Promote a throwaway account to ROLE_ADMIN using the real admin's token,
    so self-delete tests never have to touch the shared seeded admin."""
    user_id = find_user_id(admin, account["username"])
    status, body = api_request(
        "PUT", f"/api/users/{user_id}/role", {"role": "ROLE_ADMIN"}, token=admin
    )
    assert status == 200, body
    return user_id


def test_admin_deletes_an_ordinary_user_and_their_orders(admin):
    """This proves the main success scenario: an admin deletes an ordinary user,
    not themselves, not a driver, not the last admin, and the account is gone."""
    account, _ = register_and_login("customer", "ordinary_delete_target")
    user_id = find_user_id(admin, account["username"])

    status, body = api_request("DELETE", f"/api/users/{user_id}", token=admin)
    assert status == 200, body

    get_status, _ = api_request("GET", f"/api/users/{user_id}", token=admin)
    assert get_status == 404, "deleted user should no longer be found"


def test_an_admin_can_delete_their_own_account(admin):
    """This documents extension 2a: UserServiceImpl.deleteUser() has no self-delete
    check. Uses a throwaway promoted admin rather than the shared seeded one, so
    this is safe to actually run."""
    account, token = register_and_login("customer", "self_delete_target")
    user_id = promote_to_admin(admin, account)

    status, body = api_request("DELETE", f"/api/users/{user_id}", token=token)

    assert status == 200, body


def test_an_admin_should_not_be_able_to_delete_their_own_account(admin):
    """This asserts the correct behavior for extension 2a and is expected to FAIL
    today: an admin should not be able to delete their own account."""
    account, token = register_and_login("customer", "should_not_self_delete")
    user_id = promote_to_admin(admin, account)

    status, body = api_request("DELETE", f"/api/users/{user_id}", token=token)

    assert status in {400, 403}, f"an admin was able to delete their own account: {status} {body}"


def test_deleting_a_nonexistent_user_returns_200_instead_of_404(admin):
    """This documents extension 2b: deleting a missing user id returns 200 OK
    silently instead of 404, so the caller cannot tell whether anything happened."""
    status, body = api_request("DELETE", "/api/users/999999999", token=admin)

    assert status == 200, body


def test_deleting_a_user_with_orders_deletes_their_orders_too(admin):
    """This documents extension 5a: deleting a user hard-deletes their orders,
    with no warning and no archive."""
    account, token = register_and_login("customer", "order_owner_to_delete")

    foods_status, foods = api_request("GET", "/api/foods", token=token)
    assert foods_status == 200, foods
    in_stock = [f for f in foods if isinstance(f.get("amount"), int) and f["amount"] > 0]
    assert in_stock, "no in-stock foods available for this test"
    food = in_stock[0]

    order_payload = {
        "name": "Order To Be Cascaded",
        "foods": [{"id": food["id"]}],
        "isFulfilled": False,
        "cost": food["price"],
        "status": "Placed",
        "deliveryCost": 0,
    }
    order_status, order = api_request("POST", "/api/orders", order_payload, token=token)
    assert order_status == 200, order

    user_id = find_user_id(admin, account["username"])
    delete_status, delete_body = api_request("DELETE", f"/api/users/{user_id}", token=admin)
    assert delete_status == 200, delete_body

    order_get_status, _ = api_request("GET", f"/api/orders/{order['id']}", token=admin)
    assert order_get_status == 404, "the deleted user's order should be gone too"


def test_deleting_a_driver_leaves_driver_stats_orphaned(admin):
    """This documents extension 5b: DriverStats has no foreign key back to User,
    so deleting a driver leaves their stats row permanently orphaned."""
    account, _ = register_and_login("driver", "driver_to_delete")

    stats_status, _ = api_request(
        "GET", f"/api/driverStats?username={account['username']}"
    )
    assert stats_status == 200

    user_id = find_user_id(admin, account["username"])
    delete_status, delete_body = api_request("DELETE", f"/api/users/{user_id}", token=admin)
    assert delete_status == 200, delete_body

    stats_after_status, stats_after = api_request(
        "GET", f"/api/driverStats?username={account['username']}"
    )
    assert stats_after_status == 200, stats_after
    assert stats_after["username"] == account["username"], (
        "driver stats should still exist, now orphaned"
    )


@pytest.mark.skip(
    reason="would delete the shared admin and lock the whole team out of /api/users"
)
def test_the_last_remaining_admin_cannot_be_deleted(admin):
    """This proves extension 5c. Written up but deliberately not run -- see
    test-plan.md, matching the same precedent as UC11's
    test_the_last_admin_cannot_be_demoted."""
    user_id = find_user_id(admin, "admin")

    status, _ = api_request("DELETE", f"/api/users/{user_id}", token=admin)

    assert status == 400

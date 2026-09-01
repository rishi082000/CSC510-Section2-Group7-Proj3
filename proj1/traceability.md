# Project 1 Traceability

| Use Case | Scenario or Extension | Test |
| --- | --- | --- |
| Use Case #1 | Main success scenario: register and login as driver, then access dashboard data | `test_register_and_login_as_driver_allows_dashboard_access` |
| Use Case #1 | Extension 3a: incomplete or invalid registration information | `test_rejects_incomplete_driver_registration_information` |
| Use Case #1 | Extension 5a: email account already being used | `test_rejects_duplicate_driver_email_address` |
| Use Case #1 | Extension 7a: invalid login credentials | `test_rejects_driver_login_with_invalid_credentials` |
| Use Case #2 | Main success scenario: prevent duplicate email registration and preserve original account | `test_prevents_duplicate_email_registration_and_preserves_original_account` |
| Use Case #2 | Extension 2a: username already being used with a different account | `test_rejects_duplicate_username_registration` |
| Use Case #2 | Extension 4a: password format/length validation failure | `test_rejects_registration_with_invalid_password` |
| Use Case #2 | Extension 5a: user does not select a role or missing fields | `test_rejects_incomplete_registration_missing_role` |
| Use Case #2 | Cross-role duplicate email prevention (customer email used for driver) | `test_prevents_cross_role_duplicate_email_registration` |
| Use Case #3 | Extension 2a: username already taken, rejected with a username-specific message | `test_rejects_duplicate_username_with_specific_message` |
| Use Case #3 | Extension 2b: email already tied to another account | `test_rejects_duplicate_email_registration_with_client_error` |
| Use Case #3 | Extension 2c: email entered in an invalid format | `test_rejects_invalid_email_format_with_specific_message` |
| Use Case #3 | Extension 2d: password entered in an invalid format, rejected with a password-specific message | `test_rejects_invalid_password_format_with_specific_message` |
| Use Case #3 | Extension 2f: user does not select a role | `test_rejects_missing_role_with_client_error` |
| Use Case #3 | Postconditions 1 & 2: rejected registration does not create an account | `test_invalid_registration_does_not_create_account` |
| Use Case #3 | Main success scenario step 7 & Postcondition 4: user corrects the offending field and resubmits successfully | `test_user_can_correct_and_resubmit_after_registration_error` |
| Use Case #4 | Main success scenario steps 1-4 & Postconditions 1-2: valid login is authenticated and issues a session token | `test_login_authenticates_valid_credentials_and_returns_session_token` |
| Use Case #4 | Extension 2a: invalid username or password | `test_rejects_login_with_invalid_credentials` |
| Use Case #4 | Main success scenario step 5 & Extension 6d: role identified as ROLE_CUSTOMER for redirection | `test_login_identifies_role_for_customer_redirection` |
| Use Case #4 | Extension 6c: role identified as ROLE_DRIVER for redirection | `test_login_identifies_role_for_driver_redirection` |
| Use Case #4 | Extension 6b: role identified as ROLE_STAFF for redirection | `test_login_identifies_role_for_staff_redirection` |
| Use Case #4 | Postcondition 4: user can access an endpoint appropriate to their role | `test_customer_can_access_role_appropriate_endpoint` |
| Use Case #4 | Postcondition 5 & Extension 6e: customer is blocked from an admin-only endpoint | `test_customer_is_blocked_from_admin_only_endpoint` |
| Use Case #4 | Postcondition 5 & Extension 6e: driver is blocked from a customer-only endpoint | `test_driver_is_blocked_from_customer_only_endpoint` |
| Use Case #4 | Preconditions: unauthenticated request rejected from a protected endpoint | `test_unauthenticated_request_is_rejected_from_protected_endpoint` |
| Use Case #4 | Extension 5a: user account not associated with a valid role | `test_login_fails_for_account_with_no_valid_role` |
| Use Case #5 | Extension 2a & 4a: unauthenticated request rejected from an admin-only endpoint | `test_unauthenticated_request_rejected_from_admin_only_endpoint` |
| Use Case #5 | Extension 2a: unauthenticated request rejected from the current-user profile endpoint | `test_own_profile_endpoint_rejects_unauthenticated_access` |
| Use Case #5 | Extension 4a: unauthenticated request rejected from a role-restricted order endpoint | `test_unauthenticated_request_rejected_from_role_restricted_order_endpoint` |
| Use Case #5 | Extension 3b & main success scenario steps 2-4: customer blocked from admin-only endpoint | `test_customer_blocked_from_admin_only_endpoint` |
| Use Case #5 | Extension 3a & main success scenario steps 2-4: customer blocked from driver/admin-only order endpoint | `test_customer_blocked_from_driver_or_admin_order_endpoint` |
| Use Case #5 | Precondition 2 & Extension 4a: driver dashboard stats endpoint should reject unauthenticated requests | `test_driver_stats_endpoint_allows_unauthenticated_access` |
| Use Case #5 | Extension 3a: customer should be blocked from viewing driver dashboard stats | `test_customer_blocked_from_viewing_driver_dashboard_stats` |
| Use Case #6 | Main success scenario + Postconditions: fulfill an order, confirm it appears in `/api/orders/fulfilledOrders` and the owning customer's `/my-orders/fulfilled` | `test_fulfilling_an_order_updates_status_and_both_order_lists` |
| Use Case #6 | Extension 2a: order id does not exist, rejected with 412 | `test_fulfilling_a_nonexistent_order_returns_412` |
| Use Case #6 | Extension 2b: order is already fulfilled, rejected with 410 | `test_fulfilling_an_already_fulfilled_order_returns_410` |
| Use Case #7 | Main success scenario: admin deletes an ordinary user and their orders, admin count unaffected | `test_admin_deletes_an_ordinary_user_and_their_orders` |
| Use Case #7 | Extension 2a: admin deletes their own account (documents current behavior — passes) | `test_an_admin_can_delete_their_own_account` |
| Use Case #7 | Extension 2a: admin account should survive a self-delete attempt (asserts correct behavior — currently FAILS) | `test_an_admin_should_not_be_able_to_delete_their_own_account` |
| Use Case #7 | Extension 2b: deleting a non-existent user | `test_deleting_a_nonexistent_user_returns_200_instead_of_404` |
| Use Case #7 | Extension 5a: deleting a user with placed orders | `test_deleting_a_user_with_orders_deletes_their_orders_too` |
| Use Case #7 | Extension 5b: deleting a driver account | `test_deleting_a_driver_leaves_driver_stats_orphaned` |
| Use Case #7 | Extension 5c: deleting the last remaining admin (written, not run — see finding below) | `test_the_last_remaining_admin_cannot_be_deleted` |
| Use Case #8 | Main success scenario: staff adds a food item | `test_staff_can_add_a_food_item` |
| Use Case #8 | Extension 2a: food should not actually be created by a non-Admin/Staff account (asserts correct behavior — currently FAILS) | `test_customer_should_not_be_able_to_add_a_food_item` |
| Use Case #9 | Extension 4a: item appears in at least one unfulfilled order | `test_deletion_is_blocked_while_an_unfulfilled_order_contains_the_item` |
| Use Case #9 | Extension 1a: customer's delete request should be rejected at the authorization layer (asserts correct behavior — currently FAILS, and fails worse than expected — see finding below) | `test_customer_should_not_be_able_to_delete_a_food_item` |
| Use Case #9 | Extension 5a: deleting a food in a fulfilled order (live environment cannot reach this state — see finding below) | `test_deleting_a_food_in_a_fulfilled_order_fails_before_reaching_extension_5a` |
| Use Case #9 | Extension 5b: deleting a rated food (live environment cannot reach this state — see finding below) | `test_deleting_a_rated_food_also_fails_before_reaching_extension_5b` |
| Use Case #10 | Main success scenario: a driver picks up then delivers an order, earnings credited exactly once | `test_a_driver_can_pick_up_and_deliver_an_order` |
| Use Case #10 | Extension 2b: order already assigned to another driver, or already "Picked Up" | `test_any_driver_can_pick_up_an_order_already_assigned_to_another_driver` |
| Use Case #10 | Extension 5a: "Delivered" submitted by a driver who is NOT the order's assigned driver | `test_delivered_by_the_wrong_driver_leaves_the_order_unfulfilled` |
| Use Case #10 | Extension 5b: order's driver is still unassigned when "Delivered" arrives | `test_delivering_an_order_with_no_assigned_driver_crashes` |
| Use Case #10 | Extension 5c: status value is missing | `test_a_missing_status_value_crashes` |
| Use Case #10 | Extension 5d: status value is an unrecognized string | `test_an_unrecognized_status_value_is_accepted` |
| Use Case #10 | Extension 5e: already-delivered order receives a second "Delivered" call (documents current behavior — passes) | `test_a_second_delivered_call_double_credits_earnings` |
| Use Case #10 | Extension 5e: earnings should not be credited again on a second "Delivered" call (asserts correct behavior — currently FAILS) | `test_a_second_delivered_call_should_not_double_credit_earnings` |
| Use Case #10 | Extension 5f: stale "Pick Up" click lands on an already-delivered order | `test_a_stale_pickup_reverts_an_already_delivered_order` |
| Use Case #11 | Main success scenario: admin promotes a customer to staff | `test_admin_can_promote_a_customer_to_staff` |
| Use Case #11 | Extension 2a: a non-admin cannot list users | `test_a_customer_cannot_list_every_user` |
| Use Case #11 | Extension 5a: role change on an unknown user id | `test_changing_the_role_of_a_user_that_does_not_exist_is_not_found` |
| Use Case #11 | Extension 5b: an invented role is rejected | `test_rejects_a_role_that_is_not_a_real_role` |
| Use Case #11 | Extension 5b: an empty role is rejected | `test_rejects_an_empty_role` |
| Use Case #11 | Extension 4a: registering with an unrecognized role is rejected | `test_registering_with_an_unknown_role_is_rejected` |
| Use Case #11 | Extension 5c: the last admin cannot be demoted (written, not run) | `test_the_last_admin_cannot_be_demoted` |
| Use Case #12 | Main success scenario: staff reads the inventory | `test_staff_can_read_the_inventory` |
| Use Case #12 | Extension 2a: the inventory requires a login | `test_the_inventory_cannot_be_read_without_logging_in` |
| Use Case #12 | Extension 4a: a customer cannot rewrite the inventory | `test_a_customer_cannot_rewrite_the_inventory` |
| Use Case #12 | Extension 4b: a driver cannot add a menu item | `test_a_driver_cannot_add_a_menu_item` |
| Use Case #12 | Extension 4b: a customer cannot delete a menu item | `test_a_customer_cannot_delete_a_menu_item` |
| Use Case #12 | Extension 4c: a negative price is rejected | `test_rejects_a_negative_price` |
| Use Case #12 | Extension 4e: a duplicate food name is rejected | `test_rejects_a_duplicate_food_name` |
| Use Case #13 | Main success scenario: a driver reads their own statistics | `test_a_driver_can_read_their_own_statistics` |
| Use Case #13 | Extension 2a: statistics require a login | `test_driver_statistics_require_a_login` |
| Use Case #13 | Extension 2b: one driver cannot read another's statistics | `test_one_driver_cannot_read_another_drivers_statistics` |
| Use Case #13 | Extension 2b: a customer cannot read driver statistics | `test_a_customer_cannot_read_driver_statistics` |
| Use Case #13 | Extension 2c: an unknown driver username is not found | `test_an_unknown_username_is_not_found` |
| Use Case #14 | Precondition: a customer saves budget and dietary preferences | `test_a_customer_can_save_a_budget_and_a_restriction` |
| Use Case #14 | Extension 4a (exact match): a matching allergen hides the food | `test_an_exactly_worded_restriction_hides_the_food` |
| Use Case #14 | Extension 4a (plural): "peanuts" does not match "peanut" | `test_a_plural_restriction_still_hides_the_food` |
| Use Case #14 | Extension 4a (category): "nuts" does not match "tree nuts" | `test_a_broader_restriction_still_hides_the_food` |
| Use Case #14 | Extension 3b: the premium tier hides food above $35 | `test_premium_customers_can_see_the_whole_menu` |
| Use Case #14 | Extension 5a: the food API applies no preference filtering | `test_the_food_api_applies_no_preferences_of_its_own` |
| Use Case #15 | Authentication is required to use the assistant | `test_the_assistant_cannot_be_used_without_logging_in` |
| Use Case #15 | Extension 1a: a driver cannot use the assistant | `test_a_driver_cannot_use_the_assistant` |
| Use Case #15 | Main success scenario: the assistant answers a greeting (requires Ollama running) | `test_the_assistant_answers_a_greeting` |
| Use Case #15 | Extension 4a: an unreachable model is reported as a failure | `test_an_unreachable_model_is_reported_as_a_failure` |
| Use Case #16 | Main success scenario: authenticated customer updates food preferences | `test_customer_can_update_food_preferences` |
| Use Case #16 | Success postcondition: updated preferences persist | `test_updated_food_preferences_are_returned_in_customer_profile` |
| Use Case #16 | Extension 3a: unsupported cost preference is rejected | `test_rejects_unsupported_cost_preference` |
| Use Case #16 | Extension 3b: customer selects no dietary restrictions | `test_customer_can_save_preferences_without_dietary_restrictions` |
| Use Case #17 | Main success scenario: authenticated customer retrieves food inventory | `test_authenticated_customer_can_browse_food_inventory` |
| Use Case #17 | Displayed information: foods contain inventory-page information | `test_inventory_foods_contain_displayed_information` |
| Use Case #17 | Extension 1a: unauthenticated inventory request is rejected | `test_rejects_unauthenticated_food_inventory_request` |
| Use Case #17 | Success postcondition: browsing does not change inventory | `test_browsing_food_inventory_does_not_change_inventory` |
| Use Case #18 | Main success scenario: authenticated customer creates a food order | `test_authenticated_customer_can_create_food_order` |
| Use Case #18 | Success postcondition: created order appears in personal orders | `test_created_order_appears_in_customer_orders` |
| Use Case #18 | Extension 7a: backend rejects an order without food | `test_rejects_order_without_food` |
| Use Case #18 | Extension 4a: backend rejects quantity above available stock | `test_rejects_order_quantity_above_available_stock` |
| Use Case #19 | Main success scenario: customer retrieves personal orders | `test_customer_can_view_personal_orders` |
| Use Case #19 | Extension 2b: another customer's order is excluded | `test_personal_orders_do_not_include_another_customers_order` |
| Use Case #19 | Extension 2a: new customer receives an empty order list | `test_new_customer_receives_empty_personal_order_list` |
| Use Case #19 | Extension 3a: repeated food entries are preserved | `test_personal_order_details_preserve_repeated_food_entries` |
| Use Case #20 | Main success scenario: customer rates food from a fulfilled order | `test_customer_can_rate_food_from_fulfilled_order` |
| Use Case #20 | Extension 6a: duplicate rating is rejected | `test_rejects_duplicate_rating_for_same_food_and_order` |
| Use Case #20 | Extension 2a: food from an unfulfilled order cannot be rated | `test_rejects_rating_food_from_unfulfilled_order` |
| Use Case #20 | Extension 6b: another customer cannot rate the order | `test_rejects_rating_another_customers_order` |

Current findings:
- Use Case #1 Extension 5a & Use Case #2 Main Success Scenario / Cross-role tests, plus Use Case #3 Extension 2b, expose a backend defect. The `users.email` column is unique, but registration does not check `existsByEmail` before saving, so duplicate email registration returns HTTP 500 instead of a user-facing validation error (matching Use Case #2 Extension 7a).
- Use Case #3 Extension 2c exposes a backend defect. The email-format validation branch in `AuthServiceImpl.register` logs and returns the wrong string literal ("Username must be between 3-50 characters" instead of an email-specific message), so the frontend cannot tell the user their email was the problem.
- Use Case #3 Extension 2f exposes a backend defect. `AuthServiceImpl.register` has no explicit check for a missing role; when `role` is absent, `setCorrectRoles` calls `.toLowerCase()` on a null value, throwing a `NullPointerException` that `GlobalExceptionHandler`'s generic handler reports as HTTP 500 with a raw Java error message instead of a "select a role" validation error.
- Use Case #4 Extension 5a exposes a backend defect, the sibling of the Use Case #3 Extension 2f finding above. When `role` is present but not one of `driver`/`customer`/`staff` (e.g. `"not-a-real-role"`), `setCorrectRoles` returns an empty string and `AuthServiceImpl.register` stores the account anyway instead of rejecting it. That empty role then breaks Spring Security's authority parsing at login time, so the account can never log in: the backend returns HTTP 401 with the technical message "A granted authority textual representation is required" instead of a message telling the user a role must be selected, permanently locking the account out.
- Use Case #5 Precondition 2 & Extension 3a/4a expose a significant authorization gap. `DriverStatsController.getDriverStats` has no `@PreAuthorize` annotation, and `SpringSecurityConfig` explicitly `permitAll()`s `GET /api/driverStats/**`. As a result, any driver's earnings, delivery count, and rating can be read by supplying their username, with no login required and with no role check once logged in, unlike every other dashboard-data endpoint in the system (`/api/users`, `/api/orders/my-orders`, `/api/orders/availableOrders`), which correctly reject unauthenticated or wrong-role requests.
- Use Case #7, Extension 2a: `UserServiceImpl.deleteUser()` (lines 55-71) has no self-delete check — the guard exists only in `UserManagement.js`. An admin can delete their own account via a direct API call.
- Use Case #7, Extension 2b: deleting a non-existent user ID returns 200 OK instead of 404 — `deleteUser()` (lines 59-61) returns silently when the user isn't found.
- Use Case #7, Extension 5b: `DriverStats` has no foreign key back to `User`; deleting a driver's account leaves their stats row permanently orphaned.
- Use Case #7, Extension 5c: no check anywhere prevents deleting the last remaining admin, leaving the platform with zero admins and no in-app recovery path.
- Use Cases #8/#9, Extensions 2a/1a: `FoodController` has no `@PreAuthorize` on `createFood()` or `deleteFood()`, and neither endpoint is restricted in `SpringSecurityConfig` — any authenticated account, regardless of role, can create or delete catalog items.
- Use Case #9, Extension 5a: `FoodServiceImpl.deleteFood()` (lines 166-172) silently strips a deleted food from every fulfilled order's item list but never recomputes `Order.cost`/`deliveryCost` — price and item list become permanently inconsistent. Confirmed via the JUnit suite in an isolated database (`testDeleteFood_removesItemFromFulfilledOrder_priceStaysStale`).
- Use Case #9, Extension 5b: `deleteFood()` never touches `Order.ratedFoodIds` — a rating recorded against a food survives as a dangling reference after the food is deleted. Confirmed via the JUnit suite in an isolated database (`testDeleteFood_ratingRecordStillReferencesDeletedItem`).
- Use Case #9, new finding from the pytest suite: against the live, persistent database, `DELETE /api/foods/{id}` fails with an unhandled 500 for *any* food that was ever added through the normal creation path, for *any* caller, admin included — `deleteFood()` never removes the food from `Inventory.foods` before deleting the row, so MySQL's FK constraint on `inventory_foods` blocks it every time. This is more severe than the authorization gap alone: even the legitimate admin path is broken. It also means the live pytest suite cannot reach the states extensions 5a/5b describe (deletion never succeeds), while the JUnit suite can, because `@BeforeEach` wipes the Inventory row entirely each test. Independently corroborated against Use Case #12's own `test_a_customer_cannot_delete_a_menu_item`, whose docstring predicts this exact failure.
- Use Case #10, Extensions 5b/5c: `OrderServiceImpl.updateOrder()` (lines 274, 278) has no null-checks on `status` or `order.getDriver()`, causing unhandled `NullPointerException`s that surface as 500 errors.
- Use Case #10, Extension 5e: `DriverStatsImpl.updateTotalEarnings()` (lines 46-47) is called unconditionally on every "Delivered" transition with no check on `order.getIsFulfilled()` — reprocessing an already-delivered order double-credits both earnings and delivery count.
- Use Case #10, Extension 5f: a stale "Pick Up" call on an already-delivered order silently reverts `isFulfilled` to false (lines 281-283), while previously-credited earnings are never clawed back — leaving no way to reconcile the mismatch through the app.
- Use Case #11, Extension 5b: `UserServiceImpl.updateUserRole()` (lines 47-54) calls `user.setRole(role)` with no validation against any role registry. `ROLE_WIZARD` and an empty string both return 200 and persist. The account can still log in but is authorized for nothing, and no screen shows the role is invalid.
- Use Case #11, Extension 5c: no last-admin guard exists anywhere. Because only an admin can change roles, demoting the last remaining admin permanently removes all administrative access with no in-app recovery path.
- Use Case #11, Extension 5d: a role change does not revoke outstanding JWTs, so a demoted user keeps their old authority until the token expires.
- Use Case #11, Extension 4a: two divergent role registries exist, `FoodSeer.config.Roles` (CUSTOMER, STAFF) and `FoodSeer.constant.Roles` (CUSTOMER, STAFF, DRIVER), and neither is referenced by the update path. `config/Roles.java` is still commented "Defines user roles for WolfCafe," left over from the assignment this was forked from.
- Use Case #12, Extension 2a: `InventoryController` (line 51) guards its write endpoint with `@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")`, so any customer can rewrite stock levels. Same class of gap as Use Cases #8/#9, reached through a different endpoint.
- Use Case #12, Extension 4a: deleting a food still referenced by the Inventory trips a MySQL foreign-key constraint. `FoodController.deleteFood()` (lines 87-96) catches only `IllegalStateException`, so the raw constraint message returns as an unhandled HTTP 500, leaking internal SQL and schema to any caller.
- Use Case #13, Precondition 2 & Extension 2a: independently reproduces the Use Case #5 finding above. `DriverStatsController.getDriverStats` (line 23) imports `@PreAuthorize` and never applies it, and `SpringSecurityConfig` (line 64) explicitly `permitAll()`s `GET /api/driverStats/**`.
- Use Case #13, Extension 3a: `getDriverStats` takes `@RequestParam String username` (line 24) and never checks it against the caller, so any driver can read any other driver's earnings, delivery count, and rating.
- Use Case #14, Extension 3a: `Recommendations.js` (lines 68-69) matches allergens by exact string equality (`allergy === restriction`). "peanuts" does not match "peanut" and "nuts" does not match "tree nuts," so a customer is shown food they explicitly said they cannot eat. Safety critical.
- Use Case #14, Extension 2a: the "premium" cost tier caps at `food.price <= 35` (line 48) rather than being unbounded, silently hiding the most expensive items from the customers most willing to pay for them.
- Use Case #14, Postconditions: all preference filtering is client-side. `GET /api/foods` returns the full catalog regardless of the caller's dietary restrictions or budget.
- Use Case #15, Extension 2a: `ChatServiceImpl` wraps the entire Ollama call in `catch (Exception)` and returns HTTP 200 with `"Error: " + e.getMessage()` (lines 107-108) in the same field a real answer uses. The client cannot distinguish failure from success, and the user is shown raw Java exception text as though the assistant had said it.
- Use Case #15, Precondition 3: `OLLAMA_URL` (line 28) and `MODEL` (line 29) are hardcoded constants rather than configuration, so the assistant cannot be pointed at another host or model without a rebuild.
- Use Case #15, Extension 4a: the user's stored dietary restrictions are never included in the prompt sent to the model, so the assistant can recommend food the customer cannot eat.
- Use Case #16: The backend accepts and persists unsupported cost-preference values.
- Use Case #17: All tested inventory retrieval and access-control behavior passes.
- Use Case #18: The backend accepts empty orders and orders exceeding available stock.
- Use Case #19: All tested personal-order retrieval and account-isolation behavior passes.
- Use Case #20: The backend allows an authenticated customer to rate another customer's fulfilled order.

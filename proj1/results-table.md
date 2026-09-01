# Project 1 Results Table

Format: `Test | Why we tried it | Expected | What happened`. All 29 tests below
were designed for Use Cases #6-#10 and actually executed with `mvn test`
against the real FoodSeer backend (MySQL, no mocked HTTP layer for the
controller tests). Four are deliberately red — they assert the *correct*
behavior for a confirmed bug and are expected to fail against the current
code; that failure is itself the evidence.

## Use Case #6 — Fulfilled Order

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `testFulfillOrder_UseCase6_MainScenarioAndPostconditions` | No existing test verified the use case's stated Postconditions (order appears in `/api/orders/fulfilledOrders` and the owning customer's `/my-orders/fulfilled`), only that the fulfill call returns 200 | All three postconditions hold | PASS |

## Order creation (`OrderServiceImpl.createOrder`, precondition to Use Case #6)

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `test_createsOrder_whenAllFoodsExistAndUserIsAuthenticated` | Happy-path baseline before testing edge cases | Order saved, `isFulfilled` forced false regardless of request | PASS |
| `test_rejectsOrder_whenFoodIdDoesNotExist` | Code has an explicit `ResourceNotFoundException` check for missing food IDs | Exception thrown before any save | PASS |
| `test_createsOrder_withEmptyFoodsList` | No visible guard requiring at least one food item | Order saved with zero items, no rejection | PASS — confirms no guard exists |
| `test_createsOrder_withNegativeCost` | No visible validation on the `cost` field | Negative cost persisted as-is | PASS — confirms no validation |
| `test_createsOrder_withInvalidStatusValue` | No visible validation against an allowed status set | Arbitrary string persisted as-is | PASS — confirms no validation |
| `test_rejectsOrder_whenNoAuthenticatedUser` | Code has an explicit null-check on the current user | `IllegalStateException` thrown before save | PASS |

## Use Case #7 — Delete user account

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `shouldDeleteOrdinaryUserAndTheirOrders_mainSuccessScenario` | No test verified the ordinary case end-to-end, distinct from the last-admin edge case | User and their orders gone, admin count unchanged | PASS |
| `shouldAllowAdminToDeleteOwnAccount_becauseBackendHasNoSelfDeleteCheck` | UC7 extension 2a claims the self-delete guard is frontend-only | Delete succeeds despite being a self-delete | PASS — confirms the guard is missing server-side |
| `shouldNotActuallyDeleteAdminAccount_onSelfDeleteAttempt` | Same bug, asserted as correct behavior instead of documented behavior | Admin account should survive | **FAIL** — `expected: not <null>` but was null. Real bug: `UserServiceImpl.deleteUser()` has no self-delete check. |
| `shouldReturn200WhenDeletingNonExistentUser` | UC7 extension 2b claims a missing-user delete returns 200 instead of 404 | 200 OK, silent no-op | PASS — confirms the wrong status code |
| `shouldDeleteUsersOrders_whenDeletingUserWithOrders` | UC7 extension 5a claims orders are hard-deleted with the user, no warning | Orders gone after user deletion | PASS |
| `shouldLeaveDriverStatsOrphaned_whenDeletingADriverAccount` | UC7 extension 5b claims `DriverStats` has no FK to `User` and is never cleaned up | Stats row survives the user's deletion | PASS — confirms permanent orphaning |
| `shouldAllowDeletingTheLastRemainingAdmin_withNoGuard` | UC7 extension 5c claims no last-admin protection exists | Deletion succeeds, zero admins remain | PASS — confirms no guard |

## Use Case #8 — Add a food item

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `testCreateFood_succeedsForNonAdminNonStaffRole_becauseNoRoleCheckExists` | UC8 extension 2a claims `FoodController` has no `@PreAuthorize` at all | A CUSTOMER account can create food | PASS — confirms the missing role check |
| `testCreateFood_shouldNotActuallyCreateFood_whenSubmittedByNonAdminNonStaffRole` | Same bug, asserted as correct behavior | Food should not be created by a non-staff account | **FAIL** — `expected: false but was: true`. Real bug: no role restriction on `POST /api/foods`. |

## Use Case #9 — Remove a food item from the menu

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `testDeleteFood_succeedsForNonAdminNonStaffRole_becauseNoRoleCheckExists` | UC9 extension 1a claims the same missing role check applies to delete | A CUSTOMER account can delete food | PASS — confirms the missing role check |
| `testDeleteFood_blockedByUnfulfilledOrder` | UC9 extension 4a: deletion should be refused, naming the blocking count | 409 Conflict, message states the count | PASS |
| `testDeleteFood_removesItemFromFulfilledOrder_priceStaysStale` | UC9 extension 5a: item silently stripped from fulfilled orders, price never recomputed | Item list shrinks, `cost` field unchanged | PASS — confirms the inconsistency |
| `testDeleteFood_ratingRecordStillReferencesDeletedItem` | UC9 extension 5b claims `ratedFoodIds` is never cleaned up | Dangling ID reference survives deletion | PASS — confirms the dangling reference |
| `testDeleteFood_ratedFoodIdShouldBeRemoved_whenFoodIsDeleted` | Same bug, asserted as correct behavior | Rating reference should be cleaned up | **FAIL** — `expected: false but was: true`. Real bug: `deleteFood()` never touches `Order.ratedFoodIds`. |

## Use Case #10 — Update order status (pick up / deliver)

| Test | Why we tried it | Expected | What happened |
|---|---|---|---|
| `updateOrder_pickUp_reassignsOrder_evenIfAlreadyPickedUpByAnotherDriver` | UC10 extension 2b claims no guard against reassigning an already-picked-up order | Second driver silently overwrites the assignment | PASS — confirms no guard |
| `updateOrder_delivered_byWrongDriver_leavesUnfulfilled_noError` | UC10 extension 5a: wrong driver's "Delivered" call silently no-ops | Status overwritten, `isFulfilled` stays false, no error | PASS |
| `updateOrder_delivered_withNoAssignedDriver_throwsNPE` | UC10 extension 5b claims an unhandled NPE when driver is unassigned | `NullPointerException` before save | PASS — confirms the crash |
| `updateOrder_statusIsNull_throwsNPE_beforeSave` | UC10 extension 5c claims an unhandled NPE when status is missing | `NullPointerException` before save | PASS — confirms the crash |
| `updateOrder_unrecognizedStatus_isPersistedVerbatim_andFallsIntoUnfulfilledBranch` | UC10 extension 5d claims no validation against an allowed status set | Arbitrary string persisted, falls into unfulfilled branch | PASS — confirms no validation |
| `updateOrder_secondDeliveredCall_doubleCreditsEarnings` | UC10 extension 5e claims reprocessing an already-delivered order double-credits earnings | `updateTotalEarnings` invoked again | PASS — confirms the double-credit |
| `updateOrder_secondDeliveredCall_shouldNotCreditEarningsAgain` | Same bug, asserted as correct behavior | Earnings should not be credited twice | **FAIL** — Mockito `NeverWantedButInvoked`: `updateTotalEarnings` was called when it shouldn't have been. Real bug confirmed. |
| `updateOrder_stalePickUp_onAlreadyDeliveredOrder_revertsToUnfulfilled` | UC10 extension 5f claims a stale "Pick Up" silently reverts a completed delivery | `isFulfilled` flips back to false, earnings not clawed back | PASS — confirms the reversion with no reconciliation |

## Summary

- **29 tests total**, all executed against the real FoodSeer backend and MySQL — no test doubles at the controller layer, Mockito only at the isolated service-unit layer (`OrderServiceImplTest.java`, `OrderServiceImplCreateOrderTest.java`).
- **25 passed** — of these, roughly half are ordinary correctness checks (happy paths, expected exceptions already coded for) and the rest deliberately document confirmed bugs as *current* behavior.
- **4 failed on purpose** — each is the same bug as a passing neighbor, re-asserted as the *correct* behavior instead. The failure itself is the proof the bug is real, not a broken test.
- **Zero passes came from lucky guesses** — every PASS and every FAIL was traced back to a specific line in `UserServiceImpl.java`, `FoodServiceImpl.java`, `FoodController.java`, or `OrderServiceImpl.java` before the test was written, then confirmed by actually running it.

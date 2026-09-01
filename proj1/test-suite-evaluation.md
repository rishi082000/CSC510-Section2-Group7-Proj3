# D4: Evaluation of FoodSeer's Existing Test Suite

Scope: the 31 JUnit test classes under `food-seer-backend/src/test/java/FoodSeer/`,
containing 174 `@Test` methods and no parameterized tests. Counts taken from the
merged `main` at commit `8d6bc47`. Findings 1-6 and the first paragraph of the
closing section cover Use Cases #11-15 and the wider suite; Findings 7-8, and
the closing section's second paragraph, extend this evaluation with evidence
specific to Use Cases #6-#10.

## Verdict

The suite is substantial in volume and competent in the layers where the questions
are easy. Its mapper, repository, and service tests exercise real objects against a
real persistence layer and assert on specific values. What it does not do is check
the system against its requirements. Almost without exception, its tests were
written by reading the implementation and asserting that the implementation does
what it does. That is why a suite of 174 tests sits on top of a codebase in which
five endpoints are missing authorization entirely, and reports nothing wrong.

Every defect we found in Sections 3 and 5 is invisible to this suite by
construction, not by oversight. The findings below give the mechanism.

## Finding 1: authorization is asserted only where authorization already works

The suite contains exactly nine negative-authorization assertions (six
`status().isForbidden()` and three `status().isUnauthorized()`) across 174 tests.
They are distributed as follows, against the `@PreAuthorize` annotations actually
present in each controller:

| Controller | `@PreAuthorize` methods | Negative authz assertions | Authorization correct? |
| --- | --- | --- | --- |
| `UserController` | 4 | 5 | yes |
| `OrderController` | 11 (one commented out, line 174) | 3 | mostly |
| `AuthController` | 0 (correct; public by design) | 1 | yes |
| `InventoryController` | 2, both admitting `CUSTOMER` | 0 | **no** |
| `ChatController` | 1 | 0 | yes |
| `FoodController` | 0 | 0 | **no** |
| `DriverStatsController` | 0 (imports the annotation, never uses it) | 0 | **no** |

The correlation is exact. Denial is asserted where an annotation exists and nowhere
else. This is the signature of tests generated from the code rather than from the
use cases: a test author reading `UserController` sees `hasRole('ADMIN')` and writes
a test that a non-admin gets 403; a test author reading `FoodController` sees no
annotation and therefore has nothing to write a test about. A missing authorization
check produces no artifact for such a process to notice.

`InventoryController` is the sharpest case. It carries `@PreAuthorize`, so it looks
covered, but the expression is
`hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')` on the write paths, so customers can
rewrite stock. Two tests exist for the controller and both authenticate as `STAFF`
and assert success. Nobody tested the boundary, so the wrong role list survives.

*Independent corroboration (Use Cases #8-#9):* the same `FoodController` gap was
found separately, from the test side rather than the annotation side. All ten of
`FoodControllerTest.java`'s pre-existing tests, `testGetFoods`, `testCreateFood`,
`testUpdateFood`, `testCreateFoodDuplicateName`, `testCreateFoodInvalid`,
`testDeleteFoodSuccess`, `testDeleteFoodNotFound`, `testUpdateFoodMissingName`,
`testUpdateFoodNotFound`, `testUpdateFoodInvalidValues`, are annotated
`@WithMockUser(username = "staff", roles = "STAFF")` with no exception. A `CUSTOMER`
or unauthenticated caller is never tried once. Two new red tests, written against
Use Case #8 extension 2a and Use Case #9 extension 1a, confirm the consequence
directly: a `CUSTOMER` account can call both `POST /api/foods` and
`DELETE /api/foods/{id}` and succeed.

## Finding 2: the one endpoint we broke is tested with security switched off

`DriverStatsControllerTest` is the suite's only test of the endpoint at the center
of our UC13 findings, and it is declared:

```java
@WebMvcTest(DriverStatsController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverStatsControllerTest {
```

`addFilters = false` removes the Spring Security filter chain. Both of its tests
then issue an unauthenticated request carrying an arbitrary username:

```java
mockMvc.perform(get("/api/driverStats").param("username", "driver1"))
       .andExpect(status().isOk());
```

Our UC13 test `test_driver_statistics_require_a_login` sends the same request to the
running application and observes the same 200. The observation is identical; the
verdict is opposite. Theirs passes because it asserts what the endpoint does. Ours
fails because it asserts what the endpoint should do. The defect is not merely
uncovered by their suite. It is documented by it as the expected result.

## Finding 3: two tests pin defects in place as required behavior

A test that encodes a defect is worse than no test, because it converts a future fix
into a build failure.

**`DriverStatsControllerTest.shouldReturnInternalServerErrorWhenServiceThrows`**
asserts that a `RuntimeException` from the service layer must surface as HTTP 500.
This is the same unhandled-exception behavior that our UC12 food-deletion test
reports as a defect (a raw MySQL foreign-key constraint message returned to the
caller). Adding the exception handling that would fix our finding breaks their test.

**`ChatServiceImplTest.test_ErrorHandling_NoCrash`** asserts:

```java
assertTrue(resp.contains("Error:") || resp.toLowerCase().contains("error"));
```

That is a direct assertion that a failed call to the language model should be
returned in the same field, and with the same 200 status, as a real answer,
which is Section 5 defect 15 restated as a requirement. `ChatServiceImpl.sendMessage` wraps
its entire body in `catch (Exception e) { return new ChatResponseDto("Error: " +
e.getMessage()); }` (line 107), and this test exists to confirm that it does.

That test class has a second problem. It constructs `ChatServiceImpl` with `new` and
injects the repository by reflection, so it never goes through Spring, but
`sendMessage` posts to a hardcoded `http://localhost:11434/api/chat` (line 28)
regardless. All five of its tests therefore make live network calls whose outcome
depends on whether Ollama happens to be running on the machine, and all five pass
either way, because ten of their eleven assertions are `assertNotNull`. Its
`extractResponse` helper reflects over six candidate getter names and swallows every
exception, meaning the test does not know the shape of the DTO it is checking.

## Finding 4: dead test configuration, and the accident that saves the suite

`config/TestSecurityConfig.java` and `config/TestJpaConfig.java` are never imported.
No `@Import`, `@ContextConfiguration`, or `@SpringBootTest(classes = ...)` anywhere
in the suite references either class. They are dead code.

This matters, because `TestSecurityConfig` does not describe the production policy.
It declares `anyRequest().permitAll()`, invents a web-layer rule
(`/api/users/** hasRole("ADMIN")`) that production does not have, mocks out
`JwtTokenProvider` and `UserDetailsService`, and, critically, carries
`@EnableWebSecurity` without `@EnableMethodSecurity`, which is what makes
`@PreAuthorize` effective. Had it ever been wired in, every `@PreAuthorize` in the
application would have been inert during testing and the nine denial assertions of
Finding 1 would all have been meaningless.

The suite's only real authorization coverage survives by accident: the controller
tests use `@SpringBootTest` with `@AutoConfigureMockMvc`, which loads the production
`SpringSecurityConfig`, because the test configuration that would have replaced it
was never connected.

## Finding 5: vacuous tests

- **`AppTest.shouldAnswerWithTrue`** is `assertTrue(true)`, with the Maven archetype's
  original comment `/** Rigorous Test :-) */` still attached. It has never been
  removed and counts toward the suite's totals.

- **`DriverDashboardTest.testPickUpOrder` and `testDeliverOrder`** wrap their entire
  bodies in `if (availableOrders.size() > 0)` and `if (activeOrders.size() > 0)`
  respectively (lines 191 and 224; the pattern recurs at line 299). Against a clean
  database, which is the state any fresh checkout or CI run starts from, both
  conditions are false, no assertion executes, and both tests report green. The two
  tests covering the driver's core workflow are no-ops in precisely the environment
  where they are most likely to run.

- **`frontend/DashboardTest` and `frontend/OrdersTest`** sit in the `frontend` package
  alongside five Selenium classes but start no browser, make no HTTP request, and
  load no Spring context. They construct `User`, `Food`, and `Order` objects and
  assert on their getters. Eleven of the suite's 174 tests are entity tests filed
  under a name that implies end-to-end coverage.

*Related, found independently (Use Case #10):* two more pre-existing tests pass
while testing nothing, for a different reason, see Finding 8.

## Finding 6: the suite does not pass

`mvn clean test` does not complete successfully on the merged `main`;
`DriverDashboardTest#testDriverDashboardLoads` fails. A suite whose own baseline is
red cannot function as a regression check, because there is no green state to
regress from. Notably, this failure has not blocked development: it is a
pre-existing condition of the project as delivered.

> TODO before submission: run `mvn clean test` on the Mac with MySQL up and paste
> the Tests run / Failures / Errors / Skipped summary line here, plus the failing
> test's stack trace, as the raw evidence sample for this finding.

## Finding 7: Use Case #7 (Delete user account) has essentially zero coverage of its own extensions

`UserControllerTest.shouldDeleteUserWhenAdmin` is the only pre-existing test that
exercises `DELETE /api/users/{id}`:

```java
@Test
@WithMockUser(roles = "ADMIN")
void shouldDeleteUserWhenAdmin() throws Exception {
    mockMvc.perform(delete("/api/users/" + testUser.getId()))
            .andExpect(status().isOk());
    mockMvc.perform(get("/api/users/" + testUser.getId()))
            .andExpect(status().isNotFound());
}
```

It deletes a user that is not the caller, has no orders, is not a driver, and is
not the last admin, the one case in which nothing can go wrong. Every extension
the use case itself documents is untouched:

| Extension | What it claims | Pre-existing test | Verdict |
| --- | --- | --- | --- |
| 2a: self-delete | Admin can delete their own account; no backend guard | none | uncovered |
| 2b: delete a non-existent user | Returns 200 instead of 404 | none | uncovered |
| 5a: user has placed orders | Orders are hard-deleted with no warning | none | uncovered |
| 5b: deleted user is a driver | `DriverStats` row is permanently orphaned | none | uncovered |
| 5c: last remaining admin | No guard; platform left with zero admins | none | uncovered |

Five documented failure extensions, five with no test. Our own
`shouldNotActuallyDeleteAdminAccount_onSelfDeleteAttempt` (extension 2a, JUnit) and
`test_an_admin_should_not_be_able_to_delete_their_own_account` (extension 2a,
pytest) are red for exactly this reason: nothing in the pre-existing suite would
have told anyone the guard was frontend-only.

## Finding 8: two pre-existing driver tests send a status value that doesn't match the code, and pass without testing anything

`OrderControllerTest.java` has two tests, both added under the comment
`// --- Missing tests added below ---`, that are meant to exercise a driver picking
up an order:

```java
void testGetActiveOrders_ForDriver() throws Exception {
    ...
    orderService.updateOrder(saved.getId(), "driver", "PICKED_UP");
    mvc.perform(get("/api/orders/activeOrders/driver"))
            .andExpect(status().isOk());
}

void testUpdateOrderStatus_AsDriver() throws Exception {
    ...
    String payload = "{\"username\":\"driver\",\"status\":\"PICKED_UP\"}";
    mvc.perform(post("/api/orders/" + saved.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isOk());
}
```

Both send the literal string `"PICKED_UP"`. `OrderServiceImpl.updateOrder()`
requires an exact match against `"Picked Up"` (line 274,
`status.equals("Picked Up")`) before it will assign a driver to the order.
`"PICKED_UP".equals("Picked Up")` is false, so the driver-assignment branch never
runs, so the order's `driver` field stays null, and its `status` column is left
holding the literal, non-canonical string `"PICKED_UP"`.

The consequence compounds in `testGetActiveOrders_ForDriver`:
`OrderServiceImpl.getActiveOrders()` queries
`orderRepository.findByDriverUsernameAndStatus(username, "Picked Up")`, the same
exact-match string, and now also a driver field that was never set. The query can
only return an empty list. The test asserts `status().isOk()` and nothing else;
it never checks that the returned list contains the order, or that it is
non-empty. Both tests pass every time, and both pass without ever exercising the
driver-assignment code path their names claim to test.

This is a defect in the *tests*, not in the application, but it means Use Case
#10's main success scenario (driver picks up, then delivers an order) had a
green-looking but empty test before our own coverage was added. Our pytest suite's
`test_a_driver_can_pick_up_and_deliver_an_order` uses the correct literal
`"Picked Up"` and exercises the real path end to end.

## What this implies for our own tests

Our pytest suite (29 tests, Use Cases #1-5 and #11-20) found 15 real failures
against the same code these 174 tests pass on. The difference is not effort or
volume; it is the source of the oracle. Their tests take their expected values
from the implementation, so the expected value and the actual value can never
disagree. Ours take expected values from the use cases, such as "a driver's
earnings require a login" or "an allergic customer is not shown food they cannot
eat," so a disagreement is possible, and fifteen times it happened.

This is also the strongest argument for the black-box, over-HTTP approach we chose.
A test that runs inside the application's own context can be made to pass by
adjusting the context, as `addFilters = false` demonstrates. A test that sends an
HTTP request to a running server and reads the status code cannot.

A separate pair of suites (25 pytest tests plus 29 JUnit tests, Use Cases #6-#10)
reaches the same conclusion from a different angle. Both found the same underlying
defects the same way: expected values were taken from the use cases' own
extensions, not from the code. Several tests in each suite were written twice on
purpose, once asserting the code's actual (buggy) behavior, once asserting the
behavior the use case actually specifies, and the second version fails every time,
which is the point: a failing test against a real fault is a finding, not a
mistake. Running both a JUnit suite and a black-box pytest suite against the same
use cases also surfaced something the black-box suite alone could not have shown
on its own: against the live, persistent database, `DELETE /api/foods/{id}` fails
with an unhandled 500 for any food ever added, for any caller, admin included,
because `deleteFood()` never removes the food from `Inventory.foods` before
deleting the row. The JUnit suite, running against an isolated database wiped
before each test, does not hit this blocker, and so is the one that actually
demonstrates the deeper order-mutation and dangling-rating defects extensions 5a
and 5b describe. Neither suite alone would have shown both halves of this story.

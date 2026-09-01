# Project 1 Use Cases

## Use Case #1

Part: Driver Registration and Dashboard Access

Name: Register and login as driver

Primary Actor: Driver

Stakeholders and Interests:
- Drivers: want to create an account and access their driver dashboard.
- Company and admin: want drivers to be registered with the correct role.
- Customers: benefit from available registered drivers for order delivery.

Preconditions:
- Frontend and backend are running correctly.
- The driver does not already have an account with the chosen username or email.

Trigger:
- A potential driver chooses to make an account.

Main Success Scenario:
1. Driver accesses the main web page.
2. Driver clicks the register button.
3. Driver enters username, email, password, and password confirmation.
4. Driver selects Driver as the role.
5. Driver submits registration.
6. System creates the driver account and associated driver statistics record.
7. Driver logs in with the just-created account.
8. System redirects the driver to the driver dashboard.
9. Driver checks that the dashboard content and statistics load.

Extensions:
- 3a. User enters incomplete or invalid information.
  Error message prompts user to enter the information correctly.
- 3b. Password and password confirmation do not match.
  Error message prompts user to have passwords match.
- 4a. User does not select a role.
  Error message prompts user to select a role.
- 5a. Email account is already being used.
  Error message prompts user to select a different email address.
- 7a. Login fails due to invalid credentials.
  Error message prompts user to enter valid credentials.
- 9a. Account exists but dashboard content does not load.
  Error message says content does not exist or cannot be loaded.

Postconditions:
- New driver's account exists with the proper role.
- The account can be logged into and accessed.
- Relevant statistics and dashboard information can be accessed.


## Use Case #6
Part:  Fulfilled Order
Name: Fulfilled Order
Primary Actor: Admin or staff
Stakeholders and Interests:
- Admin/Driver: wants to accurately mark completed work.
- Customer: wants their order status to reflect reality.
- Platform: wants to prevent double-fulfillment or fraud.
Preconditions:
- Frontend and backend are running correctly.
- The order exists and is currently unfulfilled.
- The actor is authenticated as an admin or driver.
Trigger:
- An admin or driver marks an order as fulfilled.
Main Success Scenario:
1. Actor selects an unfulfilled order.
2. Actor submits a fulfill request for that order.
3. System marks the order as fulfilled.
4. System confirms the update to the actor.
Extensions:
- 2a. The order does not exist.
  System returns HTTP 412 (OrderController.java:89-92).
- 2b. The order has already been fulfilled.
  System returns HTTP 410, preventing double-fulfillment (OrderController.java:96-98).
Postconditions:
- The order's status is fulfilled.
- The order now appears in /api/orders/fulfilledOrders.
- The order appears in the customer's /my-orders/fulfilled view.

## Use Case #7
Part: User Account Management
Name: Delete user account
Primary Actor: Admin
Stakeholders and Interests:
- Admin: wants to remove accounts that are no longer needed (e.g. spam, misuse).
- Deleted user: loses their account.
- Platform: needs at least one admin to remain functional, and shouldn't silently destroy order history.
Preconditions:
- Frontend and backend are running correctly.
- Actor is authenticated with the Admin role.
- A target user account exists to delete.
Trigger:
- Admin clicks "Delete" on a user in the User Management page.
Main Success Scenario:
1. Admin opens the User Management page and views the list of accounts.
2. Admin clicks "Delete" on a target user (not their own account).
3. System asks the admin to confirm.
4. Admin confirms.
5. System permanently deletes all of that user's orders, then deletes the user account.
6. System confirms the deletion and refreshes the user list.
Extensions:
- 2a. Admin attempts to delete their own account.
  The Delete button is disabled for the admin's own row, and a direct click attempt shows a warning message ,but this is enforced only on the frontend. Bypassing the UI allows an admin to delete their own account, since the backend has no self-delete check at all.
- 2b. Target user does not exist.
  System reports success anyway, without indicating whether a user was actually found or deleted.
- 5a. The deleted user has placed orders.
  All of that user's orders are permanently deleted along with the account, with no warning shown to the admin beforehand and no soft-delete or archive.
- 5b. The deleted user was a driver.
  Their driver statistics record (total deliveries, earnings, rating history) is never touched ,it's left in the database, now orphaned and tied to a username with no account.
- 5c. The deleted user is the last remaining admin.
  System has no last-admin protection; the account is deleted exactly like any other, potentially leaving the platform with zero admins and no in-app way to create a new one.
  
Postconditions:
- On success: the user account and every order they placed are permanently removed from the system.


## Use Case #8
Part: Food Catalog Management
Name: Add a new food item
Primary Actor: Admin or Staff
Stakeholders and Interests:
- Admin/Staff: wants to keep the menu accurate and complete.
- Customers: want to see accurate food options and prices.
- Platform: wants only authorized staff able to modify the shared menu.
Preconditions:
- Frontend and backend are running correctly.
- Actor is logged in (the interface only shows this feature to Admin/Staff accounts).
Trigger:
- Admin or staff submits the "Add Food" form.
Main Success Scenario:
1. Admin/Staff opens Inventory Management and clicks "Add Food".
2. Admin/Staff enters a food name, amount, price, and any allergies, then submits.
3. System checks that the name isn't already in use and that the fields are valid.
4. System creates the new food item.
5. System adds the new item to the shared inventory
6. System confirms the addition and the food list refreshes.
Extensions:
- 2a. A logged-in account that is not Admin or Staff submits a request to this same action directly, bypassing the interface.
  The system has no check on who is allowed to perform this action ,the request succeeds exactly as if it came from an admin. Only the interface itself hides this feature from non-admin/staff accounts; nothing prevents the underlying action itself.
- 3a. The submitted name already matches an existing food item.
  System rejects the submission as a duplicate.
- 3b. The submitted fields are invalid ,missing name, a negative amount or price, or a blank allergies entry.
  System rejects the submission.

Postconditions:
- On success, a new food item exists in the shared menu with the given name, amount, price, and allergens, visible to everyone browsing food.

## Use Case #9
Part: Food Catalog Management
Name: Remove a food item from the menu
Primary Actor: Admin or Staff
Stakeholders and Interests:
- Admin/Staff: wants to remove discontinued items from the menu.
- Customers: rely on their past order history staying accurate.
- Platform: wants historical order records to remain trustworthy.
Preconditions:
- Frontend and backend are running correctly.
- Actor is logged in (the interface only shows this feature to Admin/Staff accounts).
- The target food item exists.
Trigger:
- Admin or staff clicks "Delete" on a food item and confirms.
Main Success Scenario:
1. Admin/Staff opens Inventory Management and clicks "Delete" on a food item.
2. System asks for confirmation, naming the item.
3. Admin/Staff confirms.
4. System checks whether the item appears in any unfulfilled orders.
5. System removes the item from any past fulfilled orders that contained it, then deletes it from the catalog.
6. System confirms the removal and the food list refreshes.
Extensions:
- 1a. A logged-in account that is not Admin or Staff submits a request to this same action directly, bypassing the interface.
  The system has no check on who is allowed to perform this action ,the request succeeds exactly as if it came from an admin. Only the interface itself hides this feature from non-admin/staff accounts.
- 1b. The specified food item does not exist.
  System rejects the request, naming the missing item.
- 4a. The item appears in at least one unfulfilled order.
  System refuses to delete it, naming how many unfulfilled orders are blocking the removal.
- 5a. The item appears only in fulfilled (historical) orders.
  System proceeds with the deletion, silently removing that item from every one of those past orders' item lists as part of the same action. Each affected order's stored total price is not adjusted to match, so the order's price and its listed items become permanently inconsistent, with no warning given beforehand and no way to recover what the order originally contained.
- 5b. The item was previously rated as part of a now-affected order.
  The rating record still refers to the deleted item after it no longer exists in the catalog.
Postconditions:
- On success, the food item no longer exists in the catalog.
- Any fulfilled orders that once contained it have had it silently removed from their item list, while their recorded price remains unchanged.

## Use Case #10
Part: Order Delivery Lifecycle
Name: Update order status (pick up / deliver)
Primary Actor: Driver
Stakeholders and Interests:
- Driver: wants pickup and delivery correctly recorded, and earnings credited exactly once.
- Customer: wants their order's status to reflect reality.
- Platform: wants driver payouts accurate and orders not double-processed.
Preconditions:
- Frontend and backend are running correctly.
- Actor is authenticated with the Driver (or Admin) role.
- An order exists that the driver wants to update.
Trigger:
- Driver clicks "Pick Up" or "Mark as Delivered" on an order in the Driver Dashboard.
Main Success Scenario:
1. Driver selects an order on their dashboard.
2. Driver clicks "Pick Up".
3. System assigns the order to the driver and sets its status to "Picked Up".
4. Driver later clicks "Mark as Delivered" on the same order.
5. System confirms the requesting driver matches the order's assigned driver, marks the order fulfilled, and credits the driver's earnings.
6. Dashboard reloads to reflect the updated order.
Extensions:
- 2a. Order ID does not exist.
  System detects that the order doesn't exist, but still reports success with an empty result instead of a clear "not found" response ,the failure is swallowed rather than surfaced to the caller.
- 2b. Order is already assigned to another driver, or already "Picked Up".
  No check exists , any driver can "pick up" any order regardless of its current state, silently reassigning it.
- 5a. "Delivered" is submitted by a driver who is NOT the order's assigned driver.
  System does not reject or flag this , it silently leaves the order unfulfilled with no message returned, even though the order's status was already overwritten to "Delivered" regardless.
- 5b. Order's driver is still unassigned when "Delivered" arrives (e.g. sent without a prior "Picked Up").
  The request crashes with an unhandled error before the update is saved.
- 5c. The status value is missing.
  The request crashes with an unhandled error before the update is saved.
- 5d. The status value is an unrecognized string (e.g. "Bananas").
  No validation against an allowed set of values , it's accepted and persisted as-is, then silently falls into the unfulfilled branch.
- 5e. An already-delivered order receives a second "Delivered" call.
  No guard against reprocessing a completed order , the driver's earnings and total delivery count are both increased again, as if a second, separate delivery had occurred.
- 5f. A stale "Pick Up" click lands on an order that has already been delivered (e.g. the driver's dashboard hadn't refreshed).
  The system reverts the order's fulfilled status back to unfulfilled, even though the driver was already paid for delivering it ,leaving the platform's fulfilled-order count and the driver's credited earnings permanently out of agreement, with no way to reconcile them through the app.
Postconditions:
- On success: order status is "Picked Up" or "Delivered" as requested; for a valid "Delivered" by the correct assigned driver, the order is marked fulfilled and earnings are credited exactly once.
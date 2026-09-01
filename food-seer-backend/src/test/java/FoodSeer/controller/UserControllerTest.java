package FoodSeer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import FoodSeer.config.Roles;
import FoodSeer.dto.OrderDto;
import FoodSeer.dto.RegisterRequestDto;
import FoodSeer.dto.UpdateRoleDto;
import FoodSeer.dto.UserPreferencesDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.User;
import FoodSeer.repositories.DriverStatsRepository;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.AuthService;
import FoodSeer.service.OrderService;
import FoodSeer.service.UserService;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private DriverStatsRepository driverStatsRepository;

    private User testUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequestDto("testuser", "test@example.com", "password123", "customer"));
        authService.register(new RegisterRequestDto("admin", "admin@example.com", "adminpass", "customer"));
        userService.updateUserRole(userService.getByUsername("admin").getId(), "ROLE_ADMIN");

        // Reload persisted users with their database-generated IDs
        testUser = userService.getByUsername("testuser");
        adminUser = userService.getByUsername("admin");
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void shouldNotAllowNonAdminToListUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(testUser.getUsername()));

        mockMvc.perform(get("/api/users/" + adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(adminUser.getUsername()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateUserRole() throws Exception {
        mockMvc.perform(put("/api/users/" + testUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Roles.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(Roles.ROLE_ADMIN));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = "ROLE_CUSTOMER")
    void shouldNotAllowNonAdminToUpdateRole() throws Exception {
        mockMvc.perform(put("/api/users/" + testUser.getId() + "/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRoleDto(Roles.ROLE_ADMIN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteUserWhenAdmin() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/" + testUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "STANDARD")
    void shouldNotAllowNonAdminToDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldUpdateUserPreferences() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN");

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.costPreference").value("LOW"))
                .andExpect(jsonPath("$.dietaryRestrictions").value("VEGAN"));
    }

    @Test
    void shouldRejectUnauthenticatedUpdatePreferences() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN");

        mockMvc.perform(put("/api/users/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void shouldRejectUnauthenticatedGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "ghostuser")
    void shouldReturn404WhenUpdatingPreferencesForMissingUser() throws Exception {
        UserPreferencesDto prefs = new UserPreferencesDto("LOW", "VEGAN");

        mockMvc.perform(put("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefs)))
                .andExpect(status().isNotFound());
    }

    // --- Use Case #7 (Delete user account): Main Success Scenario ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteOrdinaryUserAndTheirOrders_mainSuccessScenario() throws Exception {
        // Arrange: a third user, distinct from the acting admin, from testUser (used by
        // the 5a test below), and from any driver -- an ordinary, unremarkable delete target.
        authService.register(new RegisterRequestDto("ordinaryuser", "ordinary@example.com", "password123", "customer"));
        User ordinaryUser = userService.getByUsername("ordinaryuser");

        List<Food> foods = foodRepository.findAll();
        Food food = foods.isEmpty() ? foodRepository.save(new Food("Bagel", 10, 3, List.of())) : foods.get(0);

        OrderDto orderDto = new OrderDto(0L, "OrdinaryUserOrder");
        orderDto.setFoods(List.of(food));

        String orderJson = mockMvc.perform(post("/api/orders")
                        .with(user("ordinaryuser").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OrderDto savedOrder = objectMapper.readValue(orderJson, OrderDto.class);

        // Sanity checks: preconditions actually hold before we act
        assertTrue(orderRepository.findById(savedOrder.getId()).isPresent(),
                "Precondition: order exists before deletion");
        long adminCountBefore = userService.listUsers().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole())).count();
        assertEquals(1, adminCountBefore, "Precondition: exactly one admin, and it isn't the target");

        // Act: Steps 1-4 -- admin selects and confirms deletion of an ordinary user
        mockMvc.perform(delete("/api/users/" + ordinaryUser.getId()))
                // Steps 5-6: system deletes orders, then the account, confirms 200
                .andExpect(status().isOk());

        // Assert: Postconditions -- user and their orders are gone
        assertNull(userService.findById(ordinaryUser.getId()));
        assertTrue(orderRepository.findById(savedOrder.getId()).isEmpty());

        // Assert: this is NOT the 5c scenario -- the admin count must be unchanged, not zero
        long adminCountAfter = userService.listUsers().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole())).count();
        assertEquals(1, adminCountAfter,
                "The remaining admin count must be unchanged -- this test is the ordinary "
              + "case, not the last-admin case covered separately by test 5c");
    }
    // This proves the Main Success Scenario (steps 1-6): an admin deleting an unremarkable
    // user (not themselves, not a driver, not the last admin) removes both the user and
    // their orders, while leaving the rest of the system -- specifically the admin roster --
    // untouched. Distinguishes cleanly from 5c, which asserts the admin count DOES hit zero.

    // --- Extension 2a: self-delete has no backend guard ---
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldAllowAdminToDeleteOwnAccount_becauseBackendHasNoSelfDeleteCheck() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminUser.getId()))
                .andExpect(status().isOk());

        assertNull(userService.findById(adminUser.getId()));
    }
    // This proves extension 2a: the self-delete guard in UserManagement.js:67-70 is
    // frontend-only -- a direct DELETE /api/users/{own id} call succeeds, because
    // UserServiceImpl.deleteUser() (UserServiceImpl.java:55-71) has no self-check.

    // --- Same bug, asserted as a RED test: this is what a correct implementation
    // would guarantee. It is EXPECTED TO FAIL against the current code. ---
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldNotActuallyDeleteAdminAccount_onSelfDeleteAttempt() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminUser.getId()));

        assertNotNull(userService.findById(adminUser.getId()),
                "A correct implementation must not allow an admin to delete their own account, "
              + "but UserServiceImpl.deleteUser() (lines 55-71) has no self-check.");
    }

    // --- Extension 2b: deleting a non-existent user still returns 200 ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WhenDeletingNonExistentUser() throws Exception {
        mockMvc.perform(delete("/api/users/999999"))
                .andExpect(status().isOk());
    }
    // This proves extension 2b: deleteUser() returns silently when the user isn't found
    // (UserServiceImpl.java:59-61), and the controller always responds 200 OK
    // (UserController.java:60-63) regardless of whether anything was actually deleted.

    // --- Extension 5a / Postcondition: deleting a user hard-deletes their orders too ---
    @Test
    @WithMockUser(username = "testuser", roles = "CUSTOMER")
    void shouldDeleteUsersOrders_whenDeletingUserWithOrders() throws Exception {
        List<Food> foods = foodRepository.findAll();
        Food food = foods.isEmpty() ? foodRepository.save(new Food("Bagel", 10, 3, List.of())) : foods.get(0);

        OrderDto orderDto = new OrderDto(0L, "TestUserOrder");
        orderDto.setFoods(List.of(food));
        OrderDto savedOrder = orderService.createOrder(orderDto); // owned by "testuser"

        assertTrue(orderRepository.findById(savedOrder.getId()).isPresent(),
                "Sanity check: order exists before deletion");

        mockMvc.perform(delete("/api/users/" + testUser.getId())
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        assertNull(userService.findById(testUser.getId()));
        assertTrue(orderRepository.findById(savedOrder.getId()).isEmpty(),
                "Postcondition: the deleted user's order must be gone too");
    }
    // This proves extension 5a and the stated Postcondition: deleting a user with placed
    // orders hard-deletes those orders (UserServiceImpl.java:66-67), no soft-delete/archive.

    // --- Extension 5b: a deleted driver's DriverStats row is orphaned, not cleaned up ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldLeaveDriverStatsOrphaned_whenDeletingADriverAccount() throws Exception {
        authService.register(new RegisterRequestDto("driveruser", "driver@example.com", "password123", "driver"));
        User driverUser = userService.getByUsername("driveruser");

        assertTrue(driverStatsRepository.findByUsername("driveruser").isPresent(),
                "Sanity check: registering a driver creates a DriverStats row");

        mockMvc.perform(delete("/api/users/" + driverUser.getId()))
                .andExpect(status().isOk());

        assertNull(userService.findById(driverUser.getId()));
        assertTrue(driverStatsRepository.findByUsername("driveruser").isPresent(),
                "Postcondition (extension 5b): DriverStats row survives, now orphaned");
    }
    // This proves extension 5b: DriverStats.username has no FK back to User, and
    // deleteUser() never touches driverStatsRepository -- the row is permanently orphaned.

    // --- Extension 5c: no last-admin protection anywhere ---
    @Test
    void shouldAllowDeletingTheLastRemainingAdmin_withNoGuard() {
        long adminCount = userService.listUsers().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole())).count();
        assertEquals(1, adminCount, "Sanity check: exactly one admin exists before deletion");

        userService.deleteUser(adminUser.getId());

        assertNull(userService.findById(adminUser.getId()));
        long remainingAdmins = userService.listUsers().stream()
                .filter(u -> "ROLE_ADMIN".equals(u.getRole())).count();
        assertEquals(0, remainingAdmins,
                "Postcondition (extension 5c): zero admins remain, nothing prevented it");
    }
    // This proves extension 5c: UserServiceImpl.deleteUser() has no last-admin guard --
    // deleting the sole remaining admin succeeds exactly like any other delete, leaving
    // the platform with no admin and no in-app way to create a new one.

}

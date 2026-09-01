package FoodSeer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import FoodSeer.TestUtils;
import FoodSeer.dto.FoodDto;
import FoodSeer.dto.OrderDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.InventoryRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.repositories.UserRepository;
import FoodSeer.service.FoodService;
import FoodSeer.service.OrderService;

/**
 * Tests FoodController
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FoodControllerTest {

    /** Mock MVC for testing controller */
    @Autowired
    private MockMvc mvc;

    /** Reference to food repository */
    @Autowired
    private FoodRepository foodRepository;

    /** Reference to inventory repository */
    @Autowired
    private InventoryRepository inventoryRepository;

    /** Reference to order repository (ensure orders removed before foods) */
    @Autowired
    private OrderRepository orderRepository;

    /** Reference to food service */
    @Autowired
    private FoodService foodService;

    /** Reference to order service (needed to set up orders for Use Case #9 tests) */
    @Autowired
    private OrderService orderService;

    /** Reference to user repository (needed so orderService.createOrder can attribute an owner) */
    @Autowired
    private UserRepository userRepository;

    /**
     * Sets up the test case.
     *
     * @throws java.lang.Exception if error
     */
    @BeforeEach
    public void setUp () throws Exception {
        // delete orders first so foreign key constraints do not block food/user deletions
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        foodRepository.deleteAll();
        userRepository.deleteAll();

        final User customer = User.builder()
                .username("customer")
                .email("customer@test.com")
                .password("password")
                .role("ROLE_CUSTOMER")
                .build();
        userRepository.save(customer);
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testGetFoods () throws Exception {
        mvc.perform( get( "/api/foods" ) ).andExpect( status().isOk() );
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testCreateFood () throws Exception {
        final FoodDto food1 = new FoodDto( "COFFEE", 5, 3, Arrays.asList( "MILK", "SUGAR" ) );

        mvc.perform( post( "/api/foods" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( food1 ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.amount" ).value( "5" ) )
                .andExpect( jsonPath( "$.price" ).value( "3" ) )
                .andExpect( jsonPath( "$.foodName" ).value( "COFFEE" ) );

        final FoodDto food2 = new FoodDto( "PUMPKIN_SPICE", 10, 7, Arrays.asList( "CINNAMON" ) );
        final FoodDto savedFood2 = foodService.createFood( food2 );

        mvc.perform(
                        get( "/api/foods/" + foodService.getFoodById( savedFood2.getId() ).getId() ) )
                .andExpect( status().isOk() );
    }

    @Test
    @WithMockUser ( username = "staff", roles = "STAFF" )
    void testUpdateFood () throws Exception {
        final FoodDto food1 = new FoodDto( "COFFEE", 5, 3, Arrays.asList( "MILK" ) );

        mvc.perform( post( "/api/foods" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( food1 ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.amount" ).value( "5" ) )
                .andExpect( jsonPath( "$.foodName" ).value( "COFFEE" ) );

        final FoodDto updatedFood = new FoodDto( "COFFEE", 12, 4, Arrays.asList( "MILK", "SUGAR" ) );

        mvc.perform( post( "/api/foods/updateFood" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( TestUtils.asJsonString( updatedFood ) )
                        .accept( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() );
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testCreateFoodDuplicateName() throws Exception {
        FoodDto food = new FoodDto("COFFEE", 5, 3, Arrays.asList("MILK"));
        foodService.createFood(food);

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(food)))
                .andExpect(status().isConflict()); // 409
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testCreateFoodInvalid() throws Exception {
        // Negative amount = invalid
        FoodDto invalid = new FoodDto("INVALID", -5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(invalid)))
                .andExpect(status().isBadRequest()); // 400
    }

    // --- Use Case #8, Extension 2a: no role check exists on this endpoint ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testCreateFood_succeedsForNonAdminNonStaffRole_becauseNoRoleCheckExists() throws Exception {
        // A logged-in CUSTOMER (not Admin/Staff) hits POST /api/foods directly,
        // bypassing the frontend, which only shows "Add Food" to Admin/Staff.
        FoodDto food = new FoodDto("BYPASSED_ITEM", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(food)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foodName").value("BYPASSED_ITEM"));
    }
    // This proves extension 2a: FoodController.createFood() has no @PreAuthorize, and
    // POST /api/foods isn't role-restricted in SpringSecurityConfig either -- any
    // authenticated account, regardless of role, can create food items exactly as if
    // they were Admin/Staff. Only the frontend UI hides the "Add Food" button from other roles.

    // --- Same bug, asserted as a RED test: this is what a correct implementation
    // would guarantee. It is EXPECTED TO FAIL against the current code. ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testCreateFood_shouldNotActuallyCreateFood_whenSubmittedByNonAdminNonStaffRole() throws Exception {
        FoodDto food = new FoodDto("SHOULD_BE_BLOCKED", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(food)));

        boolean exists = foodService.getAllFoods().stream()
                .anyMatch(f -> "SHOULD_BE_BLOCKED".equals(f.getFoodName()));
        assertFalse(exists,
                "A correct implementation must not allow a non-Admin/Staff account to create catalog "
              + "items, but FoodController has no @PreAuthorize on createFood() at all.");
    }

    // --- Use Case #9, Extension 1a: no role check exists on this endpoint either ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testDeleteFood_succeedsForNonAdminNonStaffRole_becauseNoRoleCheckExists() throws Exception {
        FoodDto food = new FoodDto("BYPASS_DELETE", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);

        mvc.perform(delete("/api/foods/" + saved.getId()))
                .andExpect(status().isOk());
    }
    // This proves extension 1a: FoodController.deleteFood() has no @PreAuthorize, and
    // DELETE /api/foods/{id} isn't role-restricted in SpringSecurityConfig either -- any
    // authenticated account, regardless of role, can delete catalog items.

    // --- Use Case #9, Extension 4a: an item in an unfulfilled order blocks deletion ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testDeleteFood_blockedByUnfulfilledOrder() throws Exception {
        FoodDto food = new FoodDto("BLOCKED_ITEM", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);
        Food foodEntity = foodRepository.findById(saved.getId()).orElseThrow();

        OrderDto orderDto = new OrderDto(0L, "BlockingOrder");
        orderDto.setFoods(List.of(foodEntity));
        orderService.createOrder(orderDto); // owned by "customer", unfulfilled by default

        mvc.perform(delete("/api/foods/" + saved.getId()))
                .andExpect(status().isConflict()) // 409
                .andExpect(content().string(containsString("1 unfulfilled order")));
    }
    // This proves extension 4a: an item that appears in at least one unfulfilled order
    // is refused, and the response body names the blocking count (FoodServiceImpl.java:161-164).

    // --- Use Case #9, Extension 5a: fulfilled orders are silently mutated, price stays stale ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testDeleteFood_removesItemFromFulfilledOrder_priceStaysStale() throws Exception {
        FoodDto food = new FoodDto("FULFILLED_ITEM", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);
        Food foodEntity = foodRepository.findById(saved.getId()).orElseThrow();

        OrderDto orderDto = new OrderDto(0L, "FulfilledOrder");
        orderDto.setFoods(List.of(foodEntity));
        orderDto.setCost(new BigDecimal("3.00"));
        OrderDto createdOrder = orderService.createOrder(orderDto);
        OrderDto fulfilledOrder = orderService.fulfillOrder(createdOrder.getId());
        BigDecimal costBeforeDeletion = fulfilledOrder.getCost();

        assertTrue(orderRepository.findById(createdOrder.getId()).orElseThrow()
                        .getFoods().stream().anyMatch(f -> f.getId().equals(saved.getId())),
                "Precondition: the fulfilled order still contains the food before deletion");

        mvc.perform(delete("/api/foods/" + saved.getId()))
                .andExpect(status().isOk());

        Order persistedOrder = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertTrue(persistedOrder.getFoods().isEmpty(),
                "Postcondition (5a): the deleted food must be silently stripped from the fulfilled order's item list");
        assertEquals(0, costBeforeDeletion.compareTo(persistedOrder.getCost()),
                "Postcondition (5a): the order's stored cost is never recomputed, so it stays stale");
    }
    // This proves extension 5a and the Postconditions: deleting a food that's only in
    // fulfilled orders strips it from those orders' item lists (FoodServiceImpl.java:166-172)
    // while their stored cost is left untouched, making price and items permanently inconsistent.

    // --- Use Case #9, Extension 5b: a rating record becomes a dangling reference ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testDeleteFood_ratingRecordStillReferencesDeletedItem() throws Exception {
        FoodDto food = new FoodDto("RATED_ITEM", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);
        Food foodEntity = foodRepository.findById(saved.getId()).orElseThrow();

        OrderDto orderDto = new OrderDto(0L, "RatedOrder");
        orderDto.setFoods(List.of(foodEntity));
        OrderDto createdOrder = orderService.createOrder(orderDto);
        orderService.fulfillOrder(createdOrder.getId());
        foodService.rateFoodInOrder(createdOrder.getId(), saved.getId(), 4.5);

        assertTrue(orderRepository.findById(createdOrder.getId()).orElseThrow()
                        .getRatedFoodIds().contains(saved.getId()),
                "Precondition: rating recorded before deletion");

        mvc.perform(delete("/api/foods/" + saved.getId()))
                .andExpect(status().isOk());

        Order afterDeletion = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertTrue(afterDeletion.getRatedFoodIds().contains(saved.getId()),
                "Postcondition (5b): ratedFoodIds still references the now-deleted food's ID");
        assertTrue(foodRepository.findById(saved.getId()).isEmpty(),
                "Sanity check: the food itself is actually gone from the catalog");
    }
    // This proves extension 5b: deleteFood() never touches Order.ratedFoodIds, so a
    // rating recorded against the food survives as a dangling reference to an ID that
    // no longer resolves to any row in the foods table.

    // --- Same bug, asserted as a RED test: this is what a correct implementation
    // would guarantee. It is EXPECTED TO FAIL against the current code. ---
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void testDeleteFood_ratedFoodIdShouldBeRemoved_whenFoodIsDeleted() throws Exception {
        FoodDto food = new FoodDto("RATED_ITEM_2", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);
        Food foodEntity = foodRepository.findById(saved.getId()).orElseThrow();

        OrderDto orderDto = new OrderDto(0L, "RatedOrder2");
        orderDto.setFoods(List.of(foodEntity));
        OrderDto createdOrder = orderService.createOrder(orderDto);
        orderService.fulfillOrder(createdOrder.getId());
        foodService.rateFoodInOrder(createdOrder.getId(), saved.getId(), 4.5);

        mvc.perform(delete("/api/foods/" + saved.getId()));

        Order afterDeletion = orderRepository.findById(createdOrder.getId()).orElseThrow();
        assertFalse(afterDeletion.getRatedFoodIds().contains(saved.getId()),
                "A correct implementation should clean up rating references when the rated food is "
              + "deleted, but FoodServiceImpl.deleteFood() (lines 147-179) never touches ratedFoodIds.");
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testDeleteFoodSuccess() throws Exception {
        FoodDto food = new FoodDto("TEA", 5, 3, Arrays.asList("NONE"));
        FoodDto saved = foodService.createFood(food);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/foods/" + saved.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testDeleteFoodNotFound() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/foods/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodMissingName() throws Exception {
        FoodDto invalid = new FoodDto("", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodNotFound() throws Exception {
        FoodDto notExist = new FoodDto("NOFOOD", 5, 3, Arrays.asList("NONE"));

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(notExist)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void testUpdateFoodInvalidValues() throws Exception {
        FoodDto food = new FoodDto("BREAD", 5, 2, Arrays.asList("GLUTEN"));
        foodService.createFood(food);

        FoodDto update = new FoodDto("BREAD", -1, 2, Arrays.asList("GLUTEN")); // invalid amount

        mvc.perform(post("/api/foods/updateFood")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtils.asJsonString(update)))
                .andExpect(status().isBadRequest());
    }
}

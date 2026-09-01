// java
package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import FoodSeer.dto.OrderDto;
import FoodSeer.entity.Food;
import FoodSeer.entity.Order;
import FoodSeer.entity.User;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.mapper.OrderMapper;
import FoodSeer.repositories.FoodRepository;
import FoodSeer.repositories.OrderRepository;
import FoodSeer.service.impl.OrderServiceImpl;

/**
 * Unit tests for OrderServiceImpl.createOrder() (OrderServiceImpl.java:57-91).
 * These tests document the method's CURRENT behavior, including gaps that are
 * not validated. They do not assert that the current behavior is desirable.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplCreateOrderTest {

    @Mock
    private FoodRepository foodRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void test_createsOrder_whenAllFoodsExistAndUserIsAuthenticated() {
        // Arrange: one food ID that FoodRepository can resolve, and an authenticated user
        Food managedFood = new Food();
        managedFood.setId(1L);
        managedFood.setFoodName("BURGER");
        when(foodRepository.findById(1L)).thenReturn(Optional.of(managedFood));

        User currentUser = new User();
        currentUser.setUsername("customer1");
        when(userService.getCurrentUser()).thenReturn(currentUser);

        OrderDto request = new OrderDto();
        request.setName("Lunch Order");
        request.setFoods(List.of(managedFood));
        request.setIsFulfilled(true); // deliberately true, to prove line 77 overrides it
        request.setCost(new BigDecimal("12.50"));
        request.setStatus("Placed");
        request.setDeliveryCost(new BigDecimal("2.00"));

        Order saved = new Order();
        saved.setId(100L);
        saved.setName("Lunch Order");
        saved.setFoods(List.of(managedFood));
        saved.setIsFulfilled(false);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        try (MockedStatic<OrderMapper> mockedMapper = mockStatic(OrderMapper.class)) {
            OrderDto expectedDto = new OrderDto(100L, "Lunch Order");
            mockedMapper.when(() -> OrderMapper.mapToOrderDto(saved)).thenReturn(expectedDto);

            // Act
            OrderDto result = orderService.createOrder(request);

            // Assert
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(captor.capture());
            Order persisted = captor.getValue();

            assertFalse(persisted.getIsFulfilled());
            assertEquals("Lunch Order", persisted.getName());
            assertEquals(currentUser, persisted.getUser());
            assertSame(expectedDto, result);
            assertEquals(100L, result.getId());
        }
    }
    // This proves that a well-formed order with an existing food ID and an authenticated
    // user is saved and mapped back successfully, and that isFulfilled is unconditionally
    // forced to false (OrderServiceImpl.java:77) regardless of what the request DTO sent.

    @Test
    void test_rejectsOrder_whenFoodIdDoesNotExist() {
        // Arrange: a food ID with no matching row in FoodRepository
        Food unknownFood = new Food();
        unknownFood.setId(99L);
        when(foodRepository.findById(99L)).thenReturn(Optional.empty());

        OrderDto request = new OrderDto();
        request.setName("Bad Order");
        request.setFoods(List.of(unknownFood));

        // Act / Assert
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(request));
        assertEquals("A Food item does not exist within the order.", ex.getMessage());

        verify(userService, never()).getCurrentUser();
        verify(orderRepository, never()).save(any());
    }
    // This proves that createOrder() rejects a request whose foods list references a
    // non-existent food ID by throwing ResourceNotFoundException (OrderServiceImpl.java:67-71),
    // and that it does so before ever checking for an authenticated user or saving anything.

    @Test
    void test_createsOrder_withEmptyFoodsList() {
        // Arrange: orderDto.getFoods() is an empty list
        User currentUser = new User();
        currentUser.setUsername("customer2");
        when(userService.getCurrentUser()).thenReturn(currentUser);

        OrderDto request = new OrderDto();
        request.setName("Empty Order");
        request.setFoods(new ArrayList<>());

        Order saved = new Order();
        saved.setId(101L);
        saved.setName("Empty Order");
        saved.setFoods(new ArrayList<>());
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        try (MockedStatic<OrderMapper> mockedMapper = mockStatic(OrderMapper.class)) {
            mockedMapper.when(() -> OrderMapper.mapToOrderDto(saved)).thenReturn(new OrderDto(101L, "Empty Order"));

            // Act
            OrderDto result = orderService.createOrder(request);

            // Assert
            assertNotNull(result);
            verify(orderRepository, times(1)).save(any(Order.class));
            verify(foodRepository, never()).findById(any());
        }
    }
    // This proves the code lets an empty foods list through with no rejection: the for-loop
    // at OrderServiceImpl.java:67-71 simply never executes when orderDto.getFoods() is empty,
    // and there is no guard anywhere in createOrder() requiring at least one food item.

    @Test
    void test_createsOrder_withNegativeCost() {
        // Arrange: a negative BigDecimal cost
        Food managedFood = new Food();
        managedFood.setId(2L);
        managedFood.setFoodName("SOUP");
        when(foodRepository.findById(2L)).thenReturn(Optional.of(managedFood));

        User currentUser = new User();
        currentUser.setUsername("customer3");
        when(userService.getCurrentUser()).thenReturn(currentUser);

        OrderDto request = new OrderDto();
        request.setName("Negative Cost Order");
        request.setFoods(List.of(managedFood));
        request.setCost(new BigDecimal("-50.00"));

        Order saved = new Order();
        saved.setId(102L);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        try (MockedStatic<OrderMapper> mockedMapper = mockStatic(OrderMapper.class)) {
            mockedMapper.when(() -> OrderMapper.mapToOrderDto(saved)).thenReturn(new OrderDto(102L, "Negative Cost Order"));

            // Act
            orderService.createOrder(request);

            // Assert
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(captor.capture());
            assertEquals(new BigDecimal("-50.00"), captor.getValue().getCost());
        }
    }
    // This proves the code accepts a negative cost with no validation: line 78
    // (order.setCost(orderDto.getCost())) copies whatever BigDecimal the client sent,
    // including a negative one, straight onto the persisted Order with no range check.

    @Test
    void test_createsOrder_withInvalidStatusValue() {
        // Arrange: a status string that is not part of any known status set
        Food managedFood = new Food();
        managedFood.setId(3L);
        managedFood.setFoodName("SALAD");
        when(foodRepository.findById(3L)).thenReturn(Optional.of(managedFood));

        User currentUser = new User();
        currentUser.setUsername("customer4");
        when(userService.getCurrentUser()).thenReturn(currentUser);

        OrderDto request = new OrderDto();
        request.setName("Weird Status Order");
        request.setFoods(List.of(managedFood));
        request.setStatus("Bananas");

        Order saved = new Order();
        saved.setId(103L);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        try (MockedStatic<OrderMapper> mockedMapper = mockStatic(OrderMapper.class)) {
            mockedMapper.when(() -> OrderMapper.mapToOrderDto(saved)).thenReturn(new OrderDto(103L, "Weird Status Order"));

            // Act
            orderService.createOrder(request);

            // Assert
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(1)).save(captor.capture());
            assertEquals("Bananas", captor.getValue().getStatus());
        }
    }
    // This proves the code accepts an arbitrary, unrecognized status string with no
    // validation: line 79 (order.setStatus(orderDto.getStatus())) persists whatever
    // value orderDto.getStatus() contains verbatim, with no check against an allowed set.

    @Test
    void test_rejectsOrder_whenNoAuthenticatedUser() {
        // Arrange: food resolves fine, but there is no authenticated user
        Food managedFood = new Food();
        managedFood.setId(4L);
        managedFood.setFoodName("PASTA");
        when(foodRepository.findById(4L)).thenReturn(Optional.of(managedFood));
        when(userService.getCurrentUser()).thenReturn(null);

        OrderDto request = new OrderDto();
        request.setName("Unauthenticated Order");
        request.setFoods(List.of(managedFood));

        // Act / Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> orderService.createOrder(request));
        assertEquals("No authenticated user found", ex.getMessage());

        verify(orderRepository, never()).save(any());
    }
    // This proves that createOrder() rejects an otherwise-valid order (existing food IDs)
    // when there is no authenticated user, throwing IllegalStateException per
    // OrderServiceImpl.java:84-86, after the food-existence loop but before any save() call.
}

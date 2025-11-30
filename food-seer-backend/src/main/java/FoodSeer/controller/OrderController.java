package FoodSeer.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import FoodSeer.dto.OrderDto;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.service.OrderService;

/**
 * Controller for Orders in the FoodSeer system.
 * Provides endpoints for managing and fulfilling food orders.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    Logger logger = LoggerFactory.getLogger(OrderController.class);
    /** Connection to OrderService */
    @Autowired
    private OrderService orderService;

    /**
     * Retrieves all orders in the system.
     *
     * @return JSON list of all orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping
    public List<OrderDto> getOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Retrieves all fulfilled orders.
     *
     * @return JSON list of fulfilled orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping("/fulfilledOrders")
    public List<OrderDto> getFulfilledOrders() {
        return orderService.getAllFulfilledOrders();
    }

    /**
     * Retrieves all unfulfilled orders.
     *
     * @return JSON list of unfulfilled orders
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'DRIVER')")
    @GetMapping("/unfulfilledOrders")
    public List<OrderDto> getUnfulfilledOrders() {
        return orderService.getAllUnfulfilledOrders();
    }

    /**
     * Creates a new order.
     *
     * @param orderDto the order to create
     * @return ResponseEntity containing the created order
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody final OrderDto orderDto) {
        final OrderDto savedOrderDto = orderService.createOrder(orderDto);
        return ResponseEntity.ok(savedOrderDto);
    }

    /**
     * Marks an order as fulfilled.
     *
     * @param orderDto the order to fulfill
     * @return ResponseEntity with status depending on fulfillment result
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @PostMapping("/fulfillOrder")
    public ResponseEntity<OrderDto> fulfillOrder(@RequestBody final OrderDto orderDto) {
        try {
            orderService.getOrderById(orderDto.getId());
        } catch (final ResourceNotFoundException e) {
            return new ResponseEntity<>(orderDto, HttpStatus.PRECONDITION_FAILED);
        }

        final OrderDto existingOrder = orderService.getOrderById(orderDto.getId());

        if (existingOrder.getIsFulfilled()) {
            return new ResponseEntity<>(orderDto, HttpStatus.GONE);
        }

        try {
            final OrderDto updatedOrder = orderService.fulfillOrder(existingOrder.getId());
            return ResponseEntity.ok(updatedOrder);
        } catch (final Exception e) {
            return new ResponseEntity<>(orderDto, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Retrieves a specific order by ID.
     *
     * @param id the ID of the order
     * @return ResponseEntity containing the order or 404 if not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable("id") final Long id) {
        try {
            final OrderDto orderDto = orderService.getOrderById(id);
            return ResponseEntity.ok(orderDto);
        } catch (final ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    /**
     * Retrieves all orders for the current authenticated user.
     *
     * @return JSON list of current user's orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders")
    public List<OrderDto> getMyOrders() {
        return orderService.getCurrentUserOrders();
    }

    /**
     * Retrieves fulfilled orders for the current authenticated user.
     *
     * @return JSON list of current user's fulfilled orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders/fulfilled")
    public List<OrderDto> getMyFulfilledOrders() {
        return orderService.getCurrentUserFulfilledOrders();
    }

    /**
     * Retrieves unfulfilled orders for the current authenticated user.
     *
     * @return JSON list of current user's unfulfilled orders
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my-orders/unfulfilled")
    public List<OrderDto> getMyUnfulfilledOrders() {
        return orderService.getCurrentUserUnfulfilledOrders();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable("id") final Long id){
        logger.info("Updating order status for order ID: {}", id);
        final OrderDto updatedOrderDto = orderService.updateOrder(id);
        return ResponseEntity.ok(updatedOrderDto);
    }
}

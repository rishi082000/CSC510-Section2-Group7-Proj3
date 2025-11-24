package FoodSeer.dto;

import java.util.ArrayList;
import java.util.HashSet; // Added import
import java.util.List;
import java.util.Set;     // Added import

import FoodSeer.entity.Food;

/**
 * Data Transfer Object for Order.
 * Used to transfer Order data between the client and server.
 */
public class OrderDto {

    /** Order ID */
    private Long id;

    /** Order name */
    private String name;

    /** List of foods in the order */
    private List<Food> foods;

    /** Boolean used to track if the order has been fulfilled */
    private boolean isFulfilled;

    /**
     * Set of IDs representing foods in this order that have already been rated.
     * Used by frontend to disable rating buttons.
     */
    private Set<Long> ratedFoodIds;

    /**
     * Default constructor for OrderDto.
     */
    public OrderDto() {
        this.foods = new ArrayList<>();
        this.isFulfilled = false;
        this.ratedFoodIds = new HashSet<>(); // Initialize to avoid NullPointerException
    }

    /**
     * Creates an OrderDto with the given ID and name.
     *
     * @param id   the order ID
     * @param name the order name
     */
    public OrderDto(final Long id, final String name) {
        this.id = id;
        this.name = name;
        this.foods = new ArrayList<>();
        this.isFulfilled = false;
        this.ratedFoodIds = new HashSet<>();
    }

    /**
     * Gets the order ID.
     *
     * @return the order ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the order ID.
     *
     * @param id the order ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the order name.
     *
     * @return the order name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the order name.
     *
     * @param name the order name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Adds a food item to the order.
     *
     * @param food the food to add
     */
    public void addFood(final Food food) {
        this.foods.add(food);
    }

    /**
     * Gets the list of foods in the order.
     *
     * @return the list of foods
     */
    public List<Food> getFoods() {
        return this.foods;
    }

    /**
     * Sets the list of foods in the order.
     *
     * @param foods the list of foods
     */
    public void setFoods(final List<Food> foods) {
        this.foods = foods;
    }

    /**
     * Checks if the order is fulfilled.
     *
     * @return true if fulfilled, false otherwise
     */
    public boolean getIsFulfilled() {
        return isFulfilled;
    }

    /**
     * Sets whether the order is fulfilled.
     *
     * @param isFulfilled true if fulfilled, false otherwise
     */
    public void setIsFulfilled(final boolean isFulfilled) {
        this.isFulfilled = isFulfilled;
    }

    // --- NEW GETTERS AND SETTERS ---

    /**
     * Gets the list of IDs for foods that have already been rated.
     *
     * @return Set of Long IDs
     */
    public Set<Long> getRatedFoodIds() {
        return ratedFoodIds;
    }

    /**
     * Sets the list of rated food IDs.
     *
     * @param ratedFoodIds the set of IDs
     */
    public void setRatedFoodIds(final Set<Long> ratedFoodIds) {
        this.ratedFoodIds = ratedFoodIds;
    }
}
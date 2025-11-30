package FoodSeer.dto;

import java.math.BigDecimal;

public record DriverStatsDto(String username, int totalDeliveries, BigDecimal totalEarning, BigDecimal averageRating, int activeOrders) {
}

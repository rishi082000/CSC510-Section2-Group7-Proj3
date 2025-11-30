package FoodSeer.mapper;

import FoodSeer.dto.DriverStatsDto;
import FoodSeer.entity.DriverStats;

public class DriverStatsMapper {

    public static DriverStatsDto mapToDriverStatsDto(final DriverStats driverStats) {
        if (driverStats == null) {
            return null;
        }
        return new DriverStatsDto(driverStats.getUsername(), driverStats.getTotalDeliveries(),
                driverStats.getTotalEarnings(), driverStats.getAverageRating(), driverStats.getActiveOrders());

    }

}

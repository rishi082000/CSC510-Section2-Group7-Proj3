package FoodSeer.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "driver_stats")
@Data
public class DriverStats {

    @Id
    private String username;

    private int totalDeliveries;
    private BigDecimal totalEarnings;
    private BigDecimal averageRating;
    private int activeOrders;
}

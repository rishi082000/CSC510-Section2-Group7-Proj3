package FoodSeer.repositories;

import FoodSeer.entity.DriverStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverStatsRepository extends JpaRepository<DriverStats, String> {
    Optional<DriverStats> findByUsername(String username);
}

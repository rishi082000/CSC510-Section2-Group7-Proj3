package FoodSeer.controller;

import FoodSeer.dto.DriverStatsDto;
import FoodSeer.entity.DriverStats;
import FoodSeer.service.DriverStatsService;
import FoodSeer.service.impl.DriverStatsImpl;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/driverStats")
public class DriverStatsController {

    @Autowired
    DriverStatsService driverStatsService;

    @GetMapping
    public ResponseEntity<?> getDriverStats(@RequestParam String username) {

        DriverStatsDto driverStatsDto = driverStatsService.getDriverStats(username);
        return ResponseEntity.ok().body(driverStatsDto);
    }
}

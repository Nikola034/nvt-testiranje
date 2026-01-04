package nvt.backend.services.vehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import nvt.backend.dto.vehicle.AvailabilityStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class VehicleAvailabilityWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(VehicleAvailabilityWebSocketService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final VehicleService vehicleService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Set<Long> subscribedVehicles = ConcurrentHashMap.newKeySet();

    public void subscribeToVehicle(Long vehicleId) {
        subscribedVehicles.add(vehicleId);
        log.info("Vehicle {} subscribed for real-time availability updates", vehicleId);
    }

    public void unsubscribeFromVehicle(Long vehicleId) {
        subscribedVehicles.remove(vehicleId);
        log.info("Vehicle {} unsubscribed from real-time availability updates", vehicleId);
    }

    @Scheduled(fixedRate = 30000)
    public void pushAvailabilityUpdates() {
        log.info("Checking for subscribed vehicles: {}", subscribedVehicles.size());
        for (Long vehicleId : subscribedVehicles) {
            try {
                Instant end = Instant.now();
                Instant start = end.minus(3, ChronoUnit.HOURS);

                AvailabilityStatisticsDTO stats = vehicleService.getAvailabilityStatistics(vehicleId, start, end);

                messagingTemplate.convertAndSend(
                        "/topic/vehicle/" + vehicleId + "/availability",
                        stats
                );
                log.info("Pushed availability update for vehicle {}", vehicleId);
            } catch (Exception e) {
                log.error("Failed to push availability update for vehicle {}", vehicleId, e);
            }
        }
    }

    public void notifyAvailabilityChange(Long vehicleId, boolean online) {
        if (subscribedVehicles.contains(vehicleId)) {
            try {
                Map<String, Object> notification = Map.of(
                        "vehicleId", vehicleId,
                        "online", online,
                        "timestamp", Instant.now().toString()
                );
                messagingTemplate.convertAndSend(
                        "/topic/vehicle/" + vehicleId + "/availability/status",
                        notification
                );
            } catch (Exception e) {
                log.error("Failed to notify availability change for vehicle {}", vehicleId, e);
            }
        }
    }
}

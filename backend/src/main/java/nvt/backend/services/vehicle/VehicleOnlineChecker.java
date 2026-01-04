package nvt.backend.services.vehicle;

import nvt.backend.model.vehicle.VehicleLocation;
import nvt.backend.repositories.vehicle.VehicleLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VehicleOnlineChecker {

    private static final Logger log = LoggerFactory.getLogger(VehicleOnlineChecker.class);

    private final VehicleLocationRepository locationRepository;
    private final VehicleTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final int heartbeatTimeoutSeconds;

    public VehicleOnlineChecker(
            VehicleLocationRepository locationRepository,
            VehicleTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate,
            @Value("${vehicle.heartbeat.timeout:60}") int heartbeatTimeoutSeconds) {
        this.locationRepository = locationRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
        this.heartbeatTimeoutSeconds = heartbeatTimeoutSeconds;
    }

    @Scheduled(fixedRateString = "${vehicle.heartbeat.check-interval:15000}")
    @Transactional
    public void checkVehicleStatus() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(heartbeatTimeoutSeconds);
        
        List<VehicleLocation> staleVehicles = locationRepository.findStaleOnlineVehicles(threshold);
        
        for (VehicleLocation location : staleVehicles) {
            log.info("Vehicle {} marked as offline due to heartbeat timeout", location.getVehicle().getId());
            
            location.setOnline(false);
            locationRepository.save(location);
            
            telemetryService.recordHeartbeat(
                    location.getVehicle().getId(),
                    location.getVehicle().getLicensePlate(),
                    false
            );
            
            telemetryService.recordStateChange(
                    location.getVehicle().getId(),
                    location.getVehicle().getLicensePlate(),
                    "OFFLINE_TIMEOUT"
            );

            Map<String, Object> notification = new HashMap<>();
            notification.put("vehicleId", location.getVehicle().getId());
            notification.put("online", false);
            notification.put("reason", "HEARTBEAT_TIMEOUT");
            notification.put("timestamp", LocalDateTime.now().toString());
            
            messagingTemplate.convertAndSend(
                    "/topic/vehicle/" + location.getVehicle().getId() + "/status",
                    notification
            );
        }
        
        if (!staleVehicles.isEmpty()) {
            log.info("Marked {} vehicles as offline due to heartbeat timeout", staleVehicles.size());
        }
    }
}

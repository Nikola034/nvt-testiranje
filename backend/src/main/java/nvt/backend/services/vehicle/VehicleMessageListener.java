package nvt.backend.services.vehicle;

import nvt.backend.config.RabbitMQConfig;
import nvt.backend.dto.vehicle.VehicleHeartbeatDTO;
import nvt.backend.dto.vehicle.VehicleStateDTO;
import nvt.backend.dto.vehicle.VehicleTelemetryDTO;
import nvt.backend.model.vehicle.Vehicle;
import nvt.backend.model.vehicle.VehicleLocation;
import nvt.backend.repositories.vehicle.VehicleLocationRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class VehicleMessageListener {

    private static final Logger log = LoggerFactory.getLogger(VehicleMessageListener.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;
    private final VehicleTelemetryService telemetryService;
    private final SimpMessagingTemplate messagingTemplate;

    public VehicleMessageListener(
            VehicleRepository vehicleRepository,
            VehicleLocationRepository locationRepository,
            VehicleTelemetryService telemetryService,
            SimpMessagingTemplate messagingTemplate) {
        this.vehicleRepository = vehicleRepository;
        this.locationRepository = locationRepository;
        this.telemetryService = telemetryService;
        this.messagingTemplate = messagingTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.HEARTBEAT_QUEUE)
    @Transactional
    public void handleHeartbeat(VehicleHeartbeatDTO heartbeat) {
        try {
            log.info("Received heartbeat from vehicle {}: {}", heartbeat.getVehicleId(), heartbeat.getStatus());

            VehicleLocation location = getOrCreateLocation(heartbeat.getVehicleId());
            location.setLastHeartbeat(parseTimestamp(heartbeat.getTimestamp()));
            location.setOnline(true);
            locationRepository.save(location);

            telemetryService.recordHeartbeat(heartbeat.getVehicleId(), heartbeat.getLicensePlate(), true);

            messagingTemplate.convertAndSend("/topic/vehicle/" + heartbeat.getVehicleId() + "/heartbeat", heartbeat);

        } catch (Exception e) {
            log.error("Failed to process heartbeat message: {}", heartbeat, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.TELEMETRY_QUEUE)
    @Transactional
    public void handleTelemetry(VehicleTelemetryDTO telemetry) {
        try {
            log.info("Received telemetry from vehicle {}: lat={}, lon={}, distance={}",
                    telemetry.getVehicleId(), telemetry.getLatitude(),
                    telemetry.getLongitude(), telemetry.getDistanceTraveled());

            VehicleLocation location = getOrCreateLocation(telemetry.getVehicleId());
            location.setLatitude(telemetry.getLatitude());
            location.setLongitude(telemetry.getLongitude());
            location.setLastTelemetryUpdate(parseTimestamp(telemetry.getTimestamp()));

            Double currentTotal = location.getTotalDistance() != null ? location.getTotalDistance() : 0.0;
            Double newTotal = currentTotal + telemetry.getDistanceTraveled();
            location.setTotalDistance(newTotal);

            locationRepository.save(location);

            telemetryService.recordDistance(
                    telemetry.getVehicleId(),
                    telemetry.getLicensePlate(),
                    telemetry.getDistanceTraveled()
            );

            telemetryService.recordLocation(
                    telemetry.getVehicleId(),
                    telemetry.getLicensePlate(),
                    telemetry.getLatitude(),
                    telemetry.getLongitude()
            );

            messagingTemplate.convertAndSend("/topic/vehicle/" + telemetry.getVehicleId() + "/telemetry", telemetry);

        } catch (Exception e) {
            log.error("Failed to process telemetry message: {}", telemetry, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.STATE_QUEUE)
    @Transactional
    public void handleStateChange(VehicleStateDTO state) {
        try {
            log.info("Received state change from vehicle {}: {}", state.getVehicleId(), state.getState());

            VehicleLocation location = getOrCreateLocation(state.getVehicleId());
            location.setCurrentState(state.getState());

            if ("STOPPED".equals(state.getState())) {
                location.setOnline(false);
                telemetryService.recordHeartbeat(state.getVehicleId(), state.getLicensePlate(), false);
            }

            locationRepository.save(location);

            telemetryService.recordStateChange(state.getVehicleId(), state.getLicensePlate(), state.getState());

            messagingTemplate.convertAndSend("/topic/vehicle/" + state.getVehicleId() + "/state", state);

        } catch (Exception e) {
            log.error("Failed to process state change message: {}", state, e);
        }
    }

    private VehicleLocation getOrCreateLocation(Long vehicleId) {
        return locationRepository.findByVehicleId(vehicleId)
                .orElseGet(() -> {
                    Vehicle vehicle = vehicleRepository.findById(vehicleId)
                            .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));
                    VehicleLocation newLocation = new VehicleLocation();
                    newLocation.setVehicle(vehicle);
                    newLocation.setOnline(false);
                    newLocation.setTotalDistance(0.0);
                    return newLocation;
                });
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
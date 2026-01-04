package nvt.backend.controllers.vehicle;

import nvt.backend.dto.vehicle.VehicleStatusDTO;
import nvt.backend.model.vehicle.Vehicle;
import nvt.backend.model.vehicle.VehicleLocation;
import nvt.backend.repositories.vehicle.VehicleLocationRepository;
import nvt.backend.repositories.vehicle.VehicleRepository;
import nvt.backend.services.vehicle.VehicleTelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vehicles/tracking")
public class VehicleTrackingController {

    private final VehicleRepository vehicleRepository;
    private final VehicleLocationRepository locationRepository;
    private final VehicleTelemetryService telemetryService;

    public VehicleTrackingController(
            VehicleRepository vehicleRepository,
            VehicleLocationRepository locationRepository,
            VehicleTelemetryService telemetryService) {
        this.vehicleRepository = vehicleRepository;
        this.locationRepository = locationRepository;
        this.telemetryService = telemetryService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getAllVehicleStatus() {
        List<Vehicle> vehicles = vehicleRepository.findAllWithDetails();
        
        List<VehicleStatusDTO> statuses = vehicles.stream()
                .map(this::buildStatusDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/status/{vehicleId}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<VehicleStatusDTO> getVehicleStatus(@PathVariable Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdWithDetails(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        return ResponseEntity.ok(buildStatusDTO(vehicle));
    }

    @GetMapping("/online")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleStatusDTO>> getOnlineVehicles() {
        List<VehicleLocation> onlineLocations = locationRepository.findAllOnline();
        
        List<VehicleStatusDTO> statuses = onlineLocations.stream()
                .map(location -> buildStatusDTO(location.getVehicle()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/{vehicleId}/availability")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAvailabilityHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getAvailabilityHistory(vehicleId, range));
    }

    @GetMapping("/{vehicleId}/distance")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getDistanceHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getDistanceHistory(vehicleId, range));
    }

    @GetMapping("/{vehicleId}/states")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getStateHistory(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "-24h") String range) {
        return ResponseEntity.ok(telemetryService.getStateHistory(vehicleId, range));
    }

    private VehicleStatusDTO buildStatusDTO(Vehicle vehicle) {
        VehicleLocation location = locationRepository.findByVehicleId(vehicle.getId())
                .orElse(null);
        
        VehicleStatusDTO.VehicleStatusDTOBuilder builder = VehicleStatusDTO.builder()
                .vehicleId(vehicle.getId())
                .licensePlate(vehicle.getLicensePlate())
                .brandName(vehicle.getBrand() != null ? vehicle.getBrand().getName() : null)
                .modelName(vehicle.getModel() != null ? vehicle.getModel().getName() : null);
        
        if (location != null) {
            builder.online(location.isOnline())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .totalDistance(location.getTotalDistance())
                    .lastHeartbeat(location.getLastHeartbeat())
                    .lastTelemetry(location.getLastTelemetryUpdate())
                    .currentState(location.getCurrentState());
        } else {
            builder.online(false);
        }
        
        return builder.build();
    }
}

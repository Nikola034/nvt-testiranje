package nvt.backend.controllers.vehicle;

import lombok.RequiredArgsConstructor;
import nvt.backend.dto.common.PageResponseDTO;
import nvt.backend.dto.vehicle.*;
import nvt.backend.model.vehicle.VehicleBrand;
import nvt.backend.model.vehicle.VehicleModel;
import nvt.backend.services.vehicle.VehicleService;
import nvt.backend.services.vehicle.VehicleTelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final VehicleTelemetryService telemetryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> create(
            @RequestPart("data") CreateVehicleDTO dto,
            @RequestPart("images") List<MultipartFile> images) {
        try {
            if (images == null || images.isEmpty()) {
                return ResponseEntity.badRequest().body("At least one image is required");
            }
            VehicleResponseDTO response = vehicleService.create(dto, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleListDTO>> getAll() {
        return ResponseEntity.ok(vehicleService.getAll());
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<VehicleListDTO>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(vehicleService.getAllPaged(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehicleService.getById(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleListDTO>> search(@RequestParam String query) {
        return ResponseEntity.ok(vehicleService.search(query));
    }

    @GetMapping("/search/paged")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponseDTO<VehicleListDTO>> searchPaged(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(vehicleService.searchPaged(query, page, size));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestPart("data") UpdateVehicleDTO dto,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages) {
        try {
            VehicleResponseDTO response = vehicleService.update(id, dto, newImages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            vehicleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/brands")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleBrand>> getAllBrands() {
        return ResponseEntity.ok(vehicleService.getAllBrands());
    }

    @GetMapping("/brands/{brandId}/models")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<List<VehicleModel>> getModelsByBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(vehicleService.getModelsByBrand(brandId));
    }

    @GetMapping("/{id}/location")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getLastLocation(@PathVariable Long id) {
        try {
            VehicleLocationDTO location = vehicleService.getLastLocation(id);
            if (location == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No location data available for this vehicle");
            }
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/distance/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getDistanceStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            LocalDate start;
            LocalDate end = LocalDate.now();

            if (period != null) {
                start = switch (period.toLowerCase()) {
                    case "week" -> end.minusWeeks(1);
                    case "month" -> end.minusMonths(1);
                    case "3months" -> end.minusMonths(3);
                    case "6months" -> end.minusMonths(6);
                    case "year" -> end.minusYears(1);
                    default -> throw new IllegalArgumentException("Invalid period. Use: week, month, 3months, 6months, year");
                };
            } else if (startDate != null && endDate != null) {
                if (endDate.isBefore(startDate)) {
                    return ResponseEntity.badRequest().body("End date must be after start date");
                }
                long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
                if (daysBetween > 365) {
                    return ResponseEntity.badRequest().body("Date range cannot exceed one year");
                }
                start = startDate;
                end = endDate;
            } else {
                start = end.minusWeeks(1);
            }

            DistanceStatisticsDTO stats = vehicleService.getDistanceStatistics(id, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/availability/stats")
    @PreAuthorize("hasAnyAuthority('MANAGER', 'ADMIN')")
    public ResponseEntity<?> getAvailabilityStatistics(
            @PathVariable Long id,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        try {
            Instant start;
            Instant end = Instant.now();

            if (period != null) {
                start = switch (period.toLowerCase()) {
                    case "1h" -> end.minus(1, ChronoUnit.HOURS);
                    case "3h" -> end.minus(3, ChronoUnit.HOURS);
                    case "12h" -> end.minus(12, ChronoUnit.HOURS);
                    case "24h" -> end.minus(24, ChronoUnit.HOURS);
                    case "week" -> end.minus(7, ChronoUnit.DAYS);
                    case "month" -> end.minus(30, ChronoUnit.DAYS);
                    case "3months" -> end.minus(90, ChronoUnit.DAYS);
                    case "year" -> end.minus(365, ChronoUnit.DAYS);
                    default -> throw new IllegalArgumentException("Invalid period. Use: 1h, 3h, 12h, 24h, week, month, 3months, year");
                };
            } else if (startTime != null && endTime != null) {
                if (endTime.isBefore(startTime)) {
                    return ResponseEntity.badRequest().body("End time must be after start time");
                }
                long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);
                if (daysBetween > 365) {
                    return ResponseEntity.badRequest().body("Date range cannot exceed one year");
                }
                start = startTime;
                end = endTime;
            } else {
                start = end.minus(24, ChronoUnit.HOURS);
            }

            AvailabilityStatisticsDTO stats = vehicleService.getAvailabilityStatistics(id, start, end);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
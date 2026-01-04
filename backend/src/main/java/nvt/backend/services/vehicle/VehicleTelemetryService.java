package nvt.backend.services.vehicle;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import nvt.backend.dto.vehicle.AvailabilityStatisticsDTO;
import nvt.backend.dto.vehicle.DistanceStatisticsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VehicleTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(VehicleTelemetryService.class);
    private static final int MAX_DATA_POINTS = 100;

    private final InfluxDBClient influxDBClient;
    private final QueryApi queryApi;
    private final String org;
    private final String bucket;

    public VehicleTelemetryService(
            InfluxDBClient influxDBClient,
            @Value("${influxdb.org}") String org,
            @Value("${influxdb.bucket}") String bucket) {
        this.influxDBClient = influxDBClient;
        this.queryApi = influxDBClient.getQueryApi();
        this.org = org;
        this.bucket = bucket;
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordHeartbeat(Long vehicleId, String licensePlate, boolean online) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_availability")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("online", online ? 1 : 0)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded heartbeat for vehicle {}: online={}", vehicleId, online);
        } catch (Exception e) {
            log.error("Failed to record heartbeat for vehicle {}", vehicleId, e);
        }
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordStateChange(Long vehicleId, String licensePlate, String state) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_state")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("state", state)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded state change for vehicle {}: state={}", vehicleId, state);
        } catch (Exception e) {
            log.error("Failed to record state change for vehicle {}", vehicleId, e);
        }
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordDistance(Long vehicleId, String licensePlate, Double distance) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_distance")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("distance", distance)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded distance for vehicle {}: distance={}", vehicleId, distance);
        } catch (Exception e) {
            log.error("Failed to record distance for vehicle {}", vehicleId, e);
        }
    }

    @org.springframework.scheduling.annotation.Async("telemetryExecutor")
    public void recordLocation(Long vehicleId, String licensePlate, Double latitude, Double longitude) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            Point point = Point.measurement("vehicle_location")
                    .addTag("vehicleId", String.valueOf(vehicleId))
                    .addTag("licensePlate", licensePlate)
                    .addField("latitude", latitude)
                    .addField("longitude", longitude)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(point);
            log.debug("Recorded location for vehicle {}: lat={}, lon={}", vehicleId, latitude, longitude);
        } catch (Exception e) {
            log.error("Failed to record location for vehicle {}", vehicleId, e);
        }
    }

    public List<Map<String, Object>> getAvailabilityHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_availability\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> aggregateWindow(every: 1h, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: true) " +
                            "|> limit(n: %d)",
                    bucket, range, vehicleId, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("online", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query availability history for vehicle {}", vehicleId, e);
        }
        return results;
    }

    public List<Map<String, Object>> getDistanceHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_distance\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> aggregateWindow(every: 1h, fn: sum, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: true) " +
                            "|> limit(n: %d)",
                    bucket, range, vehicleId, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("distance", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query distance history for vehicle {}", vehicleId, e);
        }
        return results;
    }

    public List<Map<String, Object>> getStateHistory(Long vehicleId, String range) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_state\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> sort(columns: [\"_time\"], desc: true) " +
                            "|> limit(n: %d)",
                    bucket, range, vehicleId, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("state", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query state history for vehicle {}", vehicleId, e);
        }
        return results;
    }

    @Cacheable(value = "distanceStats", key = "#vehicleId + '-' + #startDate + '-' + #endDate")
    public DistanceStatisticsDTO getAggregatedDistance(Long vehicleId, String licensePlate, 
                                                        LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        
        String aggregationType;
        String windowPeriod;
        
        if (daysBetween <= 14) {
            aggregationType = "daily";
            windowPeriod = "1d";
        } else if (daysBetween <= 90) {
            aggregationType = "weekly";
            windowPeriod = "1w";
        } else {
            aggregationType = "monthly";
            windowPeriod = "1mo";
        }

        List<DistanceStatisticsDTO.DistanceDataPoint> dataPoints = new ArrayList<>();
        double totalDistance = 0.0;

        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_distance\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"distance\") " +
                            "|> aggregateWindow(every: %s, fn: sum, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTimeStr, endTimeStr, vehicleId, windowPeriod, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            DateTimeFormatter labelFormatter = switch (aggregationType) {
                case "daily" -> DateTimeFormatter.ofPattern("MMM dd");
                case "weekly" -> DateTimeFormatter.ofPattern("MMM dd");
                default -> DateTimeFormatter.ofPattern("MMM yyyy");
            };

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    if (record.getTime() != null && record.getValue() != null) {
                        LocalDate pointDate = record.getTime().atZone(ZoneId.systemDefault()).toLocalDate();
                        Double distance = ((Number) record.getValue()).doubleValue();
                        totalDistance += distance;

                        LocalDate pointEndDate = switch (aggregationType) {
                            case "daily" -> pointDate;
                            case "weekly" -> pointDate.plusDays(6);
                            default -> pointDate.plusMonths(1).minusDays(1);
                        };

                        String label = switch (aggregationType) {
                            case "weekly" -> pointDate.format(labelFormatter) + " - " + pointEndDate.format(labelFormatter);
                            default -> pointDate.format(labelFormatter);
                        };

                        dataPoints.add(DistanceStatisticsDTO.DistanceDataPoint.builder()
                                .label(label)
                                .startDate(pointDate)
                                .endDate(pointEndDate)
                                .distance(distance)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query aggregated distance for vehicle {}", vehicleId, e);
        }

        return DistanceStatisticsDTO.builder()
                .vehicleId(vehicleId)
                .licensePlate(licensePlate)
                .startDate(startDate)
                .endDate(endDate)
                .aggregationType(aggregationType)
                .totalDistance(totalDistance)
                .dataPoints(dataPoints)
                .build();
    }

    public Double getTotalDistanceInRange(Long vehicleId, LocalDate startDate, LocalDate endDate) {
        try {
            String startTimeStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString();
            String endTimeStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString();

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_distance\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"distance\") " +
                            "|> sum()",
                    bucket, startTimeStr, endTimeStr, vehicleId
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    if (record.getValue() != null) {
                        return ((Number) record.getValue()).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query total distance for vehicle {}", vehicleId, e);
        }
        return 0.0;
    }

    @Cacheable(value = "availabilityStats", key = "#vehicleId + '-' + #startTime.toEpochMilli() + '-' + #endTime.toEpochMilli()")
    public AvailabilityStatisticsDTO getAggregatedAvailability(Long vehicleId, String licensePlate,
                                                                Instant startTime, Instant endTime) {
        Duration totalDuration = Duration.between(startTime, endTime);
        long totalHours = totalDuration.toHours();

        String aggregationType;
        Duration windowDuration;

        if (totalHours <= 24) {
            aggregationType = "hourly";
            windowDuration = Duration.ofHours(1);
        } else if (totalHours <= 720) {
            aggregationType = "daily";
            windowDuration = Duration.ofDays(1);
        } else {
            aggregationType = "weekly";
            windowDuration = Duration.ofDays(7);
        }

        List<AvailabilityStatisticsDTO.AvailabilityDataPoint> dataPoints = new ArrayList<>();
        long totalOnlineSeconds = 0;
        long totalOfflineSeconds = 0;

        try {
            String windowPeriod = aggregationType.equals("hourly") ? "1h" :
                    aggregationType.equals("daily") ? "1d" : "1w";

            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_availability\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                            "|> sort(columns: [\"_time\"], desc: false) " +
                            "|> limit(n: %d)",
                    bucket, startTime.toString(), endTime.toString(), vehicleId, windowPeriod, MAX_DATA_POINTS
            );

            List<FluxTable> tables = queryApi.query(flux, org);

            DateTimeFormatter labelFormatter = switch (aggregationType) {
                case "hourly" -> DateTimeFormatter.ofPattern("MMM dd HH:mm").withZone(ZoneId.systemDefault());
                case "daily" -> DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());
                default -> DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());
            };

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    if (record.getTime() != null) {
                        Instant pointTime = record.getTime();
                        Instant pointEndTime = pointTime.plus(windowDuration);
                        if (pointEndTime.isAfter(endTime)) {
                            pointEndTime = endTime;
                        }

                        long windowSeconds = Duration.between(pointTime, pointEndTime).getSeconds();
                        double onlineRatio = 0.0;
                        if (record.getValue() != null) {
                            onlineRatio = ((Number) record.getValue()).doubleValue();
                        }

                        long onlineSeconds = (long) (windowSeconds * onlineRatio);
                        long offlineSeconds = windowSeconds - onlineSeconds;

                        totalOnlineSeconds += onlineSeconds;
                        totalOfflineSeconds += offlineSeconds;

                        double onlinePct = windowSeconds > 0 ? (onlineSeconds * 100.0 / windowSeconds) : 0;
                        double offlinePct = windowSeconds > 0 ? (offlineSeconds * 100.0 / windowSeconds) : 0;

                        String label = switch (aggregationType) {
                            case "weekly" -> labelFormatter.format(pointTime) + " - " + labelFormatter.format(pointEndTime);
                            default -> labelFormatter.format(pointTime);
                        };

                        dataPoints.add(AvailabilityStatisticsDTO.AvailabilityDataPoint.builder()
                                .label(label)
                                .startTime(pointTime)
                                .endTime(pointEndTime)
                                .onlineSeconds(onlineSeconds)
                                .offlineSeconds(offlineSeconds)
                                .onlinePercentage(Math.round(onlinePct * 100.0) / 100.0)
                                .offlinePercentage(Math.round(offlinePct * 100.0) / 100.0)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to query aggregated availability for vehicle {}", vehicleId, e);
        }

        long totalSeconds = totalOnlineSeconds + totalOfflineSeconds;
        double overallOnlinePct = totalSeconds > 0 ? (totalOnlineSeconds * 100.0 / totalSeconds) : 0;
        double overallOfflinePct = totalSeconds > 0 ? (totalOfflineSeconds * 100.0 / totalSeconds) : 0;

        return AvailabilityStatisticsDTO.builder()
                .vehicleId(vehicleId)
                .licensePlate(licensePlate)
                .startTime(startTime)
                .endTime(endTime)
                .aggregationType(aggregationType)
                .totalOnlineSeconds(totalOnlineSeconds)
                .totalOfflineSeconds(totalOfflineSeconds)
                .onlinePercentage(Math.round(overallOnlinePct * 100.0) / 100.0)
                .offlinePercentage(Math.round(overallOfflinePct * 100.0) / 100.0)
                .dataPoints(dataPoints)
                .build();
    }

    public List<Map<String, Object>> getRawAvailabilityData(Long vehicleId, Instant startTime, Instant endTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String flux = String.format(
                    "from(bucket: \"%s\") " +
                            "|> range(start: %s, stop: %s) " +
                            "|> filter(fn: (r) => r._measurement == \"vehicle_availability\") " +
                            "|> filter(fn: (r) => r.vehicleId == \"%d\") " +
                            "|> filter(fn: (r) => r._field == \"online\") " +
                            "|> sort(columns: [\"_time\"], desc: false)",
                    bucket, startTime.toString(), endTime.toString(), vehicleId
            );

            QueryApi queryApi = influxDBClient.getQueryApi();
            List<FluxTable> tables = queryApi.query(flux, org);

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("time", record.getTime());
                    entry.put("online", record.getValue());
                    results.add(entry);
                }
            }
        } catch (Exception e) {
            log.error("Failed to query raw availability for vehicle {}", vehicleId, e);
        }
        return results;
    }
}
# Load Testing for Student 3 Functionalities

This directory contains load testing scripts for Student 3's functionalities using Locust.

## Tested Endpoints (12 endpoints)

### Vehicle Management (5.9)
1. `GET /api/v1/vehicles` - Get all vehicles
2. `GET /api/v1/vehicles/search?query=` - Search vehicles
3. `GET /api/v1/vehicles/{id}` - Get vehicle by ID
4. `GET /api/v1/vehicles/brands` - Get all vehicle brands
5. `POST /api/v1/vehicles` - Create vehicle (multipart with images)

### Vehicle Telemetry (5.12)
6. `GET /api/v1/vehicles/{id}/location` - Get vehicle last location
7. `GET /api/v1/vehicles/{id}/distance/stats` - Get distance statistics

### Vehicle Availability (5.15 - Bonus)
8. `GET /api/v1/vehicles/{id}/availability/stats` - Get availability statistics

### Registration Requests (5.4, 5.5)
9. `GET /api/v1/registration-requests/all` - Get all registration requests
10. `GET /api/v1/registration-requests/pending` - Get pending requests
11. `GET /api/v1/registration-requests/{id}` - Get request by ID
12. `PUT /api/v1/registration-requests/{id}/process` - Process registration request

## Setup

```bash
cd load-testing
pip install -r requirements.txt
```

## Bulk Data Generation

Generate test data for load testing and final defense:

```bash
# Generate PostgreSQL data (full scale - ~3M users, 100K vehicles)
python generate_bulk_data.py --vehicles 100000 --managers 1000000 --customers 2000000 --requests 2000000

# Generate smaller test set
python generate_bulk_data.py --vehicles 1000 --managers 10000 --customers 20000 --requests 20000

# Generate InfluxDB vehicle telemetry data (100 vehicles, 5 years, ~26M records)
python generate_bulk_data.py --influx-vehicles 100 --influx-years 5

# Generate everything at moderate scale
python generate_bulk_data.py --vehicles 10000 --managers 100000 --customers 200000 --requests 200000 --influx-vehicles 100 --influx-years 5
```

### Data Generation Targets

**PostgreSQL (Relational DB):**
- 100,000 delivery vehicles
- 1,000,000 managers
- 2,000,000 customers
- 2,000,000 registration requests (companies)

**InfluxDB (Time Series DB):**
- Vehicle distance/availability data: 100 vehicles × 5 years × 365.25 days × 144 readings/day = ~26.3M records

## Running Load Tests

### Web UI Mode
```bash
locust -f locustfile.py --host=http://localhost:8080
```
Then open http://localhost:8089 in your browser.

### Headless Mode (CLI)
```bash
# 10 users
locust -f locustfile.py --headless -u 10 -r 2 -t 1m --host=http://localhost:8080

# 100 users
locust -f locustfile.py --headless -u 100 -r 10 -t 5m --host=http://localhost:8080

# 1000 users
locust -f locustfile.py --headless -u 1000 -r 50 -t 10m --host=http://localhost:8080
```

### Parameters
- `-u`: Number of users
- `-r`: Spawn rate (users per second)
- `-t`: Test duration
- `--host`: Target host

## Task Weights

Read-heavy operations with the following task weights:
- Get Vehicle By ID: 20
- Search Vehicles: 15
- Get Vehicle Location: 15
- Create Vehicle (with image upload): 10
- Get All Vehicles: 10
- Get Distance Statistics: 10
- Get Availability Statistics: 10
- Get Pending Requests: 10
- Get Request By ID: 10
- Get All Requests: 5
- Get Vehicle Brands: 5
- Process Registration Request: 2

## Expected Results

The system should handle:
- 10 concurrent users with <100ms response time
- 100 concurrent users with <500ms response time
- 1000 concurrent users with <2s response time

## Notes

- Ensure the backend is running before starting tests
- Ensure test data is populated (vehicles, registration requests)
- Manager credentials: admin/admin123 (or update in locustfile.py)

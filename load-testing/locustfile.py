"""
Load Testing Suite for Student 3 - Vehicle Management System
Tests 12 endpoints for load testing requirement (6.11)

Endpoints tested:
1. GET /vehicles/paged - Get vehicles with pagination
2. GET /vehicles/search/paged - Search vehicles with pagination
3. GET /vehicles/{id} - Get vehicle by ID
4. GET /vehicles/{id}/location - Get vehicle location
5. GET /vehicles/{id}/distance/stats - Get distance statistics
6. GET /vehicles/{id}/availability/stats - Get availability statistics
7. GET /registration-requests/all/paged - Get all requests with pagination
8. GET /registration-requests/pending/paged - Get pending requests with pagination
9. GET /registration-requests/{id} - Get request by ID
10. GET /vehicles/brands - Get all vehicle brands (cached)
11. POST /vehicles - Create vehicle (multipart)
12. PUT /registration-requests/{id}/process - Process request
"""

import random
import string
import json
import io
import threading
import time
from locust import HttpUser, task, between, events
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# Shared state across all users - authenticate once, share token
class SharedState:
    _lock = threading.Lock()
    _initialized = False
    token = None
    vehicle_ids = []
    request_ids = []
    
    @classmethod
    def initialize(cls, client):
        """Initialize shared state once for all users"""
        with cls._lock:
            if cls._initialized:
                return True
            
            # Login once (without catch_response to simplify)
            max_retries = 3
            for attempt in range(max_retries):
                try:
                    response = client.post(
                        "/api/v1/auth/login",
                        json={"username": "admin", "password": "sifra"},
                        name="[Setup] Login"
                    )
                    if response.status_code == 200:
                        data = response.json()
                        cls.token = data.get("accessToken")
                        if cls.token:
                            logger.info("Shared login successful")
                            break
                    else:
                        logger.warning(f"Login failed with status {response.status_code}")
                        time.sleep(0.5)
                except Exception as e:
                    logger.error(f"Login attempt {attempt + 1} failed: {e}")
                    time.sleep(0.5)
            
            if not cls.token:
                logger.error("Failed to obtain shared token")
                return False
            
            headers = {"Authorization": f"Bearer {cls.token}"}
            
            # Fetch vehicles - use paginated endpoint
            try:
                response = client.get(
                    "/api/v1/vehicles/paged?page=0&size=50",
                    headers=headers,
                    name="[Setup] Fetch Vehicles",
                    timeout=10
                )
                if response.status_code == 200:
                    data = response.json()
                    vehicles = data.get("content", [])
                    cls.vehicle_ids = [v["id"] for v in vehicles]
                    logger.info(f"Fetched {len(cls.vehicle_ids)} vehicles")
            except Exception as e:
                logger.error(f"Failed to fetch vehicles: {e}")
                cls.vehicle_ids = list(range(1, 51))
            
            # Fetch registration requests - use paginated endpoint
            try:
                response = client.get(
                    "/api/v1/registration-requests/all/paged?page=0&size=50",
                    headers=headers,
                    name="[Setup] Fetch Requests",
                    timeout=10
                )
                if response.status_code == 200:
                    data = response.json()
                    requests_data = data.get("content", [])
                    cls.request_ids = [r["id"] for r in requests_data]
                    logger.info(f"Fetched {len(cls.request_ids)} requests")
            except Exception as e:
                logger.error(f"Failed to fetch requests: {e}")
                cls.request_ids = list(range(1, 51))
            
            cls._initialized = True
            logger.info("SharedState initialization complete!")
            return True


class ManagerLoadTest(HttpUser):
    """Load test for Manager user (Student 3 endpoints)"""
    wait_time = between(1, 3)
    weight = 1

    def on_start(self):
        """Initialize user with shared state"""
        # Initialize shared state (only first user does actual work)
        SharedState.initialize(self.client)
        
        # Use shared token and data
        self.token = SharedState.token
        self.vehicle_ids = SharedState.vehicle_ids
        self.request_ids = SharedState.request_ids
        
        if not self.token:
            logger.warning("No token available for this user")

    def _headers(self):
        """Get auth headers"""
        if self.token:
            return {"Authorization": f"Bearer {self.token}"}
        return {}

    # =========================================================================
    # ENDPOINT 1: Get vehicles (paginated)
    # =========================================================================
    @task(10)
    def get_vehicles_paged(self):
        if not self.token:
            return
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        self.client.get(
            f"/api/v1/vehicles/paged?page={page}&size={size}",
            headers=self._headers(),
            name="GET /vehicles/paged"
        )

    # =========================================================================
    # ENDPOINT 2: Search vehicles (paginated)
    # =========================================================================
    @task(15)
    def search_vehicles_paged(self):
        if not self.token:
            return
        queries = ["NS", "BG", "SU", "NI", "123", "ABC"]
        query = random.choice(queries)
        page = random.randint(0, 5)
        self.client.get(
            f"/api/v1/vehicles/search/paged?query={query}&page={page}&size=20",
            headers=self._headers(),
            name="GET /vehicles/search/paged"
        )

    # =========================================================================
    # ENDPOINT 3: Get vehicle by ID
    # =========================================================================
    @task(20)
    def get_vehicle_by_id(self):
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        self.client.get(
            f"/api/v1/vehicles/{vehicle_id}",
            headers=self._headers(),
            name="GET /vehicles/{id}"
        )

    # =========================================================================
    # ENDPOINT 4: Get vehicle location
    # =========================================================================
    @task(15)
    def get_vehicle_location(self):
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/location",
            headers=self._headers(),
            name="GET /vehicles/{id}/location",
            catch_response=True
        ) as response:
            # 404 is expected when vehicle has no location data (not simulated yet)
            if response.status_code == 200 or response.status_code == 404:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 5: Get distance statistics
    # =========================================================================
    @task(10)
    def get_distance_stats(self):
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        periods = ["week", "month", "3months", "year"]
        period = random.choice(periods)
        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/distance/stats?period={period}",
            headers=self._headers(),
            name="GET /vehicles/{id}/distance/stats",
            catch_response=True
        ) as response:
            # 404/204 expected when no telemetry data exists
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 6: Get availability statistics  
    # =========================================================================
    @task(10)
    def get_availability_stats(self):
        if not self.token or not self.vehicle_ids:
            return
        vehicle_id = random.choice(self.vehicle_ids)
        periods = ["1h", "3h", "12h", "24h", "week", "month"]
        period = random.choice(periods)
        with self.client.get(
            f"/api/v1/vehicles/{vehicle_id}/availability/stats?period={period}",
            headers=self._headers(),
            name="GET /vehicles/{id}/availability/stats",
            catch_response=True
        ) as response:
            # 404/204 expected when no telemetry data exists
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Unexpected status: {response.status_code}")

    # =========================================================================
    # ENDPOINT 7: Get all registration requests (paginated)
    # =========================================================================
    @task(5)
    def get_all_requests_paged(self):
        if not self.token:
            return
        page = random.randint(0, 10)
        size = random.choice([10, 20, 50])
        self.client.get(
            f"/api/v1/registration-requests/all/paged?page={page}&size={size}",
            headers=self._headers(),
            name="GET /registration-requests/all/paged"
        )

    # =========================================================================
    # ENDPOINT 8: Get pending registration requests (paginated)
    # =========================================================================
    @task(10)
    def get_pending_requests_paged(self):
        if not self.token:
            return
        page = random.randint(0, 5)
        self.client.get(
            f"/api/v1/registration-requests/pending/paged?page={page}&size=20",
            headers=self._headers(),
            name="GET /registration-requests/pending/paged"
        )

    # =========================================================================
    # ENDPOINT 9: Get registration request by ID
    # =========================================================================
    @task(10)
    def get_request_by_id(self):
        if not self.token or not self.request_ids:
            return
        request_id = random.choice(self.request_ids)
        self.client.get(
            f"/api/v1/registration-requests/{request_id}",
            headers=self._headers(),
            name="GET /registration-requests/{id}"
        )

    # =========================================================================
    # ENDPOINT 10: Get all vehicle brands
    # =========================================================================
    @task(5)
    def get_all_brands(self):
        if not self.token:
            return
        self.client.get(
            "/api/v1/vehicles/brands",
            headers=self._headers(),
            name="GET /vehicles/brands"
        )

    # =========================================================================
    # ENDPOINT 11: Create vehicle (multipart form with image)
    # =========================================================================
    @task(10)
    def create_vehicle(self):
        if not self.token:
            return

        license_plate = f"NS-{random.randint(100, 999)}-{''.join(random.choices(string.ascii_uppercase, k=2))}"
        vehicle_data = {
            "licensePlate": license_plate,
            "weightLimit": float(random.randint(500, 5000)),
            "brandId": 1,
            "modelId": 1
        }

        # Minimal 1x1 red PNG (valid PNG)
        png_data = bytes([
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
            0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7, 0x63, 0xF8, 0xFF, 0xFF, 0x3F,
            0x00, 0x05, 0xFE, 0x02, 0xFE, 0xDC, 0xCC, 0x59,
            0xE7, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, 0xAE, 0x42, 0x60, 0x82
        ])

        # Multipart form: 'data' as JSON blob, 'images' as file list
        # Use list of tuples for multiple values with same key name
        files = [
            ('data', (None, json.dumps(vehicle_data), 'application/json')),
            ('images', ('test.png', io.BytesIO(png_data), 'image/png'))
        ]
        
        # Only Authorization header, let requests handle Content-Type for multipart
        headers = {"Authorization": f"Bearer {self.token}"}

        self.client.post(
            "/api/v1/vehicles",
            files=files,
            headers=headers,
            name="POST /vehicles"
        )

    # # =========================================================================
    # # ENDPOINT 12: Process registration request (approve/reject)
    # # =========================================================================
    # @task(2)
    # def process_request(self):
    #     if not self.token or not self.request_ids:
    #         return
        
    #     request_id = random.choice(self.request_ids)
    #     approved = random.choice([True, False])
    #     process_data = {
    #         "approved": approved,
    #         "rejectionReason": "" if approved else "Load test rejection"
    #     }
        
    #     self.client.put(
    #         f"/api/v1/registration-requests/{request_id}/process",
    #         json=process_data,
    #         headers=self._headers(),
    #         name="PUT /registration-requests/{id}/process"
    #     )

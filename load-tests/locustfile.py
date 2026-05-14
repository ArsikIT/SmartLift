import random
import time
from threading import Lock

from locust import HttpUser, between, task


PASSWORD = "password123"


class TestData:
    lock = Lock()
    ready = False
    prefix = f"lt{int(time.time())}"
    manufacturer = {}
    service = {}
    management = {}
    lift_ids = []
    maintenance_ids = []


class SmartLiftUser(HttpUser):
    wait_time = between(1, 3)
    setup_timeout_seconds = 60

    def on_start(self):
        self.ensure_seed_data()

    def ensure_seed_data(self):
        if TestData.ready:
            return

        with TestData.lock:
            if TestData.ready:
                return

            manufacturer = self.register_and_login("manufacturer", "MANUFACTURER")
            service = self.register_and_login("service", "SERVICE")
            management = self.register_and_login("management", "MANAGEMENT")

            TestData.manufacturer = manufacturer
            TestData.service = service
            TestData.management = management

            for index in range(1, 6):
                lift_id = self.create_lift(index)
                TestData.lift_ids.append(lift_id)

            TestData.ready = True

    def register_and_login(self, suffix, org_type):
        username = f"{TestData.prefix}_{suffix}"
        email = f"{username}@test.com"
        org_name = f"{TestData.prefix}_{suffix}_org"

        register_payload = {
            "username": username,
            "email": email,
            "password": PASSWORD,
            "organizationName": org_name,
            "organizationType": org_type,
        }

        with self.client.post(
            "/api/auth/register",
            json=register_payload,
            name="POST /api/auth/register [setup]",
            catch_response=True,
        ) as response:
            if response.status_code != 201:
                response.failure(
                    f"Registration failed for {suffix}: {response.status_code} {response.text}"
                )
                raise RuntimeError("Load-test setup registration failed")
            organization = response.json()["organization"]

        login_payload = {
            "username": username,
            "password": PASSWORD,
        }

        with self.client.post(
            "/api/auth/login",
            json=login_payload,
            name="POST /api/auth/login [setup]",
            catch_response=True,
        ) as response:
            if response.status_code != 200:
                response.failure(
                    f"Login failed for {suffix}: {response.status_code} {response.text}"
                )
                raise RuntimeError("Load-test setup login failed")
            auth = response.json()

        return {
            "username": username,
            "password": PASSWORD,
            "token": auth["token"],
            "userId": auth["userId"],
            "organizationId": organization["id"],
        }

    def create_lift(self, index):
        serial = f"{TestData.prefix}-LIFT-{index:03d}"
        payload = {
            "serialNumber": serial,
            "model": "Load Test Model",
            "manufacturer": "SmartLift QA",
            "manufacturerOrganizationId": TestData.manufacturer["organizationId"],
            "serviceOrganizationId": TestData.service["organizationId"],
            "managementOrganizationId": TestData.management["organizationId"],
        }
        headers = self.auth_headers(TestData.manufacturer["token"])

        with self.client.post(
            "/api/lifts",
            json=payload,
            headers=headers,
            name="POST /api/lifts [setup]",
            catch_response=True,
        ) as response:
            if response.status_code != 201:
                response.failure(
                    f"Lift seed failed: {response.status_code} {response.text}"
                )
                raise RuntimeError("Load-test setup lift creation failed")
            return response.json()["id"]

    @staticmethod
    def auth_headers(token):
        return {"Authorization": f"Bearer {token}"}

    @staticmethod
    def random_lift_id():
        deadline = time.time() + SmartLiftUser.setup_timeout_seconds
        while not TestData.lift_ids:
            if time.time() > deadline:
                raise RuntimeError("Timed out waiting for seeded lift IDs")
            time.sleep(0.1)
        return random.choice(TestData.lift_ids)


class AuthUser(SmartLiftUser):
    weight = 1

    @task
    def login(self):
        payload = {
            "username": TestData.management["username"],
            "password": TestData.management["password"],
        }
        self.client.post("/api/auth/login", json=payload, name="POST /api/auth/login")


class ReadOnlyApiUser(SmartLiftUser):
    weight = 5

    def on_start(self):
        super().on_start()
        self.token = TestData.management["token"]

    @task(4)
    def list_lifts(self):
        self.client.get(
            "/api/lifts?page=0&size=20",
            headers=self.auth_headers(self.token),
            name="GET /api/lifts",
        )

    @task(3)
    def get_lift(self):
        lift_id = self.random_lift_id()
        self.client.get(
            f"/api/lifts/{lift_id}",
            headers=self.auth_headers(self.token),
            name="GET /api/lifts/{id}",
        )

    @task(2)
    def list_events_by_lift(self):
        lift_id = self.random_lift_id()
        self.client.get(
            f"/api/events?liftId={lift_id}&page=0&size=20",
            headers=self.auth_headers(self.token),
            name="GET /api/events?liftId",
        )

    @task(2)
    def list_maintenances_by_lift(self):
        lift_id = self.random_lift_id()
        self.client.get(
            f"/api/maintenances?liftId={lift_id}&page=0&size=20",
            headers=self.auth_headers(self.token),
            name="GET /api/maintenances?liftId",
        )


class ServiceWorkflowUser(SmartLiftUser):
    weight = 3

    def on_start(self):
        super().on_start()
        self.token = TestData.service["token"]
        self.user_id = TestData.service["userId"]

    @task(2)
    def create_event(self):
        lift_id = self.random_lift_id()
        payload = {
            "liftId": lift_id,
            "type": random.choice(["FAULT", "REPAIR", "ACTIVATED"]),
            "description": f"Load-test event for lift {lift_id}",
            "performedByUserId": self.user_id,
        }
        self.client.post(
            "/api/events",
            json=payload,
            headers=self.auth_headers(self.token),
            name="POST /api/events",
        )

    @task(2)
    def create_maintenance(self):
        lift_id = self.random_lift_id()
        payload = {
            "liftId": lift_id,
            "title": f"Load-test maintenance {int(time.time() * 1000)}",
            "description": "Synthetic maintenance request generated by Locust",
            "status": "PENDING",
            "assignedTechnicianId": self.user_id,
            "requestedByUserId": self.user_id,
        }
        with self.client.post(
            "/api/maintenances",
            json=payload,
            headers=self.auth_headers(self.token),
            name="POST /api/maintenances",
            catch_response=True,
        ) as response:
            if response.status_code == 201:
                maintenance_id = response.json()["id"]
                with TestData.lock:
                    TestData.maintenance_ids.append(maintenance_id)
                response.success()

    @task(1)
    def update_maintenance(self):
        with TestData.lock:
            if not TestData.maintenance_ids:
                return
            maintenance_id = random.choice(TestData.maintenance_ids)

        lift_id = self.random_lift_id()
        payload = {
            "liftId": lift_id,
            "title": f"Updated maintenance {maintenance_id}",
            "description": "Synthetic maintenance update generated by Locust",
            "status": random.choice(["IN_PROGRESS", "DONE"]),
            "assignedTechnicianId": self.user_id,
            "requestedByUserId": self.user_id,
        }
        self.client.put(
            f"/api/maintenances/{maintenance_id}",
            json=payload,
            headers=self.auth_headers(self.token),
            name="PUT /api/maintenances/{id}",
        )

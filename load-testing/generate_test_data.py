import requests
import random
import string
import time
import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:8080/api/v1"

def random_string(length=8):
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))

def random_license_plate():
    cities = ["NS", "BG", "SU", "NI", "KG", "PA", "ZR", "SO"]
    return f"{random.choice(cities)}-{random.randint(100, 999)}-{random_string(2)}"

def login(username, password):
    response = requests.post(f"{BASE_URL}/auth/login", json={
        "username": username,
        "password": password
    })
    if response.status_code == 200:
        return response.json().get("accessToken")
    return None

def get_brands(token):
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/vehicles/brands", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def get_models(token, brand_id):
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/vehicles/brands/{brand_id}/models", headers=headers)
    if response.status_code == 200:
        return response.json()
    return []

def create_vehicle(token, brand_id, model_id, index):
    headers = {"Authorization": f"Bearer {token}"}

    data = {
        "licensePlate": random_license_plate(),
        "weightLimit": random.randint(500, 5000),
        "brandId": brand_id,
        "modelId": model_id
    }

    dummy_image = b'\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82'

    files = {
        'data': (None, str(data).replace("'", '"'), 'application/json'),
        'images': ('test.png', dummy_image, 'image/png')
    }

    import json
    files = [
        ('data', (None, json.dumps(data), 'application/json')),
        ('images', ('test.png', dummy_image, 'image/png'))
    ]

    response = requests.post(f"{BASE_URL}/vehicles", headers=headers, files=files)
    return response.status_code == 201

def generate_vehicles(token, count):
    brands = get_brands(token)
    if not brands:
        print("No brands found. Please seed the database first.")
        return

    brand_models = {}
    for brand in brands:
        models = get_models(token, brand["id"])
        if models:
            brand_models[brand["id"]] = [m["id"] for m in models]

    if not brand_models:
        print("No models found. Please seed the database first.")
        return

    success = 0
    failed = 0

    print(f"Generating {count} vehicles...")

    for i in range(count):
        brand_id = random.choice(list(brand_models.keys()))
        model_id = random.choice(brand_models[brand_id])

        if create_vehicle(token, brand_id, model_id, i):
            success += 1
        else:
            failed += 1

        if (i + 1) % 10 == 0:
            print(f"Progress: {i + 1}/{count} (Success: {success}, Failed: {failed})")

    print(f"\nCompleted: {success} vehicles created, {failed} failed")

def main():
    parser = argparse.ArgumentParser(description='Generate test data for load testing')
    parser.add_argument('--vehicles', type=int, default=100, help='Number of vehicles to create')
    parser.add_argument('--username', type=str, default='admin', help='Manager username')
    parser.add_argument('--password', type=str, default='admin123', help='Manager password')
    args = parser.parse_args()

    print("Logging in...")
    token = login(args.username, args.password)
    if not token:
        print("Login failed. Check credentials.")
        return

    print("Login successful.")
    generate_vehicles(token, args.vehicles)

if __name__ == "__main__":
    main()

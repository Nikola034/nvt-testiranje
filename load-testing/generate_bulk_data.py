"""
Bulk Data Generation Script for Load Testing and Final Defense

This script generates the required test data based on the existing system models:

Relational Database (PostgreSQL):
- 100,000 delivery vehicles (with brands/models)
- 1,000,000 managers
- 2,000,000 customers

Time Series Database (InfluxDB):
- Vehicle distance data: 100 vehicles * 5 years * 365.25 days * 144 readings/day = ~26.3M records

Usage:
    python generate_bulk_data.py --help
    python generate_bulk_data.py --vehicles 100000 --managers 1000000 --customers 2000000
    python generate_bulk_data.py --influx-vehicles 100 --influx-years 5
"""

import argparse
import random
import string
import time
import sys
import os
from datetime import datetime, timedelta
from concurrent.futures import ThreadPoolExecutor, as_completed
import threading

# PostgreSQL
import psycopg2
from psycopg2.extras import execute_values

# InfluxDB
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# Configuration
POSTGRES_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "smartly",
    "user": "postgres",
    "password": "admin"
}

INFLUX_CONFIG = {
    "url": "http://localhost:8086",
    "token": "smartly-influx-token",
    "org": "smartly",
    "bucket": "vehicle_telemetry"
}

# Bcrypt hash for password "sifra"
PASSWORD_HASH = "$2a$10$vYTIHEUfK0xyiSp1q8EMwuELaXDFp0VnHdkHUqzg5AvSTkz6VPZku"

# Serbian names for realistic data
FIRST_NAMES = ["Marko", "Stefan", "Nikola", "Aleksandar", "Luka", "Vuk", "Miloš", "Nemanja", "Petar", "Filip",
               "Ana", "Marija", "Jovana", "Milica", "Sara", "Teodora", "Jelena", "Ivana", "Nina", "Maja"]
LAST_NAMES = ["Petrović", "Nikolić", "Jovanović", "Marković", "Đorđević", "Stojanović", "Ilić", "Stanković",
              "Pavlović", "Milošević", "Kovačević", "Popović", "Tomić", "Živković", "Kostić"]
CITIES = ["NS", "BG", "SU", "NI", "KG", "PA", "ZR", "SO", "VR", "SM", "KI", "JA", "ČA", "VA", "PO"]


def random_string(length=8):
    return ''.join(random.choices(string.ascii_uppercase + string.digits, k=length))


def random_license_plate():
    city = random.choice(CITIES)
    return f"{city}-{random.randint(100, 999)}-{random_string(2)}"


def random_name():
    return random.choice(FIRST_NAMES)


def random_surname():
    return random.choice(LAST_NAMES)


def get_postgres_connection():
    return psycopg2.connect(**POSTGRES_CONFIG)


def get_influx_client():
    return InfluxDBClient(
        url=INFLUX_CONFIG["url"],
        token=INFLUX_CONFIG["token"],
        org=INFLUX_CONFIG["org"]
    )


# =============================================================================
# PostgreSQL Data Generation
# =============================================================================

def ensure_brands_and_models(conn):
    """Ensure vehicle brands and models exist, return mapping"""
    cur = conn.cursor()
    
    # Fix sequences if they're out of sync
    cur.execute("""
        SELECT setval('vehicle_brands_id_seq', COALESCE((SELECT MAX(id) FROM vehicle_brands), 0) + 1, false)
    """)
    cur.execute("""
        SELECT setval('vehicle_models_id_seq', COALESCE((SELECT MAX(id) FROM vehicle_models), 0) + 1, false)
    """)
    conn.commit()
    
    brands_models = {
        "Mercedes-Benz": ["Sprinter", "Vito", "Citan", "Actros"],
        "Volkswagen": ["Crafter", "Transporter", "Caddy"],
        "Ford": ["Transit", "Transit Custom", "Ranger"],
        "Iveco": ["Daily", "Eurocargo", "S-Way"],
        "MAN": ["TGE", "TGL", "TGM", "TGX"],
        "Renault": ["Master", "Trafic", "Kangoo"],
        "Fiat": ["Ducato", "Talento", "Doblo"],
        "Peugeot": ["Boxer", "Expert", "Partner"],
        "Citroen": ["Jumper", "Dispatch", "Berlingo"],
        "Opel": ["Movano", "Vivaro", "Combo"]
    }
    
    brand_id_map = {}
    model_id_map = {}
    
    # First, get all existing brands and models
    cur.execute("SELECT id, name FROM vehicle_brands")
    existing_brands = {row[1]: row[0] for row in cur.fetchall()}
    
    cur.execute("SELECT id, name, brand_id FROM vehicle_models")
    existing_models = {(row[1], row[2]): row[0] for row in cur.fetchall()}
    
    for brand_name, models in brands_models.items():
        if brand_name in existing_brands:
            brand_id = existing_brands[brand_name]
        else:
            cur.execute("INSERT INTO vehicle_brands (name) VALUES (%s) RETURNING id", (brand_name,))
            brand_id = cur.fetchone()[0]
            existing_brands[brand_name] = brand_id
        brand_id_map[brand_name] = brand_id
        
        for model_name in models:
            key = (model_name, brand_id)
            if key in existing_models:
                model_id = existing_models[key]
            else:
                cur.execute("INSERT INTO vehicle_models (name, brand_id) VALUES (%s, %s) RETURNING id",
                           (model_name, brand_id))
                model_id = cur.fetchone()[0]
                existing_models[key] = model_id
            model_id_map[(brand_name, model_name)] = model_id
    
    conn.commit()
    print(f"Ensured {len(brand_id_map)} brands and {len(model_id_map)} models exist")
    return brand_id_map, model_id_map


def generate_vehicles(conn, count, brand_id_map, model_id_map, batch_size=10000):
    """Generate vehicles in bulk using batch inserts"""
    cur = conn.cursor()
    
    brands_list = list(brand_id_map.keys())
    
    print(f"Generating {count:,} vehicles...")
    start_time = time.time()
    
    # Get current max ID
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM vehicles")
    start_id = cur.fetchone()[0] + 1
    
    total_inserted = 0
    batch_data = []
    used_plates = set()
    
    # Get existing plates
    cur.execute("SELECT license_plate FROM vehicles")
    for row in cur.fetchall():
        used_plates.add(row[0])
    
    for i in range(count):
        # Generate unique license plate
        while True:
            plate = random_license_plate()
            if plate not in used_plates:
                used_plates.add(plate)
                break
        
        brand = random.choice(brands_list)
        brand_id = brand_id_map[brand]
        
        # Get models for this brand
        brand_models = [(k, v) for k, v in model_id_map.items() if k[0] == brand]
        model_key, model_id = random.choice(brand_models)
        
        weight_limit = random.uniform(500, 5000)
        created_at = datetime.now()
        
        batch_data.append((plate, weight_limit, brand_id, model_id, created_at))
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO vehicles (license_plate, weight_limit, brand_id, model_id, created_at) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed
            print(f"  Vehicles: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO vehicles (license_plate, weight_limit, brand_id, model_id, created_at) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    elapsed = time.time() - start_time
    print(f"  Completed: {total_inserted:,} vehicles in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    return total_inserted


def generate_users(conn, count, user_type, role, authorities, batch_size=10000):
    """Generate users (managers or customers) in bulk"""
    cur = conn.cursor()
    
    print(f"Generating {count:,} {user_type}s...")
    start_time = time.time()
    
    total_inserted = 0
    batch_data = []
    
    # Get existing usernames to avoid duplicates
    cur.execute("SELECT username FROM \"user\"")
    used_usernames = set(row[0] for row in cur.fetchall())
    
    # Get the current max ID and update the id_generator
    cur.execute("SELECT COALESCE(MAX(id), 0) FROM \"user\"")
    current_max_id = cur.fetchone()[0]
    
    # Update the id_generator table
    cur.execute("""
        UPDATE id_generator SET next_val = %s WHERE sequence_name = 'user'
    """, (current_max_id + 1,))
    conn.commit()
    
    next_id = current_max_id + 1
    
    for i in range(count):
        name = random_name()
        surname = random_surname()
        
        # Generate unique username
        while True:
            username = f"{name.lower()}.{surname.lower()}.{random.randint(1, 999999)}"
            if username not in used_usernames:
                used_usernames.add(username)
                break
        
        batch_data.append((
            next_id,
            name,
            surname,
            username,
            PASSWORD_HASH,
            role,
            authorities,
            True,  # active
            user_type
        ))
        next_id += 1
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO "user" (id, name, surname, username, password, role, authorities, active, user_type) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f"  {user_type}s: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO "user" (id, name, surname, username, password, role, authorities, active, user_type) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    # Update the id_generator for next time
    cur.execute("""
        UPDATE id_generator SET next_val = %s WHERE sequence_name = 'user'
    """, (next_id,))
    conn.commit()
    
    elapsed = time.time() - start_time
    print(f"  Completed: {total_inserted:,} {user_type}s in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    return total_inserted


def generate_registration_requests(conn, count, batch_size=10000):
    """Generate registration requests (representing companies)"""
    cur = conn.cursor()
    
    print(f"Generating {count:,} registration requests (companies)...")
    start_time = time.time()
    
    # Get city and country IDs
    cur.execute("SELECT id FROM cities LIMIT 10")
    city_ids = [row[0] for row in cur.fetchall()]
    if not city_ids:
        city_ids = [1]  # Default
    
    cur.execute("SELECT id FROM countries LIMIT 5")
    country_ids = [row[0] for row in cur.fetchall()]
    if not country_ids:
        country_ids = [1]  # Default
    
    # Get user IDs for owners
    cur.execute("SELECT id FROM \"user\" WHERE user_type = 'Customer' LIMIT 1000")
    owner_ids = [row[0] for row in cur.fetchall()]
    if not owner_ids:
        owner_ids = [1]  # Default to admin
    
    total_inserted = 0
    batch_data = []
    
    company_types = ["DOO", "AD", "PR", "SZR", "KD"]
    streets = ["Bulevar oslobođenja", "Futoška", "Laze Telečkog", "Zmaj Jovina", "Miletica",
               "Narodnih heroja", "Cara Dušana", "Temerinska", "Kisačka", "Novosadskog sajma"]
    
    for i in range(count):
        company_name = f"{random_surname()} {random.choice(company_types)} {random.randint(1, 9999)}"
        city_id = random.choice(city_ids)
        country_id = random.choice(country_ids)
        street = random.choice(streets)
        street_number = str(random.randint(1, 200))
        latitude = 45.2 + random.uniform(-0.5, 0.5)
        longitude = 19.8 + random.uniform(-0.5, 0.5)
        status = random.choice(["APPROVED", "APPROVED", "APPROVED"])
        # status = random.choice(["APPROVED", "APPROVED", "APPROVED", "PENDING", "REJECTED"])
        owner_id = random.choice(owner_ids)
        created_at = datetime.now() - timedelta(days=random.randint(0, 365))
        
        batch_data.append((
            company_name,
            city_id,
            country_id,
            street,
            street_number,
            latitude,
            longitude,
            status,
            owner_id,
            created_at
        ))
        
        if len(batch_data) >= batch_size:
            execute_values(
                cur,
                """INSERT INTO registration_requests 
                   (name, city_id, country_id, street, street_number, latitude, longitude, status, owner_id, created_at) 
                   VALUES %s""",
                batch_data
            )
            conn.commit()
            total_inserted += len(batch_data)
            elapsed = time.time() - start_time
            rate = total_inserted / elapsed if elapsed > 0 else 0
            print(f"  Requests: {total_inserted:,}/{count:,} ({rate:.0f}/sec)")
            batch_data = []
    
    # Insert remaining
    if batch_data:
        execute_values(
            cur,
            """INSERT INTO registration_requests 
               (name, city_id, country_id, street, street_number, latitude, longitude, status, owner_id, created_at) 
               VALUES %s""",
            batch_data
        )
        conn.commit()
        total_inserted += len(batch_data)
    
    elapsed = time.time() - start_time
    print(f"  Completed: {total_inserted:,} requests in {elapsed:.1f}s ({total_inserted/elapsed:.0f}/sec)")
    return total_inserted


# =============================================================================
# InfluxDB Data Generation
# =============================================================================

def generate_influx_vehicle_data(num_vehicles=100, years=5, readings_per_day=144):
    """
    Generate vehicle telemetry data for InfluxDB using existing vehicles from PostgreSQL
    
    Target: 100 vehicles * 5 years * 365.25 days * 144 readings/day = ~26.3M records
    """
    # First, fetch existing vehicles from PostgreSQL
    try:
        conn = get_postgres_connection()
        cur = conn.cursor()
        cur.execute("""
            SELECT id, license_plate FROM vehicles 
            ORDER BY id 
            LIMIT %s
        """, (num_vehicles,))
        vehicles = cur.fetchall()
        conn.close()
        
        if not vehicles:
            print("  ✗ No vehicles found in database. Generate vehicles first with --vehicles")
            return 0
        
        if len(vehicles) < num_vehicles:
            print(f"  ⚠ Only {len(vehicles)} vehicles exist in database (requested {num_vehicles})")
        
        print(f"  Found {len(vehicles)} existing vehicles in PostgreSQL")
        
    except Exception as e:
        print(f"  ✗ Failed to fetch vehicles from PostgreSQL: {e}")
        return 0
    
    client = get_influx_client()
    write_api = client.write_api(write_options=SYNCHRONOUS)
    bucket = INFLUX_CONFIG["bucket"]
    org = INFLUX_CONFIG["org"]
    
    # Calculate time range
    end_time = datetime.now()
    start_time = end_time - timedelta(days=int(years * 365.25))
    
    # Time between readings (in seconds)
    seconds_per_reading = 86400 / readings_per_day  # 600 seconds = 10 minutes for 144/day
    
    actual_num_vehicles = len(vehicles)
    total_readings = int(actual_num_vehicles * years * 365.25 * readings_per_day)
    print(f"Generating ~{total_readings:,} InfluxDB records for {actual_num_vehicles} vehicles over {years} years...")
    
    batch_size = 5000
    points = []
    total_written = 0
    start_gen_time = time.time()
    
    for idx, (vehicle_id, license_plate) in enumerate(vehicles, 1):
        # Starting position (random location in Serbia)
        lat = 44.0 + random.uniform(0, 2)
        lon = 19.5 + random.uniform(0, 2)
        total_distance = 0.0
        
        current_time = start_time
        daily_distance = 0.0
        
        while current_time < end_time:
            # Simulate realistic patterns
            hour = current_time.hour
            
            # Vehicles more active during work hours (6am-10pm)
            if 6 <= hour <= 22:
                is_online = random.random() < 0.8
                distance = random.uniform(0.5, 5.0) if is_online else 0.0
            else:
                is_online = random.random() < 0.2
                distance = random.uniform(0.1, 1.0) if is_online else 0.0
            
            total_distance += distance
            daily_distance += distance
            
            # Update position
            if is_online and distance > 0:
                lat += random.uniform(-0.01, 0.01)
                lon += random.uniform(-0.01, 0.01)
                # Keep within bounds
                lat = max(42.0, min(46.2, lat))
                lon = max(18.8, min(23.0, lon))
            
            timestamp = current_time
            
            # Create distance point
            point_distance = Point("vehicle_distance") \
                .tag("vehicleId", str(vehicle_id)) \
                .tag("licensePlate", license_plate) \
                .field("distance", distance) \
                .time(timestamp, WritePrecision.MS)
            points.append(point_distance)
            
            # Create availability point
            point_availability = Point("vehicle_availability") \
                .tag("vehicleId", str(vehicle_id)) \
                .tag("licensePlate", license_plate) \
                .field("online", 1 if is_online else 0) \
                .time(timestamp, WritePrecision.MS)
            points.append(point_availability)
            
            # Create location point (less frequently, every 10 readings)
            if len(points) % 20 == 0:
                point_location = Point("vehicle_location") \
                    .tag("vehicleId", str(vehicle_id)) \
                    .tag("licensePlate", license_plate) \
                    .field("latitude", lat) \
                    .field("longitude", lon) \
                    .time(timestamp, WritePrecision.MS)
                points.append(point_location)
            
            # Write batch
            if len(points) >= batch_size:
                try:
                    write_api.write(bucket=bucket, org=org, record=points)
                    total_written += len(points)
                    elapsed = time.time() - start_gen_time
                    rate = total_written / elapsed if elapsed > 0 else 0
                    progress = (idx - 1) / actual_num_vehicles * 100 + \
                              ((current_time - start_time).total_seconds() / 
                               (end_time - start_time).total_seconds()) / actual_num_vehicles * 100
                    print(f"  InfluxDB: {total_written:,} records ({rate:.0f}/sec) - {progress:.1f}% complete")
                except Exception as e:
                    print(f"  Error writing batch: {e}")
                points = []
            
            current_time += timedelta(seconds=seconds_per_reading)
        
        print(f"  Vehicle {idx}/{actual_num_vehicles} (ID: {vehicle_id}, Plate: {license_plate}) - Total distance: {total_distance:.1f} km")
    
    # Write remaining points
    if points:
        try:
            write_api.write(bucket=bucket, org=org, record=points)
            total_written += len(points)
        except Exception as e:
            print(f"  Error writing final batch: {e}")
    
    elapsed = time.time() - start_gen_time
    print(f"  Completed: {total_written:,} InfluxDB records in {elapsed:.1f}s ({total_written/elapsed:.0f}/sec)")
    
    client.close()
    return total_written


# =============================================================================
# Main
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description='Generate bulk test data for load testing and final defense',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Generate all PostgreSQL data (full scale)
  python generate_bulk_data.py --vehicles 100000 --managers 1000000 --customers 2000000

  # Generate smaller test set
  python generate_bulk_data.py --vehicles 1000 --managers 10000 --customers 20000

  # Generate only InfluxDB data (100 vehicles, 5 years)
  python generate_bulk_data.py --influx-only --influx-vehicles 100 --influx-years 5

  # Generate everything at smaller scale
  python generate_bulk_data.py --vehicles 10000 --managers 100000 --customers 200000 --requests 200000 --influx-vehicles 100 --influx-years 5
        """
    )
    
    # PostgreSQL options
    parser.add_argument('--vehicles', type=int, default=0, help='Number of vehicles to create (target: 100,000)')
    parser.add_argument('--managers', type=int, default=0, help='Number of managers to create (target: 1,000,000)')
    parser.add_argument('--customers', type=int, default=0, help='Number of customers to create (target: 2,000,000)')
    parser.add_argument('--requests', type=int, default=0, help='Number of registration requests/companies (target: 2,000,000)')
    
    # InfluxDB options
    parser.add_argument('--influx-vehicles', type=int, default=0, help='Number of vehicles for InfluxDB data (target: 100)')
    parser.add_argument('--influx-years', type=int, default=5, help='Years of historical data (default: 5)')
    parser.add_argument('--influx-readings', type=int, default=144, help='Readings per day (default: 144)')
    parser.add_argument('--influx-only', action='store_true', help='Only generate InfluxDB data')
    
    # Other options
    parser.add_argument('--batch-size', type=int, default=10000, help='Batch size for PostgreSQL inserts')
    
    args = parser.parse_args()
    
    # Check if anything was requested
    if not any([args.vehicles, args.managers, args.customers, args.requests, args.influx_vehicles]):
        parser.print_help()
        print("\n⚠️  No data generation requested. Use --help for options.")
        return
    
    print("=" * 70)
    print("BULK DATA GENERATION")
    print("=" * 70)
    
    total_start = time.time()
    
    # PostgreSQL data generation
    if not args.influx_only and any([args.vehicles, args.managers, args.customers, args.requests]):
        print("\n📊 PostgreSQL Data Generation")
        print("-" * 40)
        
        try:
            conn = get_postgres_connection()
            print("✓ Connected to PostgreSQL")
            
            # Ensure brands and models exist
            brand_id_map, model_id_map = ensure_brands_and_models(conn)
            
            if args.customers > 0:
                generate_users(conn, args.customers, "Customer", 0, "CUSTOMER", args.batch_size)
            
            if args.managers > 0:
                generate_users(conn, args.managers, "Manager", 1, "MANAGER", args.batch_size)
            
            if args.vehicles > 0:
                generate_vehicles(conn, args.vehicles, brand_id_map, model_id_map, args.batch_size)
            
            if args.requests > 0:
                generate_registration_requests(conn, args.requests, args.batch_size)
            
            conn.close()
            print("✓ PostgreSQL data generation complete")
            
        except Exception as e:
            print(f"✗ PostgreSQL error: {e}")
            import traceback
            traceback.print_exc()
    
    # InfluxDB data generation
    if args.influx_vehicles > 0:
        print("\n📈 InfluxDB Data Generation")
        print("-" * 40)
        
        try:
            expected_records = int(args.influx_vehicles * args.influx_years * 365.25 * args.influx_readings * 2.5)
            print(f"  Target: ~{expected_records:,} records")
            print(f"  Vehicles: {args.influx_vehicles}")
            print(f"  Time span: {args.influx_years} years")
            print(f"  Readings/day: {args.influx_readings}")
            
            generate_influx_vehicle_data(
                num_vehicles=args.influx_vehicles,
                years=args.influx_years,
                readings_per_day=args.influx_readings
            )
            print("✓ InfluxDB data generation complete")
            
        except Exception as e:
            print(f"✗ InfluxDB error: {e}")
            import traceback
            traceback.print_exc()
    
    total_elapsed = time.time() - total_start
    print("\n" + "=" * 70)
    print(f"✅ COMPLETE - Total time: {total_elapsed/60:.1f} minutes")
    print("=" * 70)


if __name__ == "__main__":
    main()

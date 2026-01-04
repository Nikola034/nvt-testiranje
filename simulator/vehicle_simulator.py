import argparse
import json
import os
import random
import threading
import time
from datetime import datetime, timezone

import pika
from dotenv import load_dotenv


class VehicleSimulator:
    def __init__(self, config):
        self.vehicle_id = config['vehicle_id']
        self.license_plate = config['license_plate']
        self.latitude = config['initial_latitude']
        self.longitude = config['initial_longitude']
        self.heartbeat_interval = config['heartbeat_interval']
        self.telemetry_interval = config['telemetry_interval']
        self.rabbitmq_config = config['rabbitmq']
        
        self.running = False
        self.connection = None
        self.channel = None
        self.channel_lock = threading.Lock()
        
        self.heartbeat_exchange = 'vehicle.heartbeat'
        self.telemetry_exchange = 'vehicle.telemetry'
        self.state_exchange = 'vehicle.state'

    def connect(self):
        credentials = pika.PlainCredentials(
            self.rabbitmq_config['user'],
            self.rabbitmq_config['password']
        )
        parameters = pika.ConnectionParameters(
            host=self.rabbitmq_config['host'],
            port=self.rabbitmq_config['port'],
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
        )
        
        max_retries = 5
        retry_delay = 5
        
        for attempt in range(max_retries):
            try:
                self.connection = pika.BlockingConnection(parameters)
                self.channel = self.connection.channel()
                self._setup_exchanges()
                print(f"[{self._timestamp()}] Connected to RabbitMQ")
                return True
            except pika.exceptions.AMQPConnectionError as e:
                print(f"[{self._timestamp()}] Connection attempt {attempt + 1} failed: {e}")
                if attempt < max_retries - 1:
                    time.sleep(retry_delay)
        
        return False

    def _reconnect(self):
        print(f"[{self._timestamp()}] Attempting to reconnect...")
        try:
            if self.connection and self.connection.is_open:
                self.connection.close()
        except Exception:
            pass
        return self.connect()

    def _setup_exchanges(self):
        self.channel.exchange_declare(
            exchange=self.heartbeat_exchange,
            exchange_type='topic',
            durable=True
        )
        self.channel.exchange_declare(
            exchange=self.telemetry_exchange,
            exchange_type='topic',
            durable=True
        )
        self.channel.exchange_declare(
            exchange=self.state_exchange,
            exchange_type='topic',
            durable=True
        )

    def _timestamp(self):
        return datetime.now(timezone.utc).isoformat()

    def _publish_message(self, exchange, routing_key, message):
        with self.channel_lock:
            try:
                if self.channel is None or self.channel.is_closed:
                    if not self._reconnect():
                        return False
                
                self.channel.basic_publish(
                    exchange=exchange,
                    routing_key=routing_key,
                    body=json.dumps(message),
                    properties=pika.BasicProperties(
                        delivery_mode=2,
                        content_type='application/json'
                    )
                )
                return True
            except (pika.exceptions.AMQPConnectionError, 
                    pika.exceptions.AMQPChannelError,
                    pika.exceptions.StreamLostError) as e:
                print(f"[{self._timestamp()}] Connection error: {e}")
                if self._reconnect():
                    try:
                        self.channel.basic_publish(
                            exchange=exchange,
                            routing_key=routing_key,
                            body=json.dumps(message),
                            properties=pika.BasicProperties(
                                delivery_mode=2,
                                content_type='application/json'
                            )
                        )
                        return True
                    except Exception as retry_error:
                        print(f"[{self._timestamp()}] Retry failed: {retry_error}")
                return False
            except Exception as e:
                print(f"[{self._timestamp()}] Failed to publish message: {e}")
                return False

    def send_heartbeat(self):
        message = {
            'vehicleId': self.vehicle_id,
            'licensePlate': self.license_plate,
            'timestamp': self._timestamp(),
            'status': 'ONLINE'
        }
        routing_key = f'vehicle.{self.vehicle_id}.heartbeat'
        if self._publish_message(self.heartbeat_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Heartbeat sent for vehicle {self.vehicle_id}")

    def send_telemetry(self):
        distance = round(random.uniform(0.5, 15.0), 2)
        
        self.latitude += random.uniform(-0.01, 0.01)
        self.longitude += random.uniform(-0.01, 0.01)
        
        message = {
            'vehicleId': self.vehicle_id,
            'licensePlate': self.license_plate,
            'timestamp': self._timestamp(),
            'latitude': round(self.latitude, 6),
            'longitude': round(self.longitude, 6),
            'distanceTraveled': distance
        }
        routing_key = f'vehicle.{self.vehicle_id}.telemetry'
        if self._publish_message(self.telemetry_exchange, routing_key, message):
            print(f"[{self._timestamp()}] Telemetry sent: lat={message['latitude']}, "
                  f"lon={message['longitude']}, distance={distance}km")

    def send_state_change(self, new_state):
        message = {
            'vehicleId': self.vehicle_id,
            'licensePlate': self.license_plate,
            'timestamp': self._timestamp(),
            'state': new_state
        }
        routing_key = f'vehicle.{self.vehicle_id}.state'
        if self._publish_message(self.state_exchange, routing_key, message):
            print(f"[{self._timestamp()}] State change sent: {new_state}")

    def _heartbeat_loop(self):
        while self.running:
            self.send_heartbeat()
            time.sleep(self.heartbeat_interval)

    def _telemetry_loop(self):
        while self.running:
            self.send_telemetry()
            time.sleep(self.telemetry_interval)

    def start(self):
        if not self.connect():
            print("Failed to connect to RabbitMQ. Exiting.")
            return
        
        self.running = True
        self.send_state_change('STARTED')
        
        heartbeat_thread = threading.Thread(target=self._heartbeat_loop, daemon=True)
        telemetry_thread = threading.Thread(target=self._telemetry_loop, daemon=True)
        
        heartbeat_thread.start()
        telemetry_thread.start()
        
        print(f"[{self._timestamp()}] Simulator started for vehicle {self.vehicle_id} ({self.license_plate})")
        print(f"[{self._timestamp()}] Heartbeat interval: {self.heartbeat_interval}s, Telemetry interval: {self.telemetry_interval}s")
        
        try:
            while self.running:
                time.sleep(1)
        except KeyboardInterrupt:
            self.stop()

    def stop(self):
        print(f"\n[{self._timestamp()}] Shutting down simulator...")
        self.running = False
        self.send_state_change('STOPPED')
        
        time.sleep(1)
        
        with self.channel_lock:
            try:
                if self.connection and self.connection.is_open:
                    self.connection.close()
            except Exception:
                pass
        
        print(f"[{self._timestamp()}] Simulator stopped.")


def parse_arguments():
    parser = argparse.ArgumentParser(description='Vehicle Simulator')
    parser.add_argument('--vehicle-id', type=int, help='Vehicle ID')
    parser.add_argument('--license-plate', type=str, help='License plate')
    parser.add_argument('--rabbitmq-host', type=str, help='RabbitMQ host')
    parser.add_argument('--rabbitmq-port', type=int, help='RabbitMQ port')
    parser.add_argument('--rabbitmq-user', type=str, help='RabbitMQ username')
    parser.add_argument('--rabbitmq-password', type=str, help='RabbitMQ password')
    parser.add_argument('--heartbeat-interval', type=int, help='Heartbeat interval in seconds')
    parser.add_argument('--telemetry-interval', type=int, help='Telemetry interval in seconds')
    parser.add_argument('--initial-latitude', type=float, help='Initial latitude')
    parser.add_argument('--initial-longitude', type=float, help='Initial longitude')
    parser.add_argument('--env-file', type=str, default='.env', help='Path to .env file')
    return parser.parse_args()


def load_config(args):
    if os.path.exists(args.env_file):
        load_dotenv(args.env_file)
    
    config = {
        'vehicle_id': args.vehicle_id or int(os.getenv('VEHICLE_ID', 1)),
        'license_plate': args.license_plate or os.getenv('LICENSE_PLATE', 'UNKNOWN'),
        'initial_latitude': args.initial_latitude or float(os.getenv('INITIAL_LATITUDE', 44.8176)),
        'initial_longitude': args.initial_longitude or float(os.getenv('INITIAL_LONGITUDE', 20.4633)),
        'heartbeat_interval': args.heartbeat_interval or int(os.getenv('HEARTBEAT_INTERVAL', 15)),
        'telemetry_interval': args.telemetry_interval or int(os.getenv('TELEMETRY_INTERVAL', 600)),
        'rabbitmq': {
            'host': args.rabbitmq_host or os.getenv('RABBITMQ_HOST', 'localhost'),
            'port': args.rabbitmq_port or int(os.getenv('RABBITMQ_PORT', 5672)),
            'user': args.rabbitmq_user or os.getenv('RABBITMQ_USER', 'guest'),
            'password': args.rabbitmq_password or os.getenv('RABBITMQ_PASSWORD', 'guest')
        }
    }
    
    return config


def main():
    args = parse_arguments()
    config = load_config(args)
    
    print("=" * 60)
    print("Vehicle Simulator Configuration")
    print("=" * 60)
    print(f"Vehicle ID: {config['vehicle_id']}")
    print(f"License Plate: {config['license_plate']}")
    print(f"Initial Position: ({config['initial_latitude']}, {config['initial_longitude']})")
    print(f"RabbitMQ: {config['rabbitmq']['host']}:{config['rabbitmq']['port']}")
    print("=" * 60)
    
    simulator = VehicleSimulator(config)
    simulator.start()


if __name__ == '__main__':
    main()
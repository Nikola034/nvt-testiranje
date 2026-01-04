import os
import subprocess
import datetime
import json

RESULTS_DIR = "results"
HOST = "http://localhost:8080"

TEST_CONFIGS = [
    {"users": 10, "spawn_rate": 2, "duration": "1m", "name": "light"},
    {"users": 50, "spawn_rate": 5, "duration": "2m", "name": "medium"},
    {"users": 100, "spawn_rate": 10, "duration": "3m", "name": "heavy"},
    {"users": 500, "spawn_rate": 25, "duration": "5m", "name": "stress"},
    {"users": 1000, "spawn_rate": 50, "duration": "5m", "name": "peak"},
]

def ensure_results_dir():
    if not os.path.exists(RESULTS_DIR):
        os.makedirs(RESULTS_DIR)

def run_test(config):
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    result_prefix = f"{RESULTS_DIR}/{config['name']}_{timestamp}"

    cmd = [
        "locust",
        "-f", "locustfile.py",
        "--headless",
        "-u", str(config["users"]),
        "-r", str(config["spawn_rate"]),
        "-t", config["duration"],
        "--host", HOST,
        "--csv", result_prefix,
        "--html", f"{result_prefix}_report.html"
    ]

    print(f"\n{'='*60}")
    print(f"Running {config['name']} load test")
    print(f"Users: {config['users']}, Spawn Rate: {config['spawn_rate']}/s, Duration: {config['duration']}")
    print(f"{'='*60}\n")

    subprocess.run(cmd)

    print(f"\nResults saved to {result_prefix}_*.csv and {result_prefix}_report.html")

def main():
    ensure_results_dir()

    print("Student 3 Load Testing Suite")
    print("="*60)
    print("\nAvailable test configurations:")
    for i, config in enumerate(TEST_CONFIGS):
        print(f"  {i+1}. {config['name']}: {config['users']} users, {config['duration']}")

    print("\nOptions:")
    print("  a - Run all tests sequentially")
    print("  1-5 - Run specific test")
    print("  q - Quit")

    choice = input("\nSelect option: ").strip().lower()

    if choice == 'q':
        return
    elif choice == 'a':
        for config in TEST_CONFIGS:
            run_test(config)
    elif choice.isdigit() and 1 <= int(choice) <= len(TEST_CONFIGS):
        run_test(TEST_CONFIGS[int(choice) - 1])
    else:
        print("Invalid option")

if __name__ == "__main__":
    main()

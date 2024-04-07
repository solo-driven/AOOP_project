import socket
import threading
import json

SERVER_HOST = 'localhost'
SERVER_PORT = 12345
LISTENER_PORT = 54321

def send_preferences(email, preferences, method="POST"):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_HOST, SERVER_PORT))
        message = {
            "method": method,
            "email": email,
            "preferences": preferences
        }
        s.sendall(json.dumps(message).encode('utf-8'))

def listen_for_updates():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener_socket:
        listener_socket.bind((SERVER_HOST, LISTENER_PORT))
        listener_socket.listen()
        print(f"Listening for updates on {SERVER_HOST}:{LISTENER_PORT}...")
        while True:
            conn, addr = listener_socket.accept()
            with conn:
                print(f"Connected by {addr}")
                while True:
                    data = conn.recv(1024)
                    if not data:
                        break
                    print(f"Update received: {data.decode('utf-8')}")

def main():
    listener_thread = threading.Thread(target=listen_for_updates, daemon=True)
    listener_thread.start()

    # Sending preferences with city names
    email = "student@example.com"
    preferences = ["New York", "Paris", "Tokyo", "London", "Sydney"]
    send_preferences(email, preferences)

    # Updating preferences
    new_preferences = ["Sydney", "London", "Tokyo", "Paris", "New York"]
    send_preferences(email, new_preferences, "PUT")

if __name__ == "__main__":
    main()

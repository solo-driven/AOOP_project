import socket
import threading
import json

SERVER_HOST = 'localhost'
SERVER_PORT = 8080
LISTENER_PORT = 54321

class Response:
    def __init__(self, status_code, status_message, headers, body):
        self.status_code = status_code
        self.status_message = status_message
        self.headers = headers
        self.body = body
        

    def __str__(self):
        return f"Response(status_code={self.status_code}, status_message={self.status_message}, headers={self.headers}, body={self.body})"
    
    @staticmethod
    def from_string(response_str):
        lines = response_str.split("\r\n")
        _, status_code, status_message = lines[0].split(" ", 2)
        status_code = int(status_code)

        headers = {}
        body = ""
        for line in lines[1:]:
            if not line:
                break
            key, value = line.split(": ")
            headers[key] = value

        print(lines[-1])
        try:
            body = json.loads(lines[-1])
        except json.JSONDecodeError:
            body = lines[-1]

        return Response(status_code, status_message, headers, body)

def send_http_request(method, path, headers, body):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_HOST, SERVER_PORT))
        body_line = f"{body}"
        headers["Content-Length"] = str(len(body))  # Add Content-Length header
        request_line = f"{method} {path} HTTP/1.1\r\n"
        headers_line = "".join(f"{k}: {v}\r\n" for k, v in headers.items())
        request_message = f"{request_line}{headers_line}\r\n{body_line}"
        s.sendall(request_message.encode('utf-8'))

        print(f"Request sent: {request_message}")
        # Receive the server's response
        response = ''
        while True:
            print("Receiving data...")
            data = s.recv(1024)
            print(f"Received data: {data}")
            if not data:
                break
            response += data.decode('utf-8')
        return Response.from_string(response)

def send_preferences(email, preferences, method="POST"):
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    resp = send_http_request(method, path, headers, body)
    print(resp)



def update_preferences(email, preferences):
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    resp = send_http_request("PUT", path, headers, body)
    print(resp)

def get_destinations() -> list:
    path = "/destinations"
    headers = {}
    body = ""
    response = send_http_request("GET", path, headers, body)
    destinations = response.body
    if response.status_code != 200:
        raise Exception(f"Failed to get destinations: {response.body}")
    
    print(response)
    print(f"Received destinations: {destinations} , of type {type(destinations)}")
    return destinations


def get_assignment(email):
    path = "/assign-student"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email})
    send_http_request("POST", path, headers, body)

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


from time import sleep

def main():
    # listener_thread = threading.Thread(target=listen_for_updates, daemon=True)
    # listener_thread.start()

    # Sending preferences with city names
    email = "student@example.com"
    preferences = ["New York", "Paris", "Tokyo", "London", "Sydney"]
    print(f"Sending preferences: {preferences}")
    send_preferences(email, preferences)
    sleep(5)


    # Updating preferences
    new_preferences = ["Sydney", "London", "Tokyo", "Paris", "New York"]
    send_preferences(email, new_preferences, "PUT")
    sleep(5)

    get_destinations()


if __name__ == "__main__":
    main()
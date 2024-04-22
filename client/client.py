import socket
import json
from client.response import Response

SERVER_HOST = 'localhost'
SERVER_PORT = 8080


# sending an HTTP request to the server
def send_http_request(method, path, headers={}, body="") -> Response:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_HOST, SERVER_PORT))
        if body:
            headers["Content-Length"] = str(len(body))
            headers["Content-Type"] = "application/json"

        headers["Host"] = f"{SERVER_HOST}:{SERVER_PORT}"

        request_line = f"{method} {path} HTTP/1.1\r\n"
        headers_line = "".join(f"{k}: {v}\r\n" for k, v in headers.items())
        request_message = f"{request_line}{headers_line}\r\n{body}"
        s.sendall(request_message.encode('utf-8'))

        # Receiving the server's response
        data=''
        response = ''
        while True:
            data = s.recv(1024)
            if not data:
                break
            response += data.decode('utf-8')

        return Response.from_string(response)

# sending preferences to the server
def send_preferences(email, preferences, method="POST") -> Response:
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    
    return send_http_request(method, path, headers, body)

# updating preferences on the server
def update_preferences(email, preferences) -> Response:
    return send_preferences(email, preferences, "PUT")

# taking destinations from the server
def get_destinations() -> Response:
    path = "/destinations"
    return send_http_request("GET", path)

# gets an assignment from the server
def get_assignment(email, preferences) -> Response:
    path = "/assign"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    
    return send_http_request("POST", path, headers, body)

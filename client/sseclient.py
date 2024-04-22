import socket
import threading
from typing import Callable

from client.response import Response

# Server-Sent Events client,
# connects to the server and listens for messages, calling the on_message callback when a message is received
class SSEClient:
    def __init__(self, ip: str, port: int, path: str, on_message: Callable[[dict], None]):
        self.ip = ip
        self.port = port
        self.path = path
        self.on_message = on_message
        self.initial_response_received = threading.Event()

    def connect_to_server(self, client_id: str):  
        threading.Thread(target=self._connect_to_server, args=(client_id,)).start()

    def _connect_to_server(self, client_id: str):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((self.ip, self.port))
            request_line = f"GET {self.path}?clientId={client_id} HTTP/1.1\r\n"
            headers = {
                "Host": f"{self.ip}:{self.port}",
                "Accept": "text/event-stream",
                "Connection": "keep-alive"
            }
            headers_line = "".join(f"{k}: {v}\r\n" for k, v in headers.items())
            request_message = f"{request_line}{headers_line}\r\n"
            s.sendall(request_message.encode('utf-8'))

            event_sep = "\n"
            response = ''
            reading_headers = True
            while True:
                data = s.recv(1024)
                if not data:
                    break
                data = data.decode('utf-8')
                response += data

                if reading_headers:
                    # Check if all the headers are received 
                    if '\r\n\r\n' in response:
                        resp = Response.from_string(response)
                        if resp.status_code != 200:
                            raise Exception(f"Failed to connect to server: {resp}")
                        reading_headers = False
                        self.initial_response_received.set()
                        continue

                if not reading_headers and data:
                    events = data.split(event_sep + event_sep)
                    for event in events:
                        if event:
                            lines = event.split(event_sep)
                            #print("Lines are", lines)
                            event_dict = {}
                            for line in lines:
                                if line.startswith("event:"):
                                    event_dict["event"] = line.split(": ", 1)[1]
                                elif line.startswith("data:"):
                                    event_dict["data"] = line.split(": ", 1)[1]

                            self.on_message(event_dict)
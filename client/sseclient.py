import socket
import threading
from typing import Callable

from response import Response
from time import sleep


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
            headers_line = f"Host: {self.ip}:{self.port}\r\nAccept: text/event-stream\r\nConnection: keep-alive\r\n"
            request_message = f"{request_line}{headers_line}\r\n"
            s.sendall(request_message.encode('utf-8'))

            sep = "\n"
            response = ''
            reading_headers = True
            while True:
                data = s.recv(1024)
                #print("data", data, len(data))
                if not data:
                    break
                data = data.decode('utf-8')
                response += data

                if reading_headers:
                    # Check if we've received all the headers
                    if '\r\n\r\n' in response:
                        resp = Response.from_string(response)
                        if resp.status_code != 200:
                            raise Exception(f"Failed to connect to server: {resp}")
                        reading_headers = False
                        self.initial_response_received.set()
                        continue

                if not reading_headers and data:
                    events = data.split(sep + sep)
                    for event in events:
                        if event:
                            lines = event.split(sep)
                            #print("Lines are", lines)
                            event_dict = {}
                            for line in lines:
                                if line.startswith("event:"):
                                    event_dict["event"] = line.split(": ", 1)[1]
                                elif line.startswith("data:"):
                                    event_dict["data"] = line.split(": ", 1)[1]
                                elif line.startswith("id:"):
                                    event_dict["id"] = line.split(": ", 1)[1]
                            self.on_message(event_dict)
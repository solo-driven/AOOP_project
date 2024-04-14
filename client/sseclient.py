import socket
import threading
from typing import Callable

from response import Response
class SSEClient:
    def __init__(self, ip: str, port: int, path: str, on_message: Callable[[dict], None]):
        self.ip = ip
        self.port = port
        self.path = path
        self.on_message = on_message

    def connect_to_server(self, client_id: str):
        threading.Thread(target=self._connect_to_server, args=(client_id,)).start()

    def _connect_to_server(self, client_id: str):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((self.ip, self.port))
            request_line = f"GET {self.path}?clientId={client_id} HTTP/1.1\r\n"
            headers_line = f"Host: {self.ip}:{self.port}\r\nAccept: text/event-stream\r\nConnection: keep-alive\r\n"
            request_message = f"{request_line}{headers_line}\r\n"
            s.sendall(request_message.encode('utf-8'))

            # Read the server's initial response (the headers)
            response = ''
            while True:
                data = s.recv(1024)
                if not data:
                    break
                response += data.decode('utf-8')


            resp = Response.from_string(response)
            print("SSEclient response: ", resp)

            if resp.status_code != 200:
                raise Exception(f"Failed to connect to server: {resp}")

            while True:
                data = s.recv(1024).decode("utf-8")
                print("received data in while: ", data)
                if data:
                    events = data.split("\n\n")
                    for event in events:
                        if event:
                            lines = event.split("\n")
                            event_dict = {}
                            for line in lines:
                                if line.startswith("event:"):
                                    event_dict["event"] = line.split(": ", 1)[1]
                                elif line.startswith("data:"):
                                    event_dict["data"] = line.split(": ", 1)[1]
                                elif line.startswith("id:"):
                                    event_dict["id"] = line.split(": ", 1)[1]

                            self.on_message(event_dict)

def main():
    ip = "localhost"
    port = 8080
    path = "/assignment-stream"

    def on_message_callback(data):
        print("Received data:", data)

    client = SSEClient(ip, port, path, on_message_callback)
    threading.Thread(target=client.connect_to_server).start()

if __name__ == "__main__":
    main()
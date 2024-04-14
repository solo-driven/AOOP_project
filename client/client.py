import socket
import json

SERVER_HOST = 'localhost'
SERVER_PORT = 8080
LISTENER_PORT = 54321

from response import Response

def send_http_request(method, path, headers, body):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_HOST, SERVER_PORT))
        body_line = f"{body}"
        headers["Content-Length"] = str(len(body))  
        request_line = f"{method} {path} HTTP/1.1\r\n"
        headers_line = "".join(f"{k}: {v}\r\n" for k, v in headers.items())
        request_message = f"{request_line}{headers_line}\r\n{body_line}"
        s.sendall(request_message.encode('utf-8'))

        # Receive the server's response
        response = ''
        while True:
            data = s.recv(1024)
            if not data:
                break
            response += data.decode('utf-8')

        return Response.from_string(response)

def send_preferences(email, preferences, method="POST"):
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    resp = send_http_request(method, path, headers, body)


    return resp



def update_preferences(email, preferences):
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    resp = send_http_request("PUT", path, headers, body)
    
    return resp

def get_destinations() -> list:
    path = "/destinations"
    headers = {}
    body = ""
    response = send_http_request("GET", path, headers, body)
    destinations = response.body
    if response.status_code != 200:
        raise Exception(f"Failed to get destinations: {response.body}")

    return destinations


def get_assignment(email, preferences):
    path = "/assign"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    

    return send_http_request("POST", path, headers, body)

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
from dataclasses import dataclass

@dataclass(frozen=True)
class Student:
    email: str
    preferences: tuple

# for testing (in gui use get destinations)
destinations = [
    "New York",
    "Paris",
    "Tokyo",
    "London",
    "Sydney",
    "Los Angeles",
    "Chicago",
    "Toronto",
    "Berlin",
    "Madrid"
]


import random

def generate_students(N, destinations) -> list[Student]:
    students = []
    for i in range(N):
        email = f"student{i}@mail.com"
        preferences = tuple(random.sample(destinations, 4))
        student = Student(email, preferences)
        students.append(student)
    return students

from sseclient import SSEClient
from thread_pool import *
import time
def main():
    students = generate_students(10, destinations)
    me = students[0]
    def on_message_callback(data):
        print("SSECLIENT Received data:", data)

    time.sleep(1)
    with ThreadPool() as pool:

        # Send preferences
        student_futures = {}
        for student in students:
            future = pool.submit(send_preferences, student.email, student.preferences)
            student_futures[student] = future
        
        for student, future in student_futures.items():
            future.result()

        # assign students
        for student in students:
            future = pool.submit(get_assignment, student.email, student.preferences)
            student_futures[student] = future
        
        for student, get_assignment_future in student_futures.items():
            resp = get_assignment_future.result()

            if resp.status_code != 200:
                print(f"Failed to assign {student.email}: {resp.body}")
            else:
                print(f"{student.email} assigned to {resp.body}")
                client = SSEClient(SERVER_HOST, SERVER_PORT, "/assignment-stream", on_message_callback)
                client.connect_to_server(student.email)



            

        
        

    # student_0 = students[0]
    # future_assign = pool.submit(get_assignment,  student_0.email, student_0.preferences)

    # res_assign = future_assign.result()
    # print("Result", res_assign)
    









if __name__ == "__main__":
    main()
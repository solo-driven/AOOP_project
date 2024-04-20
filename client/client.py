import socket
import json

SERVER_HOST = 'localhost'
SERVER_PORT = 8080

from client.response import Response

def send_http_request(method, path, headers={}, body="") -> Response:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.connect((SERVER_HOST, SERVER_PORT))
        headers["Content-Length"] = str(len(body))  
        request_line = f"{method} {path} HTTP/1.1\r\n"
        headers_line = "".join(f"{k}: {v}\r\n" for k, v in headers.items())
        request_message = f"{request_line}{headers_line}\r\n{body}"
        s.sendall(request_message.encode('utf-8'))

        # Receive the server's response
        data=''
        response = ''
        while True:

            data = s.recv(1024)
            if not data:
                break
            response += data.decode('utf-8')

        return Response.from_string(response)

def send_preferences(email, preferences, method="POST") -> Response:
    path = "/preferences"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    resp = send_http_request(method, path, headers, body)

    return resp



def update_preferences(email, preferences) -> Response:
    return send_preferences(email, preferences, "PUT")

def get_destinations() -> Response:
    path = "/destinations"
    return send_http_request("GET", path)


def get_assignment(email, preferences) -> Response:
    path = "/assign"
    headers = {"Content-Type": "application/json"}
    body = json.dumps({"email": email, "preferences": preferences})
    

    return send_http_request("POST", path, headers, body)




def main():
    
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
    from client.student import Student
    def generate_students(N, destinations) -> list[Student]:
        students = []
        for i in range(N):
            email = f"student{i}@mail.com"
            preferences = tuple(random.sample(destinations, 4))
            student = Student(email, preferences)
            students.append(student)
        return students

    from client.sseclient import SSEClient
    from client.thread_pool import ThreadPool

    students = generate_students(4, destinations)
    def on_message_callback(data):
        print("SSECLIENT Received data:", data)

    with ThreadPool() as pool:

        # Send preferences
        student_futures = {}
        for student in students:
            future = pool.submit(send_preferences, student.email, student.preferences)
            student_futures[student] = future
        
        for student, future in student_futures.items():
            print(student.email, future.result())
        
        for student in students:
            client = SSEClient(SERVER_HOST, SERVER_PORT, "/assignment-stream", on_message_callback)
            client.connect_to_server(student.email)
            client.initial_response_received.wait()
            print("waited for stream to connect for", student.email)
        
        

        # assign students
        for student in students:
            future = pool.submit(get_assignment, student.email, student.preferences)
            student_futures[student] = future
        
        for student, get_assignment_future in student_futures.items():
            resp = get_assignment_future.result()
            
            print(f"Result for {student.email}: {resp}")




if __name__ == "__main__":
    main()
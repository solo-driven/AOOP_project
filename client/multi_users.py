
import random
from client.student import Student
from client.client import *

from client.sseclient import SSEClient
from client.thread_pool import ThreadPool

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



def generate_students(N, destinations) -> list[Student]:
    students = []
    for i in range(N):
        email = f"student{i}@mail.com"
        preferences = tuple(random.sample(destinations, 4))
        student = Student(email, preferences)
        students.append(student)
    return students




if __name__ == "__main__":
    students = generate_students(4, destinations)
    print("Students: ", students)

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




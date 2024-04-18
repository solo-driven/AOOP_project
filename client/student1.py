from sseclient import SSEClient
from student import Student 
from client import get_destinations, send_preferences, get_assignment
from time import sleep
destinations = (
    "New York",
    "Paris",
    "Tokyo",
    "London",
)



student = Student("student1@mail.com", destinations)

# destinations = get_destinations()

# print(destinations)
import time

resp = send_preferences(student.email, student.preferences)
print(resp)

def on_message_callback(data):
    print("Received data:", data)

client = SSEClient("localhost", 8080, "/assignment-stream", on_message_callback)
client.connect_to_server(student.email)

st = time.time()
print("waiting for initial response")
client.initial_response_received.wait()
print("Time taken to receive initial response: ", time.time()-st)


student_assign = get_assignment(student.email, student.preferences)
print("!"*50)
print(student_assign)
print("!"*50)

from sseclient import SSEClient
from student import Student 
from client import get_destinations, send_preferences, get_assignment

destinations = (
    "New York",
    "Paris",
    "Tokyo",
    "London",
)



student = Student("student2@mail.com", destinations)

# destinations = get_destinations()

# print(destinations)

send_preferences(student.email, student.preferences)

def on_message_callback(data):
    print("Received data:", data)

client = SSEClient("localhost", 8080, "/assignment-stream", on_message_callback)
client.connect_to_server(student.email)

student_assign = get_assignment(student.email, student.preferences)
print("!"*50)
print(student_assign)
print("!"*50)


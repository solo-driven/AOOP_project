import tkinter as tk
from tkinter import messagebox
import re

from client.client import get_assignment, get_destinations, send_preferences
from client.sseclient import SSEClient


class CitySelectionForm:
    def __init__(self, master):
        self.master = master
        master.title("City Selection Form")
        self.master.resizable(False, False)

        self.cities = [(city.title(), False) for city in get_destinations().body]
        self.selected_cities = []
        self.email = ""
        self.label = None  # Initialize label as None

        self.create_widgets()

    def create_widgets(self):
        # Frame for city selection
        city_frame = tk.LabelFrame(
            self.master, text="Select up to 5 cities", font=("Helvetica", 30))
        city_frame.grid(row=0, column=0, padx=20, pady=20, sticky=tk.W)

        # City buttons
        self.city_vars = []
        self.city_buttons = []
        row = 0
        col = 0
        for city, selected in self.cities:
            var = tk.IntVar(value=selected)
            self.city_vars.append(var)
            button = tk.Checkbutton(city_frame, text=city, variable=var,
                                    command=self.update_button_states, font=("Helvetica", 28))
            button.grid(row=row, column=col, padx=10, pady=10, sticky=tk.W)
            self.city_buttons.append(button)
            col += 1
            if col > 2:
                row += 1
                col = 0

        email_frame = tk.LabelFrame(
            self.master, text="Enter your email", font=("Helvetica", 30))
        email_frame.grid(row=1, column=0, padx=20, pady=20, sticky=tk.W)

        self.email_entry = tk.Entry(
            email_frame, width=30, font=("Helvetica", 28))
        self.email_entry.grid(row=0, column=0, padx=10, pady=10)

        tk.Button(self.master, text="Submit", command=self.submit_form, font=(
            "Helvetica", 28)).grid(row=2, column=0, padx=20, pady=20)

    def update_button_states(self):
        selected_count = sum(var.get() for var in self.city_vars)
        for i, var in enumerate(self.city_vars):
            button = self.city_buttons[i]
            if selected_count >= 5 and not var.get():
                button['state'] = 'disabled'
            else:
                button['state'] = 'normal'

    def submit_form(self):
        selected_count = sum(var.get() for var in self.city_vars)
        if selected_count > 5:
            messagebox.showerror(
                "Error", "You can select a maximum of 5 cities.")
            return

        self.cities = [(city, var.get()) for city, var in zip(self.cities, self.city_vars)]

        # Update self.selected_cities with the selected cities
        self.selected_cities = [city[0] for city, selected in self.cities if selected]
        print(self.selected_cities)

        self.email = self.email_entry.get()

        if not self.email:
            messagebox.showerror("Error", "Please enter your email")
            return
        if not self.is_valid_email(self.email):
            messagebox.showerror("Error", "Please enter a valid email address")
            return

        messagebox.showinfo("Selected Cities", f"Selected Cities: {', '.join(self.selected_cities)}\nEmail: {self.email}")
        send_preferences(email=self.email, preferences=self.selected_cities)
        self.show_assignment()


    def get_assignment_and_show(self):
        assignment = get_assignment(
            email=self.email, preferences=self.selected_cities)
        self.label.config(
            text=f"Dear {self.email}, your assignment is: {assignment.body['assignment']}. Congratulations!")

    def show_assignment(self):

        self.master.destroy()
        client = SSEClient("localhost", 10000,
                        "/assignment-stream", self.on_message_callback)
        client.connect_to_server(self.email)
        client.initial_response_received.wait()
        assignment_window = tk.Tk()
        assignment_window.title("Assignment Result")
        assignment_window.geometry("1000x800")  # Set a larger window size

        frame = tk.Frame(assignment_window)
        frame.pack(expand=True)

        self.label = tk.Label(frame, text="", font=("Helvetica", 30), wraplength=900)
        self.label.pack(expand=True, padx=20, pady=20)

        button = tk.Button(frame, text="Get Assignment",
                           command=self.get_assignment_and_show, font=("Helvetica", 28))
        button.pack(pady=20)
        assignment_window.mainloop()

    def on_message_callback(self, data):
        print("Received data:", data)
        messagebox.showinfo(
            "UPDATE!!!", f"Your city was updated here is the data:{data['data']}")
        print(data)
        self.label.config(
            text=f"Dear {self.email}, your assignment is: {data['data']}. Congratulations!")

    def is_valid_email(self, email):
        """
        Check if the email address is a valid format.
        """
        if not re.match(r"[^@]+@[^@]+\.[^@]+", email):
            return False
        return True


if __name__ == "__main__":
    root = tk.Tk()
    app = CitySelectionForm(root)
    root.mainloop()
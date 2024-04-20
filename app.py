import threading
import tkinter as tk
from tkinter import messagebox

from client.client import get_assignment, get_destinations, listen_for_updates, send_preferences
from client.sseclient import SSEClient

class CitySelectionForm:
    def __init__(self, master):
        self.master = master
        master.title("City Selection Form")
        self.master.resizable(False, False)

        self.cities = [(city.title(), False) for city in get_destinations()]
        self.selected_cities = []
        self.email = ""
        self.create_widgets()

    def create_widgets(self):
        # Frame for city selection
        city_frame = tk.LabelFrame(self.master, text="Select up to 5 cities", font=("Helvetica", 30))
        city_frame.grid(row=0, column=0, padx=20, pady=20, sticky=tk.W)

        # City buttons
        self.city_vars = []
        self.city_buttons = []
        row = 0
        col = 0
        for city, selected in self.cities:
            var = tk.IntVar(value=selected)
            self.city_vars.append(var)
            button = tk.Checkbutton(city_frame, text=city, variable=var, command=self.update_button_states, font=("Helvetica", 28))
            button.grid(row=row, column=col, padx=10, pady=10, sticky=tk.W)
            self.city_buttons.append(button)
            col += 1
            if col > 2:
                row += 1
                col = 0

        # Frame for email input
        email_frame = tk.LabelFrame(self.master, text="Enter your email", font=("Helvetica", 30))
        email_frame.grid(row=1, column=0, padx=20, pady=20, sticky=tk.W)

        # Mail input
        self.email_entry = tk.Entry(email_frame, width=30, font=("Helvetica", 28))
        self.email_entry.grid(row=0, column=0, padx=10, pady=10)

        # Submit button
        tk.Button(self.master, text="Submit", command=self.submit_form, font=("Helvetica", 28)).grid(row=2, column=0, padx=20, pady=20)

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
            messagebox.showerror("Error", "You can select a maximum of 5 cities.")
            return

        self.selected_cities = [city for city, selected in self.cities if selected]
        self.email = self.email_entry.get()
        if not self.email:
            messagebox.showerror("Error", "Please enter your email")
            return

        messagebox.showinfo("Selected Cities", f"Selected Cities: {', '.join(self.selected_cities)}\nEmail: {self.email}")
        send_preferences(email=self.email, preferences=self.selected_cities)
        client = SSEClient("localhost", 8080, "/assignment-stream", self.on_message_callback)
        client.connect_to_server(self.email)
        client.initial_response_received.wait()

        self.show_assignment()



    def show_assignment(self):

        self.master.destroy()

        assignment_window = tk.Tk()
        assignment_window.title("Assignment Result")
        assignment_window.geometry("1000x800")  # Set a larger window size

        assignment = get_assignment(email=self.email, preferences=self.selected_cities)

        frame = tk.Frame(assignment_window)
        frame.pack(expand=True)

        label = tk.Label(frame, text=f"Dear {self.email}, your assignment is: {assignment.body['assignment']}. Congratulations!", font=("Helvetica", 30))
        label.pack(expand=True, padx=20, pady=20)


        assignment_window.mainloop()

    def on_message_callback(self,data):
        print("Received data:", data)
        messagebox.showinfo(f"Your city was updated here is the data:{data}")


if __name__ == "__main__":
    root = tk.Tk()
    app = CitySelectionForm(root)
    root.mainloop()

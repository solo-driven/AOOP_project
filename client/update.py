from client.client import update_preferences

email = "studom@mail.com"
preferences = ["New York", 1]




if __name__ ==  '__main__':
    resp = update_preferences(email, preferences)
    print(resp)
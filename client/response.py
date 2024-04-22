import json

# Response class to represent the server's response, with status code, status message, headers, and body
class Response:
    def __init__(self, status_code, status_message, headers, body):
        self.status_code = status_code
        self.status_message = status_message
        self.headers = headers
        self.body = body
        
    # String representation of the response
    def __str__(self):
        return f"Response(status_code={self.status_code}, status_message={self.status_message}, headers={self.headers}, body={self.body})"
    
    # Static method to create a Response object from a string
    @staticmethod
    def from_string(response_str, sep="\r\n"):
        lines = response_str.split(sep)
        _, status_code, status_message = lines[0].split(" ", 2)
        status_code = int(status_code)

        headers = {}
        body = ""
        for line in lines[1:]:
            if not line:
                break
            key, value = line.split(": ")
            headers[key] = value

        body = ""
        if lines[-1]:
            body = json.loads(lines[-1])
     

        return Response(status_code, status_message, headers, body)

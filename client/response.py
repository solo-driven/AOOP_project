import json


class Response:
    def __init__(self, status_code, status_message, headers, body):
        self.status_code = status_code
        self.status_message = status_message
        self.headers = headers
        self.body = body
        

    def __str__(self):
        return f"Response(status_code={self.status_code}, status_message={self.status_message}, headers={self.headers}, body={self.body})"
    
    @staticmethod
    def from_string(response_str):
        lines = response_str.split("\r\n")
        _, status_code, status_message = lines[0].split(" ", 2)
        status_code = int(status_code)

        headers = {}
        body = ""
        for line in lines[1:]:
            if not line:
                break
            key, value = line.split(": ")
            headers[key] = value
        
        body = json.loads(lines[-1])
     

        return Response(status_code, status_message, headers, body)

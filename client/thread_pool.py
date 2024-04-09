import threading


class Future:
    def __init__(self):
        self._result = None
        self._done = False
        self._condition = threading.Condition()

    # to be called by thread to set the result and notify that result is ready
    # (result method will wait for this notification)
    def set_result(self, result):
        with self._condition:
            self._result = result
            self._done = True
            self._condition.notify_all()

    def result(self):
        with self._condition:
            # while not done wait for set_result to be called by thread to indicate that result is ready
            while not self._done:
                self._condition.wait()
            return self._result

class ThreadPool:
    def __init__(self):
        self._workers = []

    def submit(self, fn, *args, **kwargs):
        future = Future()

        def wrapper():
            try:
                result = fn(*args, **kwargs)
                future.set_result(result)
            except Exception as e:
                future.set_result(e)

        thread = threading.Thread(target=wrapper)
        thread.start()

        self._workers.append(thread)
        return future

    def shutdown(self, wait=True):
        if wait:
            for worker in self._workers:
                worker.join()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.shutdown()

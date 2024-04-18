
from dataclasses import dataclass


@dataclass(frozen=True)
class Student:
    email: str
    preferences: tuple
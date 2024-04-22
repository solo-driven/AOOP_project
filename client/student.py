
from dataclasses import dataclass

# Dataclass represents a student with email and preferences
@dataclass(frozen=True)
class Student:
    email: str
    preferences: tuple
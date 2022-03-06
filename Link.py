from abc import *
from typing import *
from Machine import Machine

from Counter import Counter

class Link(ABC):
    id: int
    capacity: int
    source: Machine
    destination: Machine

    def __repr__(self):
        return f"{self.id} - {self.capacity}"

    def attach(self, machine_1: Machine, machine_2: Machine):
        self.source = machine_1
        self.destination = machine_2
        machine_1.partners.append(machine_2)
        machine_1.links.append(self)
        machine_2.partners.append(machine_1)
        machine_2.links.append(self)
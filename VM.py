from abc import *
from typing import *

from Counter import Counter
from Machine import Machine
from Package import Package

class VM(Machine, Package):
    def __init__(self, capacity: int, partners = [], links = [], inBin = None):
        # super().__init__()
        self.id = Counter().count_vm()
        self.capacity = capacity
        self.partners = partners if partners else []
        self.links = links if links else []
        self.inBin = inBin if inBin else None

    def show(self) -> None:
        print(self.id, self.capacity, self.partners, self.links, self.inBin)

    def placed_in(self, bin):
        self.inBin = bin
    

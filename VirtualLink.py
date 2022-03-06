from abc import *
from typing import *

from Counter import Counter
from Link import Link
from Machine import Machine

class VirtualLink(Link):
    def __init__(self, capacity: int, source = None, destination = None):
        self.id = Counter().count_vlink()
        self.capacity = capacity
        if(source and destination): self.attach(source, destination)
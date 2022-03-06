from abc import *
from typing import *

from Counter import Counter

class Machine(ABC):
    # id: int
    # capacity: int
    # partners: List = []
    # links: List = []

    # def __init__(self):
    #     self.partners = []
    #     self.links = []

    def __repr__(self):
        return f"{self.id}-{self.capacity}"

    def show(self) -> None: pass

    # @property
    # def partners(self) -> List[int]:
    #     return self._partners
    # @partners.setter
    # def partners(self, new) -> None:
    #     self._partners = new

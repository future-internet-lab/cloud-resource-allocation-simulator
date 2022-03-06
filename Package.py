from abc import *
from random import randrange
from typing import *



class Package(ABC):
    inBin = None
    # capacity = None

    # def __init__(self, _package: List = None):
    #     if (_package != None): self.list = _package
    #     else: self.list = []
    #     self.total = sum(self.list)

    def placed_in(self, bin):
        self.inBin = bin

    # def randomSize(self, min, max, quantity):
    #     self.list = []
    #     for i in range(quantity):
    #         self.list.append(randrange(min, max + 1))
    #         self.total = sum(self.list)
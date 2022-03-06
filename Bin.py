from abc import *
from typing import *



class Bin(ABC):
    # __packages: 
    @property
    def packages(self): return self.__packages
    @packages.setter
    def packages(self, new): self.__packages = new

    # capacity = None

    def __init__(self):
        self.__packages = []

    # def add_package(self, package):
    #     self.packages.append(package)
    #     self.capacity -= package.capacity
    #     package.placed_in(self)
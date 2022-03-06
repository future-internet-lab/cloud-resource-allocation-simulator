from abc import *
from typing import *

from Counter import Counter
from Machine import Machine
from Bin import Bin

class Server(Machine, Bin):
    def __init__(self, capacity: int, partners = [], links = [], packages = []):
        # super().__init__()
        self.id = Counter().count_server()
        self.capacity = capacity
        self.packages = packages
        self.partners = partners if partners else []
        self.links = links if links else []
        self.packages = packages if packages else []

    def show(self) -> None:
        print(self.id, self.capacity, self.packages)
        # print(self.id, self.capacity, self.partners, self.links, self.packages)
    
    # def minLink(self, requestedLink):
    #     temp = self.links.copy()
    #     for x in range(len(temp)):
    #         temp[x] = requestedLink / temp[x]
    #     return temp.index(max(temp))

    def add_package(self, package):
        self.packages.append(package)
        self.capacity -= package.capacity
        package.placed_in(self)

    def get_link_to(self, nextServerID):
        idList = [sv.id for sv in self.partners]
        return self.links[idList.index(nextServerID)]
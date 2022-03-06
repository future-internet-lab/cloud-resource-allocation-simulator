from __future__ import annotations
from abc import *
from typing import *

from Bin import Bin
from Package import Package
from Algorithm import *
from Server import Server
from VM import VM
from PhysicalLink import PhysicalLink
from VirtualLink import VirtualLink



class Worker(): #packing with bin and package
    algorithm: Algorithm = None
    performance: int = 0

    Servers: List[Server]
    VMs: List[VM]
    plinks: List[PhysicalLink]
    vlinks: List[VirtualLink]

    allocation: List = []
    # [3, -1, 2, 4]:
    # allocation[0] = 3 means package 0 is allocated in bin 3
    # bin -1 means the package is not allocated

    def __init__(self):
        self.algorithm = None
        self.performance = 0

    def manage(self, Servers, plinks, VMs, vlinks) -> None: 
        self.Servers = Servers
        self.plinks = plinks
        self.VMs = VMs
        self.vlinks = vlinks

    def mixture_resources(self, machines: List, links: List) -> List[int]:
        mixture_resources = []
        for x in range(len(machines)):
            mixture_resources.append(machines[x].capacity * links[x].capacity)
        return mixture_resources

    def packing(self) -> None:
        # sort two last VMs in DECREASING of capacity
        if(self.VMs[-2].capacity < self.VMs[-1].capacity):
            self.VMs[-2:] = list(reversed(self.VMs[-2:]))

        # find the first servers to put the bigger VM into
        sv_1 = self.algorithm.perform([sv.capacity for sv in self.Servers], self.VMs[-2].capacity)
        if(sv_1 == -1): return # sv_1 = -1 is fail, cancel both VMs

        # c_servers (candicated server) is list of servers which connected to sv_1
        c_servers = self.Servers[sv_1].partners
        # c_links corresponds to c_servers
        c_links = self.Servers[sv_1].links

        # list of product of each c_servers capacity and c_links capacity
        c_resources = self.mixture_resources(c_servers, c_links)
        for x in range(len(c_servers)):
            #if capacity of either c_servers or c_links is not enough, corresponding c_resources equals 0
            if(c_servers[x].capacity < self.VMs[-1].capacity or c_links[x].capacity < self.VMs[-1].links[0].capacity):
                c_resources[x] = 0

        # requested resources = VM capacity * VM links capacity
        r_resources = self.VMs[-1].capacity * self.VMs[-1].links[0].capacity
        # find the second server to put the smaller VM into
        # relative position to c_servers
        sv_2 = self.algorithm.perform(c_resources, r_resources)
        if(sv_2 == -1): return # sv_2 = -1 is fail, cancel both VMs
        # absolute position to Servers Systems, this í the final value of sv_2
        sv_2 = c_servers[sv_2].id

        link = self.Servers[sv_1].get_link_to(sv_2).id # link between sv_1 and sv_2

        # lets pack them!
        self.Servers[sv_1].add_package(self.VMs[-2])
        self.Servers[sv_2].add_package(self.VMs[-1])
        self.plinks[link].add_vlink(self.VMs[-1].links[0])

    def log(self) -> None:
        print("unpacked:", [vm for vm in self.VMs if vm.inBin == None])
        for x in range(len(self.Servers)):
            # print(self.Servers[x])
            self.Servers[x].show()
        # print()

        # for x in range(len(self.VMs)):
        #     # print(self.VMs[x].links[0].capacity)
        #     self.VMs[x].showall()
        # print()

        # for x in range(len(self.vlinks)): self.vlinks[x].showall()
        # print()

        # bin_quantity = len(self.bin.list)
        # package_quantity = len(self.package.list)
        # free = 0
        # unallocated = 0
        # # for i in range(bin_quantity):
        # #     free += self.bin.list[i]
        # #     print("BIN", i + 1)
        # #     print("     packages:", end=" ")
        # #     for j in range(package_quantity):
        # #         if self.allocation[j] == i: print(f"{j}({self.package.list[j]}),", end=" ")
        # #     print()
        # #     print("     free:", self.bin.list[i])

        # # print("Unallocated Package:", end=" ")
        # for i in range(package_quantity):
        #     if(self.allocation[i] == -1):
        #         unallocated += self.package.list[i]
        #         # print(f"{i}({self.package.list[i]}),", end=" ")
        # self.performance = 100 * (self.package.total - unallocated) / self.package.total
        # print(f"\nPerformance: {self.package.total - unallocated} / {self.package.total} ({self.performance :.0f}%)")
from __future__ import annotations
from abc import ABC, abstractmethod
from random import randrange
from typing import List, Tuple

from Bin import Bin
from Package import Package
from Worker import Worker
from Algorithm import *

from Server import Server
from VM import VM
from PhysicalLink import PhysicalLink
from VirtualLink import VirtualLink
from Counter import Counter

import random
import os
    
def find2nd(a, b, c):
    if(a != max(a, b, c) and a != min(a, b, c)): return a
    if(b != max(a, b, c) and b != min(a, b, c)): return b
    if(c != max(a, b, c) and c != min(a, b, c)): return c

if __name__ == '__main__':
    os.system("cls")
    print("\n\n###################################################################")

    Servers: Server = []
    # Servers.append(Server(20, [1, 3], [0, 2], []))
    # Servers.append(Server(40, [0, 2, 3, 4], [0, 1, 3, 4], []))
    # Servers.append(Server(35, [1, 4], [1, 5], []))
    # Servers.append(Server(25, [0, 1], [2, 3], []))
    # Servers.append(Server(30, [1, 2], [4, 5], []))
    Servers.append(Server(20))
    Servers.append(Server(40))
    Servers.append(Server(35))
    Servers.append(Server(25))
    Servers.append(Server(30))

    plinks: PhysicalLink = []
    plinks.append(PhysicalLink(10, Servers[0], Servers[1]))
    plinks.append(PhysicalLink(10, Servers[1], Servers[2]))
    plinks.append(PhysicalLink(25, Servers[0], Servers[3]))
    plinks.append(PhysicalLink(15, Servers[1], Servers[3]))
    plinks.append(PhysicalLink(25, Servers[1], Servers[4]))
    plinks.append(PhysicalLink(15, Servers[2], Servers[4]))

    # for i in range(len(Servers)):
    #     Servers[i].showall()
    # print()

    VMs: VM = []
    vlinks: VirtualLink = []
    # for i in range(12):
    #     VMs.append(VM(randrange(0, 15)))
    #     if(i % 2 == 1):
    #         vlinks.append(VirtualLink(randrange(0, 10), VMs[i-1], VMs[i]))

    for i in range(len(VMs)):
        VMs[i].showall()

    worker = Worker()
    worker.manage(Servers.copy(), plinks.copy(), VMs, vlinks)
    worker.algorithm = BestFit()

    for time in range(10):
        VMs.append(VM(randrange(10, 20)))
        if(time % 2 == 1):
            vlinks.append(VirtualLink(randrange(5, 10), VMs[time-1], VMs[time]))
            worker.packing()
    
    total = sum([vm.capacity for vm in VMs])
    packed = sum([vm.capacity for vm in VMs if vm.inBin != None])
    print(f"{100 * packed / total :.0f}%")

    worker.log()











    # bin = Bin([20, 30, 40, 50, 60])
    # # package = Package([4, 11, 16, 17, 18, 19, 6])
    # package = Package()
    # package.randomSize(1, 60, 12)
    # print(bin.list)
    # print(package.list)

    # worker = Worker()
    # worker.manage(bin, package)
    # worker.packing()

    # worker.algorithm = FirstFit()
    # worker.packing()
    # a = int(worker.performance)
    # worker.algorithm = BestFit()
    # worker.packing()
    # b = int(worker.performance)
    # worker.algorithm = WorstFit()
    # worker.packing()

    # stat = [[0, 0, 0], [0, 0, 0], [0, 0, 0]]
    # for i in range(1000):
    #     os.system("cls")
    #     print(i)
    #     worker.package.randomSize(20, 60, 12)
    #     worker.algorithm = FirstFit()
    #     worker.packing()
    #     a = int(worker.performance)
    #     worker.algorithm = BestFit()
    #     worker.packing()
    #     b = int(worker.performance)
    #     worker.algorithm = WorstFit()
    #     worker.packing()
    #     c = int(worker.performance)
    #     maxPfm = max(a, b, c)
    #     minPfm = min(a, b, c)
    #     secondPfm = find2nd(a, b, c)
    #     if(a == maxPfm): stat[0][0] += 1
    #     if(b == maxPfm): stat[1][0] += 1
    #     if(c == maxPfm): stat[2][0] += 1

    #     if(a == minPfm): stat[0][2] += 1
    #     if(b == minPfm): stat[1][2] += 1
    #     if(c == minPfm): stat[2][2] += 1

    #     if(a == secondPfm): stat[0][1] += 1
    #     if(b == secondPfm): stat[1][1] += 1
    #     if(c == secondPfm): stat[2][1] += 1

    # print(stat[0])
    # print(stat[1])
    # print(stat[2])
        

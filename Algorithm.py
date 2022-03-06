from __future__ import annotations
from abc import ABC, abstractmethod
from random import randrange
from typing import List, Tuple

import random
from Bin import Bin
from Package import Package

class Algorithm(ABC):
    @abstractmethod
    def perform(self, bin: List, package: List): pass

class FirstFit(Algorithm):
    def perform(self, bin: List[int], package: int) -> int:
        bin_quantity = len(bin)
        allocation = -1
        for i in range(bin_quantity):
            if bin[i] >= package:
                allocation = i
                bin[i] -= package
                break
        return allocation

class BestFit(Algorithm):
    def perform(self, bin: List[int], package: int) -> int:
        bin_quantity = len(bin)
        bestIdx = -1
        for i in range(bin_quantity):
            if bin[i] >= package:
                if bestIdx == -1: 
                    bestIdx = i 
                elif bin[bestIdx] > bin[i]: 
                    bestIdx = i
        return bestIdx

class WorstFit(Algorithm):
    def perform(self, bin: List[int], package: int) -> int:
        bin_quantity = len(bin)
        wstIdx = -1
        for i in range(bin_quantity):
            if bin[i] >= package[i]:
                if wstIdx == -1: 
                    wstIdx = i 
                elif bin[wstIdx] < bin[i]: 
                    wstIdx = i
        return wstIdx

# class FirstFit(Algorithm):
#     def perform(self, bin: List, package: List) -> List:
#         # print("\n----------USING FIRST-FIT ALGORITHM----------")
#         bin_quantity = len(bin)
#         package_quantity = len(package)
#         allocation = [-1] * package_quantity
#         for i in range(package_quantity):
#             for j in range(bin_quantity):
#                 if bin[j] >= package[i]:
#                     allocation[i] = j
#                     bin[j] -= package[i]
#                     break
#         return allocation

# class BestFit(Algorithm):
#     def perform(self, bin: List, package: List) -> List:
#         # print("\n----------USING BEST-FIT ALGORITHM----------")
#         bin_quantity = len(bin)
#         package_quantity = len(package)
#         allocation = [-1] * package_quantity
#         for i in range(package_quantity):
#             bestIdx = -1
#             for j in range(bin_quantity):
#                 if bin[j] >= package[i]:
#                     if bestIdx == -1: 
#                         bestIdx = j 
#                     elif bin[bestIdx] > bin[j]: 
#                         bestIdx = j
#             if bestIdx != -1:
#                 allocation[i] = bestIdx 
#                 bin[bestIdx] -= package[i]
#         return allocation

# class WorstFit(Algorithm):
#     def perform(self, bin: List, package: List) -> List:
#         # print("\n----------USING WORST-FIT ALGORITHM----------")
#         bin_quantity = len(bin)
#         package_quantity = len(package)
#         allocation = [-1] * package_quantity
#         for i in range(package_quantity):
#             wstIdx = -1
#             for j in range(bin_quantity):
#                 if bin[j] >= package[i]:
#                     if wstIdx == -1: 
#                         wstIdx = j 
#                     elif bin[wstIdx] < bin[j]: 
#                         wstIdx = j
#             if wstIdx != -1:
#                 allocation[i] = wstIdx 
#                 bin[wstIdx] -= package[i]
#         return allocation
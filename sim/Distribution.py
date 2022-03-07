from abc import ABC, abstractmethod

import numpy as np



class Distribution(ABC):
    def __init__(self):
        pass

    @abstractmethod
    def next(self): pass





class Poisson(Distribution):
    def __init__(self, lamda):
        super().__init__()
        self.lamda = lamda

    def next(self):
        return round(60 * np.random.exponential(scale=1/self.lamda))
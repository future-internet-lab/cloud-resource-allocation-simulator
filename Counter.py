class CounterMeta(type):
    _instances = {}
    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            instance = super().__call__(*args, **kwargs)
            cls._instances[cls] = instance
        return cls._instances[cls]

# count id of server, vm, plink, vlink, like global variable

class Counter(metaclass=CounterMeta):
    __server: int = -1
    __vm: int = -1
    __plink: int = -1
    __vlink: int = -1
    def count_server(self):
        self.__server += 1
        return self.__server

    def count_vm(self):
        self.__vm += 1
        return self.__vm

    def count_plink(self):
        self.__plink += 1
        return self.__plink

    def count_vlink(self):
        self.__vlink += 1
        return self.__vlink
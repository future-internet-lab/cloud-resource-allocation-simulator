from abc import ABC, abstractmethod
import networkx as nx
import copy
import logging



class SubstrateSelector(ABC):
    def __init__(self):
        self.name = self.__class__.__name__

    @abstractmethod
    def analyse(self, substrate):
        pass




class ShortestPath(SubstrateSelector):
    def __init__(self):
        super().__init__()


    def analyse(self, sim, sfc):
        """
            #M: MANDATORY, cho dòng nào là BẮT BUỘC

            # ...
                thứ gì custom được sẽ như này
            ##############################
        """
        failed = 0 #M số trường hợp fail, nếu fail = số DC sẽ xử lí tiếp ở bên ngoài
        failDetail = [] #M lí do fail, sẽ in ra file .csv
        decision = {"sfc": {}, "outroute": 0} #M deploy ở substratetopo như nào, init (khởi tạo)

        # hai tham số init này là phụ, tuỳ vào mục đích mà tự định nghĩa ở đây
        power = 0 # nếu cần tối ưu power, ở đây không cần
        step = 100
        ##############################

        for DC in sim.DataCentres: #M lặp từng DC một
            result = DC.consider(sim, sfc) #M kết quả trả về sau khi chạy selector cho fattree
            if(not result in [1, 2]): #M 1 là fail do không còn bw, 2 là do không còn cpu
                _topo = copy.deepcopy(sim.topology) #M topo ngoài
                _sfc = result["sfc"] #M đây mới là sfc

                # bỏ đi những outlink không đủ bw, chắc không cần custom gì thêm đâu
                for out_link in list(_topo.edges.data()):
                    if(out_link[2]["capacity"] - out_link[2]["usage"] < _sfc["outlink"]):
                        _topo.remove_edge(out_link[0], out_link[1])
                ##############################

                try: #M
                    # thuật toán ở đây
                    route = nx.shortest_path(_topo, _sfc["Ingress"], _sfc["DataCentre"])
                    ##############################
                except: # cả đoạn except này MANDATORY
                    logging.debug(f"Cannot routing from Ingress-{_sfc['Ingress']} to DC-{DC.id}")
                    failed += 1
                    continue                    
                else: #M
                    # sau khi có route ở try, làm gì thì làm ở đây
                    if(len(route) < step): # chọn cách deploy ngắn nhất
                        step = len(route)
                        result["sfc"]["outroute"] = route #M
                        decision = result["sfc"] #M
                    ##############################
            else: # cannot deploy in current DC, MANDATORY cả đoạn else này
                failDetail.append([DC.id, result])
                logging.debug(f"cannot deploy SFC-{sfc['id']} on DC-{DC.id}")
                failed += 1

        return failed, failDetail, decision #M

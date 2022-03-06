topology = {
    "node": [
        {
            "model": "switch", # mandatory
            "tag": "core", # [core, aggregation, edge]
            "basePower": 39,
            "portPower": [0.42, 0.48, 0.9],
        },
        {
            "model": "server",
            "RAM": 4,
            "power": [205.1, 232.9, 260.7, 288.6, 316.4],
            "deployed": [
                [1, 0], [2, 3], [2, 5], [3, 2]
            ] # [sfc, vnf]
        }
    ],
    "link": [
        {
            "s": 0,
            "d": 1,
            "bw": 1000,
            "usage": 90
        }
    ]
}

sfc["structure"] = {
    "VNF": [
        {
            "id": 0,
            "SFC": 1,
            "RAM": 1,
            "onServer": 2
        }
    ],
    "vLink": [
        {
            "s": 0,
            "d": 1,
            "bw": 60
        }
    ]
}

deploy = {
    "sfc": "1",
    "node": [
        [0, 25], # [vnf, server]
        [1, 23]
    ],
    "link": [
        {
            "bw": 60,
            "route": [26, 15, 7, 1, 5, 14, 24]
        }
    ]
}
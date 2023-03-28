# Cloud Resource Allocation Simulator

## 1. Introduction

This is a Event-based Simulator for Cloud Resource Allocation Problem we researched in this Paper: [Service Function Chain Embedding in Centralized and Distributed Data Centers – a
Comparison]()

## 2. Installation

This Simulator is developed in Python Programming Language, using these Library:

* networkx
* simpy
* matplotlib
* numpy

Require Python version >= 3.6

## 3. Structure

## 4. Customization

## 5. Contributor



Hướng dẫn chạy công cụ mô phỏng:

tạo file có cấu trúc giống như file main.py, cấu hình tuỳ ý:
    + selector: bộ chọn server, triển khai thuật toán VNF mapping tại đây, mỗi thuật toán tương ứng với một class trong file sim/Selector.py
    + subSelector: bộ chọn DC, mỗi phương pháp chọn DC tương ứng với một class trong file sim/SubstrateSelector.py
    + app: Tập hợp thông số định nghĩa về một SFC, bao gồm: phân phối xuất hiện dist, bộ chọn selector, subSelector, các tham số phụ khác. Mỗi kiểu app tương ứng với một class trong sim/Application.py. Hiện tại công cụ có sẵn kiểu Sequence (các VNF nối đuôi nhau, topo có dạng đường thẳng) và Waxman (SFC được tạo ra là topo random theo thuật toán Waxman)
    + substrate: đại diện cho một substrate topo,

trong hàm main, cấu hình các thông số:
    + dist: Phân phối thời gian giữa hai lần tạo SFC, mỗi phân phối ứng với một class trong sim/Distribution.py
    + avg_TTL thời gian tồn tại trung bình của mỗi SFC
    + n_VNFs: khoảng random số VNF mỗi SFC
    + demand_VNF: tài nguyên CPU yêu cầu của mỗi VNF
    + bw: khoảng băng thông yêu cầu của mỗi SFC
    + runtime: thời gian chạy mô phỏng, tinh bằng phút
    + appArgs: các thông số phụ của app
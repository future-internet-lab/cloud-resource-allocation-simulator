không chạy bằng code runner
chạy bằng command line:

python3 [file] [strategy] [sortmode]

file:
	main_distributed.py
	main_centralized.py

strategy:
	1
	2

sortmode: (chỉ áp dụng với strategy 2 mới có option này)
	d : (giảm dần kích thước vnf)
	n : no sort 
	i : (tăng dần kích thước vnf)
	
ví dụ:
	python3 main_distributed.py 1
	python3 main_distributed.py 2 d
	

file log:
	_event.csv: các sự kiện, gồm có: create, drop, deploy, remove, remap, remap_failed, remap_successfully
	
	các sự kiện deploy nằm ở giữa sự kiện remap và remap_failed hoặc remap và remap_successfully được bỏ qua trong quá trình vẽ biểu đồ
	
	_sfc.json: cấu trúc các sfc
	_stat.json: danh sách các sfc được accept và fail
	
	
import operator
import numpy as np
import matplotlib.pyplot as plt

from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.preprocessing import PolynomialFeatures

def function(val,inter,x):
	return val[0][0]*x**0 + val[0][1]*x**1 + val[0][2]*x**2 + val[0][3]*x**3 + + val[0][4]*x**4 + inter
	
x = np.loadtxt('TotalReqActiveCR.txt', usecols=[0])
y = np.loadtxt('TotalPowerCR.txt', usecols=[0])

#x2 = np.loadtxt('capacityBLOP.txt', usecols=[0])
#y2 = np.loadtxt('totalChainAcceptanceBLOP.txt', usecols=[0])

#x3 = np.loadtxt('capacityBLRD.txt', usecols=[0])
#y3 = np.loadtxt('totalChainAcceptanceBLRD.txt', usecols=[0])

# plt.scatter(x,y)
# plt.show()
x = x[:, np.newaxis]
y = y[:, np.newaxis]
x = x*100/2100
#x2 = x2[:, np.newaxis]
#y2 = y2[:, np.newaxis]

#x3 = x3[:, np.newaxis]
#y3 = y3[:, np.newaxis]

polynomial_features= PolynomialFeatures(degree=4)
x_poly = polynomial_features.fit_transform(x)

#x2_poly = polynomial_features.fit_transform(x2)

#x3_poly = polynomial_features.fit_transform(x3)

model = LinearRegression()
#model2 = LinearRegression()
#model3 = LinearRegression()
model.fit(x_poly, y)
#model2.fit(x2_poly, y2)
#model3.fit(x3_poly, y3)
y_poly_pred = model.predict(x_poly)
#y2_poly_pred = model2.predict(x2_poly)
#y3_poly_pred = model3.predict(x3_poly)

rmse = np.sqrt(mean_squared_error(y,y_poly_pred))
r2 = r2_score(y,y_poly_pred)
#print(rmse)
#print(r2)
print(model.coef_)
print(model.intercept_)
for i in range(0,101,10):
	val = function(model.coef_,model.intercept_,i)
	print('Utilizaion level ' + str(i) + ': ' + str(val))

val2 = function(model.coef_,model.intercept_,95)
print('Utilizaion level 95: ' + str(val2))
plt.scatter(x, y, c='m')
#plt.scatter(x2, y2, c='r')
#plt.scatter(x3, y3, c='b')
# sort the values of x before line plot
sort_axis = operator.itemgetter(0)
sorted_zip = sorted(zip(x,y_poly_pred), key=sort_axis)
#sorted_zip2 = sorted(zip(x2,y2_poly_pred), key=sort_axis)
#sorted_zip3 = sorted(zip(x3,y3_poly_pred), key=sort_axis)
x, y_poly_pred = zip(*sorted_zip)
#x2, y2_poly_pred = zip(*sorted_zip2)
#x3, y3_poly_pred = zip(*sorted_zip3)
plt.plot(x, y_poly_pred, color='m')
#plt.plot(x2, y2_poly_pred, color='r')
#plt.plot(x3, y3_poly_pred, color='b')

plt.xlabel('Power (W)', fontsize=12)
plt.ylabel('Teperature (oC)', fontsize=12)
plt.show()

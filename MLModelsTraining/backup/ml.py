import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import xgboost as xgb

from sklearn.decomposition import PCA
from sklearn.neural_network import MLPRegressor 
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import PolynomialFeatures
from sklearn.model_selection import train_test_split
from sklearn.model_selection import KFold
from sklearn.model_selection import cross_val_score
from sklearn.model_selection import GridSearchCV
from sklearn.linear_model import Ridge
from sklearn.linear_model import LinearRegression
from sklearn.linear_model import Lasso
from sklearn.linear_model import ElasticNet
from sklearn.tree import DecisionTreeRegressor
from sklearn.neighbors import KNeighborsRegressor
from sklearn.svm import SVR
from sklearn.pipeline import Pipeline
from sklearn.ensemble import RandomForestRegressor
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import mean_squared_error
# Loading the data from the file into mydata
mydata = pd.read_excel('listStoreValue.xlsx', header=None).to_numpy()
n = len(mydata)
X = mydata[:,0:5]
y = mydata[:,-1]

# data_dmatrix = xgb.DMatrix(data=X,label=y)
# X_train, X_test, y_train, y_test = train_test_split(X,y,train_size=0.9)

#Preprocessing data
# scaler = StandardScaler()
# scaler.fit(X)
# X_scale = scaler.transform(X)
# print(X_scale.shape)

# X_visual = PCA(n_components=1).fit_transform(X_scale)
# fig,ax = plt.subplots()
# ax.scatter(X_visual,y)
# ax.set_xlabel("PCA(n_components=1) of X")
# ax.set_ylabel("y")
# fig.suptitle("Dataset visualization of X and y")
# fig.show()

models = []
models.append(('LR', LinearRegression()))
models.append(('LASSO', Lasso()))
models.append(('RIDGE', Ridge()))
models.append(('EN', ElasticNet()))
models.append(('KNN', KNeighborsRegressor()))
models.append(('CART', DecisionTreeRegressor()))
models.append(('SVR', SVR(gamma='auto')))
models.append(('RF',RandomForestRegressor(n_estimators=100)))
models.append(('GBM',GradientBoostingRegressor()))

results = []
names = []
for name, model in models:
    kfold = KFold(n_splits=10, random_state=7)
    cv_results = cross_val_score(model, X, y, cv=kfold, scoring="neg_mean_squared_error")
    model.fit(X,y)
    # val = mean_squared_error(model.predict(X_test), y_test)
    # train = mean_squared_error(model.predict(X), y)
    results.append(cv_results)
    names.append(name)
    msg = "%s: \n Cross-validation Error: %f (%f)" % (name, cv_results.mean(), cv_results.std())
    print(msg)
    # print(" Training Error: ",train)
    # print(" Validation Error:", val)


#Choose DecisionTreeRegressor due to lowest CV
# FinalModel = DecisionTreeRegressor()
# FinalModel = MLPRegressor()
# FinalModel = GradientBoostingRegressor()
#FinalModel = KNeighborsRegressor(10)
# FinalModel.fit(X_scale,y)
# y_pred=FinalModel.predict(X_scale).round()
# kfold = KFold(n_splits=10, random_state=7)
# cv_results = cross_val_score(FinalModel, X_scale, y, cv=kfold, scoring="neg_mean_squared_error")

# print("\nFinal Model: "+str(FinalModel).split("(")[0] )
# # print(" Training Error: {}, RMSE: {}".format(mean_squared_error(y_pred,y),np.sqrt(mean_squared_error(y_pred,y))))
# print(" Cross-validation Error: {}, RMSE: {}".format(cv_results.mean(),np.sqrt(-cv_results.mean())))

# def plot_learning_curves(model, X, y):
#   X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.2)
#   train_errors, val_errors = [], []
#   for m in range(1, len(X_train), 10):
#     model.fit(X_train[:m], y_train[:m])
#     y_train_predict = model.predict(X_train[:m]).round()
#     y_val_predict = model.predict(X_val).round()
#     train_errors.append(mean_squared_error(y_train[:m], y_train_predict))
#     val_errors.append(mean_squared_error(y_val, y_val_predict))
#   plt.plot(range(1, len(X_train), 10), np.sqrt(train_errors), "r-+", linewidth=2, label="train")
#   plt.plot(range(1, len(X_train), 10), np.sqrt(val_errors), "b-", linewidth=3, label="val")
#   plt.xlabel("Training Examples")
#   plt.ylabel("RMS Errors")
#   plt.suptitle(str(model).split("(")[0] + " Learning Curves")
#   plt.legend()
#   plt.show()


# plot_learning_curves(FinalModel, X, y)
from lightgbm import LGBMRegressor
from pandas import DataFrame

import pandas

def load_csv(name):
	return pandas.read_csv("csv/" + name, na_values = ["N/A", "NA"])

def store_csv(df, name):
	df.to_csv("csv/" + name, index = False)

def store_lgbm(lgbm, name):
	lgbm.booster_.save_model("lgbm/" + name)

auto_df = load_csv("Auto.csv")

auto_X = auto_df[auto_df.columns.difference(["mpg"])]
auto_y = auto_df["mpg"]

auto_lgbm = LGBMRegressor(objective = "regression", n_estimators = 31)
auto_lgbm.fit(auto_X, auto_y, feature_name = auto_X.columns.values)

store_lgbm(auto_lgbm, "RegressionAuto.txt")

store_csv(DataFrame(auto_lgbm.predict(auto_X), columns = ["_target"]), "RegressionAuto.csv")

housing_df = load_csv("Housing.csv")

housing_X = housing_df[housing_df.columns.difference(["MEDV"])]
housing_y = housing_df["MEDV"]

housing_lgbm = LGBMRegressor(objective = "regression", n_estimators = 31)
housing_lgbm.fit(housing_X, housing_y, feature_name = housing_X.columns.values)

store_lgbm(housing_lgbm, "RegressionHousing.txt")

store_csv(DataFrame(housing_lgbm.predict(housing_X), columns = ["_target"]), "RegressionHousing.csv")

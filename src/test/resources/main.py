from lightgbm import LGBMClassifier, LGBMRegressor
from pandas import DataFrame

import pandas

def load_csv(name):
	return pandas.read_csv("csv/" + name, na_values = ["N/A", "NA"])

def store_csv(df, name):
	df.to_csv("csv/" + name, index = False)

def store_lgbm(lgbm, name):
	lgbm.booster_.save_model("lgbm/" + name)

#
# Multi-class classification
#

iris_df = load_csv("Iris.csv")

iris_X = iris_df[iris_df.columns.difference(["Species"])]
iris_y = iris_df["Species"]

iris_lgbm = LGBMClassifier(n_estimators = 11)
iris_lgbm.fit(iris_X, iris_y)

store_lgbm(iris_lgbm, "ClassificationIris.txt")

species = DataFrame(iris_lgbm.predict(iris_X), columns = ["_target"]).replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
species_proba = DataFrame(iris_lgbm.predict_proba(iris_X), columns = ["probability_0", "probability_1", "probability_2"])

store_csv(pandas.concat((species, species_proba), axis = 1), "ClassificationIris.csv")

#
# Binary classification
#

versicolor_df = load_csv("Versicolor.csv")

versicolor_X = versicolor_df[versicolor_df.columns.difference(["Species"])]
versicolor_y = versicolor_df["Species"]

versicolor_lgbm = LGBMClassifier(n_estimators = 11)
versicolor_lgbm.fit(versicolor_X, versicolor_y)

store_lgbm(versicolor_lgbm, "ClassificationVersicolor.txt")

versicolor = DataFrame(versicolor_lgbm.predict(versicolor_X), columns = ["_target"])
versicolor_proba = DataFrame(versicolor_lgbm.predict_proba(versicolor_X), columns = ["probability_0", "probability_1"])

store_csv(pandas.concat((versicolor, versicolor_proba), axis = 1), "ClassificationVersicolor.csv")

#
# Regression
#

auto_df = load_csv("Auto.csv")

#auto_X = auto_df[auto_df.columns.difference(["mpg"])]
auto_X = auto_df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
auto_y = auto_df["mpg"]

auto_lgbm = LGBMRegressor(n_estimators = 31)
auto_lgbm.fit(auto_X.as_matrix(), auto_y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"], categorical_feature = ["cylinders", "model_year", "origin"])

store_lgbm(auto_lgbm, "RegressionAuto.txt")

mpg = DataFrame(auto_lgbm.predict(auto_X), columns = ["_target"])

store_csv(mpg, "RegressionAuto.csv")

housing_df = load_csv("Housing.csv")

housing_X = housing_df[housing_df.columns.difference(["MEDV"])]
housing_y = housing_df["MEDV"]

housing_lgbm = LGBMRegressor(n_estimators = 31)
housing_lgbm.fit(housing_X, housing_y)

store_lgbm(housing_lgbm, "RegressionHousing.txt")

medv = DataFrame(housing_lgbm.predict(housing_X), columns = ["_target"])

store_csv(medv, "RegressionHousing.csv")

#
# Poisson regression
#

visit_df = load_csv("Visit.csv")

visit_X = visit_df[["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"]]
visit_y = visit_df["docvis"]

visit_lgbm = LGBMRegressor(objective = "poisson", n_estimators = 31)
visit_lgbm.fit(visit_X.as_matrix(), visit_y, feature_name = ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"], categorical_feature = ["outwork", "female", "married", "kids", "self"])

store_lgbm(visit_lgbm, "RegressionVisit.txt")

docvis = DataFrame(visit_lgbm.predict(visit_X), columns = ["_target"])

store_csv(docvis, "RegressionVisit.csv")

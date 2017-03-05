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

def build_iris(name):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(n_estimators = 11)
	lgbm.fit(X, y)
	store_lgbm(lgbm, "Classification" + name + ".txt")

	species = DataFrame(lgbm.predict(X), columns = ["_target"]).replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
	species_proba = DataFrame(lgbm.predict_proba(X), columns = ["probability_0", "probability_1", "probability_2"])
	store_csv(pandas.concat((species, species_proba), axis = 1), "Classification" + name + ".csv")

build_iris("Iris")
build_iris("IrisNA")

#
# Binary classification
#

def build_versicolor(name):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(n_estimators = 11)
	lgbm.fit(X, y)
	store_lgbm(lgbm, "Classification" + name + ".txt")

	versicolor = DataFrame(lgbm.predict(X), columns = ["_target"])
	versicolor_proba = DataFrame(lgbm.predict_proba(X), columns = ["probability_0", "probability_1"])
	store_csv(pandas.concat((versicolor, versicolor_proba), axis = 1), "Classification" + name + ".csv")

build_versicolor("Versicolor")

#
# Regression
#

def build_auto(name):
	df = load_csv(name + ".csv")
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(n_estimators = 31)
	lgbm.fit(X.as_matrix(), y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"], categorical_feature = ["cylinders", "model_year", "origin"])
	store_lgbm(lgbm, "Regression" + name + ".txt")

	mpg = DataFrame(lgbm.predict(X), columns = ["_target"])
	store_csv(mpg, "Regression" + name + ".csv")

build_auto("Auto")
build_auto("AutoNA")

def build_housing(name):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["MEDV"])]
	y = df["MEDV"]

	lgbm = LGBMRegressor(n_estimators = 31)
	lgbm.fit(X, y, categorical_feature = ["CHAS"])
	store_lgbm(lgbm, "Regression" + name + ".txt")

	medv = DataFrame(lgbm.predict(X), columns = ["_target"])
	store_csv(medv, "Regression" + name + ".csv")

build_housing("Housing")
build_housing("HousingNA")

#
# Poisson regression
#

def build_visit(name):
	df = load_csv(name + ".csv")
	X = df[["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"]]
	y = df["docvis"]

	lgbm = LGBMRegressor(objective = "poisson", n_estimators = 31)
	lgbm.fit(X.as_matrix(), y, feature_name = ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"], categorical_feature = ["female", "married", "kids"]) # categorical_feature = ["outwork", "female", "married", "kids", "self"]
	store_lgbm(lgbm, "Regression" + name + ".txt")

	docvis = DataFrame(lgbm.predict(X), columns = ["_target"])
	store_csv(docvis, "Regression" + name + ".csv")

build_visit("Visit")
build_visit("VisitNA")

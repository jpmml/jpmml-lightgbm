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

def build_iris(name, num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(n_estimators = 11)
	lgbm.fit(X, y)

	if(num_iteration == 0):
		store_lgbm(lgbm, "Classification" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	species = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"]).replace("setosa", "0").replace("versicolor", "1").replace("virginica", "2")
	species_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)", "probability(2)"])
	store_csv(pandas.concat((species, species_proba), axis = 1), "Classification" + name + ".csv")

build_iris("Iris")
build_iris("Iris", 7)
build_iris("IrisNA")
build_iris("IrisNA", 7)

#
# Binary classification
#

def build_versicolor(name, num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["Species"])]
	y = df["Species"]

	lgbm = LGBMClassifier(n_estimators = 11)
	lgbm.fit(X, y)

	if(num_iteration == 0):
		store_lgbm(lgbm, "Classification" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	versicolor = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	versicolor_proba = DataFrame(lgbm.predict_proba(X, num_iteration = num_iteration), columns = ["probability(0)", "probability(1)"])
	store_csv(pandas.concat((versicolor, versicolor_proba), axis = 1), "Classification" + name + ".csv")

build_versicolor("Versicolor")
build_versicolor("Versicolor", 9)

#
# Regression
#

def build_auto(name, num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"]]
	y = df["mpg"]

	lgbm = LGBMRegressor(n_estimators = 31)
	lgbm.fit(X.as_matrix(), y, feature_name = ["cylinders", "displacement", "horsepower", "weight", "acceleration", "model_year", "origin"], categorical_feature = ["cylinders", "model_year", "origin"])

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	mpg = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(mpg, "Regression" + name + ".csv")

build_auto("Auto")
build_auto("Auto", 17)
build_auto("AutoNA")
build_auto("AutoNA", 17)

def build_housing(name, num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[df.columns.difference(["MEDV"])]
	y = df["MEDV"]

	lgbm = LGBMRegressor(n_estimators = 51)
	lgbm.fit(X, y, categorical_feature = ["CHAS"])

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	medv = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(medv, "Regression" + name + ".csv")

build_housing("Housing")
build_housing("Housing", 31)
build_housing("HousingNA")
build_housing("HousingNA", 31)

#
# Poisson regression
#

def build_visit(name, num_iteration = 0):
	df = load_csv(name + ".csv")
	X = df[["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"]]
	y = df["docvis"]

	lgbm = LGBMRegressor(objective = "poisson", n_estimators = 71)
	lgbm.fit(X.as_matrix(), y, feature_name = ["age", "outwork", "female", "married", "kids", "hhninc", "educ", "self"], categorical_feature = ["female", "married", "kids"]) # categorical_feature = ["outwork", "female", "married", "kids", "self"]

	if(num_iteration == 0):
		store_lgbm(lgbm, "Regression" + name + ".txt")
	else:
		name = (name + "@" + str(num_iteration))

	docvis = DataFrame(lgbm.predict(X, num_iteration = num_iteration), columns = ["_target"])
	store_csv(docvis, "Regression" + name + ".csv")

build_visit("Visit")
build_visit("Visit", 31)
build_visit("VisitNA")
build_visit("VisitNA", 31)

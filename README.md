JPMML-LightGBM [![Build Status](https://github.com/jpmml/jpmml-lightgbm/workflows/maven/badge.svg)](https://github.com/jpmml/jpmml-lightgbm/actions?query=workflow%3A%22maven%22)
==============

Java library and command-line application for converting [LightGBM](https://github.com/Microsoft/LightGBM) models to PMML.

# Prerequisites #

* LightGBM 2.0.0 or newer.
* Java 1.8 or newer.

# Installation #

Enter the project root directory and build using [Apache Maven](https://maven.apache.org/):
```
mvn clean install
```

The build produces a library JAR file `pmml-lightgbm/target/pmml-lightgbm-1.4-SNAPSHOT.jar`, and an executable uber-JAR file `pmml-lightgbm-example/target/pmml-lightgbm-example-executable-1.4-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use LightGBM to train a model.
2. Save the model to a text file in a local filesystem.
3. Use the JPMML-LightGBM command-line converter application to turn this text file to a PMML file.

### The LightGBM side of operations

Using the [`lightgbm`](https://github.com/Microsoft/LightGBM/tree/master/python-package) package to train a regression model for the example Boston housing dataset:

```python
from sklearn.datasets import load_boston

boston = load_boston()

from lightgbm import LGBMRegressor

lgbm = LGBMRegressor(objective = "regression")
lgbm.fit(boston.data, boston.target, feature_name = boston.feature_names)

lgbm.booster_.save_model("lightgbm.txt")
```

### The JPMML-LightGBM side of operations

Converting the text file `lightgbm.txt` to a PMML file `lightgbm.pmml`:
```
java -jar pmml-lightgbm-example/target/pmml-lightgbm-example-executable-1.4-SNAPSHOT.jar --lgbm-input lightgbm.txt --pmml-output lightgbm.pmml
```

Getting help:
```
java -jar pmml-lightgbm-example/target/pmml-lightgbm-example-executable-1.4-SNAPSHOT.jar  --help
```

# Documentation #

* [Stacking Scikit-Learn, LightGBM and XGBoost models](https://openscoring.io/blog/2020/01/02/stacking_sklearn_lightgbm_xgboost/)
* [Deploying LightGBM models on Java/JVM platform](https://openscoring.io/blog/2019/12/03/deploying_lightgbm_java/)
* [Extending Scikit-Learn with GBDT plus LR ensemble (GBDT+LR) model type](https://openscoring.io/blog/2019/06/19/sklearn_gbdt_lr_ensemble/) (Using LightGBM models on the GBDT side of GBDT+LR ensemble)
* [Converting Scikit-Learn plus LightGBM pipelines to PMML documents](https://openscoring.io/blog/2019/04/07/converting_sklearn_lightgbm_pipeline_pmml/)

# License #

JPMML-LightGBM is licensed under the terms and conditions of the [GNU Affero General Public License, Version 3.0](https://www.gnu.org/licenses/agpl-3.0.html).

If you would like to use JPMML-LightGBM in a proprietary software project, then it is possible to enter into a licensing agreement which makes JPMML-LightGBM available under the terms and conditions of the [BSD 3-Clause License](https://opensource.org/licenses/BSD-3-Clause) instead.

# Additional information #

JPMML-LightGBM is developed and maintained by Openscoring Ltd, Estonia.

Interested in using [Java PMML API](https://github.com/jpmml) software in your company? Please contact [info@openscoring.io](mailto:info@openscoring.io)

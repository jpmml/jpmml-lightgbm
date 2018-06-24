JPMML-LightGBM
==============

Java library and command-line application for converting [LightGBM](https://github.com/Microsoft/LightGBM) models to PMML.

# Prerequisites #

* Java 1.8 or newer.

# Installation #

Enter the project root directory and build using [Apache Maven](http://maven.apache.org/):
```
mvn clean install
```

The build produces an executable uber-JAR file `target/converter-executable-1.2-SNAPSHOT.jar`.

# Usage #

A typical workflow can be summarized as follows:

1. Use LightGBM to train a model.
2. Save the model to a text file in a local filesystem.
3. Use the JPMML-LightGBM command-line converter application to turn this text file to a PMML file.

### The LightGBM side of operations

Using the [`lightgbm`](https://github.com/Microsoft/LightGBM/tree/master/python-package) package to train a regression model for the example boston housing dataset:
```Python
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
java -jar target/jpmml-lightgbm-executable-1.2-SNAPSHOT.jar --lgbm-input lightgbm.txt --pmml-output lightgbm.pmml
```

Getting help:
```
java -jar target/jpmml-lightgbm-executable-1.2-SNAPSHOT.jar --help
```

# License #

JPMML-LightGBM is licensed under the [GNU Affero General Public License (AGPL) version 3.0](http://www.gnu.org/licenses/agpl-3.0.html). Other licenses are available on request.

# Additional information #

Please contact [info@openscoring.io](mailto:info@openscoring.io)

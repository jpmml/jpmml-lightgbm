/*
 * Copyright (c) 2017 Villu Ruusmann
 *
 * This file is part of JPMML-LightGBM
 *
 * JPMML-LightGBM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-LightGBM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-LightGBM.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.lightgbm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.InvalidValueTreatmentMethod;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.BooleanFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ImportanceDecorator;
import org.jpmml.converter.InvalidValueDecorator;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelEncoder;
import org.jpmml.converter.Schema;
import org.jpmml.converter.SchemaUtil;
import org.jpmml.converter.TypeUtil;
import org.jpmml.converter.WildcardFeature;
import org.jpmml.converter.visitors.NaNAsMissingDecorator;
import org.jpmml.lightgbm.visitors.TreeModelCompactor;

public class GBDT {

	private String version;

	private int max_feature_idx_;

	private int label_idx_;

	private String[] feature_names_;

	private String[] feature_infos_;

	private ObjectiveFunction object_function_;

	private Tree[] models_;

	private Map<String, String> feature_importances = Collections.emptyMap();

	private List<List<Object>> pandas_categorical = Collections.emptyList();


	public void load(List<Section> sections){
		int index = 0;

		if(true){
			Section section = sections.get(index);

			if(!section.checkId("tree")){
				throw new IllegalArgumentException();
			}

			this.version = section.getString("version");
			if(this.version != null){

				switch(this.version){
					case "v2":
					case "v3":
						break;
					default:
						throw new IllegalArgumentException("Version " + this.version + " is not supported");
				}
			}

			this.max_feature_idx_ = section.getInt("max_feature_idx");
			this.label_idx_ = section.getInt("label_index");
			this.feature_names_ = section.getStringArray("feature_names", this.max_feature_idx_ + 1);
			this.feature_infos_ = section.getStringArray("feature_infos", this.max_feature_idx_ + 1);

			this.object_function_ = loadObjectiveFunction(section);

			index++;
		}

		List<Tree> trees = new ArrayList<>();

		while(index < sections.size()){
			Section section = sections.get(index);

			String treeId = "Tree=" + String.valueOf(index - 1);

			if(!section.checkId(treeId)){
				break;
			}

			Tree tree = new Tree();
			tree.load(section);

			trees.add(tree);

			index++;
		}

		this.models_ = trees.toArray(new Tree[trees.size()]);

		index = skipEndSection("end of trees", sections, index);

		feature_importances:
		if(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId("feature importances:") && !section.checkId("feature_importances:")){
				break feature_importances;
			}

			this.feature_importances = loadFeatureSection(section);

			index++;
		}

		parameters:
		if(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId("parameters:")){
				break parameters;
			}

			index++;

			index = skipEndSection("end of parameters", sections, index);
		}

		pandas_categorical:
		if(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId(id -> id.startsWith("pandas_categorical:"))){
				break pandas_categorical;
			}

			this.pandas_categorical = loadPandasCategorical(section);

			index++;
		}
	}

	public Schema encodeSchema(FieldName targetField, List<String> targetCategories, LightGBMEncoder encoder){
		Label label;

		{
			if(targetField == null){
				targetField = FieldName.create("_target");
			}

			label = this.object_function_.encodeLabel(targetField, targetCategories, encoder);
		}

		List<Feature> features = new ArrayList<>();

		boolean hasPandasCategories = (this.pandas_categorical.size() > 0);

		int pandasCategoryIndex = 0;

		String[] featureNames = this.feature_names_;
		String[] featureInfos = this.feature_infos_;

		if(featureNames.length != featureInfos.length){
			throw new IllegalArgumentException();
		}

		for(int i = 0; i < featureNames.length; i++){
			String featureName = featureNames[i];
			String featureInfo = featureInfos[i];

			if(LightGBMUtil.isNone(featureInfo)){
				features.add(null);

				if(hasPandasCategories){
					pandasCategoryIndex++;
				}

				continue;
			}

			Boolean binary = isBinary(i);
			if(binary == null){
				binary = Boolean.FALSE;
			}

			Boolean categorical = isCategorical(i);
			if(categorical == null){
				categorical = LightGBMUtil.isValues(featureInfo);
			}

			FieldName activeField = FieldName.create(featureNames[i]);

			if(categorical){

				if(binary){
					throw new IllegalArgumentException();
				} else

				{
					Feature feature;

					if(hasPandasCategories){
						List<?> values = this.pandas_categorical.get(pandasCategoryIndex);

						DataType dataType = TypeUtil.getDataType(values);

						DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, dataType, values);

						if((DataType.BOOLEAN).equals(dataType) && (BooleanFeature.VALUES).equals(values)){
							feature = new BooleanFeature(encoder, dataField);
						} else

						{
							feature = new CategoricalFeature(encoder, dataField);
						}

						pandasCategoryIndex++;
					} else

					{
						List<Integer> values = LightGBMUtil.parseValues(featureInfo).stream()
							.filter(value -> value != GBDT.CATEGORY_MISSING)
							.sorted()
							.collect(Collectors.toList());

						DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, DataType.INTEGER, values);

						feature = new DirectCategoricalFeature(encoder, dataField);
					}

					features.add(feature);
				}

				encoder.addDecorator(activeField, new InvalidValueDecorator(InvalidValueTreatmentMethod.AS_MISSING, null));
			} else

			{
				if(binary){
					DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, DataType.INTEGER, Arrays.asList(0, 1));

					features.add(new BinaryFeature(encoder, dataField, 1));
				} else

				{
					DataField dataField = encoder.createDataField(activeField, OpType.CONTINUOUS, DataType.DOUBLE);

					Interval interval = LightGBMUtil.parseInterval(featureInfo);
					if(interval != null){
						dataField.addIntervals(interval);
					}

					features.add(new ContinuousFeature(encoder, dataField));
				}

				encoder.addDecorator(activeField, new InvalidValueDecorator(InvalidValueTreatmentMethod.AS_IS, null));
			}

			Double importance = getFeatureImportance(featureName);
			if(importance != null){
				encoder.addDecorator(activeField, new ImportanceDecorator(importance));
			}
		}

		if(hasPandasCategories){

			if(pandasCategoryIndex != this.pandas_categorical.size()){
				throw new IllegalArgumentException();
			}
		}

		return new Schema(encoder, label, features);
	}

	public Schema toLightGBMSchema(Schema schema){
		String[] featureNames = this.feature_names_;
		String[] featureInfos = this.feature_infos_;

		Function<Feature, Feature> function = new Function<Feature, Feature>(){

			private ModelEncoder encoder = (ModelEncoder)schema.getEncoder();

			private List<? extends Feature> features = schema.getFeatures();

			{
				SchemaUtil.checkSize(featureNames.length, this.features);
				SchemaUtil.checkSize(featureInfos.length, this.features);
			}

			@Override
			public Feature apply(Feature feature){
				int index = this.features.indexOf(feature);
				if(index < 0){
					throw new IllegalArgumentException();
				}

				String featureName = featureNames[index];
				String featureInfo = featureInfos[index];

				Double importance = getFeatureImportance(featureName);
				if(importance != null){
					this.encoder.addFeatureImportance(feature, importance);
				} // End if

				if(feature instanceof BinaryFeature){
					BinaryFeature binaryFeature = (BinaryFeature)feature;

					Boolean binary = isBinary(index);
					if(binary != null && binary.booleanValue()){
						return binaryFeature;
					}

					Boolean categorical = isCategorical(index);
					if(categorical != null && categorical.booleanValue()){
						CategoricalFeature categoricalFeature = new BinaryCategoricalFeature(this.encoder, binaryFeature);

						return categoricalFeature;
					}
				} else

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					Boolean categorical = isCategorical(index);
					if(categorical != null && categorical.booleanValue()){
						return categoricalFeature;
					}
				} else

				if(feature instanceof WildcardFeature){
					WildcardFeature wildcardFeature = (WildcardFeature)feature;

					Boolean binary = isBinary(index);
					if(binary != null && binary.booleanValue()){
						wildcardFeature.toCategoricalFeature(Arrays.asList(0, 1));

						BinaryFeature binaryFeature = new BinaryFeature(this.encoder, wildcardFeature, 1);

						return binaryFeature;
					}
				}

				return feature.toContinuousFeature();
			}
		};

		return schema.toTransformedSchema(function);
	}

	public PMML encodePMML(Map<String, ?> options, FieldName targetField, List<String> targetCategories){
		LightGBMEncoder encoder = new LightGBMEncoder();

		Boolean nanAsMissing = (Boolean)options.get(HasLightGBMOptions.OPTION_NAN_AS_MISSING);

		Schema schema = encodeSchema(targetField, targetCategories, encoder);

		MiningModel miningModel = encodeMiningModel(options, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		if((Boolean.TRUE).equals(nanAsMissing)){
			Visitor visitor = new NaNAsMissingDecorator();

			visitor.applyTo(pmml);
		}

		return pmml;
	}

	public MiningModel encodeMiningModel(Map<String, ?> options, Schema schema){
		Boolean compact = (Boolean)options.get(HasLightGBMOptions.OPTION_COMPACT);
		Integer numIterations = (Integer)options.get(HasLightGBMOptions.OPTION_NUM_ITERATION);

		MiningModel miningModel = this.object_function_.encodeMiningModel(Arrays.asList(this.models_), numIterations, schema)
			.setAlgorithmName("LightGBM");

		if((Boolean.TRUE).equals(compact)){
			Visitor visitor = new TreeModelCompactor();

			visitor.applyTo(miningModel);
		}

		return miningModel;
	}

	public String[] getFeatureNames(){
		return this.feature_names_;
	}

	public String[] getFeatureInfos(){
		return this.feature_infos_;
	}

	private Boolean isBinary(int feature){
		String featureInfo = this.feature_infos_[feature];

		if(!LightGBMUtil.isBinaryInterval(featureInfo)){
			return Boolean.FALSE;
		}

		Boolean result = null;

		Tree[] trees = this.models_;
		for(Tree tree : trees){
			Boolean binary = tree.isBinary(feature);

			if(binary != null){

				if(!binary.booleanValue()){
					return Boolean.FALSE;
				}

				result = Boolean.TRUE;
			}
		}

		return result;
	}

	private Boolean isCategorical(int feature){
		String featureInfo = this.feature_infos_[feature];

		if(!LightGBMUtil.isValues(featureInfo)){
			return Boolean.FALSE;
		}

		Boolean result = null;

		Tree[] trees = this.models_;
		for(Tree tree: trees){
			Boolean categorical = tree.isCategorical(feature);

			if(categorical != null){

				if(!categorical.booleanValue()){
					return Boolean.FALSE;
				}

				result = Boolean.TRUE;
			}
		}

		return result;
	}

	private Double getFeatureImportance(String featureName){
		String value = this.feature_importances.get(featureName);

		return (value != null ? Double.valueOf(value) : null);
	}

	static
	private ObjectiveFunction loadObjectiveFunction(Section section){
		String[] tokens = section.getStringArray("objective", -1);
		if(tokens.length == 0){
			throw new IllegalArgumentException();
		}

		boolean average_output = section.containsKey("average_output");

		String objective = tokens[0];

		Section config = new Section();

		if(tokens.length > 1){

			for(int i = 1; i < tokens.length; i++){
				config.put(tokens[i], ':');
			}
		}

		objective = parseObjectiveAlias(objective.toLowerCase());

		switch(objective){
			// RegressionL2loss
			case "regression":
			// RegressionL1loss
			case "regression_l1":
			// RegressionHuberLoss
			case "huber":
			// RegressionFairLoss
			case "fair":
			// RegressionQuantileloss
			case "quantile":
				return new Regression(average_output);
			// RegressionPoissonLoss
			case "poisson":
			// RegressionGammaLoss
			case "gamma":
			// RegressionTweedieLoss
			case "tweedie":
				return new PoissonRegression(average_output);
			// LambdarankNDCG
			case "lambdarank":
				return new Lambdarank(average_output);
			// BinaryLogloss
			case "binary":
				return new BinomialLogisticRegression(average_output, config.getDouble("sigmoid"));
			// CrossEntropy
			case "cross_entropy":
				return new BinomialLogisticRegression(average_output, 1d);
			// MulticlassSoftmax
			case "multiclass":
				return new MultinomialLogisticRegression(average_output, config.getInt("num_class"));
			default:
				throw new IllegalArgumentException(objective);
		}
	}

	static
	private String parseObjectiveAlias(String objective){

		switch(objective){
			case "regression":
			case "regression_l2":
			case "mean_squared_error":
			case "mse":
			case "l2":
			case "l2_root":
			case "root_mean_squared_error":
			case "rmse":
				return "regression";
			case "regression_l1":
			case "mean_absolute_error":
			case "l1":
			case "mae":
				return "regression_l1";
			case "multiclass":
			case "softmax":
				return "multiclass";
			case "multiclassova":
			case "multiclass_ova":
			case "ova":
			case "ovr":
				return "multiclassova";
			case "xentropy":
			case "cross_entropy":
				return "cross_entropy";
			case "xentlambda":
			case "cross_entropy_lambda":
				return "cross_entropy_lambda";
			case "mean_absolute_percentage_error":
			case "mape":
				return "mape";
			case "none":
			case "null":
			case "custom":
			case "na":
				return "custom";
			default:
				return objective;
		}
	}

	private Map<String, String> loadFeatureSection(Section section){
		Map<String, String> result = new LinkedHashMap<>(section);

		(result.keySet()).retainAll(Arrays.asList(this.feature_names_));

		return result;
	}

	private List<List<Object>> loadPandasCategorical(Section section){
		String id = section.id();

		try {
			PandasCategoricalParser parser = new PandasCategoricalParser(id);

			return parser.parsePandasCategorical();
		} catch(Exception e){
			throw new IllegalArgumentException(id, e);
		}
	}

	static
	private int skipEndSection(String id, List<Section> sections, int index){

		if(index < sections.size()){
			Section section = sections.get(index);

			if(section.checkId(id)){
				return (index + 1);
			}
		}

		return index;
	}

	private static final Integer CATEGORY_MISSING = -1;
}
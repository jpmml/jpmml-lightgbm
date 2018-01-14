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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Interval;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ImportanceDecorator;
import org.jpmml.converter.Label;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.lightgbm.visitors.TreeModelCompactor;

public class GBDT {

	private String version;

	private int max_feature_idx_;

	private int label_idx_;

	private String[] feature_names_;

	private String[] feature_infos_;

	private boolean boost_from_average_;

	private ObjectiveFunction object_function_;

	private Tree[] models_;

	private Map<String, String> feature_importances = Collections.emptyMap();

	private List<List<String>> pandas_categorical = Collections.emptyList();


	public void load(List<Section> sections){
		int index = 0;

		if(true){
			Section section = sections.get(index);

			if(!section.checkId("tree")){
				throw new IllegalArgumentException();
			}

			this.version = section.getString("version");
			if(this.version != null && !("v2").equals(this.version)){
				throw new IllegalArgumentException(this.version);
			}

			this.max_feature_idx_ = section.getInt("max_feature_idx");
			this.label_idx_ = section.getInt("label_index");
			this.feature_names_ = section.getStringArray("feature_names", this.max_feature_idx_ + 1);
			this.feature_infos_ = section.getStringArray("feature_infos", this.max_feature_idx_ + 1);
			this.boost_from_average_ = section.containsKey("boost_from_average");

			this.object_function_ = parseObjectiveFunction(section.getString("objective"));

			index++;
		}

		List<Tree> trees = new ArrayList<>();

		while(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId("Tree=" + String.valueOf(index - 1))){
				break;
			}

			Tree tree = new Tree();
			tree.load(section);

			trees.add(tree);

			index++;
		}

		this.models_ = trees.toArray(new Tree[trees.size()]);

		feature_importances:
		if(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId("feature importances:")){
				break feature_importances;
			}

			this.feature_importances = loadFeatureSection(section);

			index++;
		}

		pandas_categorical:
		if(index < sections.size()){
			Section section = sections.get(index);

			String id = section.id();

			if(id != null && !(id).startsWith("pandas_categorical:")){
				break pandas_categorical;
			}

			this.pandas_categorical = loadPandasCategorical(section);

			index++;
		}
	}

	public PMML encodePMML(FieldName targetField, List<String> targetCategories, Integer numIteration, boolean transform){
		LightGBMEncoder encoder = new LightGBMEncoder();

		Label label;

		{
			if(targetField == null){
				targetField = FieldName.create("_target");
			}

			label = this.object_function_.encodeLabel(targetField, targetCategories, encoder);
		}

		List<Feature> features = new ArrayList<>();

		int categoryIndex = 0;

		String[] featureNames = this.feature_names_;
		String[] featureInfos = this.feature_infos_;
		for(int i = 0; i < featureNames.length; i++){
			String featureName = featureNames[i];
			String featureInfo = featureInfos[i];

			if(("none").equals(featureInfo)){
				features.add(null);

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

					if(this.pandas_categorical.size() > 0){
						List<String> categories = this.pandas_categorical.get(categoryIndex);

						DataType dataType = LightGBMUtil.getDataType(categories);
						switch(dataType){
							case INTEGER:
								categories = Lists.transform(Lists.transform(categories, LightGBMUtil.CATEGORY_PARSER), LightGBMUtil.CATEGORY_FORMATTER);
								break;
							default:
								break;
						}

						DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, dataType, categories);

						feature = new CategoricalFeature(encoder, dataField);
					} else

					{
						List<Integer> categories = new ArrayList<>();
						categories.addAll(LightGBMUtil.parseValues(featureInfo));

						if(categories.contains(GBDT.CATEGORY_MISSING)){
							categories.remove(GBDT.CATEGORY_MISSING);
						}

						Collections.sort(categories);

						DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, DataType.INTEGER);

						PMMLUtil.addValues(dataField, Lists.transform(categories, LightGBMUtil.CATEGORY_FORMATTER));

						feature = new DirectCategoricalFeature(encoder, dataField);
					}

					features.add(feature);
				}

				categoryIndex++;
			} else

			{
				if(binary){
					DataField dataField = encoder.createDataField(activeField, OpType.CATEGORICAL, DataType.INTEGER, Arrays.asList("0", "1"));

					features.add(new BinaryFeature(encoder, dataField, "1"));
				} else

				{
					Interval interval = LightGBMUtil.parseInterval(featureInfo);

					DataField dataField = encoder.createDataField(activeField, OpType.CONTINUOUS, DataType.DOUBLE);

					PMMLUtil.addIntervals(dataField, Arrays.asList(interval));

					features.add(new ContinuousFeature(encoder, dataField));
				}
			}

			ImportanceDecorator importanceDecorator = new ImportanceDecorator()
				.setImportance(getFeatureImportance(featureName));

			encoder.addDecorator(activeField, importanceDecorator);
		}

		Schema schema = new Schema(label, features);

		MiningModel miningModel = encodeMiningModel(numIteration, transform, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		return pmml;
	}

	public MiningModel encodeMiningModel(Integer numIteration, boolean transform, Schema schema){
		MiningModel miningModel = this.object_function_.encodeMiningModel(Arrays.asList(this.models_), numIteration, schema);

		if(transform){
			List<Visitor> visitors = Arrays.<Visitor>asList(new TreeModelCompactor());

			for(Visitor visitor : visitors){
				visitor.applyTo(miningModel);
			}
		}

		return miningModel;
	}

	public String[] getFeatureNames(){
		return this.feature_names_;
	}

	public String[] getFeatureInfos(){
		return this.feature_infos_;
	}

	Boolean isBinary(int feature){
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

	Boolean isCategorical(int feature){
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

	/**
	 * @see #getFeatureNames()
	 */
	Double getFeatureImportance(String featureName){
		String value = this.feature_importances.get(featureName);

		return (value != null ? Double.valueOf(value) : null);
	}

	private Map<String, String> loadFeatureSection(Section section){
		Map<String, String> result = new LinkedHashMap<>(section);

		(result.keySet()).retainAll(Arrays.asList(this.feature_names_));

		return result;
	}

	private List<List<String>> loadPandasCategorical(Section section){
		List<List<String>> result = new ArrayList<>();

		String id = section.id();

		if(("pandas_categorical:null").equals(id)){
			return result;
		} // End if

		if(!id.startsWith("pandas_categorical:[") || !id.endsWith("]")){
			throw new IllegalArgumentException(id);
		}

		id = id.substring("pandas_categorical:[".length(), id.length() - "]".length());

		while(true){
			int index = id.indexOf(']');

			if(index < 0){
				break;
			}

			String values = id.substring(0, index + 1);

			if(!values.startsWith("[") || !values.endsWith("]")){
				throw new IllegalArgumentException(values);
			}

			values = values.substring("[".length(), values.length() - "]".length());

			result.add(loadPandasCategoryValues(values));

			id = id.substring(index + 1);

			if(id.startsWith(", ")){
				id = id.substring(", ".length());
			}
		}

		if(!("").equals(id)){
			throw new IllegalArgumentException(id);
		}

		return result;
	}

	static
	private List<String> loadPandasCategoryValues(String string){
		List<String> values = Arrays.asList(string.split(",\\s"));

		Function<String, String> function = new Function<String, String>(){

			@Override
			public String apply(String string){

				if((string.length() > 1) && (string.startsWith("\"") && string.endsWith("\""))){
					string = string.substring("\"".length(), string.length() - "\"".length());
				}

				return string;
			}
		};

		return Lists.transform(values, function);
	}

	static
	public ObjectiveFunction parseObjectiveFunction(String string){
		String[] tokens = LightGBMUtil.parseStringArray(string, -1);

		if(tokens.length == 0){
			throw new IllegalArgumentException(string);
		}

		String objective = tokens[0];

		Section section = new Section();
		for(int i = 1; i < tokens.length; i++){
			section.put(tokens[i], ':');
		}

		switch(objective){
			// RegressionL2loss
			case "regression":
			case "regression_l2":
			case "mean_squared_error":
			case "mse":
			// RegressionL1loss
			case "regression_l1":
			case "mean_absolute_error":
			case "mae":
			// RegressionHuberLoss
			case "huber":
			// RegressionFairLoss
			case "fair":
				return new Regression();
			// RegressionPoissonLoss
			case "poisson":
				return new PoissonRegression();
			case "binary":
				return new BinomialLogisticRegression(section.getDouble("sigmoid"));
			case "multiclass":
				return new MultinomialLogisticRegression(section.getInt("num_class"));
			default:
				throw new IllegalArgumentException(objective);
		}
	}

	private static final Integer CATEGORY_MISSING = -1;
}
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

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ImportanceDecorator;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;

public class GBDT {

	private int max_feature_idx_;

	private int num_class_;

	private double sigmoid_;

	private int label_idx_;

	private String[] feature_names_;

	private ObjectiveFunction object_function_;

	private Tree[] models_;

	private Map<String, String> feature_importances = Collections.emptyMap();

	private Map<String, String> feature_values = Collections.emptyMap();


	public void load(List<Section> sections){
		int index = 0;

		if(true){
			Section section = sections.get(index);

			if(!section.checkId("tree")){
				throw new IllegalArgumentException();
			}

			this.max_feature_idx_ = section.getInt("max_feature_idx");
			this.num_class_ = section.getInt("num_class");
			this.sigmoid_ = section.getDouble("sigmoid");
			this.label_idx_ = section.getInt("label_index");
			this.feature_names_ = section.getStringArray("feature_names", this.max_feature_idx_ + 1);

			this.object_function_ = parseObjectiveFunction(section.getString("objective"), this.num_class_, this.sigmoid_);

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
		} // End if

		feature_information:
		if(index < sections.size()){
			Section section = sections.get(index);

			if(!section.checkId("feature information:")){
				break feature_information;
			}

			this.feature_values = loadFeatureSection(section);

			index++;
		}
	}

	public PMML encodePMML(FieldName targetField, List<String> targetCategories){
		LightGBMEncoder encoder = new LightGBMEncoder();

		Label label;

		{
			if(targetField == null){
				targetField = FieldName.create("_target");
			}

			label = this.object_function_.encodeLabel(targetField, targetCategories, encoder);
		}

		List<Feature> features = new ArrayList<>();

		String[] featureNames = this.feature_names_;
		for(int i = 0; i < featureNames.length; i++){
			String featureName = featureNames[i];

			FieldName activeField = FieldName.create(featureNames[i]);

			Boolean categorical = isCategorical(i);
			if(categorical == null){
				categorical = Boolean.FALSE;
			}

			DataField dataField = encoder.createDataField(activeField, (categorical ? OpType.CATEGORICAL : OpType.CONTINUOUS), DataType.DOUBLE);

			String value = getFeatureValue(featureName);
			if(value != null){

				if(categorical){
					PMMLUtil.addValues(dataField, LightGBMUtil.parseValues(value));
				} else

				{
					PMMLUtil.addIntervals(dataField, LightGBMUtil.parseIntervals(value));
				}
			}

			ImportanceDecorator importanceDecorator = new ImportanceDecorator()
				.setImportance(getFeatureImportance(featureName));

			encoder.addDecorator(activeField, importanceDecorator);

			MissingValueDecorator missingValueDecorator = new MissingValueDecorator()
				.setMissingValueReplacement("0");

			encoder.addDecorator(activeField, missingValueDecorator);

			features.add(new ContinuousFeature(encoder, dataField));
		}

		Schema schema = new Schema(label, features);

		MiningModel miningModel = encodeMiningModel(schema);

		PMML pmml = encoder.encodePMML(miningModel);

		return pmml;
	}

	public MiningModel encodeMiningModel(Schema schema){
		MiningModel miningModel = this.object_function_.encodeMiningModel(Arrays.asList(this.models_), schema);

		return miningModel;
	}

	public String[] getFeatureNames(){
		return this.feature_names_;
	}

	Boolean isBinary(int feature){
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

	/**
	 * @see #getFeatureNames()
	 */
	String getFeatureValue(String featureName){
		String value = this.feature_values.get(featureName);

		return value;
	}

	private Map<String, String> loadFeatureSection(Section section){
		Map<String, String> result = new LinkedHashMap<>(section);

		(result.keySet()).retainAll(Arrays.asList(this.feature_names_));

		return result;
	}

	static
	public ObjectiveFunction parseObjectiveFunction(String objective, int num_class, double sigmoid){

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
			case "binary":
				return new LogisticClassification(num_class, sigmoid);
			case "multiclass":
				return new SoftMaxClassification(num_class);
			default:
				throw new IllegalArgumentException(objective);
		}
	}
}
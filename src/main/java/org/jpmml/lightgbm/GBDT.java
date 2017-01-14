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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.Schema;

public class GBDT {

	private int max_feature_idx_;

	private int num_class_;

	private double sigmoid_;

	private int label_idx_;

	private String[] feature_names_;

	private ObjectiveFunction object_function_;

	private Tree[] models_;


	public void load(List<Section> sections){
		int index = 0;

		{
			Section section = sections.get(index);

			if(!("tree").equals(section.id())){
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

			if(!("Tree=" + String.valueOf(index - 1)).equals(section.id())){
				break;
			}

			Tree tree = new Tree();
			tree.load(section);

			trees.add(tree);

			index++;
		}

		this.models_ = trees.toArray(new Tree[trees.size()]);
	}

	public PMML encodePMML(){
		LightGBMEncoder encoder = new LightGBMEncoder();

		Label label;

		{
			String targetField = "_target";

			label = this.object_function_.encodeLabel(FieldName.create(targetField), encoder);
		}

		List<Feature> features = new ArrayList<>();

		String[] activeFields = this.feature_names_;
		for(int i = 0; i < activeFields.length; i++){
			String activeField = activeFields[i];

			OpType opType;

			Set<Double> featureCategories = getFeatureCategories(i);
			if(featureCategories != null && featureCategories.size() > 0){
				opType = OpType.CATEGORICAL;
			} else

			{
				opType = OpType.CONTINUOUS;
			}

			DataField dataField = encoder.createDataField(FieldName.create(activeField), opType, DataType.DOUBLE);

			MissingValueDecorator decorator = new MissingValueDecorator()
				.setMissingValueReplacement("0");

			encoder.addDecorator(dataField.getName(), decorator);

			features.add(new ContinuousFeature(encoder, dataField));
		}

		Schema schema = new Schema(label, features);

		MiningModel miningModel = encodeMiningModel(schema);

		PMML pmml = encoder.encodePMML(miningModel);

		return pmml;
	}

	public MiningModel encodeMiningModel(Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		List<TreeModel> treeModels = new ArrayList<>();

		Tree[] trees = this.models_;
		for(Tree tree : trees){
			TreeModel treeModel = tree.encodeTreeModel(segmentSchema);

			treeModels.add(treeModel);
		}

		MiningModel miningModel = this.object_function_.encodeMiningModel(treeModels, schema);

		return miningModel;
	}

	Set<Double> getFeatureCategories(int feature){
		Set<Double> result = null;

		Tree[] trees = this.models_;
		for(Tree tree : trees){
			Set<Double> categories = tree.getFeatureCategories(feature);

			if(categories != null && categories.size() > 0){

				if(result == null){
					result = new TreeSet<>();
				}

				result.addAll(categories);
			}
		}

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
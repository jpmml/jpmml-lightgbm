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
import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueDecorator;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

public class GBDT {

	private int max_feature_idx_;

	private int num_class_;

	private double sigmoid_;

	private int label_idx_;

	private String[] feature_names_;

	private ObjectiveFunction object_function_;

	private Tree[] models_;


	public void load(List<Map<String, String>> blocks){

		{
			Map<String, String> block = blocks.get(0);

			if(!block.containsKey("tree")){
				throw new IllegalArgumentException();
			}

			this.max_feature_idx_ = Integer.parseInt(block.get("max_feature_idx"));
			this.num_class_ = Integer.parseInt(block.get("num_class"));
			this.sigmoid_ = Double.parseDouble(block.get("sigmoid"));
			this.label_idx_ = Integer.parseInt(block.get("label_index"));
			this.feature_names_ = LightGBMUtil.parseStringArray(this.max_feature_idx_ + 1, block.get("feature_names"));

			this.object_function_ = parseObjectiveFunction(block.get("objective"));
		}

		List<Tree> trees = new ArrayList<>();

		for(int i = 1; i < (blocks.size() - 1); i++){
			Map<String, String> block = blocks.get(i);

			if(!(String.valueOf(i - 1)).equals(block.get("Tree"))){
				throw new IllegalArgumentException();
			}

			Tree tree = new Tree();
			tree.load(block);

			trees.add(tree);
		}

		this.models_ = trees.toArray(new Tree[trees.size()]);
	}

	public PMML encodePMML(){
		LightGBMEncoder encoder = new LightGBMEncoder();

		Label label;

		{
			String targetField = "_target";

			DataField dataField = encoder.createDataField(FieldName.create(targetField), OpType.CONTINUOUS, DataType.DOUBLE);

			label = new ContinuousLabel(dataField);
		}

		List<Feature> features = new ArrayList<>();

		String[] activeFields = this.feature_names_;
		for(String activeField : activeFields){
			DataField dataField = encoder.createDataField(FieldName.create(activeField), OpType.CONTINUOUS, DataType.DOUBLE);

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

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema))
			.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, treeModels));

		return miningModel;
	}

	static
	public ObjectiveFunction parseObjectiveFunction(String objective){

		switch(objective){
			case "regression":
				return null;
			default:
				throw new IllegalArgumentException(objective);
		}
	}
}
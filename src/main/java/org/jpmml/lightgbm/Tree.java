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

import java.util.Map;

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;

public class Tree {

	private int num_leaves_;

	private int[] left_child_;

	private int[] right_child_;

	private int[] split_feature_real_;

	private double[] threshold_;

	private int[] decision_type_;

	private double[] leaf_value_;

	private int[] leaf_count_;

	private double[] internal_value_;

	private int[] internal_count_;


	public void load(Map<String, String> block){
		this.num_leaves_ = Integer.parseInt(block.get("num_leaves"));

		this.left_child_ = LightGBMUtil.parseIntArray(this.num_leaves_ - 1, block.get("left_child"));
		this.right_child_ = LightGBMUtil.parseIntArray(this.num_leaves_ - 1, block.get("right_child"));
		this.split_feature_real_ = LightGBMUtil.parseIntArray(this.num_leaves_ - 1, block.get("split_feature"));
		this.threshold_ = LightGBMUtil.parseDoubleArray(this.num_leaves_ - 1, block.get("threshold"));
		this.decision_type_ = LightGBMUtil.parseIntArray(this.num_leaves_ - 1, block.get("decision_type"));
		this.leaf_value_ = LightGBMUtil.parseDoubleArray(this.num_leaves_, block.get("leaf_value"));
		this.leaf_count_ = LightGBMUtil.parseIntArray(this.num_leaves_, block.get("leaf_count"));
		this.internal_value_ = LightGBMUtil.parseDoubleArray(this.num_leaves_ - 1, block.get("internal_value"));
		this.internal_count_ = LightGBMUtil.parseIntArray(this.num_leaves_ - 1, block.get("internal_count"));
	}

	public TreeModel encodeTreeModel(Schema schema){
		Node root = new Node()
			.setPredicate(new True());

		encodeNode(root, 0, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT);

		return treeModel;
	}

	private void encodeNode(Node parent, int index, Schema schema){
		parent.setId(String.valueOf(index));

		// Non-leaf (aka internal) node
		if(index >= 0){
			parent.setScore(null); // XXX
			parent.setRecordCount((double)this.internal_count_[index]);

			Feature feature = schema.getFeature(this.split_feature_real_[index]);

			Predicate leftPredicate;
			Predicate rightPredicate;

			if(this.decision_type_[index] == 0){
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				String value = ValueUtil.formatValue(this.threshold_[index]);

				leftPredicate = new SimplePredicate(continuousFeature.getName(), SimplePredicate.Operator.LESS_OR_EQUAL)
					.setValue(value);

				rightPredicate = new SimplePredicate(continuousFeature.getName(), SimplePredicate.Operator.GREATER_THAN)
					.setValue(value);
			} else

			{
				throw new IllegalArgumentException();
			}

			Node leftChild = new Node()
				.setPredicate(leftPredicate);

			encodeNode(leftChild, this.left_child_[index], schema);

			Node rightChild = new Node()
				.setPredicate(rightPredicate);

			encodeNode(rightChild, this.right_child_[index], schema);

			parent.addNodes(leftChild, rightChild);
		} else

		// Leaf node
		{
			index = ~index;

			parent.setScore(ValueUtil.formatValue(this.leaf_value_[index]));
			parent.setRecordCount((double)this.leaf_count_[index]);
		}
	}
}
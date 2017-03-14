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

import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
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


	public void load(Section section){
		this.num_leaves_ = section.getInt("num_leaves");

		this.left_child_ = section.getIntArray("left_child", this.num_leaves_ - 1);
		this.right_child_ = section.getIntArray("right_child", this.num_leaves_ - 1);
		this.split_feature_real_ = section.getIntArray("split_feature", this.num_leaves_ - 1);
		this.threshold_ = section.getDoubleArray("threshold", this.num_leaves_ - 1);
		this.decision_type_ = section.getIntArray("decision_type", this.num_leaves_ - 1);
		this.leaf_value_ = section.getDoubleArray("leaf_value", this.num_leaves_);
		this.leaf_count_ = section.getIntArray("leaf_count", this.num_leaves_);
		this.internal_value_ = section.getDoubleArray("internal_value", this.num_leaves_ - 1);
		this.internal_count_ = section.getIntArray("internal_count", this.num_leaves_ - 1);
	}

	public TreeModel encodeTreeModel(Schema schema){
		Node root = new Node()
			.setPredicate(new True());

		encodeNode(root, 0, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		return treeModel;
	}

	public void encodeNode(Node parent, int index, Schema schema){
		parent.setId(String.valueOf(index));

		// Non-leaf (aka internal) node
		if(index >= 0){
			parent.setScore(null); // XXX
			parent.setRecordCount((double)this.internal_count_[index]);

			Feature feature = schema.getFeature(this.split_feature_real_[index]);

			Predicate leftPredicate;
			Predicate rightPredicate;

			boolean defaultLeft;

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				if(this.decision_type_[index] != Tree.SPLIT_NUMERIC && this.threshold_[index] != 0.5d){
					throw new IllegalArgumentException();
				}

				leftPredicate = new SimplePredicate(binaryFeature.getName(), SimplePredicate.Operator.NOT_EQUAL)
					.setValue(binaryFeature.getValue());

				rightPredicate = new SimplePredicate(binaryFeature.getName(), SimplePredicate.Operator.EQUAL)
					.setValue(binaryFeature.getValue());

				defaultLeft = true;
			} else

			if(feature instanceof CategoricalFeature){
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				if(this.decision_type_[index ] != Tree.SPLIT_CATEGORICAL){
					throw new IllegalArgumentException();
				}

				String value = ValueUtil.formatValue(this.threshold_[index]);

				leftPredicate = new SimplePredicate(categoricalFeature.getName(), SimplePredicate.Operator.EQUAL)
					.setValue(value);

				rightPredicate = new SimplePredicate(categoricalFeature.getName(), SimplePredicate.Operator.NOT_EQUAL)
					.setValue(value);

				defaultLeft = (0d == this.threshold_[index]);
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				SimplePredicate.Operator leftOperator;
				SimplePredicate.Operator rightOperator;

				switch(this.decision_type_[index]){
					case Tree.SPLIT_NUMERIC:
						leftOperator = SimplePredicate.Operator.LESS_OR_EQUAL;
						rightOperator = SimplePredicate.Operator.GREATER_THAN;

						// Send the value to the direction of the zero value
						defaultLeft = (0d <= this.threshold_[index]);
						break;
					case Tree.SPLIT_CATEGORICAL:
						leftOperator = SimplePredicate.Operator.EQUAL;
						rightOperator = SimplePredicate.Operator.NOT_EQUAL;

						// Send zero values to the left, and all other values to the right
						defaultLeft = (0d == this.threshold_[index]);
						break;
					default:
						throw new IllegalArgumentException();
				}

				String value = ValueUtil.formatValue(this.threshold_[index]);

				leftPredicate = new SimplePredicate(continuousFeature.getName(), leftOperator)
					.setValue(value);

				rightPredicate = new SimplePredicate(continuousFeature.getName(), rightOperator)
					.setValue(value);
			}

			Node leftChild = new Node()
				.setPredicate(leftPredicate);

			encodeNode(leftChild, this.left_child_[index], schema);

			Node rightChild = new Node()
				.setPredicate(rightPredicate);

			encodeNode(rightChild, this.right_child_[index], schema);

			parent.addNodes(leftChild, rightChild);

			parent.setDefaultChild(defaultLeft ? leftChild.getId() : rightChild.getId());
		} else

		// Leaf node
		{
			index = ~index;

			parent.setScore(ValueUtil.formatValue(this.leaf_value_[index]));
			parent.setRecordCount((double)this.leaf_count_[index]);
		}
	}

	Boolean isBinary(int feature){
		Boolean result = null;

		for(int i = 0; i < this.split_feature_real_.length; i++){

			if(this.split_feature_real_[i] == feature){

				if(this.decision_type_[i] != Tree.SPLIT_NUMERIC){
					return Boolean.FALSE;
				} // End if

				if(this.threshold_[i] != 0.5d){
					return Boolean.FALSE;
				}

				result = Boolean.TRUE;
			}
		}

		return result;
	}

	Boolean isCategorical(int feature){
		Boolean result = null;

		for(int i = 0; i < this.split_feature_real_.length; i++){

			if(this.split_feature_real_[i] == feature){

				if(this.decision_type_[i] != Tree.SPLIT_CATEGORICAL){
 					return Boolean.FALSE;
 				}

 				result = Boolean.TRUE;
 			}
 		}

 		return result;
 	}

	private static final int SPLIT_NUMERIC = 0;
	private static final int SPLIT_CATEGORICAL = 1;
}
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
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;

public class Tree {

	private int num_leaves_;

	private int num_cat_;

	private int[] left_child_;

	private int[] right_child_;

	private int[] split_feature_real_;

	private double[] threshold_;

	private int[] decision_type_;

	private double[] leaf_value_;

	private int[] leaf_count_;

	private double[] internal_value_;

	private int[] internal_count_;

	private int[] cat_boundaries_;

	private int[] cat_threshold_;


	public void load(Section section){
		this.num_leaves_ = section.getInt("num_leaves");
		this.num_cat_ = section.getInt("num_cat");

		this.left_child_ = section.getIntArray("left_child", this.num_leaves_ - 1);
		this.right_child_ = section.getIntArray("right_child", this.num_leaves_ - 1);
		this.split_feature_real_ = section.getIntArray("split_feature", this.num_leaves_ - 1);
		this.threshold_ = section.getDoubleArray("threshold", this.num_leaves_ - 1);
		this.decision_type_ = section.getIntArray("decision_type", this.num_leaves_ - 1);
		this.leaf_value_ = section.getDoubleArray("leaf_value", this.num_leaves_);
		this.leaf_count_ = section.getIntArray("leaf_count", this.num_leaves_);
		this.internal_value_ = section.getDoubleArray("internal_value", this.num_leaves_ - 1);
		this.internal_count_ = section.getIntArray("internal_count", this.num_leaves_ - 1);

		if(this.num_cat_ > 0){
			this.cat_boundaries_ = section.getIntArray("cat_boundaries", this.num_cat_ + 1);
			this.cat_threshold_ = section.getIntArray("cat_threshold", -1);
		}
	}

	public TreeModel encodeTreeModel(PredicateManager predicateManager, Schema schema){
		Node root = new Node()
			.setPredicate(new True());

		encodeNode(root, predicateManager, 0, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		return treeModel;
	}

	public void encodeNode(Node parent, PredicateManager predicateManager, int index, Schema schema){
		parent.setId(String.valueOf(index));

		// Non-leaf (aka internal) node
		if(index >= 0){
			parent.setScore(null); // XXX
			parent.setRecordCount((double)this.internal_count_[index]);

			Feature feature = schema.getFeature(this.split_feature_real_[index]);

			double threshold_ = this.threshold_[index];
			int decision_type_ = this.decision_type_[index];

			Predicate leftPredicate;
			Predicate rightPredicate;

			boolean defaultLeft = hasDefaultLeftMask(decision_type_);

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				if(hasCategoricalMask(decision_type_) || threshold_ != 0.5d){
					throw new IllegalArgumentException();
				}

				String value = binaryFeature.getValue();

				leftPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.NOT_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.EQUAL, value);
			} else

			if(feature instanceof CategoricalFeature){
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				if(!hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException();
				}

				List<String> values = categoricalFeature.getValues();

				int cat_idx = ValueUtil.asInt(threshold_);

				leftPredicate = predicateManager.createSimpleSetPredicate(categoricalFeature, selectValues(values, cat_idx, true));
				rightPredicate = predicateManager.createSimpleSetPredicate(categoricalFeature, selectValues(values, cat_idx, false));

				defaultLeft = false;
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				if(hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException();
				}

				String value = ValueUtil.formatValue(threshold_);

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_OR_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_THAN, value);
			}

			Node leftChild = new Node()
				.setPredicate(leftPredicate);

			encodeNode(leftChild, predicateManager, this.left_child_[index], schema);

			Node rightChild = new Node()
				.setPredicate(rightPredicate);

			encodeNode(rightChild, predicateManager, this.right_child_[index], schema);

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

	private List<String> selectValues(List<String> values, int cat_idx, boolean left){
		List<String> result;

		if(left){
			result = new ArrayList<>();
		} else

		{
			result = new ArrayList<>(values);
		}

		int n = (this.cat_boundaries_[cat_idx + 1] - this.cat_boundaries_[cat_idx]);

		for(int i = 0; i < n; i++){

			for(int j = 0; j < 32; j++){
				int cat = (i * 32) + j;

				if(findInBitset(this.cat_threshold_, this.cat_boundaries_[cat_idx], n, cat)){
					String value = String.valueOf(cat);

					if(values.indexOf(value) < 0){
						throw new IllegalArgumentException();
					} // End if

					if(left){
						result.add(value);
					} else

					{
						result.remove(value);
					}
				}
			}
		}

		return result;
	}

	Double getScore(){

		if(this.leaf_value_.length > 1){
			double leaf_value = this.leaf_value_[0];

			for(int i = 1; i < this.leaf_value_.length; i++){

				if(this.leaf_value_[i] != leaf_value){
					return null;
				}
			}

			return leaf_value;
		}

		return null;
	}

	Boolean isBinary(int feature){
		Boolean result = null;

		for(int i = 0; i < this.split_feature_real_.length; i++){

			if(this.split_feature_real_[i] == feature){

				if(hasCategoricalMask(this.decision_type_[i])){
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

				if(!hasCategoricalMask(this.decision_type_[i])){
					return Boolean.FALSE;
				}

 				result = Boolean.TRUE;
 			}
 		}

 		return result;
 	}

	static
	private boolean hasCategoricalMask(int decision_type){
		return getDecisionType(decision_type, Tree.MASK_CATEGORICAL) == Tree.MASK_CATEGORICAL;
	}

	static
	private boolean hasDefaultLeftMask(int decision_type){
		return getDecisionType(decision_type, Tree.MASK_DEFAULT_LEFT) == Tree.MASK_DEFAULT_LEFT;
	}

	static
	int getDecisionType(int decision_type, int mask){
		return (decision_type & mask);
	}

	static
	int getMissingType(int decision_type){
		return getDecisionType((decision_type >> 2), 3);
	}

	static
	private boolean findInBitset(int[] bits, int bitOffset, int n, int pos){
		int i1 = pos / 32;
		if(i1 >= n){
			return false;
		}

		int i2 = pos % 32;

		return ((bits[bitOffset + i1] >> i2) & 1) == 1;
	}

	private static final int MASK_CATEGORICAL = 1;
	private static final int MASK_DEFAULT_LEFT = 2;
}
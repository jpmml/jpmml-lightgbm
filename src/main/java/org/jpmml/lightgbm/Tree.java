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
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.CountingBranchNode;
import org.dmg.pmml.tree.CountingLeafNode;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.CategoryManager;
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

	private long[] cat_threshold_;


	public void load(Section section){
		this.num_leaves_ = section.getInt("num_leaves");
		this.num_cat_ = section.getInt("num_cat");

		if(this.num_leaves_ == 1){
			this.leaf_value_ = section.getDoubleArray("leaf_value", this.num_leaves_);
			this.leaf_count_ = new int[]{0};
		} else

		if(this.num_leaves_ > 1){
			this.left_child_ = section.getIntArray("left_child", this.num_leaves_ - 1);
			this.right_child_ = section.getIntArray("right_child", this.num_leaves_ - 1);
			this.split_feature_real_ = section.getIntArray("split_feature", this.num_leaves_ - 1);
			this.threshold_ = section.getDoubleArray("threshold", this.num_leaves_ - 1);
			this.decision_type_ = section.getIntArray("decision_type", this.num_leaves_ - 1);
			this.leaf_value_ = section.getDoubleArray("leaf_value", this.num_leaves_);
			this.leaf_count_ = section.getIntArray("leaf_count", this.num_leaves_);
			this.internal_value_ = section.getDoubleArray("internal_value", this.num_leaves_ - 1);
			this.internal_count_ = section.getIntArray("internal_count", this.num_leaves_ - 1);
		} else

		{
			throw new IllegalArgumentException("Expected one or more leaves, got " + this.num_leaves_ + " leaves");
		} // End if

		if(this.num_cat_ > 0){
			this.cat_boundaries_ = section.getIntArray("cat_boundaries", this.num_cat_ + 1);
			this.cat_threshold_ = section.getUnsignedIntArray("cat_threshold", -1);
		}
	}

	public boolean isEmpty(){
		return (this.num_leaves_ == 1);
	}

	public TreeModel encodeTreeModel(PredicateManager predicateManager, Schema schema){
		Node root = encodeNode(0, True.INSTANCE, new CategoryManager(), predicateManager, schema);

		TreeModel treeModel = new TreeModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema.getLabel()), root)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.BINARY_SPLIT)
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.DEFAULT_CHILD);

		return treeModel;
	}

	public Node encodeNode(int index, Predicate predicate, CategoryManager categoryManager, PredicateManager predicateManager, Schema schema){
		Integer id = Integer.valueOf(~index);

		// Non-leaf (aka internal) node
		if(!isEmpty() && (index >= 0)){
			Feature feature = schema.getFeature(this.split_feature_real_[index]);

			double threshold_ = this.threshold_[index];
			int decision_type_ = this.decision_type_[index];

			CategoryManager leftCategoryManager = categoryManager;
			CategoryManager rightCategoryManager = categoryManager;

			Predicate leftPredicate;
			Predicate rightPredicate;

			boolean defaultLeft = hasDefaultLeftMask(decision_type_);

			if(feature instanceof BinaryFeature){
				BinaryFeature binaryFeature = (BinaryFeature)feature;

				if(hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException("Expected a false (off) categorical split mask for binary feature " + binaryFeature.getName() + ", got true (on)");
				} // End if

				if(threshold_ != 0.5d){
					throw new IllegalArgumentException("Expected 0.5 as a threshold value for binary feature " + binaryFeature.getName() + ", got " + threshold_);
				}

				Object value = binaryFeature.getValue();

				leftPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.NOT_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(binaryFeature, SimplePredicate.Operator.EQUAL, value);
			} else

			if(feature instanceof BinaryCategoricalFeature){
				BinaryCategoricalFeature binaryCategoricalFeature = (BinaryCategoricalFeature)feature;

				if(!hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException("Expected a true (on) categorical split mask for binary categorical feature " + binaryCategoricalFeature.getName() + ", got false (off)");
				}

				FieldName name = binaryCategoricalFeature.getName();

				List<?> values = binaryCategoricalFeature.getValues();

				int cat_idx = ValueUtil.asInt(threshold_);

				List<Object> leftValues = selectValues(false, values, Objects::nonNull, cat_idx, true);
				List<Object> rightValues = selectValues(false, values, Objects::nonNull, cat_idx, false);

				Object value = values.get(1);

				if(leftValues.size() == 0 && rightValues.size() == 1){
					leftCategoryManager = leftCategoryManager;
					rightCategoryManager = rightCategoryManager.fork(name, rightValues);

					leftPredicate = predicateManager.createSimplePredicate(binaryCategoricalFeature, SimplePredicate.Operator.NOT_EQUAL, value);
					rightPredicate = predicateManager.createSimplePredicate(binaryCategoricalFeature, SimplePredicate.Operator.EQUAL, value);

					defaultLeft = true;
				} else

				if(leftValues.size() == 1 && rightValues.size() == 0){
					leftCategoryManager = leftCategoryManager.fork(name, leftValues);
					rightCategoryManager = rightCategoryManager;

					leftPredicate = predicateManager.createSimplePredicate(binaryCategoricalFeature, SimplePredicate.Operator.EQUAL, value);
					rightPredicate = predicateManager.createSimplePredicate(binaryCategoricalFeature, SimplePredicate.Operator.NOT_EQUAL, value);

					defaultLeft = false;
				} else

				{
					throw new IllegalArgumentException("Neither left nor right branch is selectable");
				}
			} else

			if(feature instanceof CategoricalFeature){
				CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

				if(!hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException("Expected a true (on) categorical split mask for categorical feature " + categoricalFeature.getName() + ", got false (off)");
				}

				FieldName name = categoricalFeature.getName();

				boolean indexAsValue = (categoricalFeature instanceof DirectCategoricalFeature);

				List<?> values = categoricalFeature.getValues();

				java.util.function.Predicate<Object> valueFilter = categoryManager.getValueFilter(name);

				int cat_idx = ValueUtil.asInt(threshold_);

				List<Object> leftValues = selectValues(indexAsValue, values, valueFilter, cat_idx, true);
				List<Object> rightValues = selectValues(indexAsValue, values, valueFilter, cat_idx, false);

				Set<?> parentValues = categoryManager.getValue(name);

				if(leftValues.size() == 0){
					throw new IllegalArgumentException("Left branch is not selectable");
				} // End if

				if(parentValues != null && rightValues.size() == parentValues.size()){
					throw new IllegalArgumentException("Right branch is not selectable");
				}

				leftCategoryManager = categoryManager.fork(name, leftValues);
				rightCategoryManager = categoryManager.fork(name, rightValues);

				leftPredicate = predicateManager.createPredicate(categoricalFeature, leftValues);

				if(rightValues.size() > 0){
					rightPredicate = predicateManager.createPredicate(categoricalFeature, rightValues);
				} else

				{
					rightPredicate = True.INSTANCE;
				}

				defaultLeft = false;
			} else

			{
				ContinuousFeature continuousFeature = feature.toContinuousFeature();

				if(hasCategoricalMask(decision_type_)){
					throw new IllegalArgumentException("Expected a false (off) categorical split mask for continuous feature " + continuousFeature.getName() + ", got true (on)");
				}

				Number value = threshold_;

				DataType dataType = continuousFeature.getDataType();
				switch(dataType){
					case INTEGER:
						if(value.doubleValue() == Tree.THRESHOLD_ZERO){
							value = 0;
						}
						break;
					default:
						break;
				}

				leftPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.LESS_OR_EQUAL, value);
				rightPredicate = predicateManager.createSimplePredicate(continuousFeature, SimplePredicate.Operator.GREATER_THAN, value);
			}

			Node leftChild = encodeNode(this.left_child_[index], leftPredicate, leftCategoryManager, predicateManager, schema);
			Node rightChild = encodeNode(this.right_child_[index], rightPredicate, rightCategoryManager, predicateManager, schema);

			Node result = new CountingBranchNode(this.leaf_value_[index], predicate)
				.setId(id)
				.setDefaultChild(defaultLeft ? leftChild.getId() : rightChild.getId())
				.setRecordCount(ValueUtil.narrow(this.internal_count_[index]))
				.addNodes(leftChild, rightChild);

			return result;
		} else

		// Leaf node
		{
			if(!isEmpty()){
				index = ~index;
			}

			Node result = new CountingLeafNode(this.leaf_value_[index], predicate)
				.setId(id)
				.setRecordCount(ValueUtil.narrow(this.leaf_count_[index]));

			return result;
		}
	}

	private List<Object> selectValues(boolean indexAsValue, List<?> values, java.util.function.Predicate<Object> valueFilter, int cat_idx, boolean left){
		List<Object> result;

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
					Object value;

					if(indexAsValue){
						value = cat;
					} else

					{
						value = values.get(cat);
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

		result.removeIf(value -> !valueFilter.test(value));

		return result;
	}

	Boolean isBinary(int feature){
		Boolean result = null;

		if(isEmpty()){
			return result;
		}

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

		if(isEmpty()){
			return result;
		}

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
	private boolean findInBitset(long[] bits, int bitOffset, int n, int pos){
		int i1 = pos / 32;
		if(i1 >= n){
			return false;
		}

		int i2 = pos % 32;

		return ((bits[bitOffset + i1] >> i2) & 1) == 1;
	}

	private static final int MASK_CATEGORICAL = 1;
	private static final int MASK_DEFAULT_LEFT = 2;

	private static final double THRESHOLD_ZERO = 1.0000000180025095E-35;
}
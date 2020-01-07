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

import org.dmg.pmml.FieldName;
import org.dmg.pmml.MiningFunction;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segmentation;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class ObjectiveFunction {

	private boolean average_output_;


	public ObjectiveFunction(boolean average_output){
		this.average_output_ = average_output;
	}

	abstract
	public Label encodeLabel(FieldName targetField, List<?> targetCategories, PMMLEncoder encoder);

	abstract
	public MiningModel encodeMiningModel(List<Tree> trees, Integer numIteration, Schema schema);

	protected MiningModel createMiningModel(List<Tree> trees, Integer numIteration, Schema schema){
		ContinuousLabel continuousLabel = (ContinuousLabel)schema.getLabel();

		Schema segmentSchema = schema.toAnonymousSchema();

		PredicateManager predicateManager = new PredicateManager();

		List<TreeModel> treeModels = new ArrayList<>();

		if(numIteration != null){

			if(numIteration > trees.size()){
				throw new IllegalArgumentException("Tree limit " + numIteration + " is greater than the number of trees");
			}

			trees = trees.subList(0, numIteration);
		}

		for(Tree tree : trees){
			TreeModel treeModel = tree.encodeTreeModel(predicateManager, segmentSchema);

			treeModels.add(treeModel);
		}

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(continuousLabel))
			.setSegmentation(MiningModelUtil.createSegmentation(this.average_output_ ? Segmentation.MultipleModelMethod.AVERAGE : Segmentation.MultipleModelMethod.SUM, treeModels));

		return miningModel;
	}

	public boolean getAverageOutput(){
		return this.average_output_;
	}
}
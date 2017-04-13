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
import org.jpmml.converter.Label;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PredicateManager;
import org.jpmml.converter.Schema;
import org.jpmml.converter.mining.MiningModelUtil;

abstract
public class ObjectiveFunction {

	abstract
	public Label encodeLabel(FieldName targetField, List<String> targetCategories, PMMLEncoder encoder);

	abstract
	public MiningModel encodeMiningModel(List<Tree> trees, Schema schema);

	static
	protected MiningModel createMiningModel(List<Tree> trees, Schema schema){
		Schema segmentSchema = schema.toAnonymousSchema();

		PredicateManager predicateManager = new PredicateManager();

		List<TreeModel> treeModels = new ArrayList<>();

		double intercept = 0d;

		for(Tree tree : trees){
			Double score = tree.getScore();

			if(score != null){
				intercept += score;

				continue;
			}

			TreeModel treeModel = tree.encodeTreeModel(predicateManager, segmentSchema);

			treeModels.add(treeModel);
		}

		MiningModel miningModel = new MiningModel(MiningFunction.REGRESSION, ModelUtil.createMiningSchema(schema))
			.setSegmentation(MiningModelUtil.createSegmentation(Segmentation.MultipleModelMethod.SUM, treeModels));

		if(intercept != 0d){
			miningModel.setTargets(ModelUtil.createRescaleTargets(schema, null, intercept));
		}

		return miningModel;
	}
}
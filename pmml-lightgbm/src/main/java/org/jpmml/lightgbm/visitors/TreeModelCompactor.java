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
package org.jpmml.lightgbm.visitors;

import java.util.List;

import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.converter.visitors.AbstractTreeModelTransformer;
import org.jpmml.model.UnsupportedAttributeException;
import org.jpmml.model.UnsupportedElementException;

public class TreeModelCompactor extends AbstractTreeModelTransformer {

	@Override
	public void enterNode(Node node){
		Object id = node.getId();
		Object score = node.getScore();
		Object defaultChild = node.getDefaultChild();

		if(id == null){
			throw new UnsupportedElementException(node);
		} // End if

		if(node.hasNodes()){
			List<Node> children = node.getNodes();

			if(defaultChild == null || children.size() != 2){
				throw new UnsupportedElementException(node);
			}

			Node firstChild = children.get(0);
			Node secondChild = children.get(1);

			if(equalsNode(defaultChild, firstChild)){
				children = swapChildren(node);

				firstChild = children.get(0);
				secondChild = children.get(1);
			} else

			if(equalsNode(defaultChild, secondChild)){
				// Ignored
			} else

			{
				throw new UnsupportedElementException(node);
			}

			node.setDefaultChild(null);

			secondChild.setPredicate(True.INSTANCE);
		} else

		{
			if(score == null || defaultChild != null){
				throw new UnsupportedElementException(node);
			}
		}

		node.setId(null);
	}

	@Override
	public void exitNode(Node node){
		Number recordCount = node.getRecordCount();
		Predicate predicate = node.requirePredicate();

		if(recordCount != null){
			node.setRecordCount(null);
		} // End if

		if(predicate instanceof True){
			Node parentNode = getParentNode();

			if(parentNode == null){
				return;
			}

			parentNode.setScore(null);

			initScore(parentNode, node);
			replaceChildWithGrandchildren(parentNode, node);
		}
	}

	@Override
	public void enterTreeModel(TreeModel treeModel){
		super.enterTreeModel(treeModel);

		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();
		if(missingValueStrategy != TreeModel.MissingValueStrategy.DEFAULT_CHILD){
			throw new UnsupportedAttributeException(treeModel, missingValueStrategy);
		}

		TreeModel.NoTrueChildStrategy noTrueChildStrategy = treeModel.getNoTrueChildStrategy();
		if(noTrueChildStrategy != TreeModel.NoTrueChildStrategy.RETURN_NULL_PREDICTION){
			throw new UnsupportedAttributeException(treeModel, noTrueChildStrategy);
		}

		TreeModel.SplitCharacteristic splitCharacteristic = treeModel.getSplitCharacteristic();
		if(splitCharacteristic != TreeModel.SplitCharacteristic.BINARY_SPLIT){
			throw new UnsupportedAttributeException(treeModel, splitCharacteristic);
		}

		treeModel
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.NONE)
			.setNoTrueChildStrategy(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.MULTI_SPLIT);
	}
}
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

import java.util.Deque;
import java.util.List;

import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.True;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.tree.Node;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.visitors.AbstractVisitor;

public class TreeModelCompactor extends AbstractVisitor {

	@Override
	public void pushParent(PMMLObject object){
		super.pushParent(object);

		if(object instanceof Node){
			handleNodePush((Node)object);
		}
	}

	@Override
	public PMMLObject popParent(){
		PMMLObject object = super.popParent();

		if(object instanceof Node){
			handleNodePop((Node)object);
		}

		return object;
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		TreeModel.MissingValueStrategy missingValueStrategy = treeModel.getMissingValueStrategy();
		TreeModel.NoTrueChildStrategy noTrueChildStrategy = treeModel.getNoTrueChildStrategy();
		TreeModel.SplitCharacteristic splitCharacteristic = treeModel.getSplitCharacteristic();

		if(!(TreeModel.MissingValueStrategy.DEFAULT_CHILD).equals(missingValueStrategy) || !(TreeModel.NoTrueChildStrategy.RETURN_NULL_PREDICTION).equals(noTrueChildStrategy) || !(TreeModel.SplitCharacteristic.BINARY_SPLIT).equals(splitCharacteristic)){
			throw new IllegalArgumentException();
		}

		treeModel
			.setMissingValueStrategy(TreeModel.MissingValueStrategy.NONE)
			.setNoTrueChildStrategy(TreeModel.NoTrueChildStrategy.RETURN_LAST_PREDICTION)
			.setSplitCharacteristic(TreeModel.SplitCharacteristic.MULTI_SPLIT);

		return super.visit(treeModel);
	}

	private void handleNodePush(Node node){
		String defaultChild = node.getDefaultChild();
		String id = node.getId();
		String score = node.getScore();

		if(id == null){
			throw new IllegalArgumentException();
		} // End if

		if(node.hasNodes()){
			List<Node> children = node.getNodes();

			if(children.size() != 2 || defaultChild == null || score != null){
				throw new IllegalArgumentException();
			}

			Node firstChild = children.get(0);
			Node secondChild = children.get(1);

			if((defaultChild).equals(firstChild.getId())){
				children.remove(0);
				children.add(1, firstChild);

				firstChild = children.get(0);
				secondChild = children.get(1);
			} else

			if((defaultChild).equals(secondChild.getId())){
				// Ignored
			} else

			{
				throw new IllegalArgumentException();
			}

			node.setDefaultChild(null);

			secondChild.setPredicate(new True());
		} else

		{
			if(defaultChild != null || score == null){
				throw new IllegalArgumentException();
			}
		}

		node.setId(null);
	}

	private void handleNodePop(Node node){
		Double recordCount = node.getRecordCount();
		String score = node.getScore();
		Predicate predicate = node.getPredicate();

		if(recordCount != null){
			node.setRecordCount(null);
		} // End if

		if(predicate instanceof True){
			Node parentNode = getParentNode();

			if(parentNode == null){
				return;
			}

			String parentScore = parentNode.getScore();
			if(parentScore != null){
				throw new IllegalArgumentException();
			}

			parentNode.setScore(score);

			List<Node> parentChildren = parentNode.getNodes();

			boolean success = parentChildren.remove(node);
			if(!success){
				throw new IllegalArgumentException();
			} // End if

			if(node.hasNodes()){
				List<Node> children = node.getNodes();

				parentChildren.addAll(children);
			}
		}
	}

	private Node getParentNode(){
		Deque<PMMLObject> parents = getParents();

		PMMLObject parent = parents.peekFirst();
		if(parent instanceof Node){
			return (Node)parent;
		}

		return null;
	}
}
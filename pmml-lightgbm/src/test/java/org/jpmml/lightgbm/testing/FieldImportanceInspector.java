/*
 * Copyright (c) 2022 Villu Ruusmann
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
package org.jpmml.lightgbm.testing;

import java.util.List;

import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Model;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLAttributes;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.mining.MiningModel;
import org.dmg.pmml.mining.Segment;
import org.dmg.pmml.tree.TreeModel;
import org.jpmml.model.MisplacedAttributeException;
import org.jpmml.model.MissingAttributeException;
import org.jpmml.model.visitors.AbstractVisitor;

public class FieldImportanceInspector extends AbstractVisitor {

	@Override
	public VisitorAction visit(MiningModel miningModel){
		PMMLObject parent = getParent();

		if(parent instanceof PMML){
			ensureImportancesDefined(miningModel);
		} else

		if(parent instanceof Segment){
			ensureImportancesNotDefined(miningModel);
		}

		return super.visit(miningModel);
	}

	@Override
	public VisitorAction visit(TreeModel treeModel){
		ensureImportancesNotDefined(treeModel);

		return super.visit(treeModel);
	}

	static
	private void ensureImportancesDefined(Model model){
		MiningSchema miningSchema = model.requireMiningSchema();

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				Number importance = miningField.getImportance();
				MiningField.UsageType usageType = miningField.getUsageType();

				switch(usageType){
					case TARGET:
						if(importance != null){
							throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_IMPORTANCE, importance);
						}
						break;
					case ACTIVE:
						if(importance == null){
							throw new MissingAttributeException(miningField, PMMLAttributes.MININGFIELD_IMPORTANCE);
						}
						break;
					default:
						break;
				}
			}
		}
	}

	static
	private void ensureImportancesNotDefined(Model model){
		MiningSchema miningSchema = model.requireMiningSchema();

		if(miningSchema.hasMiningFields()){
			List<MiningField> miningFields = miningSchema.getMiningFields();

			for(MiningField miningField : miningFields){
				Number importance = miningField.getImportance();
				MiningField.UsageType usageType = miningField.getUsageType();

				switch(usageType){
					case TARGET:
					case ACTIVE:
						if(importance != null){
							throw new MisplacedAttributeException(miningField, PMMLAttributes.MININGFIELD_IMPORTANCE, importance);
						}
						break;
					default:
						break;
				}
			}
		}
	}
}
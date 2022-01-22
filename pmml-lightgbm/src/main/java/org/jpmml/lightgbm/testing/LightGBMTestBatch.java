/*
 * Copyright (c) 2020 Villu Ruusmann
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

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.MiningField;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLObject;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.VisitorAction;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.testing.ModelEncoderBatch;
import org.jpmml.converter.testing.OptionsUtil;
import org.jpmml.evaluator.ResultField;
import org.jpmml.lightgbm.GBDT;
import org.jpmml.lightgbm.HasLightGBMOptions;
import org.jpmml.lightgbm.LightGBMUtil;
import org.jpmml.model.visitors.AbstractVisitor;

abstract
public class LightGBMTestBatch extends ModelEncoderBatch {

	public LightGBMTestBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);
	}

	@Override
	abstract
	public LightGBMTest getArchiveBatchTest();

	@Override
	public List<Map<String, Object>> getOptionsMatrix(){
		String dataset = getDataset();

		Integer numIteration = null;

		int index = dataset.indexOf('@');
		if(index > -1){
			numIteration = new Integer(dataset.substring(index + 1));
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasLightGBMOptions.OPTION_COMPACT, new Boolean[]{false, true});
		options.put(HasLightGBMOptions.OPTION_NAN_AS_MISSING, true);
		options.put(HasLightGBMOptions.OPTION_NUM_ITERATION, numIteration);

		return OptionsUtil.generateOptionsMatrix(options);
	}

	public String getModelTxtPath(){
		return "/lgbm/" + (getAlgorithm() + truncate(getDataset())) + ".txt";
	}

	@Override
	public PMML getPMML() throws Exception {
		GBDT gbdt;

		try(InputStream is = open(getModelTxtPath())){
			gbdt = LightGBMUtil.loadGBDT(is);
		}

		Map<String, ?> options = getOptions();

		PMML pmml = gbdt.encodePMML(options, null, null);

		validatePMML(pmml);

		return pmml;
	}

	@Override
	public String getInputCsvPath(){
		return "/csv/" + truncate(getDataset()) + ".csv";
	}

	@Override
	public String getOutputCsvPath(){
		return super.getOutputCsvPath();
	}

	@Override
	protected void validatePMML(PMML pmml) throws Exception {
		super.validatePMML(pmml);

		Visitor visitor = new AbstractVisitor(){

			@Override
			public VisitorAction visit(MiningModel miningModel){
				PMMLObject parent = getParent();

				if(parent instanceof PMML){
					MiningSchema miningSchema = miningModel.getMiningSchema();

					if(miningSchema.hasMiningFields()){
						List<MiningField> miningFields = miningSchema.getMiningFields();

						for(MiningField miningField : miningFields){
							Number importance = miningField.getImportance();
							MiningField.UsageType usageType = miningField.getUsageType();

							switch(usageType){
								case TARGET:
									if(importance != null){
										throw new AssertionError();
									}
									break;
								case ACTIVE:
									if(importance == null){
										throw new AssertionError();
									}
									break;
								default:
									break;
							}
						}
					}
				}

				return super.visit(miningModel);
			}
		};
		visitor.applyTo(pmml);
	}
}
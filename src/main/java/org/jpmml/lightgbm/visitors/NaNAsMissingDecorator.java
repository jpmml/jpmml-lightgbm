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
package org.jpmml.lightgbm.visitors;

import java.util.Collections;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Value.Property;
import org.dmg.pmml.VisitorAction;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.visitors.ActiveFieldFinder;

public class NaNAsMissingDecorator extends ActiveFieldFinder {

	@Override
	public VisitorAction visit(DataField dataField){
		DataType dataType = dataField.getDataType();

		switch(dataType){
			case FLOAT:
			case DOUBLE:
				PMMLUtil.addValues(dataField, Collections.singletonList("NaN"), Property.MISSING);
				break;
			default:
				break;
		}

		return super.visit(dataField);
	}
}
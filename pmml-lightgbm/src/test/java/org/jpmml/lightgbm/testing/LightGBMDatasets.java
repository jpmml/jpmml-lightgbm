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

import org.jpmml.converter.testing.Datasets;

public interface LightGBMDatasets extends Datasets {

	String AUDIT_BIN = AUDIT + "Bin";
	String AUDIT_BIN_NA = AUDIT_BIN + "NA";
	String AUDIT_INVALID = AUDIT + "Invalid";
	String AUDIT_LIMIT = AUDIT + "@17";
	String AUDIT_NA_LIMIT = AUDIT_NA + "@17";
	String AUTO_DIRECT = AUTO + "Direct";
	String AUTO_DIRECT_NA = AUTO_DIRECT + "NA";
	String AUTO_LIMIT = AUTO + "@17";
	String AUTO_NA_LIMIT = AUTO_NA + "@17";
	String HOUSING_LIMIT = HOUSING + "@31";
	String HOUSING_NA_LIMIT = HOUSING_NA + "@31";
	String IRIS_LIMIT = IRIS + "@7";
	String IRIS_NA_LIMIT = IRIS_NA + "@7";
	String VERSICOLOR_LIMIT = VERSICOLOR + "@9";
	String VISIT_LIMIT = VISIT + "@31";
	String VISIT_NA_LIMIT = VISIT_NA + "@31";
}
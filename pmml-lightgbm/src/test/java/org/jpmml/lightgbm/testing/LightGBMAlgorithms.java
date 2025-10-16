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

public interface LightGBMAlgorithms {

	String CLASSIFICATION = "Classification";
	String LINEARTREE_CLASSIFICATION = "LinearTree" + CLASSIFICATION;
	String RF_CLASSIFICATION = "RF" + CLASSIFICATION;
	String REGRESSION = "Regression";
	String LINEARTREE_REGRESSION = "LinearTree" + REGRESSION;
	String RF_REGRESSION = "RF" + REGRESSION;
}
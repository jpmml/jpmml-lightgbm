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

import org.junit.Test;

public class RegressionTest extends LightGBMTest {

	@Test
	public void evaluateAuto() throws Exception {
		evaluate("Regression", "Auto");
	}

	@Test
	public void evaluateAutoDirect() throws Exception {
		evaluate("Regression", "AutoDirect");
	}

	@Test
	public void evaluateAutoLimit() throws Exception {
		evaluate("Regression", "Auto@17");
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate("Regression", "AutoNA");
	}

	@Test
	public void evaluateAutoDirectNA() throws Exception {
		evaluate("Regression", "AutoDirectNA");
	}

	@Test
	public void evaluateAutoNALimit() throws Exception {
		evaluate("Regression", "AutoNA@17");
	}

	@Test
	public void evaluateHousing() throws Exception {
		evaluate("Regression", "Housing");
	}

	@Test
	public void evaluateHousingLimit() throws Exception {
		evaluate("Regression", "Housing@31");
	}

	@Test
	public void evaluateHousingNA() throws Exception {
		evaluate("Regression", "HousingNA");
	}

	@Test
	public void evaluateHousingNALimit() throws Exception {
		evaluate("Regression", "HousingNA@31");
	}

	@Test
	public void evaluateVisit() throws Exception {
		evaluate("Regression", "Visit");
	}

	@Test
	public void evaluateVisitLimit() throws Exception {
		evaluate("Regression", "Visit@31");
	}

	@Test
	public void evaluateVisitNA() throws Exception {
		evaluate("Regression", "VisitNA");
	}

	@Test
	public void evaluateVisitNALimit() throws Exception {
		evaluate("Regression", "VisitNA@31");
	}
}
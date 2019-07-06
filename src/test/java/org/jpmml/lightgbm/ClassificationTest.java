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

import org.jpmml.evaluator.RealNumberEquivalence;
import org.junit.Test;

public class ClassificationTest extends LightGBMTest {

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("Classification", "Audit");
	}

	@Test
	public void evaluateAuditLimit() throws Exception {
		evaluate("Classification", "Audit@17", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateAuditInvalid() throws Exception {
		evaluate("Classification", "AuditInvalid");
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("Classification", "AuditNA", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateAuditNALimit() throws Exception {
		evaluate("Classification", "AuditNA@17");
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("Classification", "Iris");
	}

	@Test
	public void evaluateIrisLimit() throws Exception {
		evaluate("Classification", "Iris@7", new RealNumberEquivalence(2));
	}

	@Test
	public void evaluateIrisNA() throws Exception {
		evaluate("Classification", "IrisNA");
	}

	@Test
	public void evaluateIrisNALimit() throws Exception {
		evaluate("Classification", "IrisNA@7");
	}

	@Test
	public void evaluateVersicolor() throws Exception {
		evaluate("Classification", "Versicolor");
	}

	@Test
	public void evaluateVersicolorLimit() throws Exception {
		evaluate("Classification", "Versicolor@9");
	}
}
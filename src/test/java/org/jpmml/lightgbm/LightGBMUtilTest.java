/*
 * Copyright (c) 2019 Villu Ruusmann
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

import org.dmg.pmml.Interval;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LightGBMUtilTest {

	@Test
	public void parseInterval(){
		Interval interval = LightGBMUtil.parseInterval("[-inf:0]");

		assertEquals(null, interval.getLeftMargin());
		assertEquals(0d, interval.getRightMargin());

		interval = LightGBMUtil.parseInterval("[0:inf]");

		assertEquals(0d, interval.getLeftMargin());
		assertEquals(null, interval.getRightMargin());

		interval = LightGBMUtil.parseInterval("[-inf:inf]");

		assertNull(interval);
	}

	@Test
	public void unescape(){
		assertEquals("\u5426", LightGBMUtil.unescape("\\u5426"));
		assertEquals("\u662f", LightGBMUtil.unescape("\\u662f"));
	}
}
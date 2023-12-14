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
package org.jpmml.lightgbm;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PandasUtilTest {

	@Test
	public void parse(){
		assertNull(parsePandasCategorical("null"));

		assertEquals(PandasUtilTest.CATEGORIES_LIST_OF_LISTS, parsePandasCategorical(PandasUtilTest.CATEGORIES_STRING));
	}

	@Test
	public void format(){
		assertEquals("null", formatPandasCategorical(null));

		assertEquals(removeWhitespace(PandasUtilTest.CATEGORIES_STRING), removeWhitespace(formatPandasCategorical(PandasUtilTest.CATEGORIES_LIST_OF_LISTS)));
	}

	static
	private List<List<?>> parsePandasCategorical(String value){
		return PandasUtil.parsePandasCategorical(PandasUtil.PREFIX_PANDAS_CATEGORICAL + value);
	}

	static
	private String formatPandasCategorical(List<List<?>> objects){
		String result = PandasUtil.formatPandasCategorical(objects);

		return result.substring(PandasUtil.PREFIX_PANDAS_CATEGORICAL.length());
	}

	static
	private String removeWhitespace(String string){
		return string.replaceAll("\\s","");
	}

	private static final String CATEGORIES_STRING = "[[\"null\", \"A\", \"B, B\", \"C, [C], C\"], [-2, -1, 0, 1, 2], [-2.0, -1.0, 0.0, 1.0, 2.0], [false, true]]";
	private static final List<List<?>> CATEGORIES_LIST_OF_LISTS = Arrays.asList(Arrays.asList("null", "A", "B, B", "C, [C], C"), Arrays.asList(-2L, -1L, 0L, 1L, 2L), Arrays.asList(-2d, -1d, 0d, 1d, 2d), Arrays.asList(Boolean.FALSE, Boolean.TRUE));
}
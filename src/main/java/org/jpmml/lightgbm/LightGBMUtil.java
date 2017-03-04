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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.io.CharStreams;
import org.dmg.pmml.Interval;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;

public class LightGBMUtil {

	private LightGBMUtil(){
	}

	static
	public GBDT loadGBDT(InputStream is) throws IOException {
		return loadGBDT(parseText(is));
	}

	static
	public GBDT loadGBDT(Iterator<String> lines){
		List<Section> sections = loadText(lines);

		GBDT gbdt = new GBDT();
		gbdt.load(sections);

		return gbdt;
	}

	static
	public Schema toLightGBMSchema(final GBDT gbdt, final Schema schema){
		Function<Feature, Feature> function = new Function<Feature, Feature>(){

			private List<Feature> features = schema.getFeatures();


			@Override
			public Feature apply(Feature feature){

				if(feature instanceof BinaryFeature){
					int index = this.features.indexOf(feature);

					if(index < 0){
						throw new IllegalArgumentException();
					}

					Boolean binary = gbdt.isBinary(index);
					if(binary != null && binary.booleanValue()){
						return feature;
					}
				}

				return feature.toContinuousFeature();
			}
		};

		return schema.toTransformedSchema(function);
	}

	static
	private List<Section> loadText(Iterator<String> lines){
		List<Section> sections = new ArrayList<>();

		Section section = new Section();

		loop:
		while(lines.hasNext()){
			String line = lines.next();

			if(("").equals(line)){

				if(section.size() > 0){
					sections.add(section);

					section = new Section();
				}

				continue loop;
			}

			String key;
			String value;

			int equals = line.indexOf('=');
			if(equals > 0){
				key = line.substring(0, equals);
				value = line.substring(equals + 1);
			} else

			{
				key = line;
				value = null;
			}

			section.put(key, value);
		}

		if(section.size() > 0){
			sections.add(section);
		}

		return sections;
	}

	static
	public Iterator<String> parseText(InputStream is) throws IOException {
		Reader reader = new InputStreamReader(is, "US-ASCII");

		List<String> lines = CharStreams.readLines(reader);

		return lines.iterator();
	}

	static
	public String[] parseStringArray(String string, int length){
		String[] result = string.split("\\s");

		if(result.length != length){
			throw new IllegalArgumentException();
		}

		return result;
	}

	static
	public int[] parseIntArray(String string, int length){
		int[] result = new int[length];

		String[] values = parseStringArray(string, length);
		for(int i = 0; i < length; i++){
			result[i] = Integer.parseInt(values[i]);
		}

		return result;
	}

	static
	public double[] parseDoubleArray(String string, int length){
		double[] result = new double[length];

		String[] values = parseStringArray(string, length);
		for(int i = 0; i < length; i++){
			result[i] = Double.parseDouble(values[i]);
		}

		return result;
	}

	static
	public boolean isInterval(String string){
		return string.startsWith("[") && string.endsWith("]");
	}

	static
	public boolean isBinaryInterval(String string){
		return string.equals("[0:1]");
	}

	static
	public boolean isValues(String string){
		return !isInterval(string);
	}

	static
	public Interval parseInterval(String string){

		if(string.length() < 3){
			throw new IllegalArgumentException();
		}

		String bounds = string.substring(0, 1) + string.substring(string.length() - 1, string.length());
		String margins = string.substring(1, string.length() - 1);

		Interval.Closure closure;

		switch(bounds){
			case "[]":
				closure = Interval.Closure.CLOSED_CLOSED;
				break;
			default:
				throw new IllegalArgumentException(bounds);
		}

		String[] values = margins.split(":");
		if(values.length != 2){
			throw new IllegalArgumentException(margins);
		}

		Double leftMargin = Double.valueOf(values[0]);
		Double rightMargin = Double.valueOf(values[1]);

		Interval interval = new Interval(closure)
			.setLeftMargin(leftMargin)
			.setRightMargin(rightMargin);

		return interval;
	}

	static
	public List<String> parseValues(String string){
		String[] values = string.split(":");

		return Arrays.asList(values);
	}
}
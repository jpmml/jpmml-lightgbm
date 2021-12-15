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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.io.CharStreams;
import org.dmg.pmml.Interval;

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

			section.put(line);
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

		if(length > -1 && result.length != length){
			throw new IllegalArgumentException("Expected " + length + " elements, got " + result.length + " elements");
		}

		return result;
	}

	static
	public int[] parseIntArray(String string, int length){
		String[] values = parseStringArray(string, length);

		int[] result = new int[values.length];

		for(int i = 0; i < result.length; i++){
			result[i] = parseInt(values[i]);
		}

		return result;
	}

	static
	public long[] parseUnsignedIntArray(String string, int length){
		String[] values = parseStringArray(string, length);

		long[] result = new long[values.length];

		for(int i = 0; i < result.length; i++){
			result[i] = parseUnsignedInt(values[i]);
		}

		return result;
	}

	static
	public double[] parseDoubleArray(String string, int length){
		String[] values = parseStringArray(string, length);

		double[] result = new double[values.length];

		for(int i = 0; i < result.length; i++){
			result[i] = parseDouble(values[i]);
		}

		return result;
	}

	static
	private int parseInt(String string){
		return Integer.parseInt(string);
	}

	static
	private long parseUnsignedInt(String string){
		return Long.parseLong(string);
	}

	static
	private double parseDouble(String string){

		switch(string){
			case "inf":
				return Double.POSITIVE_INFINITY;
			default:
				return Double.parseDouble(string);
		}
	}

	static
	public boolean isNone(String string){
		return string.equals("none");
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
				throw new IllegalArgumentException(string);
		}

		String[] values = margins.split(":");
		if(values.length != 2){
			throw new IllegalArgumentException(margins);
		}

		Double leftMargin = null;
		if(!(values[0]).equalsIgnoreCase("-inf")){
			leftMargin = Double.valueOf(values[0]);
		}

		Double rightMargin = null;
		if(!(values[1]).equalsIgnoreCase("inf")){
			rightMargin = Double.valueOf(values[1]);
		} // End if

		if(leftMargin == null && rightMargin == null){
			return null;
		}

		Interval interval = new Interval(closure)
			.setLeftMargin(leftMargin)
			.setRightMargin(rightMargin);

		return interval;
	}

	static
	public List<Integer> parseValues(String string){
		String[] values = string.split(":");

		return Stream.of(values)
			.map(Integer::valueOf)
			.collect(Collectors.toList());
	}

	static
	public String unescape(String string){

		if(string == null || !string.contains("\\u")){
			return string;
		}

		StringBuffer sb = new StringBuffer(string.length());

		Matcher matcher = LightGBMUtil.PATTERN_UNICODE_ESCAPE.matcher(string);
		while(matcher.find()){
			int c = Integer.parseInt(matcher.group(1), 16);

			matcher.appendReplacement(sb, Character.toString((char)c));
		}

		matcher.appendTail(sb);

		return sb.toString();
	}

	private static final Pattern PATTERN_UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
}
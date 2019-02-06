/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 * 
 * Complete list of developers available at our web site:
 * 
 * http://rapidminer.com
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
*/
package com.rapidminer.parameter.value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;


/**
 * A list of parameter values.
 *
 * @author Tobias Malbrecht
 */
public class ParameterValueList extends ParameterValues implements Iterable<String> {

	List<String> values;

	public ParameterValueList(Operator operator, ParameterType type) {
		this(operator, type, new LinkedList<String>());
	}

	public ParameterValueList(Operator operator, ParameterType type, String[] valuesArray) {
		super(operator, type);
		this.values = new ArrayList<>(Arrays.asList(valuesArray));
	}

	public ParameterValueList(Operator operator, ParameterType type, List<String> values) {
		super(operator, type);
		this.values = values;
	}

	@Override
	public void move(int index, int direction) {
		int newPosition = index + direction;
		if (newPosition >= 0 && newPosition < values.size()) {
			String object = values.remove(index);
			values.add(newPosition, object);
		}
	}

	public List<String> getValues() {
		return values;
	}

	@Override
	public String[] getValuesArray() {
		String[] valuesArray = new String[values.size()];
		values.toArray(valuesArray);
		return valuesArray;
	}

	public void add(String value) {
		values.add(value);
	}

	public boolean contains(String value) {
		return values.contains(value);
	}

	public void remove(String value) {
		values.remove(value);
	}

	@Override
	public Iterator<String> iterator() {
		return values.iterator();
	}

	@Override
	public int getNumberOfValues() {
		return values.size();
	}

	@Override
	public String getValuesString() {
		StringBuffer valuesStringBuffer = new StringBuffer();
		boolean first = true;
		for (String value : values) {
			if (!first) {
				valuesStringBuffer.append(",");
			}
			first = false;
			valuesStringBuffer.append(value);
		}
		return valuesStringBuffer.toString();
	}

	@Override
	public String toString() {
		return "list: " + getValuesString();
	}
}

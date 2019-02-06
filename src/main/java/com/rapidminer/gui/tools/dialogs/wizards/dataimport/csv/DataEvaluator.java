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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport.csv;

import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.container.Range;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * @author Tobias Malbrecht
 */
public abstract class DataEvaluator {

	private final NumberFormat numberFormat;

	private int rowCount = 0;

	private int columnCount = 0;

	private String[] columnNames = null;

	private boolean[] canParseDouble = null;

	private boolean[] canParseInteger = null;

	private double[] minValues = null;

	private double[] maxValues = null;

	private int[] numberOfMissings = null;

	private ArrayList<LinkedHashSet<String>> valueSets = new ArrayList<LinkedHashSet<String>>();

	private int[] valueTypes = null;

	private boolean complete = false;

	// TODO add date guessing and missing evaluation
	// TODO add different formats for each string array value
	public DataEvaluator(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		start();
	}

	public void start() {
		rowCount = 0;
		columnCount = 0;
		columnNames = null;
		canParseDouble = new boolean[columnCount];
		canParseInteger = new boolean[columnCount];
		minValues = new double[columnCount];
		maxValues = new double[columnCount];
		numberOfMissings = new int[columnCount];
		valueTypes = null;
		valueSets.clear();
		complete = false;
	}

	public void setColumnNames(String[] columnNames) {
		if (columnCount < columnNames.length) {
			this.columnNames = new String[columnNames.length];
			for (int i = 0; i < columnNames.length; i++) {
				this.columnNames[i] = columnNames[i];
			}
			extendToLength(columnNames.length);
		} else {
			this.columnNames = columnNames;
		}
	}

	public void setValueTypes(int[] valueTypes) {
		if (columnCount < valueTypes.length) {
			this.valueTypes = new int[valueTypes.length];
			for (int i = 0; i < valueTypes.length; i++) {
				this.valueTypes[i] = valueTypes[i];
			}
			extendToLength(valueTypes.length);
		} else {
			this.valueTypes = valueTypes;
		}
	}

	public void update(String[] values) {
		if (columnCount < values.length) {
			extendToLength(values.length);
		}
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null || values[i].isEmpty()) {
				numberOfMissings[i]++;
				continue;
			}
			valueSets.get(i).add(values[i]);
			// TODO add date handling
			if (canParseDouble[i]) {
				try {
					Number number = numberFormat.parse(values[i]);
					if (minValues[i] > number.doubleValue()) {
						minValues[i] = number.doubleValue();
					}
					if (maxValues[i] < number.doubleValue()) {
						maxValues[i] = number.doubleValue();
					}
					if (canParseInteger[i]) {
						if (!Tools.isEqual(Math.round(number.doubleValue()), number.intValue())) {
							canParseInteger[i] = false;
						}
					}
				} catch (ParseException e) {
					canParseDouble[i] = false;
					canParseInteger[i] = false;
				}
			}
		}
		rowCount++;
	}

	private void extendToLength(int length) {
		boolean[] newCanParseDouble = new boolean[length];
		boolean[] newCanParseInteger = new boolean[length];
		double[] newMinValues = new double[length];
		double[] newMaxValues = new double[length];
		int[] newNumberOfMissings = new int[length];
		for (int i = 0; i < length; i++) {
			newCanParseDouble[i] = true;
			newCanParseInteger[i] = true;
			newMinValues[i] = Double.MAX_VALUE;
			newMaxValues[i] = Double.MIN_VALUE;
			newNumberOfMissings[i] = 0;
		}
		for (int i = 0; i < columnCount; i++) {
			newCanParseDouble[i] = canParseDouble[i];
			newCanParseInteger[i] = canParseInteger[i];
			newMinValues[i] = minValues[i];
			newMaxValues[i] = maxValues[i];
			newNumberOfMissings[i] = numberOfMissings[i];
		}
		canParseDouble = newCanParseDouble;
		canParseInteger = newCanParseInteger;
		minValues = newMinValues;
		maxValues = newMaxValues;
		numberOfMissings = newNumberOfMissings;
		int difference = length - valueSets.size();
		for (int i = 0; i < difference; i++) {
			valueSets.add(new LinkedHashSet<String>());
		}
		columnCount = length;
	}

	public void finish(boolean complete) {
		this.complete = complete;
		if (columnNames == null) {
			this.columnNames = new String[columnCount];
		} else if (columnCount > columnNames.length) {
			String[] newColumnNames = new String[columnNames.length];
			for (int i = 0; i < columnNames.length; i++) {
				newColumnNames[i] = columnNames[i];
			}
			this.columnNames = newColumnNames;
		}
		for (int i = 0; i < columnNames.length; i++) {
			if (columnNames[i] == null || columnNames[i].isEmpty()) {
				columnNames[i] = getGenericColumnName(i);
			}
		}
		this.valueTypes = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			if (canParseInteger[i]) {
				valueTypes[i] = Ontology.INTEGER;
				continue;
			}
			if (canParseDouble[i]) {
				valueTypes[i] = Ontology.REAL;
				continue;
			}
			if (valueSets.get(i).size() <= 2) {
				valueTypes[i] = Ontology.BINOMINAL;
				continue;
			}
			valueTypes[i] = Ontology.NOMINAL;
		}
	}

	protected String[] getColumnNames() {
		return columnNames;
	}

	protected int getColumnCount() {
		return columnCount;
	}

	protected int getRowCount() {
		return rowCount;
	}

	protected int[] getValueTypes() {
		return valueTypes;
	}

	protected int[] getNumberOfMissings() {
		return numberOfMissings;
	}

	protected Set<String> getValueSet(int column) {
		return valueSets.get(column);
	}

	protected boolean isGuess() {
		return !complete;
	}

	public ExampleSetMetaData getMetaData() {
		ExampleSetMetaData metaData = new ExampleSetMetaData();
		for (int i = 0; i < getColumnCount(); i++) {
			AttributeMetaData amd = new AttributeMetaData(getColumnNames()[i], getValueTypes()[i]);
			MDInteger missings = new MDInteger(getNumberOfMissings()[i]);
			SetRelation relation = SetRelation.EQUAL;
			if (isGuess()) {
				relation = SetRelation.SUPERSET;
				missings.increaseByUnknownAmount();
			}
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(getValueTypes()[i], Ontology.NUMERICAL)) {
				amd.setValueRange(new Range(minValues[i], maxValues[i]), relation);
			} else {
				amd.setValueSet(getValueSet(i), relation);
			}
			amd.setNumberOfMissingValues(missings);
			metaData.addAttribute(amd);
		}
		metaData.setNumberOfExamples(new MDInteger(getRowCount()));
		if (isGuess()) {
			metaData.getNumberOfExamples().increaseByUnknownAmount();
			metaData.attributesAreSuperset();
		}
		return metaData;
	}

	public abstract String getGenericColumnName(int column);
}

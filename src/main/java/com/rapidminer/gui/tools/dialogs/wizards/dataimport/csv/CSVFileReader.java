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

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jfree.util.Log;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowReader;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.CSVParseException;
import com.rapidminer.tools.LineParser;
import com.rapidminer.tools.Ontology;


/**
 * A helper class for reading CSV files
 *
 * @author Tobias Malbrecht
 */
public class CSVFileReader {

	private static final int MAX_LINES = 2000;

	private final File file;

	private final boolean useFirstRowAsColumnNames;

	private final LineParser parser;

	private final NumberFormat numberFormat;

	private boolean eofReached = false;

	private int rowCount = -1;

	private final DataEvaluator dataEvaluator;

	public CSVFileReader(final File file, boolean useFirstRowAsColumnNames, LineParser parser, NumberFormat numberFormat) {
		this.file = file;
		this.useFirstRowAsColumnNames = useFirstRowAsColumnNames;
		this.parser = parser;
		this.numberFormat = numberFormat;
		this.dataEvaluator = new DataEvaluator(numberFormat) {

			@Override
			public String getGenericColumnName(int column) {
				return file.getName() + "_" + (column + 1);
			}
		};
	}

	public LinkedList<String[]> readData(int maxLines) throws IOException {
		String line = null;
		eofReached = false;
		boolean first = true;
		LinkedList<String[]> valueLines = new LinkedList<String[]>();
		try (LineReader reader = new LineReader(file)) {
			dataEvaluator.start();
			do {
				line = reader.readLine();
				if (line != null) {
					String[] valueLine = parser.parse(line);
					if (valueLine != null) {
						if (first) {
							first = false;
							if (useFirstRowAsColumnNames) {
								dataEvaluator.setColumnNames(valueLine);
								continue;
							}
						}
						dataEvaluator.update(valueLine);
						valueLines.add(valueLine);
					}
					rowCount++;
				} else {
					eofReached = true;
					break;
				}
			} while (rowCount < maxLines);
		}
		dataEvaluator.finish(eofReached);
		return valueLines;
	}

	private void guessMetaData() throws IOException {
		readData(MAX_LINES);
	}

	public MetaData getMetaData() throws IOException {
		guessMetaData();
		return dataEvaluator.getMetaData();
	}

	public ExampleSet createExampleSet() throws IOException {
		guessMetaData();
		ExampleSetMetaData metaData = dataEvaluator.getMetaData();
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(metaData.getAllAttributes().size());
		for (AttributeMetaData amd : metaData.getAllAttributes()) {
			attributes.add(AttributeFactory.createAttribute(amd.getName(), amd.getValueType()));
		}
		return ExampleSets.from(attributes).withDataRowReader(getDataRowReader(attributes)).build();
	}

	public Iterator<String[]> getDataReader() throws IOException {
		Iterator<String[]> iterator = new Iterator<String[]>() {

			private String line = null;

			private boolean first = useFirstRowAsColumnNames;

			private LineReader reader = new LineReader(file);

			@Override
			public boolean hasNext() {
				try {
					if (first) {
						do {
							line = reader.readLine();
							if (line == null) {
								return false;
							}
						} while (parser.parse(line) == null);
						first = false;
					}
					do {
						line = reader.readLine();
						if (line == null) {
							reader.close();
							return false;
						}
					} while (parser.parse(line) == null);
					return true;
				} catch (IOException e) {
					return false;
				}
			}

			@Override
			public String[] next() {
				try {
					return parser.parse(line);
				} catch (CSVParseException e) {
					throw new IllegalArgumentException(e);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can not remove data rows from reader.");
			}

		};
		return iterator;
	}

	public DataRowReader getDataRowReader(final List<Attribute> attributeList) throws IOException {
		DataRowReader dataRowReader = new DataRowReader() {

			private Iterator<String[]> iterator = getDataReader();

			private int columnCount = attributeList.size();

			private Attribute[] attributes = new Attribute[columnCount];
			{
				attributes = attributeList.toArray(attributes);
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public DataRow next() {
				String[] valueLine = iterator.next();
				double[] values = new double[columnCount];
				for (int i = 0; i < columnCount; i++) {
					values[i] = Double.NaN;
				}
				for (int i = 0; i < valueLine.length; i++) {
					if (i >= valueLine.length) {
						Log.warn("Metadata was not correctly specified.");
						continue;
					}
					if (valueLine[i] == null || valueLine[i].isEmpty()) {
						continue;
					}
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[i].getValueType(), Ontology.NUMERICAL)) {
						try {
							values[i] = numberFormat.parse(valueLine[i]).doubleValue();
						} catch (ParseException e) {
							System.err.println("cannot parse");
						}
						continue;
					}
					if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attributes[i].getValueType(), Ontology.NOMINAL)) {
						values[i] = attributes[i].getMapping().mapString(valueLine[i]);
						continue;
					}
				}
				return new DoubleArrayDataRow(values);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("Can not remove data rows from reader.");
			}
		};
		return dataRowReader;
	}
}

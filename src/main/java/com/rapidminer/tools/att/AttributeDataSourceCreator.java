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
package com.rapidminer.tools.att;

import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.RapidMinerLineReader;
import com.rapidminer.tools.LoggingHandler;
import com.rapidminer.tools.Ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


/**
 * This class can be used as a simple attribute data source creation factory for many types of table
 * like data.
 * 
 * @author Ingo Mierswa
 */
public class AttributeDataSourceCreator {

	/** The list of the abstract attribute informations. */
	private ArrayList<AttributeDataSource> sources = new ArrayList<AttributeDataSource>();

	public AttributeDataSourceCreator() {}

	public List<AttributeDataSource> getAttributeDataSources() {
		return sources;
	}

	public void loadData(File file, char[] commentChars, String columnSeparators, char decimalPointCharacter,
			boolean useQuotes, char quoteChar, char escapeChar, boolean trimLines, boolean firstLineAsNames, int maxCounter,
			boolean skipErrorLines, Charset encoding, LoggingHandler logging) throws IOException {
		this.sources.clear();
		String[] columnNames = null;
		int maxColumns = -1;
		int[] valueTypes = null;
		boolean[] onlyMissing = null;

		RapidMinerLineReader lineReader = new RapidMinerLineReader(columnSeparators, commentChars, useQuotes, quoteChar,
				escapeChar, trimLines, skipErrorLines);
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));

		int counter = 0;
		boolean first = true;
		while ((maxCounter < 0) || (counter <= maxCounter)) {
			String[] columns = lineReader.readLine(in, -1);
			if (columns == null) {
				break;
			}

			boolean lineOk = true;
			if ((maxColumns != -1) && (maxColumns != columns.length)) {
				lineOk = false;
				if (skipErrorLines) {
					if (logging != null) {
						logging.logWarning("Number of columns in line " + counter + " was unexpected, was: "
								+ columns.length + ", expected: " + maxColumns + ". Skipping line...");
					}
				} else {
					throw new IOException("Number of columns in line " + counter + " was unexpected, was: " + columns.length
							+ ", expected: " + maxColumns);
				}
			}

			if (lineOk) {
				if (first) {
					maxColumns = columns.length;
					valueTypes = new int[maxColumns];
					onlyMissing = new boolean[maxColumns];
					for (int i = 0; i < valueTypes.length; i++) {
						valueTypes[i] = Ontology.INTEGER;
						onlyMissing[i] = true;
					}
					if (firstLineAsNames) {
						columnNames = columns;
					} else {
						guessValueTypes(columns, valueTypes, onlyMissing, decimalPointCharacter);
					}
					first = false;
				} else {
					guessValueTypes(columns, valueTypes, onlyMissing, decimalPointCharacter);
				}
			}

			counter++;
		}
		in.close();

		// set the value type of all columns with missing values only to nominal
		for (int i = 0; i < valueTypes.length; i++) {
			if (onlyMissing[i]) {
				valueTypes[i] = Ontology.NOMINAL;
			}
		}

		if (columnNames == null) {
			String defaultName = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(File.separator) + 1);
			columnNames = new String[maxColumns];
			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i] = defaultName + " (" + (i + 1) + ")";
			}
		} else if (columnNames.length < maxColumns) {
			String defaultName = file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(File.separator) + 1);
			String[] newColumnNames = new String[maxColumns];
			System.arraycopy(columnNames, 0, newColumnNames, 0, columnNames.length);
			for (int i = columnNames.length; i < newColumnNames.length; i++) {
				newColumnNames[i] = defaultName + " (" + (i + 1) + ")";
			}
		}
		for (int i = 0; i < maxColumns; i++) {
			this.sources.add(new AttributeDataSource(AttributeFactory.createAttribute(columnNames[i], valueTypes[i]), file,
					i, "attribute"));
		}
	}

	public static void guessValueTypes(String[] data, int[] valueTypes, boolean[] onlyMissing, char decimalPointCharacter) {
		for (int c = 0; c < valueTypes.length; c++) {
			String value = data[c];
			if ((value != null) && (!value.equals("?")) && (value.length() > 0)) {
				onlyMissing[c] = false;
				try {
					String valueString = value.replace(decimalPointCharacter, '.');
					double d = Double.parseDouble(valueString);
					if ((valueTypes[c] == Ontology.INTEGER) && ((int) d != d)) {
						valueTypes[c] = Ontology.REAL;
					}
				} catch (NumberFormatException e) {
					valueTypes[c] = Ontology.NOMINAL;
				}
			}
		}
	}
}

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
package com.rapidminer.example.table;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * Reads the data rows in sparse format. The format is specified in the class comment of
 * {@link com.rapidminer.operator.io.SparseFormatExampleSource}. {@link Attribute}s may be passed to
 * the reader in its constructor. If they are ommitted, they are generated on the fly. In either
 * case, indices are assigned to the attributes. If an {@link AbstractExampleTable} is generated
 * using instances of this class, the constructor of {@link AbstractExampleTable} will reassign
 * these indexes.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class SparseFormatDataRowReader extends AbstractDataRowReader {

	/** Names of the formats. */
	public static final String[] FORMAT_NAMES = { "xy", "yx", "prefix", "separate_file", "no_label" };

	/** Label succeeds attributes. */
	public static final int FORMAT_XY = 0;

	/** Label preceeds attributes. */
	public static final int FORMAT_YX = 1;

	/** Label has a prefix specified in the prefix map. */
	public static final int FORMAT_PREFIX = 2;

	/** Label is in separate file. */
	public static final int FORMAT_SEPARATE_FILE = 3;

	/** Label is missing. */
	public static final int FORMAT_NO_LABEL = 4;

	/** Reader for the labels. */
	private BufferedReader inAttributes, inLabels;

	/** The attribute set with regular and special attributes. */
	private AttributeSet attributeSet = null;

	/** Remember if an end of file has occured. */
	private boolean eof;

	/** Remember if a line has already been read. */
	private boolean lineRead;

	/** The maximum number of attributes to read. */
	private int maxNumber;

	/** Number of lines already read. */
	private int linesRead;

	/** The DataRow that will be returned in the next call to {@link #next()} */
	private DataRow currentDataRow;

	/**
	 * One out of FORMAT_XY, FORMAT_YX, FORMAT_PREFIX, FORMAT_SEPARATE_FILE, and FORMAT_NO_LABEL.
	 */
	private int format;

	/**
	 * The dimension of the examples, i.e. the total number of regular and special attributes.
	 */
	private int dimension;

	/** Maps prefixes to special attribute names, e.g. "l:" to "label". */
	private Map<String, String> prefixMap = new HashMap<String, String>();

	private boolean useQuotesForNominalValues;

	private char quoteChar;

	/**
	 * Creates a new data row reader for sparse format. The attributes indices must not be set. If
	 * they are, they are reassigned new values when this constructor is called!
	 * 
	 * @param factory
	 *            Factory used to create {@link DataRow} instances.
	 * @param format
	 *            One Out of FORMAT_XY, FORMAT_YX, FORMAT_PREFIX, and FORMAT_SEPARATE_FILE.
	 * @param prefixMap
	 *            Maps prefixes to special attribute names (e.g. &quot;l&quot; to
	 *            &quot;label&quot;).
	 * @param attributeSet
	 *            Set of regular and special attributes.
	 * @param attributeReader
	 *            Reader for the data
	 * @param labelReader
	 *            Reader for the labels. Only necessary if format is FORMAT_SEPARATE_FILE.
	 * @param sampleSize
	 *            sample size, may be -1 for no limit.
	 * 
	 * @param useQuotesForNominalValues
	 *            Determines whether nominal values are surrounded by quotes or not. If
	 *            <code>useQuotesForNominalValues == true</code> the first and last character of the
	 *            nominal values are ignored.
	 * @param quoteChar
	 *            The char that is used to surround nominal values.
	 */
	public SparseFormatDataRowReader(DataRowFactory factory, int format, Map<String, String> prefixMap,
			AttributeSet attributeSet, Reader attributeReader, Reader labelReader, int sampleSize,
			boolean useQuotesForNominalValues, char quoteChar) {
		super(factory);
		this.format = format;
		this.prefixMap = prefixMap;
		this.attributeSet = attributeSet;
		if (attributeSet == null) {
			throw new IllegalArgumentException("AttributeSet must not be null.");
		}
		this.dimension = attributeSet.getAllAttributes().size();
		this.maxNumber = sampleSize;
		this.inAttributes = new BufferedReader(attributeReader);
		if (format == FORMAT_SEPARATE_FILE) {
			if (labelReader == null) {
				throw new IllegalArgumentException("labelReader must not be null if format is 'separate_file'!");
			}
			this.inLabels = new BufferedReader(labelReader);
		}
		if (format != FORMAT_NO_LABEL) {
			if (attributeSet.getSpecialAttribute("label") == null) {
				throw new IllegalArgumentException("If format is not no_label, label attribute must be defined.");
			}
		}
		this.useQuotesForNominalValues = useQuotesForNominalValues;
		this.quoteChar = quoteChar;
	}

	/** Checks if further examples exist. Returns false if one of the files end. */
	@Override
	public boolean hasNext() {
		if ((maxNumber > -1) && (linesRead >= maxNumber)) {
			return false;
		}
		if (lineRead) {
			return !eof;
		}
		try {
			eof = !readLine();
			if (eof) {
				inAttributes.close();
				if (inLabels != null) {
					inLabels.close();
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		lineRead = true;
		return (!eof);
	}

	private boolean readLine() throws IOException {
		String attributeLine = null;
		do {
			attributeLine = inAttributes.readLine();
			if (attributeLine == null) {
				return false;
			}
		} while (attributeLine.startsWith("#") || (attributeLine.length() == 0));

		this.currentDataRow = getFactory().create(dimension);

		StringTokenizer tokenizer = new StringTokenizer(attributeLine);

		String labelString = null;
		if (format == FORMAT_YX) {
			labelString = tokenizer.nextToken();
		} else if (format == FORMAT_SEPARATE_FILE) {
			do {
				labelString = inLabels.readLine();
				if (labelString == null) {
					return false;
				}
			} while (labelString.startsWith("#") || (labelString.length() == 0));
		}

		while (tokenizer.hasMoreTokens()) {
			String attributeToken = tokenizer.nextToken();

			int colonIndex = attributeToken.indexOf(':');
			if ((format == FORMAT_XY) && (colonIndex == -1)) {
				if (labelString != null) {
					throw new IOException("Malformed line in examplefile: " + attributeToken);
				} else {
					labelString = attributeToken;
				}
			} else {
				String pos = attributeToken.substring(0, colonIndex);// references
																		// the
																		// attribute
				String value = attributeToken.substring(colonIndex + 1); // the
																			// attribute
																			// value
				Attribute attribute = null; // the referenced attribute

				try {
					int index = Integer.parseInt(pos) - 1;
					if ((index < 0) || (index >= attributeSet.getNumberOfRegularAttributes())) {
						throw new IOException("Attribute index out of range: '" + (index + 1)
								+ "'! Index must be between 1 and dimension " + attributeSet.getNumberOfRegularAttributes()
								+ "!");
					}
					attribute = attributeSet.getAttribute(index);
				} catch (NumberFormatException e) {
					String specialAttributeName = prefixMap.get(pos);
					if (specialAttributeName == null) {
						attribute = attributeSet.getSpecialAttribute(pos);
						if (attribute == null) {
							throw new IOException(
									"Illegal attribute index: '"
											+ pos
											+ "' (legal values are integers and defined prefixes for special attributes (Parameter prefix_map of SparseFormatExampleSource))!");
						}

					} else {
						attribute = attributeSet.getSpecialAttribute(specialAttributeName);
					}
					if (attribute == null) {
						throw new IOException("Unknown special attribute: " + specialAttributeName);
					}
				}

				if (attribute != null) {
					if (attribute.isNominal()) {
						if (useQuotesForNominalValues) {
							String quote = Character.toString(quoteChar);
							if (value.startsWith(quote) && value.endsWith(quote)) {
								value = value.substring(1, value.length() - 1);
							} else {
								throw new RuntimeException("The value ' " + value
										+ " ' does not start and end with a quote character ' " + quote + " '.");
							}
							Tools.unescape(value);
						}
						currentDataRow.set(attribute, attribute.getMapping().mapString(value));
					} else {
						try {
							currentDataRow.set(attribute, Double.parseDouble(value));
						} catch (NumberFormatException e) {
							throw new IOException("Attribute is not numerical: '" + value + "'!");
						}
					}
				}
			}
		}

		if (labelString != null) {
			Attribute label = attributeSet.getSpecialAttribute("label");
			if (label.isNominal()) {
				currentDataRow.set(label, label.getMapping().mapString(labelString));
			} else {
				try {
					currentDataRow.set(label, Double.parseDouble(labelString));
				} catch (NumberFormatException e) {
					throw new IOException("Label is not numerical: '" + labelString + "'.");
				}
			}
		}

		currentDataRow.trim();
		return true;
	}

	/** Returns the next Example. */
	@Override
	public DataRow next() {
		if (eof == true) {
			return null;
		}
		if (!lineRead) {
			if (!hasNext()) {
				return null;
			}
		}
		linesRead++;
		lineRead = false;
		return currentDataRow;
	}
}

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
package com.rapidminer.example;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Ontology;


/**
 * <p>
 * This class can be used to easily create @link {ExampleSet}s and the underlying
 * {@link ExampleTable} with simple method calls. Please note that it is often better to explicitly
 * fill the data table yourself using an {@link ExampleSetBuilder} provided by {@link ExampleSets}.
 * For memory usage reasons, it is also often not recommended to create the data matrix from your
 * existing data in an extra step and then use one of the factory methods. In these cases, it is
 * better to directly fill an {@link ExampleSetBuilder} from your data source.
 * </p>
 *
 * <p>
 * However, in some cases it might be more convenient to use this class in order to create example
 * sets from data matrices in a fast and simple way. The resulting example set will be backed up by
 * a {@link ExampleTable} created by a {@link ExampleSetBuilder}. If the data set at hand is
 * completely numerical, one can simply use one of the double matrix methods provided by this class.
 * This will lead to an {@link ExampleSet} containing only numerical attributes. Otherwise, one have
 * to use the Object matrix methods. Please note that only String objects and Number objects
 * (Double, Integer) are allowed in this case. Otherwise an Exception will be thrown. In case of the
 * Object matrix methods the method tries to identify the type itself and initialized the example
 * set with the correct attribute types (nominal or numerical).
 * </p>
 *
 * <p>
 * Please note that the internal representation of the nominal attribute values depend on the order
 * they appear in the data set. If this is not allowed (e.g. for the label attribute of different
 * training and testing sets, where the internal representation should be the same in order to
 * prevent label flips) one should definitely use the usual ExampleTable - ExampleSet construction
 * where the nominal attribute value mapping can and should be performed beforehand. In these cases
 * the usage of this class is definitely not recommended.
 * </p>
 *
 * @author Ingo Mierswa
 */
public class ExampleSetFactory {

	/**
	 * Create a numerical example set from the given data matrix. The resulting example set will not
	 * contain a label and consists of numerical attributes only.
	 */
	public static ExampleSet createExampleSet(double[][] data) {
		return createExampleSet(data, null);
	}

	/**
	 * Create a numerical example set from the given data matrix. The label of the resulting example
	 * set be build from the column with the given index. The example set consists of numerical
	 * attributes only.
	 */
	public static ExampleSet createExampleSet(double[][] data, int classColumn) {
		if (data.length == 0) {
			throw new RuntimeException(
					"ExampleSetFactory.createExampleSet(double[][], int): data matrix is not allowed to be empty.");
		}
		double[][] dataWithoutLabel = new double[data.length][data[0].length - 1];
		double[] labels = new double[data.length];

		for (int e = 0; e < data.length; e++) {
			int counter = 0;
			for (int a = 0; a < data[e].length; a++) {
				if (a == classColumn) {
					labels[e] = data[e][a];
				} else {
					dataWithoutLabel[e][counter++] = data[e][a];
				}
			}
		}

		return createExampleSet(dataWithoutLabel, labels);
	}

	/**
	 * Create a numerical example set from the given data matrix. The label of the resulting example
	 * set be build from the given double array. The example set consists of numerical attributes
	 * only.
	 */
	public static ExampleSet createExampleSet(double[][] data, double[] labels) {
		if (data.length == 0) {
			throw new RuntimeException(
					"ExampleSetFactory.createExampleSet(double[][], double[]): data matrix is not allowed to be empty.");
		}

		// create attributes
		int numberOfAttributes = data[0].length;
		List<Attribute> attributeList = new ArrayList<Attribute>(numberOfAttributes + (labels != null ? 1 : 0));
		for (int a = 0; a < numberOfAttributes; a++) {
			attributeList.add(AttributeFactory.createAttribute("att" + (a + 1), Ontology.NUMERICAL));
		}
		Attribute labelAttribute = null;
		if (labels != null) {
			labelAttribute = AttributeFactory.createAttribute("label", Ontology.NUMERICAL);
			attributeList.add(labelAttribute);
		}

		// create example set
		ExampleSetBuilder builder = ExampleSets.from(attributeList).withExpectedSize(data.length);
		for (int e = 0; e < data.length; e++) {
			double[] dataRow = data[e];
			if (labelAttribute != null) {
				dataRow = new double[numberOfAttributes + 1];
				System.arraycopy(data[e], 0, dataRow, 0, data[e].length);
				dataRow[dataRow.length - 1] = labels[e];
			}
			builder.addRow(dataRow);
		}
		if (labelAttribute != null) {
			builder.withRole(labelAttribute, Attributes.LABEL_NAME);
		}

		return builder.build();
	}

	/**
	 * Create a mixed-type example set from the given data matrix. The resulting example set will
	 * not contain a label and might consist of numerical, nominal or date attributes.
	 */
	public static ExampleSet createExampleSet(Object[][] data) {
		return createExampleSet(data, null);
	}

	/**
	 * Create a numerical example set from the given data matrix. The label of the resulting example
	 * set be build from the column with the given index. The example set might consist of
	 * numerical, nominal or date attributes.
	 */
	public static ExampleSet createExampleSet(Object[][] data, int classColumn) {
		if (data.length == 0) {
			throw new RuntimeException(
					"ExampleSetFactory.createExampleSet(Object[][], int): data matrix is not allowed to be empty.");
		}
		Object[][] dataWithoutLabel = new Object[data.length][data[0].length - 1];
		Object[] labels = new Object[data.length];

		for (int e = 0; e < data.length; e++) {
			int counter = 0;
			for (int a = 0; a < data[e].length; a++) {
				if (a == classColumn) {
					labels[e] = data[e][a];
				} else {
					dataWithoutLabel[e][counter++] = data[e][a];
				}
			}
		}

		return createExampleSet(dataWithoutLabel, labels);
	}

	/**
	 * Create a numerical example set from the given data matrix. The label of the resulting example
	 * set be build from the given double array. The example set might consist of numerical, nominal
	 * or date attributes.
	 */
	public static ExampleSet createExampleSet(Object[][] data, Object[] labels) {
		if (data.length == 0) {
			throw new RuntimeException(
					"ExampleSetFactory.createExampleSet(Object[][], Object[]): data matrix is not allowed to be empty.");
		}

		// create attributes
		int numberOfAttributes = data[0].length;
		int totalNumber = numberOfAttributes + (labels != null ? 1 : 0);
		boolean[] nominal = new boolean[totalNumber];
		List<Attribute> attributeList = new ArrayList<Attribute>(totalNumber);
		for (int a = 0; a < numberOfAttributes; a++) {
			Object current = getFirstNonNull(data, a);

			if (current instanceof Number) {
				attributeList.add(AttributeFactory.createAttribute("att" + (a + 1), Ontology.NUMERICAL));
				nominal[a] = false;
			} else if (current instanceof String) {
				attributeList.add(AttributeFactory.createAttribute("att" + (a + 1), Ontology.NOMINAL));
				nominal[a] = true;
			} else if (current instanceof Date) {
				attributeList.add(AttributeFactory.createAttribute("att" + (a + 1), Ontology.DATE_TIME));
				nominal[a] = false;
			} else {
				throw new RuntimeException(
						"ExampleSetFactory.createExampleSet(Object[][], Object[]): only objects of type String or Number (Double, Integer) are allowed for the object data matrix.");
			}
		}
		Attribute labelAttribute = null;
		if (labels != null) {
			Object current = labels[0];
			if (current instanceof Number) {
				labelAttribute = AttributeFactory.createAttribute("label", Ontology.NUMERICAL);
				nominal[nominal.length - 1] = false;
			} else if (current instanceof String) {
				labelAttribute = AttributeFactory.createAttribute("label", Ontology.NOMINAL);
				nominal[nominal.length - 1] = true;
			} else if (current instanceof Date) {
				labelAttribute = AttributeFactory.createAttribute("label", Ontology.DATE_TIME);
				nominal[nominal.length - 1] = false;
			} else {
				throw new RuntimeException(
						"ExampleSetFactory.createExampleSet(Object[][], Object[]): only objects of type String or Number (Double, Integer) are allowed for the object data matrix.");
			}
			attributeList.add(labelAttribute);
		}

		// create example set
		ExampleSetBuilder builder = ExampleSets.from(attributeList).withExpectedSize(data.length);
		for (int e = 0; e < data.length; e++) {
			double[] dataRow = new double[totalNumber];
			for (int a = 0; a < numberOfAttributes; a++) {
				Object current = data[e][a];
				if (current == null) {
					dataRow[a] = Double.NaN;
				} else if (current instanceof Number) {
					if (nominal[a]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[a] = ((Number) current).doubleValue();
				} else if (current instanceof String) {
					if (!nominal[a]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[a] = attributeList.get(a).getMapping().mapString((String) current);
				} else if (current instanceof Date) {
					if (nominal[a]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[a] = ((Date) current).getTime();
				} else {
					throw new RuntimeException(
							"ExampleSetFactory.createExampleSet(Object[][], Object[]): only objects of type String or Number (Double, Integer) are allowed for the object data matrix.");
				}
			}
			if (labelAttribute != null) {
				Object current = labels[e];
				if (current == null) {
					dataRow[dataRow.length - 1] = Double.NaN;
				} else if (current instanceof Number) {
					if (nominal[nominal.length - 1]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[dataRow.length - 1] = ((Number) current).doubleValue();
				} else if (current instanceof String) {
					if (!nominal[nominal.length - 1]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[dataRow.length - 1] = attributeList.get(attributeList.size() - 1).getMapping()
							.mapString((String) current);
				} else if (current instanceof Date) {
					if (nominal[nominal.length - 1]) {
						throw new RuntimeException(
								"ExampleSetFactory.createExampleSet(Object[][], Object[]): type of objects did change in column. Only the same type of objects is allowed for complete columns.");
					}
					dataRow[dataRow.length - 1] = ((Date) current).getTime();
				} else {
					throw new RuntimeException(
							"ExampleSetFactory.createExampleSet(Object[][], Object[]): only objects of type String or Number (Double, Integer) are allowed for the object data matrix.");
				}
			}
			builder.addRow(dataRow);
		}

		if (labelAttribute != null) {
			builder.withRole(labelAttribute, Attributes.LABEL_NAME);
		}

		return builder.build();
	}

	/**
	 * @param data
	 *            the data array object
	 * @param a
	 *            the current attribute index
	 * @return the first non-null object
	 */
	private static Object getFirstNonNull(Object[][] data, int a) {
		int tryCounter = 0;
		Object current = data[tryCounter][a];
		while (current == null) {
			tryCounter++;
			if (tryCounter < data.length) {
				current = data[tryCounter][a];
			} else {
				throw new RuntimeException(
						"ExampleSetFactory.createExampleSet(Object[][], Object[]): provided attribute at column " + a
								+ " does only contain null values.");
			}
		}
		return current;
	}
}

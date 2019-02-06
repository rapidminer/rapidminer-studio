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
package com.rapidminer.operator.learner.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.internal.Resources;


/**
 * This is a fast, read-only representation of an {@link ExampleSet} with emphasis of reading the
 * values by attribute. It is useful for algorithms that iterate over (subsets of) the example set
 * multiple times. It is not useful for algorithms that consist of only one iteration over the
 * example set because of the cost for creating this representation.
 *
 * A {@link ColumnExampleTable} should be viewed as a table consisting of the columns representing
 * all nominal attributes followed by the columns representing all numerical attributes. To consider
 * only a subset of the attributes, a selection can be represented by the numbers of the columns
 * that are selected. Analogously, if only a subset of the examples (rows) should be considered, a
 * selection can be represented by the numbers of the selected rows.
 *
 * The {@link ExampleSet} is stored inside a table of arrays. The values at numerical attributes are
 * stored as double values while the values at numerical attributes are stored as byte values coming
 * from their {@link NominalMapping} or, if they are missing values, as the size of the mapping. The
 * label must not have missing values.
 *
 * @author Gisa Schaefer
 *
 */
public class ColumnExampleTable {

	/** Number of rows between checking for stop */
	private static final int CHECK_FOR_STOP_INTERVAL = 1000;

	/**
	 * the table creation is done in parallel if the product of the number of attributes and the
	 * number of examples is greater than this number
	 */
	private static final int THRESHOLD_PRODUCT_PARALLEL = 3_000_000;

	/** If a nominal attribute has more than this number of different values, it is ignored. */
	private static final int MAXIMAL_NOMINAL_VALUES = 127;

	private int numberOfExamples = 0;

	private int numberOfRegularNominalAttributes = 0;

	private int numberOfRegularNumericalAttributes = 0;

	private Attribute[] regularNominalAttributes;

	private Attribute[] regularNumericalAttributes;

	private Attribute label;

	private Attribute weight;

	/** the column containing the values of the label attribute */
	private int[] labelColumn;

	/** the column containing the values of a numerical label attribute */
	private double[] numericalLabelColumn;

	/** the column containing the values of the weight attribute - if it exists */
	private double[] weightColumn = null;

	/**
	 * table containing the values of the nominal attributes from an example set:
	 * nominalColumnTable[c][r] contains the value of the example in the row r at the attribute
	 * regularNominalAttributes[c]. nominalColumnTable[c] is the column containing the values for
	 * the nominal attribute number c.
	 */
	private byte[][] nominalColumnTable;

	/**
	 * table containing the values of the numerical attributes from an example set:
	 * numericalColumnTable[c][r] contains the value of the example in the row r at the attribute
	 * regularNumericalAttributes[c]. nominalColumnTable[c] is the column containing the values for
	 * the numerical attribute number c.
	 */
	private double[][] numericalColumnTable;

	/**
	 * The nominal column table is initialized with the values of the regular nominal attributes,
	 * the numerical column table with the ones of the regular numeric attributes. The values of the
	 * label attribute are stored in the label column and if a weight attribute exists, its values
	 * are stored in the weight column. Nominal values are stored by their number in the
	 * {@link NominalMapping} as byte if they are not missing, otherwise as the size of the mapping.
	 *
	 * @param parallelAllowed
	 *            if the table creation can be done in parallel
	 * @throws OperatorException
	 *             if the label has missing values
	 *
	 */
	public ColumnExampleTable(ExampleSet exampleSet, Operator operator, boolean parallelAllowed) throws OperatorException {
		numberOfExamples = exampleSet.size();
		label = exampleSet.getAttributes().getLabel();
		weight = exampleSet.getAttributes().getWeight();

		exampleSet.recalculateAttributeStatistics(label);
		if (exampleSet.getStatistics(label, Statistics.UNKNOWN) > 0) {
			throw new UserError(operator, 162, label.getName());
		}

		// split regular attributes into nominal and numerical and store in arrays
		List<Attribute> regularNominalAttributesList = new ArrayList<Attribute>();
		List<Attribute> regularNumericalAttributesList = new ArrayList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) { // only regular attributes
			if (attribute.isNominal()) {
				// ignore nominal attributes with too many different values
				if (attribute.getMapping().size() <= MAXIMAL_NOMINAL_VALUES) {
					regularNominalAttributesList.add(attribute);
				}
			} else {
				regularNumericalAttributesList.add(attribute);
			}
		}
		numberOfRegularNominalAttributes = regularNominalAttributesList.size();
		regularNominalAttributes = regularNominalAttributesList.toArray(new Attribute[numberOfRegularNominalAttributes]);
		numberOfRegularNumericalAttributes = regularNumericalAttributesList.size();
		regularNumericalAttributes = regularNumericalAttributesList
				.toArray(new Attribute[numberOfRegularNumericalAttributes]);

		// initialize tables
		nominalColumnTable = new byte[numberOfRegularNominalAttributes][numberOfExamples];
		numericalColumnTable = new double[numberOfRegularNumericalAttributes][numberOfExamples];
		if (label.isNominal()) {
			labelColumn = new int[numberOfExamples];
		} else {
			numericalLabelColumn = new double[numberOfExamples];
		}
		if (weight != null) {
			weightColumn = new double[numberOfExamples];
		}

		if (betterParallel(parallelAllowed, operator)) {
			populateParallel(exampleSet, operator);
		} else {
			populate(exampleSet, operator);
		}

	}

	/**
	 * Creates the tables from the {@link ExampleSet}.
	 *
	 * @param exampleSet
	 * @param operator
	 */
	private void populate(ExampleSet exampleSet, Operator operator) {
		int row = 0;
		for (Example example : exampleSet) {
			if (row % CHECK_FOR_STOP_INTERVAL == 0 && operator != null) {
				Resources.getConcurrencyContext(operator).checkStatus();
			}
			fillInRow(example, row);
			row++;
		}

	}

	/**
	 * Creates the tables from the {@link ExampleSet} in parallel.
	 *
	 * @param exampleSet
	 * @param operator
	 *            a non-null operator
	 * @throws OperatorException
	 */
	private void populateParallel(final ExampleSet exampleSet, Operator operator) throws OperatorException {
		final ConcurrencyContext context = Resources.getConcurrencyContext(operator);
		int numberOfThreads = context.getParallelism();
		int blocksize = numberOfExamples / numberOfThreads;
		int rest = numberOfExamples % numberOfThreads;

		List<Callable<Void>> todo = new ArrayList<>(numberOfThreads);
		int start = 0;
		int end = 0;
		while (end < numberOfExamples) {
			start = end;
			end += blocksize;
			if (rest > 0) {
				end++;
				rest--;
			}
			final int startRow = start;
			final int endRow = end;
			todo.add(new Callable<Void>() {

				@Override
				public Void call() {
					Example example;
					for (int row = startRow; row < endRow; row++) {
						if ((row - startRow) % CHECK_FOR_STOP_INTERVAL == 0) {
							context.checkStatus();
						}
						example = exampleSet.getExample(row);
						fillInRow(example, row);
					}
					return null;
				}

			});
		}

		try {
			context.call(todo);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			} else if (cause instanceof Error) {
				throw (Error) cause;
			} else {
				throw new OperatorException(cause.getMessage(), cause);
			}
		}
	}

	/**
	 * Fills the example in specified row of all attribute columns.
	 *
	 * @param example
	 * @param row
	 */
	private void fillInRow(Example example, int row) {
		int column = 0;
		for (Attribute attribute : regularNominalAttributes) {
			double value = example.getValue(attribute);
			if (Double.isNaN(value)) {
				value = attribute.getMapping().size();
			}
			nominalColumnTable[column][row] = (byte) value;
			column++;
		}
		if (label.isNominal()) {
			labelColumn[row] = (int) example.getValue(label);
		} else {
			numericalLabelColumn[row] = example.getValue(label);
		}

		column = 0;
		for (Attribute attribute : regularNumericalAttributes) {
			numericalColumnTable[column][row] = example.getValue(attribute);
			column++;
		}

		if (weight != null) {
			weightColumn[row] = example.getValue(weight);
		}
	}

	/**
	 * Calculates if it is better to fill the table in parallel.
	 *
	 * @param parallelAllowed
	 * @param operator
	 * @return
	 */
	private boolean betterParallel(boolean parallelAllowed, Operator operator) {
		return parallelAllowed
				&& operator != null
				&& Resources.getConcurrencyContext(operator).getParallelism() > 1
				&& ((long) numberOfRegularNominalAttributes + numberOfRegularNumericalAttributes) * numberOfExamples > THRESHOLD_PRODUCT_PARALLEL;
	}

	/**
	 * @return the number of examples in table
	 */
	public int getNumberOfExamples() {
		return numberOfExamples;
	}

	/**
	 * @return the label attribute
	 */
	public Attribute getLabel() {
		return label;
	}

	/**
	 * @return a int array containing the values of the nominal label column via the
	 *         {@link NominalMapping}.
	 */
	public int[] getLabelColumn() {
		return labelColumn;
	}

	/**
	 * @return a double array containing the values of the numerical label column
	 */
	public double[] getNumericalLabelColumn() {
		return numericalLabelColumn;
	}

	/**
	 * @return the weight attribute, if it exists, <code>null</code> otherwise
	 */
	public Attribute getWeight() {
		return weight;
	}

	/**
	 * @return the values of the weight attribute, if it exits, <code>null</code> otherwise
	 */
	public double[] getWeightColumn() {
		return weightColumn;
	}

	/**
	 * @param attributeNumber
	 *            a number that represents a nominal attribute
	 * @return the column containing the values of the represented nominal attribute
	 */
	public byte[] getNominalAttributeColumn(int attributeNumber) {
		return nominalColumnTable[attributeNumber];
	}

	/**
	 * @param attributeNumber
	 *            a number that represents a numerical attribute
	 * @return the column containing the values of the represented numerical attribute
	 */
	public double[] getNumericalAttributeColumn(int attributeNumber) {
		return numericalColumnTable[attributeNumber - numberOfRegularNominalAttributes];
	}

	/**
	 * @param attributeNumber
	 * @return <code>true</code> if the attributeNumber represents a nominal attribute
	 */
	public boolean representsNominalAttribute(int attributeNumber) {
		return attributeNumber < numberOfRegularNominalAttributes;
	}

	/**
	 * @param attributeNumber
	 * @return <code>true</code> if the attributeNumber represents a numerical attribute
	 */
	public boolean representsNumericalAttribute(int attributeNumber) {
		return attributeNumber >= numberOfRegularNominalAttributes;
	}

	public int getTotalNumberOfRegularAttributes() {
		return numberOfRegularNominalAttributes + numberOfRegularNumericalAttributes;
	}

	/**
	 * Returns the nominal attribute represented by the attributeNumber.
	 *
	 * @param attributeNumber
	 * @return
	 */
	public Attribute getNominalAttribute(int attributeNumber) {
		return regularNominalAttributes[attributeNumber];

	}

	/**
	 * Returns the numerical attribute represented by the attributeNumber.
	 *
	 * @param attributeNumber
	 * @return
	 */
	public Attribute getNumericalAttribute(int attributeNumber) {
		return regularNumericalAttributes[attributeNumber - numberOfRegularNominalAttributes];
	}

	public int getNumberOfRegularNominalAttributes() {
		return numberOfRegularNominalAttributes;
	}

	public int getNumberOfRegularNumericalAttributes() {
		return numberOfRegularNumericalAttributes;
	}

}

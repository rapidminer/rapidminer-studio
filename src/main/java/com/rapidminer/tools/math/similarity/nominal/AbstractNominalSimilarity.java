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
package com.rapidminer.tools.math.similarity.nominal;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.SimilarityMeasure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * This is the abstract superclass for all nominal similarity measures.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractNominalSimilarity extends SimilarityMeasure {

	private static final long serialVersionUID = 3932502337712338892L;

	private boolean initiated = false;

	private boolean[] binominal;

	private double[] falseIndexSet1;
	private double[] falseIndexSet2;

	private Map<Integer, Map<Double, Double>> indexMappingSet1;
	private Map<Integer, Map<Double, Double>> indexMappingSet2;

	@Override
	public double calculateDistance(double[] value1, double[] value2) {
		return -calculateSimilarity(value1, value2);
	}

	@Override
	public double calculateSimilarity(double[] value1, double[] value2) {
		if (!initiated) {
			throw new IllegalStateException("Similarity object is not initialized properly");
		}
		int equalNonFalseValues = 0;
		int unequalValues = 0;
		int equalFalseValues = 0;
		for (int i = 0; i < value1.length; i++) {

			// if one value is a Double.NaN, we will treat this as unequal values
			if (Double.isNaN(value1[i]) || Double.isNaN(value2[i])) {
				unequalValues++;
			} else if (binominal[i]) {
				if (value1[i] == falseIndexSet1[i] && value2[i] == falseIndexSet2[i]) {
					equalFalseValues++;
				} else {
					if (value1[i] != falseIndexSet1[i] && value2[i] != falseIndexSet2[i]) {
						equalNonFalseValues++;
					} else {
						unequalValues++;
					}
				}
			} else { // Polynominal mapping
				Map<Double, Double> indexMapping1 = indexMappingSet1.get(i);
				Map<Double, Double> indexMapping2 = indexMappingSet2.get(i);

				// Check if common mapping was created. If not the values have unequal values.
				if (!indexMapping1.containsKey(value1[i]) || !indexMapping2.containsKey(value2[i])) {
					unequalValues++;
				} else {
					double commonValue1 = indexMapping1.get(value1[i]);
					double commonValue2 = indexMapping2.get(value2[i]);

					if (commonValue1 != commonValue2) {
						unequalValues++;
					} else {
						equalNonFalseValues++;
					}
				}
			}

			// if (value1[i] == value2[i])
			// if (binominal[i]) {
			// if (value1[i] == falseIndexSet1[i])
			// falseValues++;
			// else
			// equalNonFalseValues++;
			// } else
			// equalNonFalseValues++;
			// else {
			// unequalValues++;
			// }
		}
		return calculateSimilarity(equalNonFalseValues, unequalValues, equalFalseValues);
	}

	/**
	 * Calculate a similarity given the number of attributes for which both examples agree/disagree.
	 * 
	 * @param equalNonFalseValues
	 *            the number of attributes for which both examples are equal and non-zero
	 * @param unequalValues
	 *            the number of attributes for which both examples have unequal values
	 * @param equalFalseValues
	 *            the number of attributes for which both examples have zero values
	 * @return the similarity
	 */
	protected abstract double calculateSimilarity(double equalNonFalseValues, double unequalValues, double equalFalseValues);

	@Override
	public void init(ExampleSet exampleSet) throws OperatorException {
		Tools.onlyNominalAttributes(exampleSet, "nominal similarities");
		init(exampleSet.getAttributes(), exampleSet.getAttributes());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.rapidminer.tools.math.similarity.DistanceMeasure#init(com.rapidminer.example.Attributes,
	 * com.rapidminer.example.Attributes)
	 */
	@Override
	public DistanceMeasureConfig init(Attributes firstSetAttributes, Attributes secondSetAttributes) {
		DistanceMeasureConfig config = super.init(firstSetAttributes, secondSetAttributes);
		if (config.isMatching()) {
			init(config.getFirstSetAttributes(), config.getSecondSetAttributes());
		}
		return config;
	}

	/**
	 * Initializes the private fields for similarity computation.
	 */
	private void init(Attribute[] attributes1, Attribute[] attributes2) {
		int length = attributes1.length;

		indexMappingSet1 = new HashMap<Integer, Map<Double, Double>>();
		indexMappingSet2 = new HashMap<Integer, Map<Double, Double>>();

		binominal = new boolean[length];
		falseIndexSet1 = new double[length];
		falseIndexSet2 = new double[length];

		for (int i = 0; i < length; i++) {
			Attribute attribute1 = attributes1[i];
			Attribute attribute2 = attributes2[i];

			boolean binominalAttr1 = attribute1.getValueType() == Ontology.BINOMINAL;
			boolean binominalAttr2 = attribute2.getValueType() == Ontology.BINOMINAL;
			binominal[i] = binominalAttr1 && binominalAttr2;

			if (binominal[i]) {
				falseIndexSet1[i] = attribute1.getMapping().getNegativeIndex();
				falseIndexSet2[i] = attribute2.getMapping().getNegativeIndex();

				String negativeStringAttr1 = attribute1.getMapping().getNegativeString();
				String negativeStringAttr2 = attribute2.getMapping().getNegativeString();

				String positiveStringAttr1 = attribute1.getMapping().getPositiveString();
				String positiveStringAttr2 = attribute2.getMapping().getPositiveString();

				// Testing if mappings are switched only if positive&negative mapping is complete
				if (negativeStringAttr1 != null && negativeStringAttr2 != null && positiveStringAttr1 != null
						&& positiveStringAttr2 != null) {
					// Do we have unequal strings for the binominal mapping? ...
					if (!(negativeStringAttr1.equals(negativeStringAttr2) && positiveStringAttr1.equals(positiveStringAttr2))) {
						// ... if not, are the strings switched?
						if (negativeStringAttr1.equals(positiveStringAttr2)
								&& negativeStringAttr2.equals(positiveStringAttr1)) {
							// ... if yes we will remap the false value of the second attribute to
							// the false value of the first attribute
							falseIndexSet2[i] = attribute2.getMapping().getPositiveIndex();
						} else {
							// ... otherwise we will threat them as polynominal
							binominal[i] = false;
							createCommonMapping(attribute1, attribute2, i);
							falseIndexSet1[i] = Double.NaN;
							falseIndexSet2[i] = Double.NaN;
						}
					}
				}
			} else {
				createCommonMapping(attribute1, attribute2, i);
				falseIndexSet1[i] = Double.NaN;
				falseIndexSet2[i] = Double.NaN;
			}
		}

		initiated = true;
	}

	private void createCommonMapping(Attribute attribute1, Attribute attribute2, int attributeIndex) {

		Map<Double, Double> indexMap1 = new HashMap<Double, Double>();
		Map<Double, Double> indexMap2 = new HashMap<Double, Double>();

		indexMappingSet1.put(attributeIndex, indexMap1);
		indexMappingSet2.put(attributeIndex, indexMap2);

		NominalMapping mappingAttr1 = attribute1.getMapping();
		NominalMapping mappingAttr2 = attribute2.getMapping();

		Set<String> values = new HashSet<String>(mappingAttr1.getValues());
		values.addAll(mappingAttr2.getValues());

		int index = 0;
		for (String value : values) {
			int valueIndexAttr1 = mappingAttr1.getIndex(value);
			int valueIndexAttr2 = mappingAttr2.getIndex(value);

			// Both attributes have a mapping for this value
			if (valueIndexAttr1 != -1 && valueIndexAttr2 != -1) {
				indexMap1.put((double) valueIndexAttr1, (double) index);
				indexMap2.put((double) valueIndexAttr2, (double) index);

				index++;
			}

		}
	}

}

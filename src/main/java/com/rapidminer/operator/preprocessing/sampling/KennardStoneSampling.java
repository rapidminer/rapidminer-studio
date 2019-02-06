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
package com.rapidminer.operator.preprocessing.sampling;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;


/**
 * This operator performs a Kennard-Stone Sampling. This sampling Algorithm works as follows: First
 * find the two points most separated in the training set. For each candidate point, find the
 * smallest distance to any object already selected. Select that point for the training set which
 * has the largest of these smallest distances As described above, this algorithm always gives the
 * same result, due to the two starting points which are always the same. This implementation
 * reduces number of iterations by holding a list with candidates of the largest smallest distances.
 * The parameters controll the number of examples in the sample
 * 
 * @author Sebastian Land
 */
public class KennardStoneSampling extends AbstractSamplingOperator {

	public static final String PARAMETER_SAMPLE = "sample";

	public static final String[] SAMPLE_MODES = { "absolute", "relative" };

	public static final int SAMPLE_ABSOLUTE = 0;

	public static final int SAMPLE_RELATIVE = 1;

	/** The parameter name for &quot;The fraction of examples which should be sampled&quot; */
	public static final String PARAMETER_SAMPLE_SIZE = "sample_size";

	/** The parameter name for &quot;This ratio determines the size of the new example set.&quot; */
	public static final String PARAMETER_SAMPLE_RATIO = "sample_ratio";

	private static class Candidate implements Comparable<Candidate> {

		private double[] attributeValues;
		private double distance;
		private int exampleIndex;

		public Candidate(double[] exampleValues, double distance, int exampleIndex) {
			attributeValues = exampleValues;
			this.distance = distance;
			this.exampleIndex = exampleIndex;
		}

		public double getDistance() {
			return distance;
		}

		public double[] getValues() {
			return attributeValues;
		}

		public int getExampleIndex() {
			return exampleIndex;
		}

		@Override
		public int compareTo(Candidate o) {
			return Double.compare(this.distance, o.getDistance());
		}
	}

	public KennardStoneSampling(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MDInteger getSampledSize(ExampleSetMetaData emd) throws UndefinedParameterError {
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				int absoluteNumber = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
				if (emd.getNumberOfExamples().isAtLeast(absoluteNumber) == MetaDataInfo.NO) {
					getExampleSetInputPort().addError(
							new SimpleMetaDataError(Severity.ERROR, getExampleSetInputPort(), Collections
									.singletonList(new ParameterSettingQuickFix(this, PARAMETER_SAMPLE_SIZE, emd
											.getNumberOfExamples().getValue().toString())), "need_more_examples",
									absoluteNumber + ""));
				}
				return new MDInteger(absoluteNumber);
			case SAMPLE_RELATIVE:
				MDInteger number = emd.getNumberOfExamples();
				number.multiply(getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
				return number;
			default:
				return new MDInteger();
		}
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// creating kernel and settings from Parameters
		int k = Math.min(100, exampleSet.getAttributes().size() * 2);
		int size = exampleSet.size();
		switch (getParameterAsInt(PARAMETER_SAMPLE)) {
			case SAMPLE_ABSOLUTE:
				size = getParameterAsInt(PARAMETER_SAMPLE_SIZE);
				break;
			case SAMPLE_RELATIVE:
				size = (int) Math.round(exampleSet.size() * getParameterAsDouble(PARAMETER_SAMPLE_RATIO));
				break;
		}

		DistanceMeasure distanceMeasure = new EuclideanDistance();
		distanceMeasure.init(exampleSet);

		// finding farthest and nearest example to mean Vector
		double[] meanVector = getMeanVector(exampleSet);
		Candidate min = new Candidate(meanVector, Double.POSITIVE_INFINITY, 0);
		Candidate max = new Candidate(meanVector, Double.NEGATIVE_INFINITY, 0);
		int i = 0;
		for (Example example : exampleSet) {
			this.checkForStop();
			double[] exampleValues = getExampleValues(example);
			Candidate current = new Candidate(exampleValues, Math.abs(distanceMeasure.calculateDistance(meanVector,
					exampleValues)), i);
			if (current.compareTo(min) < 0) {
				min = current;
			}
			if (current.compareTo(max) > 0) {
				max = current;
			}
			i++;
		}

		this.checkForStop();

		ArrayList<Candidate> recentlySelected = new ArrayList<>(10);
		int[] partition = new int[exampleSet.size()];
		int numberOfSelectedExamples = 2;
		recentlySelected.add(min);
		recentlySelected.add(max);
		partition[min.getExampleIndex()] = 1;
		partition[max.getExampleIndex()] = 1;
		double[] minimalDistances = new double[exampleSet.size()];
		Arrays.fill(minimalDistances, Double.POSITIVE_INFINITY);

		// running now through examples, checking for smallest distance to one of the candidates
		while (numberOfSelectedExamples < size) {
			TreeSet<Candidate> candidates = new TreeSet<>();

			this.checkForStop();
			i = 0;
			// check distance only for candidates recently selected.
			for (Example example : exampleSet) {
				// if example not has been selected allready
				if (partition[i] == 0) {
					double[] exampleValues = getExampleValues(example);
					for (Candidate candidate : recentlySelected) {
						minimalDistances[i] = Math.min(minimalDistances[i],
								Math.abs(distanceMeasure.calculateDistance(exampleValues, candidate.getValues())));
					}
					Candidate newCandidate = new Candidate(exampleValues, minimalDistances[i], i);
					candidates.add(newCandidate);
					if (candidates.size() > k) {
						Iterator<Candidate> iterator = candidates.iterator();
						iterator.next();
						iterator.remove();
					}
				}
				i++;
				this.checkForStop();
			}
			// clearing recently selected since now new ones will be selected
			recentlySelected.clear();

			// now running in descending order through candidates and adding to selected
			// IM: descendingIterator() is not available in Java versions less than 6 !!!
			Iterator<Candidate> descendingIterator = candidates.descendingIterator();
			while (descendingIterator.hasNext() && numberOfSelectedExamples < size) {
				Candidate candidate = descendingIterator.next();

				this.checkForStop();
				boolean existSmallerDistance = false;
				Iterator<Candidate> addedIterator = recentlySelected.iterator();
				// test if a distance to recently selected is smaller than previously calculated
				// minimal distance
				// if one exists: This is not selected
				while (addedIterator.hasNext()) {
					double distance = Math.abs(distanceMeasure.calculateDistance(addedIterator.next().getValues(),
							candidate.getValues()));
					existSmallerDistance = existSmallerDistance || distance < candidate.getDistance();
				}
				if (!existSmallerDistance) {
					recentlySelected.add(candidate);
					partition[candidate.getExampleIndex()] = 1;
					numberOfSelectedExamples++;
				} else {
					break;
				}

			}
		}

		// building new exampleSet containing only Examples with indices in selectedExamples

		SplittedExampleSet sample = new SplittedExampleSet(exampleSet, new Partition(partition, 2));
		sample.selectSingleSubset(1);
		return sample;
	}

	private double[] getMeanVector(ExampleSet exampleSet) {
		exampleSet.recalculateAllAttributeStatistics();
		Attributes attributes = exampleSet.getAttributes();
		double[] meanVector = new double[attributes.size()];
		int i = 0;
		for (Attribute attribute : attributes) {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
				meanVector[i] = exampleSet.getStatistics(attribute, Statistics.MINIMUM);
			} else if (attribute.isNominal()) {
				meanVector[i] = exampleSet.getStatistics(attribute, Statistics.MODE);
			} else {
				meanVector[i] = exampleSet.getStatistics(attribute, Statistics.AVERAGE);
			}
			i++;
		}
		return meanVector;
	}

	private double[] getExampleValues(Example example) {
		Attributes attributes = example.getAttributes();
		double[] attributeValues = new double[attributes.size()];

		int i = 0;
		for (Attribute attribute : attributes) {
			attributeValues[i] = example.getValue(attribute);
			i++;
		}
		return attributeValues;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new LinkedList<>();
		ParameterType type = new ParameterTypeCategory(PARAMETER_SAMPLE, "Determines how the amount of data is specified.",
				SAMPLE_MODES, SAMPLE_ABSOLUTE);
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeInt(PARAMETER_SAMPLE_SIZE, "The number of examples which should be sampled", 1,
				Integer.MAX_VALUE, 100);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_ABSOLUTE));
		type.setExpert(false);
		types.add(type);
		type = new ParameterTypeDouble(PARAMETER_SAMPLE_RATIO, "The fraction of examples which should be sampled", 0.0d,
				1.0d, 0.1d);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_SAMPLE, SAMPLE_MODES, true, SAMPLE_RELATIVE));
		type.setExpert(false);
		types.add(type);
		types.addAll(super.getParameterTypes());
		return types;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(),
				KennardStoneSampling.class, null);
	}
}

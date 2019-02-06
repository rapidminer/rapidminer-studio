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
package com.rapidminer.operator.preprocessing.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.SortedExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.preprocessing.AbstractDataProcessing;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttributes;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator sorts a given example set according to a subset of attributes and will sort pareto
 * dominated examples after non dominated.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class NonDominatedSorting extends AbstractDataProcessing {

	/**
	 * The parameter name for &quot;Indicates the attribute which should be used for determining the
	 * sorting.&quot;
	 */
	public static final String PARAMETER_ATTRIBUTES = "attributes";

	private static class SortingObject {

		private int originalIndex;

		private double[] values;

		public SortingObject(int originalIndex, double[] values) {
			this.originalIndex = originalIndex;
			this.values = values;
		}
	}

	public NonDominatedSorting(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		String attributesString = getParameterAsString(PARAMETER_ATTRIBUTES);
		Pattern pattern = Pattern.compile(attributesString);
		List<Attribute> attributeList = new LinkedList<Attribute>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (attribute.isNumerical()) {
				if (pattern.matcher(attribute.getName()).matches()) {
					attributeList.add(attribute);
				}
			}
		}
		Attribute[] attributes = new Attribute[attributeList.size()];
		attributeList.toArray(attributes);

		List<SortingObject> sortingObjects = new LinkedList<SortingObject>();
		int index = 0;
		for (Example example : exampleSet) {
			double[] values = new double[attributes.length];
			int a = 0;
			for (Attribute attribute : attributes) {
				values[a] = example.getValue(attribute);
				a++;
			}
			sortingObjects.add(new SortingObject(index, values));
			index++;
		}

		List<List<SortingObject>> ranks = new ArrayList<List<SortingObject>>();
		while (sortingObjects.size() > 0) {
			List<SortingObject> rank = getNextRank(sortingObjects);
			ranks.add(rank);
			Iterator<SortingObject> i = rank.iterator();
			while (i.hasNext()) {
				sortingObjects.remove(i.next());
			}
		}
		sortingObjects.clear();

		for (List<SortingObject> rank : ranks) {
			sortingObjects.addAll(rank);
		}

		int[] mapping = new int[sortingObjects.size()];
		index = 0;
		for (SortingObject sortingObject : sortingObjects) {
			mapping[index] = sortingObject.originalIndex;
			index++;
		}

		ExampleSet result = new SortedExampleSet(exampleSet, mapping);

		return result;
	}

	/** Returns a list of non-dominated individuals. */
	private List<SortingObject> getNextRank(List<SortingObject> population) {
		List<SortingObject> rank = new ArrayList<SortingObject>();
		for (int i = 0; i < population.size(); i++) {
			SortingObject current = population.get(i);
			rank.add(current);
			boolean delete = false;
			for (int j = rank.size() - 2; j >= 0; j--) {
				SortingObject ranked = rank.get(j);
				if (isDominated(ranked, current)) {
					rank.remove(ranked);
				}
				if (isDominated(current, ranked)) {
					delete = true;
					// break;
				}
			}
			if (delete) {
				rank.remove(current);
			}
		}
		return rank;
	}

	/**
	 * Returns true if the second performance vector is better in all fitness criteria than the
	 * first one (remember: the criteria should be maximized).
	 */
	private static boolean isDominated(SortingObject i1, SortingObject i2) {
		double[] pv1 = i1.values;
		double[] pv2 = i2.values;
		double[][] performances = new double[pv1.length][2];
		for (int p = 0; p < performances.length; p++) {
			performances[p][0] = pv1[p];
			performances[p][1] = pv2[p];
		}
		boolean dominated = true;
		for (int p = 0; p < performances.length; p++) {
			dominated &= (performances[p][1] >= performances[p][0]);
		}
		boolean oneActuallyBetter = false;
		for (int p = 0; p < performances.length; p++) {
			oneActuallyBetter |= (performances[p][1] > performances[p][0]);
		}
		dominated &= oneActuallyBetter;
		return dominated;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeAttributes(PARAMETER_ATTRIBUTES,
				"Defines the attributes which should be used for the sorting.", getExampleSetInputPort(), false);
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);
		return types;
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), NonDominatedSorting.class,
				null);
	}
}

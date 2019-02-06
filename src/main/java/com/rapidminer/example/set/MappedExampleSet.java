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
package com.rapidminer.example.set;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;


/**
 * <p>
 * This example set uses a mapping of indices to access the examples provided by the parent example
 * set. The mapping does not need to contain unique indices which is especially useful for sampling
 * with replacement. For performance reasons (iterations, database access...) the mapping will be
 * sorted during the construction of this example set (based on the parameter sort).
 * </p>
 * 
 * <p>
 * Please note that the constructor takes a boolean flag indicating if the examples from the given
 * array are used or if the examples which are not part of this mapping should be used. This might
 * be useful in the context of bootstrapped validation for example.
 * </p>
 * 
 * @author Ingo Mierswa, Martin Scholz
 */
public class MappedExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -488025806523583178L;

	/** The parent example set. */
	private ExampleSet parent;

	/** The used mapping. */
	private int[] mapping;

	/** Constructs an example set based on the given mapping. */
	public MappedExampleSet(ExampleSet parent, int[] mapping) {
		this(parent, mapping, true);
	}

	/**
	 * Constructs an example set based on the given mapping. If the boolean flag useMappedExamples
	 * is false only examples which are not part of the original mapping are used.
	 */
	public MappedExampleSet(ExampleSet parent, int[] mapping, boolean useMappedExamples) {
		this(parent, mapping, useMappedExamples, true);
	}

	/**
	 * Constructs an example set based on the given mapping. If the boolean flag useMappedExamples
	 * is false only examples which are not part of the original mapping are used. If the boolean
	 * flag sort is false the mapping is used as is.
	 */
	public MappedExampleSet(ExampleSet parent, int[] mapping, boolean useMappedExamples, boolean sort) {
		this.parent = (ExampleSet) parent.clone();
		this.mapping = mapping;
		if (sort) {
			Arrays.sort(this.mapping);
		}

		if (!useMappedExamples) {
			List<Integer> inverseIndexList = new ArrayList<Integer>();
			int currentExample = -1;
			for (int m : mapping) {
				if (m != currentExample) {
					for (int z = currentExample + 1; z < m; z++) {
						inverseIndexList.add(z);
					}
					currentExample = m;
				}
			}
			this.mapping = new int[inverseIndexList.size()];
			Iterator<Integer> i = inverseIndexList.iterator();
			int index = 0;
			while (i.hasNext()) {
				this.mapping[index++] = i.next();
			}
		}
	}

	/** Clone constructor. */
	public MappedExampleSet(MappedExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.mapping = exampleSet.mapping;
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof MappedExampleSet)) {
			return false;
		}

		MappedExampleSet other = (MappedExampleSet) o;
		if (this.mapping.length != other.mapping.length) {
			return false;
		}
		for (int i = 0; i < this.mapping.length; i++) {
			if (this.mapping[i] != other.mapping[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ Arrays.hashCode(this.mapping);
	}

	/** Returns a {@link MappedExampleReader}. */
	@Override
	public Iterator<Example> iterator() {
		return new MappedExampleReader(parent.iterator(), this.mapping);
	}

	/** Returns the i-th example in the mapping. */
	@Override
	public Example getExample(int index) {
		if ((index < 0) || (index >= this.mapping.length)) {
			throw new RuntimeException("Given index '" + index + "' does not fit the mapped ExampleSet!");
		} else {
			return parent.getExample(this.mapping[index]);
		}
	}

	/** Counts the number of examples. */
	@Override
	public int size() {
		return mapping.length;
	}

	@Override
	public Attributes getAttributes() {
		return parent.getAttributes();
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	/** Creates a new mapping for the given example set by sampling with replacement. */
	public static int[] createBootstrappingMapping(ExampleSet exampleSet, int size, Random random) {
		int[] mapping = new int[size];
		for (int i = 0; i < mapping.length; i++) {
			mapping[i] = random.nextInt(exampleSet.size());
		}
		return mapping;
	}

	public static int[] createWeightedBootstrappingMapping(ExampleSet exampleSet, int size, Random random) {
		Attribute weightAttribute = exampleSet.getAttributes().getSpecial(Attributes.WEIGHT_NAME);
		exampleSet.recalculateAttributeStatistics(weightAttribute);
		double maxWeight = exampleSet.getStatistics(weightAttribute, Statistics.MAXIMUM);

		int[] mapping = new int[size];
		for (int i = 0; i < mapping.length; i++) {
			int index = -1;
			do {
				index = random.nextInt(exampleSet.size());
				Example example = exampleSet.getExample(index);
				double currentWeight = example.getValue(weightAttribute);
				if (random.nextDouble() > currentWeight / maxWeight) {
					index = -1;
				}
			} while (index == -1);
			mapping[i] = index;
		}
		return mapping;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.operator.ResultObjectAdapter#getAnnotations()
	 */
	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}

	@Override
	public boolean isThreadSafeView() {
		return parent instanceof AbstractExampleSet && ((AbstractExampleSet) parent).isThreadSafeView();
	}

}

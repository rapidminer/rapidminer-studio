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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.AttributeTransformation;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.table.ExampleTable;
import com.rapidminer.operator.Annotations;


/**
 * An implementation of ExampleSet that allows the replacement of missing values on the fly. Missing
 * values will be replaced by the average of all other values or by the mean.
 *
 * @author Ingo Mierswa
 */
public class ReplaceMissingExampleSet extends AbstractExampleSet {

	private static final long serialVersionUID = -5662936146589379273L;

	/** Currently used attribute weights. Used also for example creation. */
	private Map<String, Double> replacementMap;

	/** The parent example set. */
	private ExampleSet parent;

	/**
	 * It is recommended to use {@link #create(ExampleSet, Map)} instead if no later access to the
	 * replacementMap is needed.
	 */
	public ReplaceMissingExampleSet(ExampleSet exampleSet) {
		this(exampleSet, null);
	}

	/**
	 * It is recommended to use {@link #create(ExampleSet, Map)} instead if no later access to the
	 * replacementMap is needed.
	 */
	public ReplaceMissingExampleSet(ExampleSet exampleSet, Map<String, Double> replacementMap) {
		this.parent = (ExampleSet) exampleSet.clone();
		if (replacementMap == null) {
			this.replacementMap = new HashMap<String, Double>();
			for (Attribute attribute : parent.getAttributes()) {
				addReplacement(attribute);
			}
		} else {
			this.replacementMap = replacementMap;
		}

		addReplacmentTransformations(this.parent, this.replacementMap);
	}

	/**
	 * Creates a new example set based on exampleSet where missing values are replaced as specified
	 * by replacementMap. If replacementMap is {@code null} then the mode (for nominal attributes)
	 * or the average (for numerical attributes) is taken as replacement.
	 *
	 * @param exampleSet
	 *            the exampleSet for which missing values should be replaced
	 * @param replacementMap
	 *            the map specifying the replacement
	 * @return an example set without missing values
	 * @since 7.5.1
	 */
	public static ExampleSet create(ExampleSet exampleSet, Map<String, Double> replacementMap) {
		ExampleSet newSet = (ExampleSet) exampleSet.clone();
		if (replacementMap == null) {
			replacementMap = new HashMap<String, Double>();
			for (Attribute attribute : newSet.getAttributes()) {
				addReplacement(newSet, attribute, replacementMap);
			}
		}
		addReplacmentTransformations(newSet, replacementMap);
		return newSet;
	}

	/**
	 * Adds a {@link AttributeTransformationReplaceMissing} for the given replacementMap to all
	 * attributes of the exampleSet.
	 */
	private static void addReplacmentTransformations(ExampleSet exampleSet, Map<String, Double> replacementMap) {
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute currentAttribute = role.getAttribute();
			currentAttribute.addTransformation(new AttributeTransformationReplaceMissing(replacementMap));
		}
	}

	/** Clone constructor. */
	public ReplaceMissingExampleSet(ReplaceMissingExampleSet exampleSet) {
		this.parent = (ExampleSet) exampleSet.parent.clone();
		this.replacementMap = new HashMap<String, Double>();
		for (String name : exampleSet.replacementMap.keySet()) {
			this.replacementMap.put(name, Double.valueOf(exampleSet.replacementMap.get(name).doubleValue()));
		}

		Iterator<AttributeRole> a = this.parent.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute currentAttribute = role.getAttribute();
			AttributeTransformation transformation = currentAttribute.getLastTransformation();
			if (transformation != null) {
				if (transformation instanceof AttributeTransformationReplaceMissing) {
					((AttributeTransformationReplaceMissing) transformation).setReplacementMap(this.replacementMap);
				}
			}
		}
	}

	public Map<String, Double> getReplacementMap() {
		return this.replacementMap;
	}

	public void addReplacement(Attribute attribute) {
		addReplacement(this, attribute, replacementMap);
	}

	/**
	 * Adds a mapping from the name of the given attribute to its mode (for nominal attributes) or
	 * its average (for numerical attributes) to the replacementMap.
	 */
	private static void addReplacement(ExampleSet exampleSet, Attribute attribute, Map<String, Double> replacementMap) {
		exampleSet.recalculateAttributeStatistics(attribute);
		if (attribute.isNominal()) {
			replacementMap.put(attribute.getName(), exampleSet.getStatistics(attribute, Statistics.MODE));
		} else {
			replacementMap.put(attribute.getName(), exampleSet.getStatistics(attribute, Statistics.AVERAGE));
		}
	}

	@Override
	public Attributes getAttributes() {
		return this.parent.getAttributes();
	}

	@Override
	public Annotations getAnnotations() {
		return parent.getAnnotations();
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		if (!(o instanceof ReplaceMissingExampleSet)) {
			return false;
		}
		boolean result = super.equals(o);
		if (result) {
			Map<String, Double> otherMap = ((ReplaceMissingExampleSet) o).replacementMap;
			if (this.replacementMap.size() != otherMap.size()) {
				return false;
			}
			for (String name : this.replacementMap.keySet()) {
				if (!this.replacementMap.get(name).equals(otherMap.get(name))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ replacementMap.hashCode();
	}

	/**
	 * Creates a new example set reader.
	 */
	@Override
	public Iterator<Example> iterator() {
		return new AttributesExampleReader(parent.iterator(), this);
	}

	@Override
	public Example getExample(int index) {
		return this.parent.getExample(index);
	}

	@Override
	public ExampleTable getExampleTable() {
		return parent.getExampleTable();
	}

	@Override
	public int size() {
		return parent.size();
	}

	@Override
	public void cleanup() {
		parent.cleanup();
	}
}

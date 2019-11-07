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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.rapidminer.example.set.AttributeWeightedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.generator.FeatureGenerator;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.preprocessing.IdTagging;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.sampling.OrderedSamplingWithoutReplacement;


/**
 * Provides some tools for calculation of certain measures and feature generation.
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class Tools {

	/**
	 * Thrown as a {@link UserError#getErrorIdentifier()} by {@link #onlyFiniteValues}
	 * @since 9.5.0
	 */
	public static String INFINITE_VALUES = "infinite_values";

	// ================================================================================
	// -------------------- Tools --------------------
	// ================================================================================

	public static String[] getAllAttributeNames(ExampleSet exampleSet) {
		String[] attributeNames = new String[exampleSet.getAttributes().allSize()];
		int counter = 0;
		Iterator<Attribute> a = exampleSet.getAttributes().allAttributes();
		while (a.hasNext()) {
			Attribute attribute = a.next();
			attributeNames[counter++] = attribute.getName();
		}
		return attributeNames;
	}

	public static String[] getRegularAttributeNames(ExampleSet exampleSet) {
		String[] attributeNames = new String[exampleSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeNames[counter++] = attribute.getName();
		}
		return attributeNames;
	}

	public static String[] getRegularAttributeConstructions(ExampleSet exampleSet) {
		String[] attributeNames = new String[exampleSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			// is generated
			if (!attribute.getConstruction().equals(attribute.getName()) && !attribute.getConstruction().startsWith("gens")) {
				attributeNames[counter++] = attribute.getConstruction();
			} else {
				attributeNames[counter++] = attribute.getName();
			}
		}
		return attributeNames;
	}

	// ================================================================================
	// -------------------- GENERATION --------------------
	// ================================================================================

	public static Attribute[] createRegularAttributeArray(ExampleSet exampleSet) {
		Attribute[] attributes = new Attribute[exampleSet.getAttributes().size()];
		int counter = 0;
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributes[counter++] = attribute;
		}
		return attributes;
	}

	public static Attribute[] getRandomCompatibleAttributes(ExampleSet exampleSet, FeatureGenerator generator,
			String[] functions, Random random) {
		List<Attribute[]> inputAttributes = generator.getInputCandidates(exampleSet, functions);
		if (inputAttributes.size() > 0) {
			return inputAttributes.get(random.nextInt(inputAttributes.size()));
		} else {
			return null;
		}
	}

	public static Attribute[] getWeightedCompatibleAttributes(AttributeWeightedExampleSet exampleSet,
			FeatureGenerator generator, String[] functions, RandomGenerator random) {
		List<Attribute[]> inputAttributes = generator.getInputCandidates(exampleSet, functions);
		double[] probs = new double[inputAttributes.size()];
		double probSum = 0.0d;
		Iterator<Attribute[]> i = inputAttributes.iterator();
		int k = 0;
		while (i.hasNext()) {
			Attribute[] candidate = i.next();
			for (int j = 0; j < candidate.length; j++) {
				double weight = exampleSet.getWeight(candidate[j]);
				probSum += weight;
				probs[k] = weight;
			}
		}
		for (int j = 0; j < probs.length; j++) {
			probs[j] /= probSum;
		}
		return inputAttributes.get(random.randomIndex(probs));
	}

	/**
	 * Creates and adds the new attributes to the given example set
	 */
	public static Attribute createSpecialAttribute(ExampleSet exampleSet, String name, int valueType) {
		Attribute attribute = AttributeFactory.createAttribute(name, valueType);
		exampleSet.getExampleTable().addAttribute(attribute);
		exampleSet.getAttributes().setSpecialAttribute(attribute, name);
		return attribute;
	}

	public static AttributeMetaData createWeightAttributeMetaData(ExampleSetMetaData emd) {
		return new AttributeMetaData(Attributes.WEIGHT_NAME, Ontology.REAL, Attributes.WEIGHT_NAME);
	}

	/**
	 * This method adds a new Weight Attribute initialized with 1.0d for each example to the example
	 * table as well as to the given ExampleSet.
	 */
	public static Attribute createWeightAttribute(ExampleSet exampleSet) {
		Attribute weight = exampleSet.getAttributes().getWeight();
		if (weight != null) {
			exampleSet.getLog().logWarning("ExampleSet.createWeightAttribute(): Overwriting old weight attribute!");
		}

		weight = AttributeFactory.createAttribute(Attributes.WEIGHT_NAME, Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(weight);
		exampleSet.getAttributes().setWeight(weight);

		for (Example example : exampleSet) {
			example.setValue(weight, 1d);
		}
		return weight;
	}

	public static boolean containsValueType(ExampleSet exampleSet, int valueType) {
		for (Attribute attribute : exampleSet.getAttributes()) {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), valueType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Replaces the given real value by the new one. Please note that this method will only work for
	 * nominal attributes.
	 */
	public static void replaceValue(Attribute attribute, String oldValue, String newValue) {
		if (!attribute.isNominal()) {
			throw new RuntimeException("Example-Tools: replaceValue is only supported for nominal attributes.");
		}
		NominalMapping mapping = attribute.getMapping();
		int oldIndex = mapping.getIndex(oldValue);
		if (oldIndex < 0) {
			throw new RuntimeException(
					"Example-Tools: replaceValue cannot be performed since old value was not defined in the attribute.");
		}
		mapping.setMapping(newValue, oldIndex);
	}

	/**
	 * Replaces the given value by the new one. This method will only work for nominal attributes.
	 */
	public static void replaceValue(ExampleSet exampleSet, Attribute attribute, String oldValue, String newValue) {
		if (!attribute.isNominal()) {
			throw new RuntimeException("Example-Tools: replaceValue is only supported for nominal attributes.");
		}
		NominalMapping mapping = attribute.getMapping();
		if (oldValue.equals("?")) {
			for (Example example : exampleSet) {
				if (Double.isNaN(example.getValue(attribute))) {
					example.setValue(attribute, mapping.mapString(newValue));
				}
			}
		} else {
			int oldIndex = mapping.getIndex(oldValue);
			if (oldIndex < 0) {
				throw new RuntimeException(
						"Example-Tools: replaceValue cannot be performed since old value was not defined in the attribute.");
			}
			if (newValue.equals("?")) {
				for (Example example : exampleSet) {
					int index = mapping.getIndex(example.getValueAsString(attribute));
					if (index == oldIndex) {
						example.setValue(attribute, Double.NaN);
					}
				}
				return;
			}
			int newIndex = mapping.getIndex(newValue);
			if (newIndex >= 0) {
				for (Example example : exampleSet) {
					int index = mapping.getIndex(example.getValueAsString(attribute));
					if (index == oldIndex) {
						example.setValue(attribute, newIndex);
					}
				}
			} else {
				mapping.setMapping(newValue, oldIndex);
			}
		}
	}

	/**
	 * Replaces the given real value by the new one. Please note that this method will only properly
	 * work for numerical attributes since for nominal attributes no remapping is performed. Please
	 * note also that this method performs a data scan.
	 */
	public static void replaceValue(ExampleSet exampleSet, Attribute attribute, double oldValue, double newValue) {
		for (Example example : exampleSet) {
			double value = example.getValue(attribute);
			if (Double.isNaN(oldValue) && Double.isNaN(value)) {
				example.setValue(attribute, newValue);
				continue;
			}
			if (com.rapidminer.tools.Tools.isEqual(value, oldValue)) {
				example.setValue(attribute, newValue);
			}
		}
	}

	/**
	 * Returns true if value and block types of the first attribute are subtypes of value and block
	 * type of the second.
	 */
	public static boolean compatible(Attribute first, Attribute second) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(first.getValueType(), second.getValueType())
				&& Ontology.ATTRIBUTE_BLOCK_TYPE.isA(first.getBlockType(), second.getBlockType());
	}

	// ================================================================================
	// P r o b a b i l t i e s
	// ================================================================================

	public static double getAverageWeight(AttributeWeightedExampleSet exampleSet) {
		int counter = 0;
		double weightSum = 0.0d;
		for (Attribute attribute : exampleSet.getAttributes()) {
			double weight = exampleSet.getWeight(attribute);
			if (!Double.isNaN(weight)) {
				weightSum += Math.abs(weight);
				counter++;
			}
		}
		return weightSum / counter;
	}

	public static double[] getProbabilitiesFromWeights(Attribute[] attributes, AttributeWeightedExampleSet exampleSet) {
		return getProbabilitiesFromWeights(attributes, exampleSet, false);
	}

	public static double[] getInverseProbabilitiesFromWeights(Attribute[] attributes, AttributeWeightedExampleSet exampleSet) {
		return getProbabilitiesFromWeights(attributes, exampleSet, true);
	}

	/**
	 * Calculates probabilities for attribute selection purposes based on the given weight.
	 * Attributes whose weight is not defined in the weight vector get a probability corresponding
	 * to the average weight. Inverse probabilities can be calculated for cases where attributes
	 * with a high weight should be selected with small probability.
	 */
	public static double[] getProbabilitiesFromWeights(Attribute[] attributes, AttributeWeightedExampleSet exampleSet,
			boolean inverse) {
		double weightSum = 0.0d;
		int counter = 0;
		for (int i = 0; i < attributes.length; i++) {
			double weight = exampleSet.getWeight(attributes[i]);
			if (!Double.isNaN(weight)) {
				weightSum += Math.abs(weight);
				counter++;
			}
		}
		double weightAverage = weightSum / counter;
		weightSum += (attributes.length - counter) * weightAverage;

		double[] probs = new double[attributes.length];
		for (int i = 0; i < probs.length; i++) {
			double weight = exampleSet.getWeight(attributes[i]);
			if (Double.isNaN(weight)) {
				probs[i] = weightAverage / weightSum;
			} else {
				probs[i] = inverse ? (2 * weightAverage - Math.abs(weight)) / weightSum : Math.abs(weight) / weightSum;
			}
		}
		return probs;
	}

	public static Attribute selectAttribute(Attribute[] attributes, double[] probs, Random random) {
		double r = random.nextDouble();
		double sum = 0.0d;
		for (int i = 0; i < attributes.length; i++) {
			sum += probs[i];
			if (r < sum) {
				return attributes[i];
			}
		}
		return attributes[attributes.length - 1];
	}

	public static boolean isDefault(double defaultValue, double value) {
		if (Double.isNaN(defaultValue)) {
			return Double.isNaN(value);
		}
		/*
		 * Don't use infinity. if (Double.isInfinite(defaultValue)) return Double.isInfinite(value);
		 */
		return defaultValue == value;
	}

	/**
	 * The data set is not allowed to contain missing values. If it does, a {@link UserError} is
	 * thrown.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param task
	 *            will be shown as the origin of the error
	 **/
	@Deprecated
	public static void onlyNonMissingValues(ExampleSet exampleSet, String task) throws OperatorException {
		onlyNonMissingValues(exampleSet, task, null, new String[] {});
	}

	/**
	 * The data set is not allowed to contain missing values. If it does, a {@link UserError} is
	 * thrown.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 **/
	public static void onlyNonMissingValues(ExampleSet exampleSet, String task, Operator operator) throws OperatorException {
		List<String> specialAttList = new LinkedList<>();
		Iterator<AttributeRole> specialAttributes = exampleSet.getAttributes().specialAttributes();
		while (specialAttributes.hasNext()) {
			specialAttList.add(specialAttributes.next().getSpecialName());
		}
		onlyNonMissingValues(exampleSet, task, operator, specialAttList.toArray(new String[specialAttList.size()]));
	}

	/**
	 * The data set is not allowed to contain missing values. If it does, a {@link UserError} is
	 * thrown. Special attributes will be ignored, except they are explicitly listed at
	 * specialAttributes. Furthermore, if a specified special attribute does not exist a
	 * {@link UserError} is also thrown!
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 * @param specialAttributes
	 *            contains the special attributes which have to be checked. If a listed attribute
	 *            does not exist or contains missing values, a {@link UserError} is thrown
	 **/
	public static void onlyNonMissingValues(ExampleSet exampleSet, String task, Operator operator,
			String... specialAttributes) throws OperatorException {
		HashSet<Attribute> specialToCheck = new HashSet<>();

		for (int i = 0; i < specialAttributes.length; i++) {
			Attribute att = exampleSet.getAttributes().getSpecial(specialAttributes[i]);
			if (att != null) {
				specialToCheck.add(att);
			} else {
				throw new UserError(operator, "113", specialAttributes[i]);
			}
		}
		
		Iterator<AttributeRole> allRoles = exampleSet.getAttributes().allAttributeRoles();
		while (allRoles.hasNext()) {
			if (operator != null) {
				operator.checkForStop();
			}
			AttributeRole role = allRoles.next();
			if (!role.isSpecial() || specialToCheck.contains(role.getAttribute())) {
				Attribute attribute = role.getAttribute();
				for (Example example : exampleSet) {
					if (Double.isNaN(example.getValue(attribute))) {
						throw new UserError(operator, 139, task);
					}
				}
			}
		}
	}

	/**
	 * The data set is not allowed to contain non-finite values. If it does, a {@link UserError} is
	 * thrown.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 * @throws UserError with {@link UserError#getErrorIdentifier()} {@link #INFINITE_VALUES} in case of infinite values
	 * @since 7.6
	 **/
	public static void onlyFiniteValues(ExampleSet exampleSet, String task, Operator operator)
			throws ProcessStoppedException, UserError {
		onlyFiniteValues(exampleSet, false, task, operator);
	}

	/**
	 * The data set is not allowed to contain infinite and, if indicated, missing values. If it
	 * does, a {@link UserError} is thrown.
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param allowMissing
	 *            indicates whether {@link Double#NaN} should be ignored
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 * @throws UserError with {@link UserError#getErrorIdentifier()} {@link #INFINITE_VALUES} in case of infinite values
	 * @since 7.6
	 **/
	public static void onlyFiniteValues(ExampleSet exampleSet, boolean allowMissing, String task, Operator operator)
			throws ProcessStoppedException, UserError {
		List<String> specialAttList = new LinkedList<>();
		Iterator<AttributeRole> specialAttributes = exampleSet.getAttributes().specialAttributes();
		while (specialAttributes.hasNext()) {
			specialAttList.add(specialAttributes.next().getSpecialName());
		}
		onlyFiniteValues(exampleSet, task, operator, specialAttList.toArray(new String[specialAttList.size()]));
	}

	/**
	 * The data set is not allowed to contain non-finite values. If it does, a {@link UserError} is
	 * thrown. Special attributes will be ignored, except they are explicitly listed at
	 * specialAttributes. Furthermore, if a specified special attribute does not exist a
	 * {@link UserError} is also thrown!
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 * @param specialAttributes
	 *            contains the special attributes which have to be checked. If a listed attribute
	 *            does not exist or contains non-finite values, a {@link UserError} is thrown
	 * @throws UserError with {@link UserError#getErrorIdentifier()} {@link #INFINITE_VALUES} in case of infinite values
	 * @since 7.6
	 **/
	public static void onlyFiniteValues(ExampleSet exampleSet, String task, Operator operator, String... specialAttributes)
			throws ProcessStoppedException, UserError {
		onlyFiniteValues(exampleSet, false, task, operator, specialAttributes);
	}

	/**
	 * The data set is not allowed to contain infinite and, if indicated, missing values. If it
	 * does, a {@link UserError} is thrown. Special attributes will be ignored, except they are
	 * explicitly listed at specialAttributes. Furthermore, if a specified special attribute does
	 * not exist a {@link UserError} is also thrown!
	 *
	 * @param exampleSet
	 *            the {@link ExampleSet} to check
	 * @param allowMissing
	 *            indicates whether {@link Double#NaN} should be ignored
	 * @param task
	 *            will be shown as the origin of the error
	 * @param operator
	 *            the offending operator. Can be <code>null</code>
	 * @param specialAttributes
	 *            contains the special attributes which have to be checked. If a listed attribute
	 *            does not exist or contains not-allowed values, a {@link UserError} is thrown
	 * @throws UserError with {@link UserError#getErrorIdentifier()} {@link #INFINITE_VALUES} in case of infinite values
 	 * @since 7.6
	 **/
	public static void onlyFiniteValues(ExampleSet exampleSet, boolean allowMissing, String task, Operator operator,
			String... specialAttributes) throws ProcessStoppedException, UserError {
		HashSet<Attribute> specialToCheck = new HashSet<>();

		for (int i = 0; i < specialAttributes.length; i++) {
			Attribute att = exampleSet.getAttributes().getSpecial(specialAttributes[i]);
			if (att != null) {
				specialToCheck.add(att);
			} else {
				throw new UserError(operator, "113", specialAttributes[i]);
			}
		}

		Iterator<AttributeRole> allRoles = exampleSet.getAttributes().allAttributeRoles();
		while (allRoles.hasNext()) {
			if (operator != null) {
				operator.checkForStop();
			}
			AttributeRole role = allRoles.next();
			if (!role.isSpecial() || specialToCheck.contains(role.getAttribute())) {
				Attribute attribute = role.getAttribute();
				for (Example example : exampleSet) {
					double value = example.getValue(attribute);
					if (Double.isInfinite(value)) {
						throw new UserError(operator, INFINITE_VALUES, task);
					}
					if (!allowMissing && Double.isNaN(value)) {
						throw new UserError(operator, 139, task);
					}
				}
			}
		}
	}

	/**
	 * The attributes all have to be numerical.
	 *
	 * @param es
	 *            the example set
	 * @throws UserError
	 */
	public static void onlyNumericalAttributes(ExampleSet es, String task) throws UserError {
		onlyNumericalAttributes(es.getAttributes(), task);
	}

	/**
	 * The attributes all have to be numerical.
	 *
	 * @param attributes
	 *            the attributes
	 * @param task task name used for the error message
	 * @throws UserError
	 */
	public static void onlyNumericalAttributes(Attributes attributes, String task) throws UserError {
		for (Attribute attribute : attributes) {
			if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NUMERICAL)) {
				throw new UserError(null, 104, task, attribute.getName());
			}
		}
	}

	/**
	 * The attributes all have to be nominal or binary.
	 *
	 * @param es
	 *            the example set
	 * @param task task name used for the error message
	 * @throws UserError
	 */
	public static void onlyNominalAttributes(ExampleSet es, String task) throws UserError {
		onlyNominalAttributes(es.getAttributes(), task);
	}

	/**
	 * The attributes all have to be nominal or binary.
	 *
	 * @param attributes
	 *            the attributes
	 * @param task task name used for the error message
	 * @throws UserError
	 */
	public static void onlyNominalAttributes(Attributes attributes, String task) throws UserError {
		for (Attribute attribute : attributes) {
			if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL)) {
				throw new UserError(null, 103, task, attribute.getName());
			}
		}
	}

	/**
	 * The attributes all have to be nominal and have a maximum of two values or binominal.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param task task name used for the error message
	 * @throws UserError
	 *
	 */
	public static void maximumTwoNominalAttributes(ExampleSet exampleSet, String task) throws UserError {
		for (Attribute attribute : exampleSet.getAttributes()) {
			int valueType = attribute.getValueType();
			boolean throwError = false;
			if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, Ontology.NOMINAL)) {
				throwError = true;
			}
			if (valueType == Ontology.NOMINAL) {
				if (attribute.getMapping().size() > 2) {
					throwError = true;
				}
			}
			if (throwError) {
				throw new UserError(null, 114, task, attribute.getName());
			}
		}
	}

	/**
	 * The example set has to contain labels.
	 *
	 * @param es
	 *            the example set
	 * @throws UserError
	 */
	public static void isLabelled(ExampleSet es) throws UserError {
		if (es.getAttributes().getLabel() == null) {
			throw new UserError(null, 105);
		}
	}

	/**
	 * The example set has to be tagged with ids.
	 *
	 * @param es
	 *            the example set
	 * @throws UserError
	 */
	public static void isIdTagged(ExampleSet es) throws UserError {
		if (es.getAttributes().getId() == null) {
			throw new UserError(null, 129);
		}
	}

	/**
	 * The example set has to have ids. If no id attribute is available, it will be automatically
	 * created with help of the IDTagging operator.
	 *
	 * @param es
	 *            the example set
	 * @throws OperatorException
	 */
	public static void checkAndCreateIds(ExampleSet es) throws OperatorException {
		if (es.getAttributes().getId() == null) {
			try {
				// create ids (and visualization)
				IdTagging idTagging = OperatorService.createOperator(IdTagging.class);
				idTagging.apply(es);
			} catch (OperatorCreationException e) {
				throw new UserError(null, 129);
			}
		}
	}

	public static void checkIds(ExampleSet exampleSet) throws UserError {
		if (exampleSet.getAttributes().getId() == null) {
			throw new UserError(null, 129);
		}
	}

	/**
	 * The example set has to have nominal labels.
	 *
	 * @param es
	 *            the example set
	 * @see #hasNominalLabels(ExampleSet, String) hasNominalLabels(ExampleSet, "clustering")
	 * @throws UserError
	 */
	public static void hasNominalLabels(ExampleSet es) throws UserError {
		hasNominalLabels(es, "clustering");
	}

	/**
	 * The example set has to have nominal labels. Generalized form allows for better use with other
	 * algorithms than clustering.
	 *
	 * @param es
	 *            the example set
	 * @param algorithm
	 *            the name of the algorithm
	 * @throws UserError
	 * @since 7.6
	 * @see #hasNominalLabels(ExampleSet)
	 */
	public static void hasNominalLabels(ExampleSet es, String algorithm) throws UserError {
		isLabelled(es);
		Attribute a = es.getAttributes().getLabel();
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(a.getValueType(), Ontology.NOMINAL)) {
			throw new UserError(null, 101, algorithm, a.getName());
		}
	}

	/**
	 * The example set has to contain at least one example.
	 *
	 * @param es
	 *            the example set
	 * @throws UserError
	 */
	public static void isNonEmpty(ExampleSet es) throws UserError {
		if (es.size() == 0) {
			throw new UserError(null, 117);
		}
	}

	/**
	 * The example set has to contain at least one regular attribute.
	 *
	 * @param es
	 *            the example set
	 * @throws UserError
	 * @since 7.6
	 */
	public static void hasRegularAttributes(ExampleSet es) throws UserError {
		if (es.getAttributes().size() == 0) {
			throw new UserError(null, 106);
		}
	}

	/**
	 * Returns a new example set based on a fresh memory example table sampled from the given set.
	 */
	public static ExampleSet getLinearSubsetCopy(ExampleSet exampleSet, int size, int offset) {
		Map<Attribute, String> specialMap = new LinkedHashMap<>();
		List<Attribute> attributes = new LinkedList<>();
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute clone = (Attribute) role.getAttribute().clone();
			attributes.add(clone);
			if (role.isSpecial()) {
				specialMap.put(clone, role.getSpecialName());
			}
		}

		ExampleSetBuilder builder = ExampleSets.from(attributes);
		int maxSize = exampleSet.size();
		for (int i = offset; i < offset + size && i < maxSize; i++) {
			Example example = exampleSet.getExample(i);
			Iterator<Attribute> allI = exampleSet.getAttributes().allAttributes();
			int counter = 0;
			double[] dataRow = new double[attributes.size()];
			while (allI.hasNext()) {
				dataRow[counter++] = example.getValue(allI.next());
			}
			builder.addDataRow(new DoubleArrayDataRow(dataRow));
		}

		return builder.withRoles(specialMap).build();
	}

	/**
	 * Returns a new example set based on a fresh memory example table sampled from the given set.
	 */
	public static ExampleSet getShuffledSubsetCopy(ExampleSet exampleSet, int size, RandomGenerator randomGenerator) {
		int[] selectedIndices = OrderedSamplingWithoutReplacement.getSampledIndices(randomGenerator, exampleSet.size(),
				size);
		Map<Attribute, String> specialMap = new LinkedHashMap<>();
		List<Attribute> attributes = new LinkedList<>();
		Iterator<AttributeRole> a = exampleSet.getAttributes().allAttributeRoles();
		while (a.hasNext()) {
			AttributeRole role = a.next();
			Attribute clone = (Attribute) role.getAttribute().clone();
			attributes.add(clone);
			if (role.isSpecial()) {
				specialMap.put(clone, role.getSpecialName());
			}
		}

		ExampleSetBuilder builder = ExampleSets.from(attributes).withExpectedSize(selectedIndices.length);

		for (int i = 0; i < selectedIndices.length; i++) {
			Example example = exampleSet.getExample(selectedIndices[i]);
			Iterator<Attribute> allI = exampleSet.getAttributes().allAttributes();
			int counter = 0;
			double[] dataRow = new double[attributes.size()];
			while (allI.hasNext()) {
				dataRow[counter++] = example.getValue(allI.next());
			}
			builder.addRow(dataRow);
		}

		return builder.withRoles(specialMap).build();
	}

	/**
	 * Check if the childMapping is a subset of the superMapping or equal to it.
	 *
	 * @param childMapping
	 *            the potential subset you want to check
	 * @param superMapping
	 *            the {@link NominalMapping} you want to check against
	 * @return will return true if the {@link NominalMapping} is a subset or equal else false will
	 *         be returned
	 */
	public static boolean isNominalMappingSubsetOrEqualTo(NominalMapping childMapping, NominalMapping superMapping) {
		if (childMapping.size() > superMapping.size()) {
			return false;
		}
		List<String> superList = superMapping.getValues();
		for (String value : childMapping.getValues()) {
			if (!superList.contains(value)) {
				return false;
			}
		}
		return true;
	}
}

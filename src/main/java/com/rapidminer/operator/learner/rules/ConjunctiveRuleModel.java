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
package com.rapidminer.operator.learner.rules;

import java.util.Collection;
import java.util.Vector;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.SimplePredictionModel;
import com.rapidminer.tools.Tools;


/**
 * Each object of this class represents a conjunctive rule with boolean target and nominal
 * attributes. It cannot be changed after construction. This simplifies applications that maintain
 * counts separately. Each attribute may be tested at most once. For two rules it can be tested
 * whether one subsumes the other. A method for refinement allows to create each rule just once.
 *
 * This model may be used to query for the prediction of a single example, as well as to predict
 * complete ExampleSets.
 *
 * @author Martin Scholz Exp $
 */
public class ConjunctiveRuleModel extends SimplePredictionModel {

	private static final long serialVersionUID = 9088700646188411002L;

	/**
	 * Helper class for maintaining attribute-value tests. Objects cannot be changed after
	 * construction.
	 */
	private static class Literal {

		private final Attribute myAttribute;

		private final double myValue;

		public Literal(Attribute attribute, double testedValue) {
			this.myAttribute = attribute;
			this.myValue = testedValue;
		}

		public Attribute getAttribute() {
			return this.myAttribute;
		}

		public double getValue() {
			return this.myValue;
		}

		public boolean testExample(Example example) {
			return example.getValue(this.getAttribute()) == this.getValue();
		}
	}

	// ------------------------------------

	// the label to be predicted if all literals evaluate to true
	private final int predictedLabel;

	// the literals (body) of the rule to be combined conjunctively
	private final Vector<Literal> myLiterals = new Vector<>();

	/**
	 * Constructor to create an empty rule that makes a default prediction
	 *
	 * @param exampleSet
	 *            the example set used for training
	 * @param predictedLabel
	 *            specifies the head of the rule, i.e. which label to predict
	 */
	public ConjunctiveRuleModel(ExampleSet exampleSet, int predictedLabel) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.predictedLabel = predictedLabel;
	}

	/**
	 * Constructor to create an empty rule that makes a default prediction
	 *
	 * @param exampleSet
	 *            the example set used for training
	 * @param predictedLabel
	 *            specifies the head of the rule, i.e. which label to predict
	 */
	public ConjunctiveRuleModel(ExampleSet exampleSet, int predictedLabel, int positives, int negatives) {
		super(exampleSet, ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.predictedLabel = predictedLabel;
	}

	/** Constructor to clone a rule, but to change the head (prediction) */
	public ConjunctiveRuleModel(ConjunctiveRuleModel ruleToClone, int predictedLabel) {
		super(ruleToClone.getTrainingHeader(), ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.predictedLabel = predictedLabel;
		this.myLiterals.addAll(ruleToClone.myLiterals);
	}

	/** Constructor to create an empty rule that makes a default prediction */
	public ConjunctiveRuleModel(ConjunctiveRuleModel ruleToExtend, Attribute attribute, double testValue)
			throws OperatorException {
		super(ruleToExtend.getTrainingHeader(), ExampleSetUtilities.SetsCompareOption.EQUAL,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);

		if (ruleToExtend.getPositionOfAttributeInRule(attribute) != -1) {
			throw new OperatorException("ConjunctiveRuleModels may not contain the same attribute twice!");
		}

		this.predictedLabel = ruleToExtend.predictedLabel;
		this.myLiterals.addAll(ruleToExtend.myLiterals);
		Literal literalToAdd = new Literal(attribute, testValue);
		this.myLiterals.add(literalToAdd);
	}

	/** @return a <code>String</code> representation of this rule model. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString() + Tools.getLineSeparator() + " ("
				+ this.getLabel().getName() + "=" + this.getLabel().getMapping().mapIndex(this.predictedLabel) + ") <-- ");
		for (int i = 0; i < this.getRuleLength(); i++) {
			Attribute att = this.getAttributeOfLiteral(i);
			String val = att.getMapping().mapIndex((int) this.getTestedValueAtLiteral(i));
			result.append((i > 0 ? ", " : "") + "(" + att.getName() + "=" + val + ")");
		}
		return result.toString();
	}

	/**
	 * Works only for boolean labels and needs to be improved in case of changing RapidMiner core
	 * classes.
	 */
	private double flipLabel(double nonPredictedLabel) {
		return 1.0d - nonPredictedLabel;
	}

	@Override
	public double predict(Example example) throws OperatorException {
		for (Literal literal : this.myLiterals) {
			if (literal.testExample(example) == false) {
				return this.flipLabel(this.predictedLabel);
			}
		}
		return this.predictedLabel;
	}

	/** @return the number of literals */
	public int getRuleLength() {
		return this.myLiterals.size();
	}

	/** @return the label this rule predicts */
	public int getConclusion() {
		return this.predictedLabel;
	}

	/**
	 * @param literalNumber
	 *            the number of the literal in the rule
	 * @return the attribute tested in the specified literal
	 */
	public Attribute getAttributeOfLiteral(int literalNumber) {
		return this.myLiterals.get(literalNumber).getAttribute();
	}

	/**
	 * @param literalNumber
	 *            the number of the literal in the rule
	 * @return the value an attribute needs to have in order to pass the test of the specified
	 *         literal
	 */
	public double getTestedValueAtLiteral(int literalNumber) {
		return this.myLiterals.get(literalNumber).getValue();
	}

	/**
	 * @param attribute
	 *            to look for in the conjunctive rule
	 * @return the position (which is unique) of the attribute in the (ordered) conjunctive rule, or
	 *         -1, if the attribute has not been found
	 */
	public int getPositionOfAttributeInRule(Attribute attribute) {
		int ruleLength = this.getRuleLength();

		int index = 0;
		while (index < ruleLength && !attribute.equals(this.getAttributeOfLiteral(index))) {
			index++;
		}

		return index == ruleLength ? -1 : index;
	}

	/**
	 * @param model
	 *            another ConjuctiveRuleModel
	 * @return true, if this rule is a refinement of the specified rule, or if both rules are equal.
	 *         A rule refines another one, if it conatains all of its lietrals and predicts the same
	 *         label.
	 */
	public boolean isRefinementOf(ConjunctiveRuleModel model) {
		if (this == model) {
			return true;
		}

		int numLiterals = model.getRuleLength();

		if (this.getRuleLength() < numLiterals || this.getConclusion() != model.getConclusion()) {
			return false;
		}

		for (int i = 0; i < numLiterals; i++) {
			Attribute attribute = model.getAttributeOfLiteral(i);
			int pos;
			if ((pos = this.getPositionOfAttributeInRule(attribute)) == -1) {
				return false;
			}
			// Any attribute is tested at most once, which simplifies
			// comparisons:
			if (model.getTestedValueAtLiteral(i) != this.getTestedValueAtLiteral(pos)) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean supportsConfidences(Attribute label) {
		return false;
	}

	/**
	 * Two rules are equal, if they are both permutations of the same set of literals and predict
	 * the same label.
	 */
	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (this == object) {
			return true;
		}

		if (!(object instanceof ConjunctiveRuleModel)) {
			return false;
		}

		ConjunctiveRuleModel rule = (ConjunctiveRuleModel) object;

		if (this.getRuleLength() != rule.getRuleLength() || this.getConclusion() != rule.getConclusion()) {
			return false;
		}

		for (int i = 0; i < this.getRuleLength(); i++) {
			Attribute att = this.getAttributeOfLiteral(i);
			int pos;
			if ((pos = rule.getPositionOfAttributeInRule(att)) == -1
					|| this.getTestedValueAtLiteral(i) != rule.getTestedValueAtLiteral(pos)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(this.getRuleLength()).hashCode() ^ Integer.valueOf(this.getConclusion()).hashCode()
				^ myLiterals.hashCode();
	}

	/**
	 * Helper method of <code>getAllRefinedRules</code>. Iterates through the Attributes of an
	 * ExampleSet and compares them to those part of the rule.
	 *
	 * @param exampleSet
	 * @return the index of the first attribute of the exampleSet that is not used in the rule, and
	 *         for which no later Attribute is found in the rule, either.
	 */
	protected int getFirstUnusedAttribute(ExampleSet exampleSet, Attribute[] allAttributes) {
		int numAttributes = allAttributes.length;
		int firstUnusedAttribute = numAttributes;
		for (int i = numAttributes - 1; i >= 0; i--) {
			Attribute exampleSetAttribute = allAttributes[i];
			if (this.getPositionOfAttributeInRule(exampleSetAttribute) != -1) {
				return firstUnusedAttribute;
			} else {
				firstUnusedAttribute = i;
			}
		}
		return firstUnusedAttribute;
	}

	/**
	 * A refinement method that - when applied sytematically during learning - generates all rules
	 * for nominal attributes and a boolean target exactly once. The top-down refinement is
	 * compatible with pruning, as long as scores decrerase monotonically in support in the typical
	 * sense known from subgroup discovery. Attributes are added in the same order in which they
	 * occur in the provided exampleSet.
	 *
	 * @param exampleSet
	 *            used to identify attributes and their values for refinement
	 * @return all refined ConjunctiveRuleModel objects
	 */
	public Collection<ConjunctiveRuleModel> getAllRefinedRules(ExampleSet exampleSet) throws OperatorException {
		Attribute[] allAttributes = exampleSet.getAttributes().createRegularAttributeArray();
		int numAttributes = allAttributes.length;
		int firstUnused = this.getFirstUnusedAttribute(exampleSet, allAttributes);
		Vector<ConjunctiveRuleModel> theRefinements = new Vector<>();
		for (int i = firstUnused; i < numAttributes; i++) {
			Attribute nextAttribute = allAttributes[i];
			for (String valueString : nextAttribute.getMapping().getValues()) {
				int value = nextAttribute.getMapping().getIndex(valueString);
				ConjunctiveRuleModel refinedRule = new ConjunctiveRuleModel(this, nextAttribute, value);
				theRefinements.add(refinedRule);
			}
		}
		return theRefinements;
	}
}

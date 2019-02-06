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
package com.rapidminer.operator.learner.subgroups.hypothesis;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;


/**
 * This is a hypothesis for subgroup discovery.
 * 
 * @author Tobias Malbrecht
 */
public class Hypothesis implements Serializable {

	private static class AttributeQueue extends LinkedList<Attribute> {

		private static final long serialVersionUID = 8693212785374243323L;

		private AttributeQueue() {}

		private AttributeQueue(Iterable<Attribute> attributes) {
			addAll(attributes);
		}

		private void addAll(Iterable<Attribute> attributes) {
			for (Attribute attribute : attributes) {
				add(attribute);
			}
		}
	}

	private static final long serialVersionUID = 8694312785374243323L;

	public static final int POSITIVE_RULE = 0;

	public static final int NEGATIVE_RULE = 1;

	public static final int PREDICTION_RULE = 2;

	public static final int POSITIVE_AND_NEGATIVE_RULES = 3;

	public static final String[] RULE_GENERATION_MODES = { "positive", "negative", "prediction", "both" };

	private LinkedHashMap<Attribute, Literal> literalMap = null;

	private AttributeQueue restrictedAttributes = null;

	private double coveredWeight = 0.0d;

	private double positiveWeight = 0.0d;

	public Hypothesis() {
		literalMap = new LinkedHashMap<>();
	}

	public Hypothesis(Collection<Literal> literals) {
		this();
		for (Literal literal : literals) {
			literalMap.put(literal.getAttribute(), literal);
		}
	}

	public void apply(Example example) {
		if (applicable(example)) {
			double weight = 1.0d;
			if (example.getAttributes().getWeight() != null) {
				weight = example.getWeight();
			}
			coveredWeight += weight;
			if (example.getLabel() == example.getAttributes().getLabel().getMapping().getPositiveIndex()) {
				positiveWeight += weight;
			}
		}
	}

	public boolean applicable(Example example) {
		for (Literal literal : literalMap.values()) {
			if (!literal.applicable(example)) {
				return false;
			}
		}
		return true;
	}

	private Hypothesis refine(Attribute attribute, double value) {
		Hypothesis hypothesis = clone();
		hypothesis.literalMap.put(attribute, new Literal(attribute, value));
		return hypothesis;
	}

	public LinkedList<Hypothesis> refine(Iterable<Attribute> attributes) {
		LinkedList<Hypothesis> hypotheses = new LinkedList<>();
		for (Attribute attribute : attributes) {
			if (!literalMap.containsKey(attribute)) {
				for (String valueString : attribute.getMapping().getValues()) {
					hypotheses.add(refine(attribute, attribute.getMapping().mapString(valueString)));
				}
			}
		}
		return hypotheses;
	}

	public LinkedList<Hypothesis> restrictedRefine(Iterable<Attribute> attributes) {
		AttributeQueue restrictedAttributes = new AttributeQueue(attributes);
		LinkedList<Hypothesis> hypotheses = new LinkedList<>();
		Attribute attribute = null;
		while ((attribute = restrictedAttributes.poll()) != null) {
			if (!literalMap.containsKey(attribute)) {
				for (String valueString : attribute.getMapping().getValues()) {
					Hypothesis hypothesis = refine(attribute, attribute.getMapping().mapString(valueString));
					hypothesis.restrictedAttributes = new AttributeQueue(restrictedAttributes);
					hypotheses.add(hypothesis);
				}
			}
		}
		return hypotheses;
	}

	public LinkedList<Hypothesis> restrictedRefine() {
		if (restrictedAttributes == null) {
			return null;
		}
		return restrictedRefine(restrictedAttributes);
	}

	public Hypothesis subsume(Hypothesis otherHypothesis) {
		LinkedList<Literal> newLiterals = new LinkedList<>();
		for (Literal otherLiteral : otherHypothesis.literalMap.values()) {
			Literal correspondingLiteral = literalMap.get(otherLiteral.getAttribute());
			if (correspondingLiteral == null) {
				continue;
			}
			if (otherLiteral.equals(correspondingLiteral)) {
				newLiterals.add(otherLiteral);
				continue;
			}
			if (otherLiteral.contradicts(literalMap.get(otherLiteral.getAttribute()))) {
				return null;
			}
		}
		return new Hypothesis(newLiterals);
	}

	public Hypothesis combine(Hypothesis otherHypothesis) {
		LinkedList<Literal> newLiterals = new LinkedList<>();
		for (Literal otherLiteral : otherHypothesis.literalMap.values()) {
			Literal correspondingLiteral = literalMap.get(otherLiteral.getAttribute());
			if (correspondingLiteral == null) {
				newLiterals.add(otherLiteral);
				continue;
			}
			if (otherLiteral.contradicts(literalMap.get(otherLiteral.getAttribute()))) {
				return null;
			}
		}
		for (Literal literal : literalMap.values()) {
			for (Literal otherLiteral : newLiterals) {
				if (literal.equals(otherLiteral)) {
					continue;
				}
			}
			newLiterals.add(literal);
		}
		return new Hypothesis(newLiterals);
	}

	private Rule getPredictionRule(Attribute label) {
		double predictionIndex = positiveWeight / coveredWeight > 0.5 ? label.getMapping().getPositiveIndex() : label
				.getMapping().getNegativeIndex();
		return new Rule(this, new Literal(label, predictionIndex));
	}

	private Rule getPositiveRule(Attribute label) {
		return new Rule(this, new Literal(label, label.getMapping().getPositiveIndex()));
	}

	private Rule getNegativeRule(Attribute label) {
		return new Rule(this, new Literal(label, label.getMapping().getNegativeIndex()));
	}

	public LinkedList<Rule> generateRules(int ruleGenerationMode, Attribute label) {
		LinkedList<Rule> rules = new LinkedList<>();
		switch (ruleGenerationMode) {
			case POSITIVE_RULE:
				rules.add(getPositiveRule(label));
				break;
			case NEGATIVE_RULE:
				rules.add(getNegativeRule(label));
				break;
			case PREDICTION_RULE:
				rules.add(getPredictionRule(label));
				break;
			case POSITIVE_AND_NEGATIVE_RULES:
				rules.add(getPositiveRule(label));
				rules.add(getNegativeRule(label));
				break;
		}
		return rules;
	}

	private Literal getLiteral(Attribute attribute) {
		return literalMap.get(attribute);
	}

	private Collection<Literal> getLiterals() {
		return literalMap.values();
	}

	public int getNumberOfLiterals() {
		return literalMap.size();
	}

	public double getCoveredWeight() {
		return coveredWeight;
	}

	public double getPositiveWeight() {
		return positiveWeight;
	}

	@Override
	public boolean equals(Object object) {
		if (object == null) {
			return false;
		}
		if (getClass() != object.getClass()) {
			return false;
		}
		Hypothesis otherHypothesis = (Hypothesis) object;
		for (Literal literal : getLiterals()) {
			if (!literal.equals(otherHypothesis.getLiteral(literal.getAttribute()))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Hypothesis clone() {
		Hypothesis newHypothesis = new Hypothesis();
		newHypothesis.literalMap.putAll(literalMap);
		return newHypothesis;
	}

	@Override
	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		for (Literal literal : literalMap.values()) {
			stringBuffer.append(literal + " , ");
		}
		if (stringBuffer.length() > 3) {
			return stringBuffer.substring(0, stringBuffer.length() - 3);
		} else {
			return stringBuffer.toString();
		}

	}
}

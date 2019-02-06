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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.operator.learner.tree.SplitCondition;


/**
 * This class combines several SplitConditions to one rule by conjunctions. For example: "if
 * condition1 and condition2 ... and conditionN then labelname = labelValue" It provides a test
 * method, to test if examples belong to the labelValue of this rule. The rule is build
 * incrementally, SplitConditions may be added by addTerm
 *
 * @author Sebastian Land, Ingo Mierswa
 */
public class Rule implements Serializable {

	private static final long serialVersionUID = 7355669588176081975L;

	private LinkedList<SplitCondition> terms = new LinkedList<>();

	private String labelName;

	private int[] frequencies;

	private int frequencySum = 0;

	public Rule() {}

	public Rule(String labelName) {
		this.labelName = labelName;
	}

	public Rule(String labelName, SplitCondition term) {
		this.labelName = labelName;
		terms.add(term);
	}

	public Rule(String labelName, Collection<SplitCondition> terms) {
		this.labelName = labelName;
		this.terms.addAll(terms);
	}

	/** Clone constructor. Performs only a shallow clone on the conditions (which are not mutable). */
	private Rule(Rule other) {
		this.labelName = other.labelName;
		for (SplitCondition condition : other.terms) {
			this.terms.add(condition);
		}
		this.frequencySum = other.frequencySum;
		if (other.frequencies != null) {
			this.frequencies = new int[other.frequencies.length];
			for (int i = 0; i < this.frequencies.length; i++) {
				this.frequencies[i] = other.frequencies[i];
			}
		}
	}

	/** Performs only a shallow clone on the conditions (which are not mutable). */
	@Override
	public Object clone() {
		return new Rule(this);
	}

	public void setLabel(String labelName) {
		this.labelName = labelName;
	}

	public boolean coversExample(Example example) {
		boolean result = true;
		for (SplitCondition term : terms) {
			result &= term.test(example);
		}
		return result;
	}

	public void setFrequencies(int[] frequencies) {
		this.frequencies = frequencies;
		this.frequencySum = 0;
		if (frequencies != null) {
			for (int i = 0; i < this.frequencies.length; i++) {
				this.frequencySum += this.frequencies[i];
			}
		}
	}

	public int[] getFrequencies() {
		return this.frequencies;
	}

	public double[] getConfidences() {
		if (frequencies != null) {
			double[] confidences = new double[this.frequencies.length];
			for (int i = 0; i < this.frequencies.length; i++) {
				confidences[i] = (double) frequencies[i] / (double) this.frequencySum;
			}
			return confidences;
		} else {
			return new double[0];
		}
	}

	/**
	 * This method returns a String representation of this rule.
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (terms.size() > 0) {
			buffer.append("if ");
			boolean firstTerm = true;
			for (SplitCondition condition : terms) {
				if (!firstTerm) {
					buffer.append(" and ");
				}
				buffer.append(condition.toString());
				firstTerm = false;
			}
			buffer.append(" then ");
		} else {
			buffer.append("else ");
		}
		buffer.append(labelName);
		if (frequencies != null) {
			buffer.append("  (");
			for (int i = 0; i < frequencies.length; i++) {
				if (i != 0) {
					buffer.append(" / ");
				}
				buffer.append(frequencies[i]);
			}
			buffer.append(")");
		}
		return buffer.toString();
	}

	/**
	 * This method adds a condition to the conjunction in the rule's head
	 *
	 * @param condition
	 *            This condition is added
	 */
	public void addTerm(SplitCondition condition) {
		terms.add(condition);
	}

	public void removeLastTerm() {
		terms.removeLast();
	}

	public List<SplitCondition> getTerms() {
		return terms;
	}

	public void setTerms(LinkedList<SplitCondition> terms) {
		this.terms = terms;
	}

	public String getLabel() {
		return this.labelName;
	}

	public ExampleSet getCovered(ExampleSet exampleSet) {
		int[] partition = new int[exampleSet.size()];
		int counter = 0;
		for (Example e : exampleSet) {
			if (coversExample(e)) {
				partition[counter] = 1;
			}
			counter++;
		}
		SplittedExampleSet result = new SplittedExampleSet(exampleSet, new Partition(partition, 2), true);
		result.selectSingleSubset(1);
		return result;
	}

	public ExampleSet removeCovered(ExampleSet exampleSet) {
		int[] partition = new int[exampleSet.size()];
		int counter = 0;
		for (Example e : exampleSet) {
			if (coversExample(e)) {
				partition[counter] = 1;
			}
			counter++;
		}
		SplittedExampleSet result = new SplittedExampleSet(exampleSet, new Partition(partition, 2), true);
		result.selectSingleSubset(0);
		return result;
	}

	public boolean isPure(ExampleSet exampleSet, double pureness) {
		Attribute label = exampleSet.getAttributes().getLabel();
		exampleSet.recalculateAttributeStatistics(label);
		return exampleSet.getStatistics(label, Statistics.COUNT, getLabel()) >= pureness * exampleSet.size();
	}
}

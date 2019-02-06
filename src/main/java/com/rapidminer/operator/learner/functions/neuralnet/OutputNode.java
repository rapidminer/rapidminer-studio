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
package com.rapidminer.operator.learner.functions.neuralnet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;


/**
 * This node represents the output node(s) of a neural network. It uses a sigmoid activation
 * function for classification tasks (one node for each class) and a linear function for regression
 * tasks.
 * 
 * @author Ingo Mierswa
 */
public class OutputNode extends Node {

	private static final long serialVersionUID = 23423534L;

	private Attribute label;

	private int classIndex = 0;

	private double labelRange;

	private double labelBase;

	public OutputNode(String nodeName, Attribute label, double labelRange, double labelBase) {
		super(nodeName, OUTPUT, OUTPUT);
		this.label = label;
		this.labelRange = labelRange;
		this.labelBase = labelBase;
	}

	public void setClassIndex(int classIndex) {
		this.classIndex = classIndex;
	}

	public int getClassIndex() {
		return this.classIndex;
	}

	@Override
	public double calculateValue(boolean shouldCalculate, Example example) {
		if (Double.isNaN(currentValue) && shouldCalculate) {
			currentValue = 0;
			for (int i = 0; i < inputNodes.length; i++) {
				currentValue += inputNodes[i].calculateValue(true, example);

			}
			if (!label.isNominal()) {
				currentValue = currentValue * labelRange + labelBase;
			}
		}
		return currentValue;
	}

	@Override
	public double calculateError(boolean shouldCalculate, Example example) {
		if (!Double.isNaN(currentValue) && Double.isNaN(currentError) && shouldCalculate) {
			if (label.isNominal()) {
				if ((int) example.getValue(label) == classIndex) {
					currentError = 1.0d - currentValue;
				} else {
					currentError = 0.0d - currentValue;
				}
			} else if (!label.isNominal()) {
				if (labelRange == 0.0d) {
					currentError = 0.0d;
				} else {
					currentError = (example.getValue(label) - currentValue) / labelRange;
				}
			}
		}
		return currentError;
	}

	public Attribute getLabel() {
		return label;
	}

	public double getLabelRange() {
		return labelRange;
	}

	public double getLabelBase() {
		return labelBase;
	}

	public double getCurrentValue() {
		return currentValue;
	}
}

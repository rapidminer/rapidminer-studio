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
package com.rapidminer.operator.performance;

import java.io.ObjectStreamException;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.ROCBias;
import com.rapidminer.tools.math.ROCData;
import com.rapidminer.tools.math.ROCDataGenerator;


/**
 * This criterion calculates the area under the ROC curve.
 * 
 * @author Ingo Mierswa, Martin Scholz
 */
public class AreaUnderCurve extends MeasuredPerformance {

	private static final long serialVersionUID = 6877715214974493828L;

	public static class Optimistic extends AreaUnderCurve {

		private static final long serialVersionUID = 1L;

		public Optimistic() {
			super(ROCBias.OPTIMISTIC);
		}

		public Optimistic(Optimistic optimistic) {
			super(optimistic);
		}
	}

	public static class Pessimistic extends AreaUnderCurve {

		private static final long serialVersionUID = 1L;

		public Pessimistic() {
			super(ROCBias.PESSIMISTIC);
		}

		public Pessimistic(Pessimistic pessimistic) {
			super(pessimistic);
		}
	}

	public static class Neutral extends AreaUnderCurve {

		private static final long serialVersionUID = 1L;

		public Neutral() {
			super(ROCBias.NEUTRAL);
		}

		public Neutral(Neutral neutral) {
			super(neutral);
		}
	}

	/** The value of the AUC. */
	private double auc = Double.NaN;

	/** The data generator for this ROC curve. */
	private transient ROCDataGenerator rocDataGenerator = new ROCDataGenerator(1.0d, 1.0d);

	/** The data for the ROC curve. */
	private LinkedList<ROCData> rocData = new LinkedList<>();

	/** A counter for average building. */
	private int counter = 1;

	/** The positive class name. */
	private String positiveClass;

	private ROCBias method;

	/** Clone constructor. */
	public AreaUnderCurve() {
		method = ROCBias.OPTIMISTIC;
	}

	/**
	 * True iff the user specified the positive class name.
	 */
	private boolean userSpecifiedPositiveClass;

	public AreaUnderCurve(ROCBias method) {
		this.method = method;
	}

	public AreaUnderCurve(AreaUnderCurve aucObject) {
		super(aucObject);
		this.auc = aucObject.auc;
		this.counter = aucObject.counter;
		this.positiveClass = aucObject.positiveClass;
		this.method = aucObject.method;
		if (!aucObject.rocData.isEmpty()) {
			this.rocData.addAll(aucObject.rocData);
		}
	}

	/** Calculates the AUC. */
	@Override
	public void startCounting(ExampleSet exampleSet, boolean useExampleWeights) throws OperatorException {
		super.startCounting(exampleSet, useExampleWeights);
		this.positiveClass = userSpecifiedPositiveClass ? positiveClass :
				exampleSet.getAttributes().getPredictedLabel().getMapping().getPositiveString();
		// create ROC data
		// using null will make the rocDataGenerator fall back to the label's intern mapping
		this.rocData.add(rocDataGenerator.createROCData(exampleSet, useExampleWeights, method,
				userSpecifiedPositiveClass ? positiveClass : null));
		this.auc = rocDataGenerator.calculateAUC(this.rocData.getLast());
	}

	/** Does nothing. Everything is done in {@link #startCounting(ExampleSet, boolean)}. */
	@Override
	public void countExample(Example example) {}

	@Override
	public double getExampleCount() {
		return 1.0d;
	}

	@Override
	public double getMikroVariance() {
		return Double.NaN;
	}

	@Override
	public double getMikroAverage() {
		return auc / counter;
	}

	/** Returns the fitness. */
	@Override
	public double getFitness() {
		return getAverage();
	}

	@Override
	public String getName() {
		if (method == ROCBias.NEUTRAL) {
			return "AUC";
		} else {
			return "AUC (" + method.toString().toLowerCase() + ")";
		}
	}

	@Override
	public String getDescription() {
		return "The area under a ROC curve. Given example weights are also considered. Please note that the second class is considered to be positive.";
	}

	@Override
	public void buildSingleAverage(Averagable performance) {
		AreaUnderCurve other = (AreaUnderCurve) performance;
		this.counter += other.counter;
		this.auc += other.auc;
		this.rocData.addAll(other.rocData);
	}

	@Override
	public String toString() {
		return super.toString() + " (positive class: " + positiveClass + ")";
	}

	public List<ROCData> getRocData() {
		return rocData;
	}

	public ROCDataGenerator getRocDataGenerator() {
		return rocDataGenerator;
	}

	public void readResolve() throws ObjectStreamException {
		rocDataGenerator = new ROCDataGenerator(1.0d, 1.0d);
	}

	/**
	 * Sets a user defined positive class (overrides the labels original mapping).
	 *
	 * @param positiveClass
	 * 		User defined positive class name. If {@code null}, the last user specified name is deleted.
	 */
	public void setUserDefinedPositiveClassName(String positiveClass) {
		this.positiveClass = positiveClass;
		userSpecifiedPositiveClass = positiveClass != null;
	}
}

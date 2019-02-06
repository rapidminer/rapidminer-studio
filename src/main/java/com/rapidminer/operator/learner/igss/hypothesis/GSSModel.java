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
package com.rapidminer.operator.learner.igss.hypothesis;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * Wrapper class for rules found by the Iterating GSS algorithm. Needed because IGSS rules need to
 * be small in order to keep many in main memory
 *
 * @author Dirk Dach
 *
 * @deprecated This model is not used anymore.
 */
@Deprecated
public class GSSModel extends PredictionModel {

	private static final long serialVersionUID = -9210831626413275099L;

	/** The all hypothesis of the model. */
	protected Hypothesis hypothesis;

	/** The confidence values for all predictions. */
	protected double[] confidences;

	/** The regular attributes used by all rules. */
	protected static Attribute[] regularAttributes;

	/** crisp only crisp ... */
	protected boolean crisp = true;

	/** Creates a new GSSModel. */
	public GSSModel(ExampleSet exampleSet, Hypothesis hypothesis, double[] confidences) {
		super(exampleSet, null, null);
		this.hypothesis = hypothesis.clone();
		this.confidences = new double[2];
		this.confidences[0] = confidences[0];
		this.confidences[1] = confidences[1];
	}

	/** Returns true if the hypothesis contained in the model are equal. */
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GSSModel)) {
			return false;
		}
		GSSModel otherModel = (GSSModel) o;
		if (otherModel.hypothesis.equals(this.hypothesis)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.hypothesis.hashCode();
	}

	/** Returns the most probable label index for this model. */
	public int getPredictionIndex() {

		if (this.confidences[Hypothesis.POSITIVE_CLASS] >= this.confidences[Hypothesis.NEGATIVE_CLASS]) {
			return Hypothesis.POSITIVE_CLASS;
		} else {
			return Hypothesis.NEGATIVE_CLASS;
		}
	}

	/** Iterates over all examples and applies the model to them.
	 *  Progress not implemented, because class is deprecated */
	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {		
		Iterator<Example> reader = exampleSet.iterator();
		int bestPrediction;
		int worstPrediction;
		if (this.confidences[Hypothesis.POSITIVE_CLASS] >= this.confidences[Hypothesis.NEGATIVE_CLASS]) {
			bestPrediction = Hypothesis.POSITIVE_CLASS;
			worstPrediction = Hypothesis.NEGATIVE_CLASS;
		} else {
			bestPrediction = Hypothesis.NEGATIVE_CLASS;
			worstPrediction = Hypothesis.POSITIVE_CLASS;
		}

		while (reader.hasNext()) {
			Example e = reader.next();
			if (applicable(e)) {
				e.setValue(predictedLabel, bestPrediction);
			} else {
				e.setValue(predictedLabel, worstPrediction);
			}

			e.setConfidence(this.getLabel().getMapping().mapIndex(Hypothesis.NEGATIVE_CLASS),
					confidences[Hypothesis.NEGATIVE_CLASS]);
			e.setConfidence(this.getLabel().getMapping().mapIndex(Hypothesis.POSITIVE_CLASS),
					confidences[Hypothesis.POSITIVE_CLASS]);
		}
		return exampleSet;
	}

	/** Returns true if the model is applicable to the current example. */
	public boolean applicable(Example example) {

		if (this.hypothesis.applicable(example)) {
			return true;
		} else {
			return false;
		}
	}

	/** Returns a String representation of the hypothesis stored in this model. */
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer(super.toString());
		result.append(Tools.getLineSeparator());
		result.append(this.hypothesis);
		return result.toString();
	}

	/** Returns the hypothesis stored in this model. */
	public Hypothesis getHypothesis() {
		return this.hypothesis;
	}
}

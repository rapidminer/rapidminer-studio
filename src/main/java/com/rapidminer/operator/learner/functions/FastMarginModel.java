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
package com.rapidminer.operator.learner.functions;

import java.util.Iterator;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.FastExample2SparseTransform;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorProgress;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;


/**
 * This is the model of the fast margin learner which learns a linear SVM in linear time.
 *
 * @author Ingo Mierswa
 */
public class FastMarginModel extends PredictionModel {

	private static final long serialVersionUID = 7701199447666181333L;

	private static final int OPERATOR_PROGRESS_STEPS = 5000;

	private Model linearModel;

	private boolean useBias;

	private String[] attributeConstructions;

	public FastMarginModel(ExampleSet headerSet, Model linearModel, boolean useBias) {
		super(headerSet, ExampleSetUtilities.SetsCompareOption.ALLOW_SUPERSET,
				ExampleSetUtilities.TypesCompareOption.ALLOW_SAME_PARENTS);
		this.linearModel = linearModel;
		this.useBias = useBias;
		this.attributeConstructions = com.rapidminer.example.Tools.getRegularAttributeConstructions(headerSet);
	}

	@Override
	public String getName() {
		return "Fast Linear Classification";
	}

	@Override
	public ExampleSet performPrediction(ExampleSet exampleSet, Attribute predictedLabel) throws OperatorException {
		FastExample2SparseTransform ripper = new FastExample2SparseTransform(exampleSet);
		Attribute label = getLabel();

		Attribute[] confidenceAttributes = null;
		if (label.isNominal() && label.getMapping().size() >= 2) {
			confidenceAttributes = new Attribute[linearModel.label.length];
			for (int j = 0; j < linearModel.label.length; j++) {
				String labelName = label.getMapping().mapIndex(linearModel.label[j]);
				confidenceAttributes[j] = exampleSet.getAttributes()
						.getSpecial(Attributes.CONFIDENCE_NAME + "_" + labelName);
			}
		}

		Iterator<Example> i = exampleSet.iterator();
		OperatorProgress progress = null;
		if (getShowProgress() && getOperator() != null && getOperator().getProgress() != null) {
			progress = getOperator().getProgress();
			progress.setTotal(exampleSet.size());
		}
		int progressCounter = 0;
		while (i.hasNext()) {
			Example e = i.next();

			// set prediction
			FeatureNode[] currentNodes = FastLargeMargin.makeNodes(e, ripper, this.useBias);

			double predictedClass = Linear.predict(linearModel, currentNodes);
			e.setValue(predictedLabel, predictedClass);

			// use simple calculation for binary cases...
			if (label.getMapping().size() == 2) {
				double[] functionValues = new double[linearModel.nr_class];
				Linear.predictValues(linearModel, currentNodes, functionValues);
				double prediction = functionValues[0];
				if (confidenceAttributes != null && confidenceAttributes.length > 0) {
					e.setValue(confidenceAttributes[0], 1.0d / (1.0d + java.lang.Math.exp(-prediction)));
					if (confidenceAttributes.length > 1) {
						e.setValue(confidenceAttributes[1], 1.0d / (1.0d + java.lang.Math.exp(prediction)));
					}
				}
			}

			if (progress != null && ++progressCounter % OPERATOR_PROGRESS_STEPS == 0) {
				progress.setCompleted(progressCounter);
			}
		}
		return exampleSet;
	}

	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		boolean first = true;
		for (int i = 0; i < this.attributeConstructions.length; i++) {
			result.append(getCoefficientString(linearModel.w[i], first) + " * " + attributeConstructions[i]
					+ Tools.getLineSeparator());
			first = false;
		}
		if (this.useBias) {
			result.append(getCoefficientString(linearModel.w[linearModel.w.length - 1], first));
		}
		return result.toString();
	}

	private String getCoefficientString(double coefficient, boolean first) {
		if (!first) {
			if (coefficient >= 0) {
				return "+ " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		} else {
			if (coefficient >= 0) {
				return "  " + Tools.formatNumber(Math.abs(coefficient));
			} else {
				return "- " + Tools.formatNumber(Math.abs(coefficient));
			}
		}
	}
}

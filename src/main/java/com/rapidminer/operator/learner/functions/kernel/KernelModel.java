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
package com.rapidminer.operator.learner.functions.kernel;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.SimpleDataTable;
import com.rapidminer.datatable.SimpleDataTableRow;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ExampleSetUtilities;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.tools.Tools;


/**
 * This is the abstract model class for all kernel models. This class actually only provide a common
 * interface for plotting SVM and other kernel method models.
 *
 * @author Ingo Mierswa
 */
public abstract class KernelModel extends PredictionModel {

	private static final long serialVersionUID = 7480153570564620067L;

	private final String[] attributeConstructions;

	/**
	 * Creates a new {@link KernelModel} which was built on the given example set. Please note that
	 * the given example set is automatically transformed into a {@link HeaderExampleSet} which
	 * means that no reference to the data itself is kept but only to the header, i.e. to the
	 * attribute meta descriptions.
	 *
	 * @deprecated Since RapidMiner Studio 6.0.009. Please use the new Constructor
	 *             {@link #KernelModel(ExampleSet, com.rapidminer.example.set.ExampleSetUtilities.SetsCompareOption, com.rapidminer.example.set.ExampleSetUtilities.TypesCompareOption)}
	 *             which offers the possibility to check for AttributeType and kind of ExampleSet
	 *             before execution.
	 */
	@Deprecated
	public KernelModel(ExampleSet exampleSet) {
		this(exampleSet, null, null);
	}

	/**
	 * Creates a new {@link KernelModel} which is build based on the given {@link ExampleSet}.
	 * Please note that the given ExampleSet is automatically transformed into a
	 * {@link HeaderExampleSet} which means that no reference to the data itself is kept but only to
	 * the header, i.e., to the attribute meta descriptions.
	 *
	 * @param sizeCompareOperator
	 *            describes the allowed relations between the given ExampleSet and future
	 *            ExampleSets on which this Model will be applied. If this parameter is null no
	 *            error will be thrown.
	 * @param typeCompareOperator
	 *            describes the allowed relations between the types of the attributes of the given
	 *            ExampleSet and the types of future attributes of ExampleSet on which this Model
	 *            will be applied. If this parameter is null no error will be thrown.
	 */
	public KernelModel(ExampleSet exampleSet, ExampleSetUtilities.SetsCompareOption sizeCompareOperator,
			ExampleSetUtilities.TypesCompareOption typeCompareOperator) {
		super(exampleSet, sizeCompareOperator, typeCompareOperator);
		this.attributeConstructions = com.rapidminer.example.Tools.getRegularAttributeConstructions(exampleSet);
	}

	public abstract double getBias();

	public abstract double getAlpha(int index);

	public abstract double getFunctionValue(int index);

	public abstract boolean isClassificationModel();

	public abstract String getClassificationLabel(int index);

	public abstract double getRegressionLabel(int index);

	public abstract String getId(int index);

	public abstract SupportVector getSupportVector(int index);

	public abstract int getNumberOfSupportVectors();

	public abstract int getNumberOfAttributes();

	public abstract double getAttributeValue(int exampleIndex, int attributeIndex);

	public String[] getAttributeConstructions() {
		return this.attributeConstructions;
	}

	/** The default implementation returns the classname without package. */
	@Override
	public String getName() {
		return "Kernel Model";
	}

	/** Returns a string representation of this model. */
	@Override
	public String toString() {
		String[] attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(getTrainingHeader());

		StringBuffer result = new StringBuffer();
		result.append("Total number of Support Vectors: " + getNumberOfSupportVectors() + Tools.getLineSeparator());
		result.append("Bias (offset): " + Tools.formatNumber(getBias()) + Tools.getLineSeparators(2));
		if (!getLabel().isNominal() || getLabel().getMapping().size() == 2) {
			double[] w = new double[getNumberOfAttributes()];
			boolean showWeights = true;
			for (int i = 0; i < getNumberOfSupportVectors(); i++) {
				SupportVector sv = getSupportVector(i);
				if (sv != null) {
					double[] x = sv.getX();
					double alpha = sv.getAlpha();
					double y = sv.getY();
					for (int j = 0; j < w.length; j++) {
						w[j] += y * alpha * x[j];
					}
				} else {
					showWeights = false;
				}
			}
			if (showWeights) {
				for (int j = 0; j < w.length; j++) {
					result.append("w["
							+ attributeNames[j]
									+ (!attributeNames[j].equals(attributeConstructions[j]) ? " = " + attributeConstructions[j] : "")
									+ "] = " + Tools.formatNumber(w[j]) + Tools.getLineSeparator());
				}
			}
		} else {
			result.append("Feature weight calculation only possible for two class learning problems."
					+ Tools.getLineSeparator() + "Please use the operator SVMWeighting instead." + Tools.getLineSeparator());
		}
		return result.toString();
	}

	public DataTable createWeightsTable() {
		String[] attributeNames = com.rapidminer.example.Tools.getRegularAttributeNames(getTrainingHeader());

		SimpleDataTable weightTable = new SimpleDataTable("Kernel Model Weights", new String[] { "Attribute", "Weight" });
		if (!getLabel().isNominal() || getLabel().getMapping().size() == 2) {
			double[] w = new double[getNumberOfAttributes()];
			boolean showWeights = true;
			for (int i = 0; i < getNumberOfSupportVectors(); i++) {
				SupportVector sv = getSupportVector(i);
				if (sv != null) {
					double[] x = sv.getX();
					double alpha = sv.getAlpha();
					double y = sv.getY();
					for (int j = 0; j < w.length; j++) {
						w[j] += y * alpha * x[j];
					}
				} else {
					showWeights = false;
				}
			}
			if (showWeights) {
				for (int j = 0; j < w.length; j++) {
					int nameIndex = weightTable.mapString(0, attributeNames[j]);
					weightTable.add(new SimpleDataTableRow(new double[] { nameIndex, w[j] }));
				}
				return weightTable;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

}

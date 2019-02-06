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
package com.rapidminer.operator.features.transformation;

import java.util.List;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.GenerateModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MDReal;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.matrix.MatrixTools;


/**
 * This operator performs a Singular Value Decomposition (SVD) of the data The user can specify the
 * number of target dimensions operator outputs a {@link SVDModel}. With the
 * <code>ModelApplier</code> you can transform the features.
 *
 * @author Sebastian Land
 */
public class SVDReduction extends Operator {

	private static final OperatorVersion OPERATOR_VERSION_CHANGED_ATTRIBUTE_NAME = new OperatorVersion(5, 1, 4);

	/**
	 * The parameter name for &quot;Keep the all components with a cumulative variance smaller than
	 * the given threshold.&quot;
	 */
	public static final String PARAMETER_PERCENTAGE_THRESHOLD = "percentage_threshold";

	/**
	 * The parameter name for &quot;Keep this number of components. If '-1' then keep all
	 * components.'&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_COMPONENTS = "dimensions";

	public static final String PARAMETER_REDUCTION_TYPE = "dimensionality_reduction";

	public static final String[] REDUCTION_METHODS = new String[] { "none", "keep percentage", "fixed number" };

	public static final int REDUCTION_NONE = 0;
	public static final int REDUCTION_PERCENTAGE = 1;
	public static final int REDUCTION_FIXED = 2;

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public SVDReduction(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL));

		getTransformer().addRule(new GenerateModelTransformationRule(exampleSetInput, modelOutput, SVDModel.class));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				int numberOfAttributes = metaData.getNumberOfRegularAttributes();
				int resultNumber = numberOfAttributes;
				if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_FIXED) {
					resultNumber = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
					metaData.attributesAreKnown();
				} else if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_PERCENTAGE) {
					resultNumber = numberOfAttributes;
					metaData.attributesAreSubset();
				}
				metaData.clearRegular();
				for (int i = 1; i <= resultNumber; i++) {
					AttributeMetaData svdAMD = new AttributeMetaData("svd_" + i, Ontology.REAL);
					svdAMD.setMean(new MDReal(0.0));
					metaData.addAttribute(svdAMD);
				}
				return metaData;
			}
		});
		getTransformer().addRule(new PassThroughRule(exampleSetInput, originalOutput, false));
	}

	/** Helper method for anonymous operators. */
	public Model doWork(ExampleSet exampleSet) throws OperatorException {
		exampleSetInput.receive(exampleSet);
		doWork();
		return modelOutput.getData(Model.class);
	}

	@Override
	public void doWork() throws OperatorException {
		// check whether all attributes are numerical
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);

		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this);
		Tools.onlyNumericalAttributes(exampleSet, "SVD");

		// create data matrix
		Matrix dataMatrix = MatrixTools.getDataAsMatrix(exampleSet);

		// Singular Value Decomposition
		SingularValueDecomposition singularValueDecomposition = dataMatrix.svd();

		// create and deliver results
		double[] singularvalues = singularValueDecomposition.getSingularValues();
		Matrix vMatrix = singularValueDecomposition.getV();

		SVDModel model = new SVDModel(exampleSet, singularvalues, vMatrix);
		if (getCompatibilityLevel().isAtMost(OPERATOR_VERSION_CHANGED_ATTRIBUTE_NAME)) {
			model.enableLegacyMode();
		}

		int reductionType = getParameterAsInt(PARAMETER_REDUCTION_TYPE);
		switch (reductionType) {
			case REDUCTION_NONE:
				model.setNumberOfComponents(exampleSet.getAttributes().size());
				break;
			case REDUCTION_PERCENTAGE:
				model.setVarianceThreshold(getParameterAsDouble(PARAMETER_PERCENTAGE_THRESHOLD));
				break;
			case REDUCTION_FIXED:
				model.setNumberOfComponents(getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS));
				break;
		}

		modelOutput.deliver(model);
		originalOutput.deliver(exampleSet);
		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply(exampleSet));
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return new OperatorVersion[] { OPERATOR_VERSION_CHANGED_ATTRIBUTE_NAME };
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_REDUCTION_TYPE,
				"Indicates which type of dimensionality reduction should be applied", REDUCTION_METHODS, REDUCTION_FIXED);
		type.setExpert(false);
		list.add(type);

		type = new ParameterTypeDouble(PARAMETER_PERCENTAGE_THRESHOLD,
				"Keep the all components with a cumulative variance smaller than the given threshold.", 0, 1, 0.95);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, REDUCTION_METHODS, true,
				REDUCTION_PERCENTAGE));
		list.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS, "Keep this number of components.", 1, Integer.MAX_VALUE,
				1);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, REDUCTION_METHODS, true,
				REDUCTION_FIXED));
		list.add(type);
		return list;
	}
}

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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.RawStore;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
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
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;


/**
 * This operator performs a principal components analysis (PCA) using the covariance matrix. The
 * user can specify the amount of variance to cover in the original data when retaining the best
 * number of principal components. The user can also specify manually the number of principal
 * components. The operator outputs a <code>PCAModel</code>. With the <code>ModelApplier</code> you
 * can transform the features.
 *
 * @author Ingo Mierswa
 * @see PCAModel
 */
public class PCA extends Operator {

	/**
	 * The parameter name for &quot;Keep the all components with a cumulative variance smaller than
	 * the given threshold.&quot;
	 */
	public static final String PARAMETER_VARIANCE_THRESHOLD = "variance_threshold";

	/**
	 * The parameter name for &quot;Keep this number of components. If '-1' then keep all
	 * components.'&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_COMPONENTS = "number_of_components";

	public static final String PARAMETER_REDUCTION_TYPE = "dimensionality_reduction";

	public static final String[] REDUCTION_METHODS = new String[] { "none", "keep variance", "fixed number" };

	public static final int REDUCTION_NONE = 0;
	public static final int REDUCTION_VARIANCE = 1;
	public static final int REDUCTION_FIXED = 2;

	/** The version before the change from JAMA to ojAlgo for Eigenvalue calculations */
	public static final OperatorVersion EIGENVALUE_ALGORITHM_CHANGED = new OperatorVersion(9, 4, 1);

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public PCA(OperatorDescription description) {
		super(description);
		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL));

		getTransformer().addRule(new GenerateModelTransformationRule(exampleSetInput, modelOutput, PCAModel.class));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				int numberOfAttributes = metaData.getNumberOfRegularAttributes();
				int resultNumber = numberOfAttributes;
				if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_FIXED) {
					resultNumber = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
					int regular_numbers = metaData.getNumberOfRegularAttributes();
					if (regular_numbers < resultNumber) {
						LogService.getRoot().log(Level.WARNING,
								"com.rapidminer.operator.features.transformation.PCA.less_attributes",
								new Object[] { resultNumber, regular_numbers });
						resultNumber = regular_numbers;
					}
					metaData.attributesAreKnown();
				} else if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_VARIANCE) {
					resultNumber = numberOfAttributes;
					metaData.attributesAreSubset();
				}
				metaData.clearRegular();
				for (int i = 1; i <= resultNumber; i++) {
					AttributeMetaData pcAMD = new AttributeMetaData("pc_" + i, Ontology.REAL);
					pcAMD.setMean(new MDReal(0.0));
					metaData.addAttribute(pcAMD);
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

		Tools.onlyNonMissingValues(exampleSet, getOperatorClassName(), this, new String[0]);
		Tools.onlyNumericalAttributes(exampleSet, "PCA");

		Iterator<Attribute> iterate = exampleSet.getAttributes().allAttributes();
		while (iterate.hasNext()) {
			Attribute curattribute = iterate.next();
			if (curattribute.getName().startsWith("pc_")) {
				throw new UserError(this, "pca_attribute_names", curattribute.getName());
			}
		}

		// create covariance matrix
		log("Creating the covariance matrix...");
		Matrix covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(exampleSet, this);

		// EigenVector and EigenValues of the covariance matrix
		log("Performing the eigenvalue decomposition...");

		double[][] eigenvectors;
		double[] eigenvalues;
		if (getCompatibilityLevel().isAtMost(EIGENVALUE_ALGORITHM_CHANGED)) {
			//calculate using JAMA
			EigenvalueDecomposition eigenvalueDecomposition = covarianceMatrix.eig();

			checkForStop();
			// create and deliver results
			eigenvalues = eigenvalueDecomposition.getRealEigenvalues();
			Matrix eigenvectorMatrix = eigenvalueDecomposition.getV();
			eigenvectors = eigenvectorMatrix.getArray();
		} else {
			//calculate using ojAlgo
			MatrixStore<Double> matA = RawStore.wrap(covarianceMatrix.getArray());
			Eigenvalue<Double> eig = Eigenvalue.PRIMITIVE.make(matA, true);
			eig.decompose(matA);

			checkForStop();
			// create and deliver results
			eigenvectors = eig.getV().toRawCopy2D();
			eigenvalues = new double[eigenvectors.length];
			eig.getEigenvalues(eigenvalues, Optional.empty());
		}

		PCAModel model = new PCAModel(exampleSet, eigenvalues, eigenvectors);

		int reductionType = getParameterAsInt(PARAMETER_REDUCTION_TYPE);
		switch (reductionType) {
			case REDUCTION_NONE:
				model.setNumberOfComponents(exampleSet.getAttributes().size());
				break;
			case REDUCTION_VARIANCE:
				model.setVarianceThreshold(getParameterAsDouble(PARAMETER_VARIANCE_THRESHOLD));
				break;
			case REDUCTION_FIXED:
				model.setNumberOfComponents(Math.min(exampleSet.getAttributes().size(),
						getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS)));
				break;
		}

		modelOutput.deliver(model);
		originalOutput.deliver(exampleSet);
		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply(exampleSet));
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		ParameterType type = new ParameterTypeCategory(PARAMETER_REDUCTION_TYPE,
				"Indicates which type of dimensionality reduction should be applied", REDUCTION_METHODS, REDUCTION_VARIANCE);
		type.setExpert(false);
		list.add(type);

		type = new ParameterTypeDouble(PARAMETER_VARIANCE_THRESHOLD,
				"Keep the all components with a cumulative variance smaller than the given threshold.", 0, 1, 0.95);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, REDUCTION_METHODS, true,
				REDUCTION_VARIANCE));
		list.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS, "Keep this number of components.", 1, Integer.MAX_VALUE,
				1);
		type.setExpert(false);
		type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, REDUCTION_METHODS, true,
				REDUCTION_FIXED));
		list.add(type);

		return list;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(), EIGENVALUE_ALGORITHM_CHANGED);
	}
}

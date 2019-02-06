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
package com.rapidminer.operator.ports.metadata;

import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.learner.meta.Binary2MultiClassLearner;
import com.rapidminer.operator.learner.meta.ClassificationByRegression;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.quickfix.OperatorInsertionQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.OperatorService;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Simon Fischer
 */
public class LearnerPrecondition extends CapabilityPrecondition {

	public LearnerPrecondition(CapabilityProvider capabilityProvider, InputPort inputPort) {
		super(capabilityProvider, inputPort);
	}

	@Override
	public void makeAdditionalChecks(ExampleSetMetaData metaData) {
		// checking Capabilities
		super.makeAdditionalChecks(metaData);
	}

	// @Override
	// protected void checkLabelPreconditions(ExampleSetMetaData metaData) {
	// // label
	// //check if needs label
	// // TODO: This checks if it is supported, but not if it is required. This test will break if
	// we add a new label type
	// // because it will then be incomplete.
	// if (capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL) ||
	// capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL) ||
	// capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
	// switch (metaData.hasSpecial(Attributes.LABEL_NAME)) {
	// case UNKNOWN:
	// getInputPort().addError(new SimpleMetaDataError(Severity.WARNING, getInputPort(),
	// Collections.singletonList(new
	// ChangeAttributeRoleQuickFix(getInputPort(), Attributes.LABEL_NAME, "change_attribute_role",
	// Attributes.LABEL_NAME)), "special_unknown", new
	// Object[] { Attributes.LABEL_NAME }));
	// break;
	// case NO:
	// getInputPort().addError(new SimpleMetaDataError(Severity.ERROR, getInputPort(),
	// Collections.singletonList(new
	// ChangeAttributeRoleQuickFix(getInputPort(), Attributes.LABEL_NAME, "change_attribute_role",
	// Attributes.LABEL_NAME)), "special_missing", new
	// Object[] { Attributes.LABEL_NAME }));
	// break;
	// case YES:
	// AttributeMetaData label = metaData.getLabelMetaData();
	// List<QuickFix> fixes = new LinkedList<QuickFix>();
	// if (label.isNominal()) {
	// if (capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
	// fixes.add(createClassificationByRegression());
	// }
	//
	// if (label.isBinominal()) {
	// if (!capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)) {
	// createLearnerError(OperatorCapability.BINOMINAL_LABEL.getDescription(), fixes);
	// }
	// } else {
	// if (!capabilityProvider.supportsCapability(OperatorCapability.POLYNOMINAL_LABEL)) {
	// if (capabilityProvider.supportsCapability(OperatorCapability.BINOMINAL_LABEL)) {
	// fixes.add(constructBinominal2MulticlassLearner());
	// //fixes.add(constructNominal2Binominal(label.getName()));
	// if ((label.getValueSetRelation() == SetRelation.EQUAL) &&
	// (label.getValueSet().size() == 2)) {
	// fixes.add(createToBinominalFix(label.getName()));
	// }
	// }
	// createLearnerError(OperatorCapability.POLYNOMINAL_LABEL.getDescription(), fixes);
	// }
	// }
	// } else if (label.isNumerical() &&
	// !capabilityProvider.supportsCapability(OperatorCapability.NUMERICAL_LABEL)) {
	// createLearnerError(OperatorCapability.NUMERICAL_LABEL.getDescription(),
	// AbstractDiscretizationOperator.createDiscretizationFixes(getInputPort(), label.getName()));
	// }
	// }
	// }
	// }

	/**
	 * This method has to return a collection of quick fixes which are appropriate when regression
	 * is supported and the data needs classification.
	 */
	@Override
	protected Collection<QuickFix> getFixesForClassificationWhenRegressionSupported() {
		Operator learner = getInputPort().getPorts().getOwner().getOperator();
		OperatorDescription ods[] = OperatorService.getOperatorDescriptions(ClassificationByRegression.class);
		String name = null;
		if (ods.length > 0) {
			name = ods[0].getName();
		}

		QuickFix fix = new OperatorInsertionQuickFix("insert_classification_by_regression_learner",
				new Object[] { name, learner.getOperatorDescription().getName() }, 3, getInputPort()) {

			@SuppressWarnings("deprecation")
			@Override
			public void apply() {
				List<Port> toUnlock = new LinkedList<Port>();
				try {
					Operator learner = getInputPort().getPorts().getOwner().getOperator();
					ExecutionUnit learnerUnit = learner.getExecutionUnit();

					// searching for model outport
					OutputPort modelOutput = null;
					MetaData modelMetaData = new PredictionModelMetaData(PredictionModel.class, new ExampleSetMetaData());
					for (OutputPort port : learner.getOutputPorts().getAllPorts()) {
						MetaData data = port.getMetaData();
						if (modelMetaData.isCompatible(data, CompatibilityLevel.VERSION_5)) {
							modelOutput = port;
							toUnlock.add(modelOutput);
							modelOutput.lock();
							break;
						}
					}

					ClassificationByRegression metaLearner = OperatorService
							.createOperator(ClassificationByRegression.class);
					learnerUnit.addOperator(metaLearner, learnerUnit.getIndexOfOperator(learner));

					// connecting meta learner input port
					OutputPort output = getInputPort().getSource();
					toUnlock.add(output);
					output.lock();
					output.disconnect();
					output.connectTo(metaLearner.getTrainingSetInputPort());

					// connecting meta learner output port
					if (modelOutput != null) {
						InputPort inputPort = modelOutput.getDestination();
						// connecting meta learner
						if (inputPort != null) {
							toUnlock.add(inputPort);
							inputPort.lock();
							modelOutput.disconnect();
							metaLearner.getModelOutputPort().connectTo(inputPort);
						}
					}

					// moving learner inside meta learner
					learner.remove();
					metaLearner.getSubprocess(0).addOperator(learner);

					// connecting learner input port to meta learner
					metaLearner.getSubprocess(0).getInnerSources().getPortByIndex(0).connectTo(getInputPort());

					// connecting learner output port to meta learner
					if (modelOutput != null) {
						modelOutput.connectTo(metaLearner.getInnerModelSink());
					}

				} catch (OperatorCreationException ex) {
				} finally {
					for (Port port : toUnlock) {
						port.unlock();
					}
				}
			}

			@Override
			public Operator createOperator() throws OperatorCreationException {
				// not needed
				return null;
			}
		};

		return Collections.singletonList(fix);
	}

	/**
	 * This has to return a list of appropriate quick fixes in the case, that only binominal labels
	 * are supported but the data contains polynomials.
	 */
	@Override
	protected Collection<QuickFix> getFixesForPolynomialClassificationWhenBinominalSupported() {
		Operator learner = getInputPort().getPorts().getOwner().getOperator();
		OperatorDescription ods[] = OperatorService.getOperatorDescriptions(Binary2MultiClassLearner.class);
		String name = null;
		if (ods.length > 0) {
			name = ods[0].getName();
		}
		QuickFix fix = new OperatorInsertionQuickFix("insert_binominal_to_multiclass_learner",
				new Object[] { name, learner.getOperatorDescription().getName() }, 8, getInputPort()) {

			@SuppressWarnings("deprecation")
			@Override
			public void apply() {
				List<Port> toUnlock = new LinkedList<Port>();
				try {
					Operator learner = getInputPort().getPorts().getOwner().getOperator();
					ExecutionUnit learnerUnit = learner.getExecutionUnit();

					// searching for model outport
					OutputPort modelOutput = null;
					MetaData modelMetaData = new PredictionModelMetaData(PredictionModel.class, new ExampleSetMetaData());
					for (OutputPort port : learner.getOutputPorts().getAllPorts()) {
						MetaData data = port.getMetaData();
						if (modelMetaData.isCompatible(data, CompatibilityLevel.VERSION_5)) {
							modelOutput = port;
							toUnlock.add(modelOutput);
							modelOutput.lock();
							break;
						}
					}

					Binary2MultiClassLearner metaLearner = OperatorService.createOperator(Binary2MultiClassLearner.class);
					learnerUnit.addOperator(metaLearner, learnerUnit.getIndexOfOperator(learner));

					// connecting meta learner input port
					OutputPort output = getInputPort().getSource();
					toUnlock.add(output);
					output.lock();
					output.disconnect();
					output.connectTo(metaLearner.getTrainingSetInputPort());

					// connecting meta learner output port
					if (modelOutput != null) {
						InputPort inputPort = modelOutput.getDestination();
						// connecting meta learner
						if (inputPort != null) {
							toUnlock.add(inputPort);
							inputPort.lock();
							modelOutput.disconnect();
							metaLearner.getModelOutputPort().connectTo(inputPort);
						}
					}

					// moving learner inside meta learner
					learner.remove();
					metaLearner.getSubprocess(0).addOperator(learner);

					// connecting learner input port to meta learner
					metaLearner.getSubprocess(0).getInnerSources().getPortByIndex(0).connectTo(getInputPort());

					// connecting learner output port to meta learner
					if (modelOutput != null) {
						modelOutput.connectTo(metaLearner.getInnerModelSink());
					}

				} catch (OperatorCreationException ex) {
				} finally {
					for (Port port : toUnlock) {
						port.unlock();
					}
				}
			}

			@Override
			public Operator createOperator() throws OperatorCreationException {
				// not needed
				return null;
			}
		};
		return Collections.singletonList(fix);
	}

}

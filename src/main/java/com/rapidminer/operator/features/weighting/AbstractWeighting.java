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
package com.rapidminer.operator.features.weighting;

import java.util.List;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.learner.CapabilityCheck;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.Tools;


/**
 * This is an abstract superclass for RapidMiner weighting operators. New weighting schemes should
 * extend this class to support the same normalization parameter as other weighting operators.
 *
 * @author Helge Homburg
 */
public abstract class AbstractWeighting extends Operator implements CapabilityProvider {

	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort weightsOutput = getOutputPorts().createPort("weights");
	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set");
	private boolean checkForLabel;

	private static final String[] SORT_DIRECTIONS = new String[] { "ascending", "descending" };

	public static final int SORT_ASCENDING = 0;
	public static final int SORT_DESCENDING = 1;

	/** The parameter name for &quot;Activates the normalization of all weights.&quot; */
	public static final String PARAMETER_NORMALIZE_WEIGHTS = "normalize_weights";
	public static final String PARAMETER_SORT_WEIGHTS = "sort_weights";
	public static final String PARAMETER_SORT_DIRECTION = "sort_direction";

	/**
	 * Constructs a full AbstractWeighting-Operator but this one will not check for the presence of
	 * a label in the ExampleSet (and ExampleSetMetaData). If you want the AbstractWeighting to do
	 * so,please use {@link #AbstractWeighting(OperatorDescription, boolean)}.
	 *
	 * @param description
	 */
	public AbstractWeighting(OperatorDescription description) {
		this(description, false);
	}

	/**
	 *
	 * @param description
	 *            description of the Operator
	 * @param checkForLabel
	 *            if no label exist, the operator throws an UserError and shows a MetaData warning
	 */
	public AbstractWeighting(OperatorDescription description, boolean checkForLabel) {
		super(description);
		if (isExampleSetMandatory()) {
			exampleSetInput.addPrecondition(new CapabilityPrecondition(this, exampleSetInput));
		}
		this.checkForLabel = checkForLabel;

		getTransformer().addRule(new GenerateNewMDRule(weightsOutput, AttributeWeights.class));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) throws UndefinedParameterError {
				boolean normalizedWeights = getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS);
				for (AttributeMetaData amd : metaData.getAllAttributes()) {
					if (!amd.isSpecial() && amd.isNumerical()) {
						if (normalizedWeights) {
							amd.setValueSetRelation(SetRelation.SUBSET);
						} else {
							amd.setValueSetRelation(SetRelation.UNKNOWN);
						}
					}
				}
				if (AbstractWeighting.this.checkForLabel
						&& metaData.containsSpecialAttribute(Attributes.LABEL_NAME) != MetaDataInfo.YES) {
					addError(new SimpleMetaDataError(Severity.WARNING, exampleSetInput, "missing_role", "label"));
				}
				return super.modifyExampleSet(metaData);
			}
		});
	}

	protected abstract AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException;

	/**
	 * Helper method for anonymous instances of this class.
	 */
	public AttributeWeights doWork(ExampleSet exampleSet) throws OperatorException {
		exampleSetInput.receive(exampleSet);

		// check capabilities and produce errors if they are not fulfilled
		CapabilityCheck check = new CapabilityCheck(this, Tools.booleanValue(
				ParameterService.getParameterValue(PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN), true)
				|| onlyWarnForNonSufficientCapabilities());
		check.checkLearnerCapabilities(this, exampleSet);

		doWork();
		return weightsOutput.getData(AttributeWeights.class);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = isExampleSetMandatory() ? exampleSetInput.getData(ExampleSet.class)
				: exampleSetInput.<ExampleSet>getDataOrNull(ExampleSet.class);
		if (checkForLabel) {
			com.rapidminer.example.Tools.isLabelled(exampleSet);
		}
		AttributeWeights weights = calculateWeights(exampleSet);
		if (getParameterAsBoolean(PARAMETER_NORMALIZE_WEIGHTS)) {
			weights.normalize();
		}
		if (getParameterAsBoolean(PARAMETER_SORT_WEIGHTS)) {
			weights.sort(getParameterAsInt(PARAMETER_SORT_DIRECTION) == SORT_ASCENDING ? AttributeWeights.INCREASING
					: AttributeWeights.DECREASING, AttributeWeights.ORIGINAL_WEIGHTS);
		}
		exampleSetOutput.deliver(exampleSet);
		weightsOutput.deliver(weights);
	}

	public InputPort getExampleSetInputPort() {
		return exampleSetInput;
	}

	public OutputPort getWeightsOutputPort() {
		return weightsOutput;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> list = super.getParameterTypes();
		list.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_WEIGHTS, "Activates the normalization of all weights.", false,
				false));
		list.add(new ParameterTypeBoolean(PARAMETER_SORT_WEIGHTS, "If activated the weights will be returned sorted.", true,
				false));
		ParameterType type = new ParameterTypeCategory(PARAMETER_SORT_DIRECTION, "Defines the sorting direction.",
				SORT_DIRECTIONS, 0);
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_SORT_WEIGHTS, true, true));
		list.add(type);
		return list;
	}

	protected boolean isExampleSetMandatory() {
		return true;
	}

	protected boolean onlyWarnForNonSufficientCapabilities() {
		return false;
	}
}

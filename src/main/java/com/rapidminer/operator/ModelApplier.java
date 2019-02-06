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
package com.rapidminer.operator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ModelApplicationRule;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.operator.ports.metadata.PassThroughRule;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * This operator applies a {@link Model} to an {@link ExampleSet}. All parameters of the training
 * process should be stored within the model. However, this operator is able to take any parameters
 * for the rare case that the particular model evaluates parameters during application. Models can
 * be read from a file by using a {@link com.rapidminer.extension.legacy.operator.io.ModelLoader}.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class ModelApplier extends Operator {

	/** The parameter name for &quot;key&quot; */
	public static final String PARAMETER_KEY = "key";

	/** The parameter name for &quot;value&quot; */
	public static final String PARAMETER_VALUE = "value";

	/** The possible parameters used by the model during application time. */
	public static final String PARAMETER_APPLICATION_PARAMETERS = "application_parameters";

	/** Indicates if preprocessing models should create a view instead of changing the data. */
	private static final String PARAMETER_CREATE_VIEW = "create_view";

	/** Last version to silently log unsupported parameters. */
	private static final OperatorVersion VERSION_ERROR_UNSUPPORTED_PARAMETER = new OperatorVersion(7, 1, 1);

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] changes = super.getIncompatibleVersionChanges();
		changes = Arrays.copyOf(changes, changes.length + 1);
		changes[changes.length - 1] = VERSION_ERROR_UNSUPPORTED_PARAMETER;
		return changes;
	}

	private final InputPort modelInput = getInputPorts().createPort("model");
	private final InputPort exampleSetInput = getInputPorts().createPort("unlabelled data");
	private final OutputPort exampleSetOutput = getOutputPorts().createPort("labelled data");
	private final OutputPort modelOutput = getOutputPorts().createPort("model");

	public ModelApplier(OperatorDescription description) {
		super(description);
		modelInput.addPrecondition(
				new SimplePrecondition(modelInput, new ModelMetaData(Model.class, new ExampleSetMetaData())));
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new ExampleSetMetaData()));
		getTransformer().addRule(new ModelApplicationRule(exampleSetInput, exampleSetOutput, modelInput, false));
		getTransformer().addRule(new PassThroughRule(modelInput, modelOutput, false));
	}

	/**
	 * Applies the operator and labels the {@link ExampleSet}. The example set in the input is not
	 * consumed.
	 */
	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputExampleSet = exampleSetInput.getData(ExampleSet.class);
		Model model = modelInput.getData(Model.class);
		if (AbstractModel.class.isAssignableFrom(model.getClass())) {
			((AbstractModel) model).setOperator(this);
			((AbstractModel) model).setShowProgress(true);
		}

		log("Set parameters for " + model.getClass().getName());
		List<String[]> modelParameters = getParameterList(PARAMETER_APPLICATION_PARAMETERS);
		Iterator<String[]> i = modelParameters.iterator();
		while (i.hasNext()) {
			String[] parameter = i.next();
			try {
				model.setParameter(parameter[0], parameter[1]);
			} catch (UnsupportedApplicationParameterError e) {
				if (getCompatibilityLevel().isAtMost(VERSION_ERROR_UNSUPPORTED_PARAMETER)) {
					log("The learned model does not support parameter");
				} else {
					e.setOperator(this);
					throw e;
				}
			}
		}

		// handling PreprocessingModels: extra treatment for views
		if (getParameterAsBoolean(PARAMETER_CREATE_VIEW)) {
			try {
				model.setParameter(PreprocessingOperator.PARAMETER_CREATE_VIEW, true);
			} catch (UnsupportedApplicationParameterError e) {
				if (getCompatibilityLevel().isAtMost(VERSION_ERROR_UNSUPPORTED_PARAMETER)) {
					log("The learned model does not have a view to create");
				} else {
					e.setOperator(this);
					throw e;
				}
			}
		}

		log("Applying " + model.getClass().getName());
		ExampleSet result = inputExampleSet;
		try {
			result = model.apply(inputExampleSet);
		} catch (UserError e) {
			if (e.getOperator() == null) {
				e.setOperator(this);
			}
			throw e;
		}

		if (AbstractModel.class.isAssignableFrom(model.getClass())) {
			((AbstractModel) model).setOperator(null);
			((AbstractModel) model).setShowProgress(false);
		}

		exampleSetOutput.deliver(result);
		modelOutput.deliver(model);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort port) {
		if (port == modelOutput) {
			return getParameterAsBoolean("keep_model");
		} else {
			return super.shouldAutoConnect(port);
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeList(PARAMETER_APPLICATION_PARAMETERS,
				"Model parameters for application (usually not needed).",
				new ParameterTypeString(PARAMETER_KEY, "The model parameter key."),
				new ParameterTypeString(PARAMETER_VALUE, "This key's value")));
		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_VIEW,
				"Indicates that models should create a new view on the data where possible. Then, instead of changing the data itself, the results are calculated on the fly if needed.",
				false));
		return types;
	}
}

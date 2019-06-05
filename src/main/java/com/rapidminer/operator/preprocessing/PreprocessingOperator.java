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
package com.rapidminer.operator.preprocessing;

import java.util.Collection;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.NonSpecialAttributesExampleSet;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.GenerateModelTransformationRule;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ProcessTools;
import com.rapidminer.tools.container.Pair;


/**
 * Superclass for all preprocessing operators. Classes which extend this class must implement the
 * method {@link #createPreprocessingModel(ExampleSet)} . This method can also be returned by this
 * operator and will be combined with other models.
 *
 * @author Ingo Mierswa
 */
public abstract class PreprocessingOperator extends AbstractDataProcessing {

	/**
	 * the name of the preprocessing model output port
	 *
	 * @since 8.2.0
	 */
	public static final String PREPROCESSING_MODEL_OUTPUT_PORT_NAME = "preprocessing model";

	/**
	 * The parameter name for &quot;Indicates if the preprocessing model should also be
	 * returned&quot;
	 */
	public static final String PARAMETER_RETURN_PREPROCESSING_MODEL = "return_preprocessing_model";

	/**
	 * Indicates if this operator should create a view (new example set on the view stack) instead
	 * of directly changing the data.
	 */
	public static final String PARAMETER_CREATE_VIEW = "create_view";

	private final OutputPort modelOutput = getOutputPorts().createPort(PREPROCESSING_MODEL_OUTPUT_PORT_NAME);

	protected final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, getExampleSetInputPort(),
			getFilterValueTypes());


	public PreprocessingOperator(OperatorDescription description) {
		super(description);
		getTransformer().addRule(
				new GenerateModelTransformationRule(getExampleSetInputPort(), modelOutput, getPreprocessingModelClass()));
		getExampleSetInputPort().addPrecondition(attributeSelector.makePrecondition());
	}

	/**
	 * Subclasses might override this method to define the meta data transformation performed by
	 * this operator. The default implementation takes all attributes specified by the
	 * {@link AttributeSubsetSelector} and passes them to
	 * {@link #modifyAttributeMetaData(ExampleSetMetaData, AttributeMetaData)} and replaces them
	 * accordingly.
	 *
	 * @throws UndefinedParameterError
	 */
	@Override
	protected ExampleSetMetaData modifyMetaData(ExampleSetMetaData exampleSetMetaData) throws UndefinedParameterError {
		ExampleSetMetaData subsetMetaData = attributeSelector.getMetaDataSubset(exampleSetMetaData,
				isSupportingAttributeRoles());
		checkSelectedSubsetMetaData(subsetMetaData);
		for (AttributeMetaData amd : subsetMetaData.getAllAttributes()) {
			Collection<AttributeMetaData> replacement = null;
			replacement = modifyAttributeMetaData(exampleSetMetaData, amd);
			if (replacement != null) {
				if (replacement.size() == 1) {
					AttributeMetaData replacementAttribute = replacement.iterator().next();
					if(replacementAttribute.getName().equals(amd.getName())){
						// In this case, the variable most likely remained the same. Therefore, we preserve its role.
						replacementAttribute.setRole(exampleSetMetaData.getAttributeByName(amd.getName()).getRole());
					}
				}
				exampleSetMetaData.removeAttribute(amd);
				exampleSetMetaData.addAllAttributes(replacement);
			}
		}
		return exampleSetMetaData;
	}

	/** Can be overridden to check the selected attributes for compatibility. */
	protected void checkSelectedSubsetMetaData(ExampleSetMetaData subsetMetaData) {}

	/**
	 * If this preprocessing operator generates new attributes, the corresponding meta data should
	 * be returned by this method. The attribute will be replaced by the collection. If this
	 * operator modifies a single one, amd itself should be modified as a side effect and null
	 * should be returned. Note: If an empty collection is returned, amd will be removed, but no new
	 * attribute will be added.
	 **/
	protected abstract Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError;

	public abstract PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException;

	/**
	 * This method allows subclasses to easily get a collection of the affected attributes.
	 *
	 * @throws UndefinedParameterError
	 * @throws UserError
	 */
	protected final ExampleSet getSelectedAttributes(ExampleSet exampleSet) throws UndefinedParameterError, UserError {
		return attributeSelector.getSubset(exampleSet, isSupportingAttributeRoles());
	}

	@Override
	public final ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		ExampleSet workingSet = isSupportingAttributeRoles() ? getSelectedAttributes(exampleSet)
				: NonSpecialAttributesExampleSet.create(getSelectedAttributes(exampleSet));

		AbstractModel model = createPreprocessingModel(workingSet);
		model.setParameter(PARAMETER_CREATE_VIEW, getParameterAsBoolean(PARAMETER_CREATE_VIEW));
		if (getExampleSetOutputPort().isConnected()) {
			model.setOperator(this);
			model.setShowProgress(true);
			exampleSet = model.apply(exampleSet);
			model.setOperator(null);
			model.setShowProgress(false);
		}

		modelOutput.deliver(model);
		return exampleSet;
	}

	/**
	 * Helper wrapper for {@link #exampleSetInput that can be called by other operators to apply
	 * this operator when it is created anonymously.
	 */
	public ExampleSet doWork(ExampleSet exampleSet) throws OperatorException {
		ExampleSet workingSet = isSupportingAttributeRoles() ? getSelectedAttributes(exampleSet)
				: NonSpecialAttributesExampleSet.create(getSelectedAttributes(exampleSet));

		AbstractModel model = createPreprocessingModel(workingSet);
		model.setParameter(PARAMETER_CREATE_VIEW, getParameterAsBoolean(PARAMETER_CREATE_VIEW));
		model.setOperator(this);
		return model.apply(exampleSet);
	}

	public Pair<ExampleSet, Model> doWorkModel(ExampleSet exampleSet) throws OperatorException {
		exampleSet = apply(exampleSet);
		Model model = modelOutput.getData(Model.class);
		return new Pair<>(exampleSet, model);

	}

	/**
	 * If a {@link PreprocessingOperator} returns a {@link PreprocessingModel}, the model is
	 * responsible for preventing data corruption. Therefore,
	 * {@link PreprocessingModel#writesIntoExistingData()} should be adjusted for the associated
	 * model and this method should be overwritten to return {@code false}.
	 */
	@Override
	public boolean writesIntoExistingData() {
		return !getParameterAsBoolean(PARAMETER_CREATE_VIEW);
	}

	@Override
	public boolean shouldAutoConnect(OutputPort outputPort) {
		if (outputPort == modelOutput) {
			return getParameterAsBoolean(PARAMETER_RETURN_PREPROCESSING_MODEL);
		} else {
			return super.shouldAutoConnect(outputPort);
		}
	}

	/**
	 * Defines the value types of the attributes which are processed or affected by this operator.
	 * Has to be overridden to restrict the attributes which can be chosen by an
	 * {@link AttributeSubsetSelector}.
	 *
	 * @return array of value types
	 */
	protected abstract int[] getFilterValueTypes();

	public abstract Class<? extends PreprocessingModel> getPreprocessingModelClass();

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterType type = new ParameterTypeBoolean(PARAMETER_RETURN_PREPROCESSING_MODEL,
				"Indicates if the preprocessing model should also be returned", false);
		type.setHidden(true);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_CREATE_VIEW,
				"Create View to apply preprocessing instead of changing the data", false);
		type.setHidden(!isSupportingView());
		types.add(type);

		types.addAll(ProcessTools.setSubsetSelectorPrimaryParameter(attributeSelector.getParameterTypes(), true));
		return types;
	}

	/**
	 * Subclasses which need to have the attribute roles must return true. Otherwise all selected
	 * attributes are converted into regular and afterwards given their old roles.
	 */
	public boolean isSupportingAttributeRoles() {
		return false;
	}

	/**
	 * Subclasses might overwrite this in order to hide the create_view parameter
	 *
	 * @return
	 */
	public boolean isSupportingView() {
		return true;
	}

	public OutputPort getPreprocessingModelOutputPort() {
		return modelOutput;
	}
}

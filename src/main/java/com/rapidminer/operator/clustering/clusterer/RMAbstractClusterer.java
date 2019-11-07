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
package com.rapidminer.operator.clustering.clusterer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.CapabilityPrecondition;
import com.rapidminer.operator.ports.metadata.DistanceMeasurePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;


/**
 * Adds a parameter by which the user can choose to generate a cluster attribute. All clusterers
 * other than the Weka clusterers should extends this class rather than AbstractClusterer directly.
 *
 * @author Simon Fischer
 *
 */
public abstract class RMAbstractClusterer extends AbstractClusterer implements CapabilityProvider {

	/**
	 * The parameter name for &quot;Indicates if a cluster id is generated as new special
	 * attribute.&quot;
	 */
	public static final String PARAMETER_ADD_CLUSTER_ATTRIBUTE = "add_cluster_attribute";
	public static final String PARAMETER_REMOVE_UNLABELED = "remove_unlabeled";
	public static final String PARAMETER_ADD_AS_LABEL = "add_as_label";

	private DistanceMeasureHelper measureHelper = null;
	private DistanceMeasure presetMeasure = null;
	private static final OperatorVersion BEFORE_LABEL_CLUSTER_FIX = BEFORE_EMPTY_CHECKS;

	public RMAbstractClusterer(OperatorDescription description) {
		super(description);
		InputPort exampleInput = getExampleSetInputPort();
		if (usesDistanceMeasures()) {
			exampleInput.addPrecondition(new DistanceMeasurePrecondition(exampleInput, this));
			measureHelper = new DistanceMeasureHelper(this);
		}
		exampleInput.addPrecondition(new CapabilityPrecondition(this, exampleInput));
	}

	@Override
	protected boolean addsClusterAttribute() {
		return getParameterAsBoolean(PARAMETER_ADD_CLUSTER_ATTRIBUTE);
	}

	@Override
	protected boolean addsLabelAttribute() {
		return getParameterAsBoolean(PARAMETER_ADD_AS_LABEL);
	}

	@Override
	protected void additionalChecks(ExampleSet exampleSet) throws OperatorException {
		super.additionalChecks(exampleSet);
		checkNoNfiniteValues(exampleSet);
	}

	/**
	 * Checks the given {@link ExampleSet} for non-finite values and adds a
	 * {@link ProcessSetupError} to this operator if infinite values are detected. Will throw a
	 * {@link UserError} if this clusterer can not handle infinite values
	 *
	 * @since 7.6
	 */
	protected void checkNoNfiniteValues(ExampleSet exampleSet) throws OperatorException {
		try {
			Tools.onlyFiniteValues(exampleSet, this.getOperatorClassName(), this, new String[0]);
		} catch (UserError ue) {
			boolean isInfinite = Tools.INFINITE_VALUES.equals(ue.getErrorIdentifier());
			if (isInfinite) {
				this.addError(new SimpleProcessSetupError(handlesInfiniteValues() ? Severity.WARNING : Severity.ERROR,
						getPortOwner(), "exampleset.contains_infinite_values", getOperatorClassName()));
			}
			if (!isInfinite || !handlesInfiniteValues()) {
				throw ue;
			}
		}
	}

	/**
	 * Indicates whether this clusterer can handle infinite values, meaning if there is no exception
	 * thrown during processing.
	 *
	 * @since 7.6
	 */
	protected boolean handlesInfiniteValues() {
		return true;
	}

	/**
	 * Creates and adds the cluster attribute to the given example set and sets the cluster for each
	 * example.
	 *
	 * @param exampleSet
	 *            the example set
	 * @param assignments
	 *            the cluster assignments by example ID
	 * @see #addClusterAttribute(ExampleSet)
	 * @since 7.6
	 */
	protected void addClusterAssignments(ExampleSet exampleSet, int[] assignments) {
		Attribute targetAttribute = addClusterAttribute(exampleSet);
		int i = 0;
		for (Example example : exampleSet) {
			example.setValue(targetAttribute, "cluster_" + assignments[i++]);
		}
	}

	/**
	 * Adds a cluster or label attribute to the given example set and returns this attribute.
	 *
	 * @since 7.6
	 */
	protected Attribute addClusterAttribute(ExampleSet exampleSet) {
		boolean asLabel = addsLabelAttribute() && operatorCanAddLabel();
		Attribute targetAttribute = AttributeFactory
				.createAttribute(asLabel ? Attributes.LABEL_NAME : Attributes.CLUSTER_NAME, Ontology.NOMINAL);
		exampleSet.getExampleTable().addAttribute(targetAttribute);
		if (asLabel) {
			exampleSet.getAttributes().setLabel(targetAttribute);
		} else {
			exampleSet.getAttributes().setCluster(targetAttribute);
		}
		return targetAttribute;
	}

	/**
	 * Indicates whether the operator's compatibility level fixes a previous bug where the behavior
	 * of the operator differs from that of the created model.
	 *
	 * @since 7.6
	 */
	protected boolean operatorCanAddLabel() {
		return !affectedByLabelFix() || getCompatibilityLevel().isAbove(BEFORE_LABEL_CLUSTER_FIX);
	}

	@Override
	protected boolean addsIdAttribute() {
		return true;
	}

	/**
	 * Indicates whether the cluster algorithm uses a {@link DistanceMeasure} for computation.
	 * Defaults to {@code false}. Subclasses should override this method if they depend on measures.
	 *
	 * @since 7.6
	 */
	protected boolean usesDistanceMeasures() {
		return false;
	}

	/**
	 * Indicates whether the cluster algorithm can be provided with a preset
	 * {@link DistanceMeasure}. Defaults to {@code false}. Subclasses should override this method if
	 * they intend to allow external provision of measures.
	 *
	 * @since 7.6
	 */
	protected boolean usesPresetMeasure() {
		return false;
	}

	/**
	 * Returns an initialized measure if the cluster algorithm is {@link DistanceMeasure} based
	 * otherwise returns {@code null}. If a preset measure is set (and allowed), this preset measure
	 * will be initialized and returned. Subclasses should override {@link #usesDistanceMeasures()}
	 * and/or {@link #usesPresetMeasure()} to change behavior.
	 *
	 * @param exampleSet
	 *            the example set to initialize the measure with
	 * @return an initialized measure or {@code null}
	 * @throws OperatorException
	 *             if the initialization of a measure fails
	 * @since 7.6
	 */
	protected DistanceMeasure getInitializedMeasure(ExampleSet exampleSet) throws OperatorException {
		if (!usesDistanceMeasures()) {
			// not initialized if operator does not use distance measures
			return null;
		}
		if (presetMeasure != null) {
			presetMeasure.init(exampleSet);
			return presetMeasure;
		}
		return measureHelper.getInitializedMeasure(exampleSet);
	}

	/**
	 * Sets the preset measure to the specified {@link DistanceMeasure} if the cluster algorithm
	 * allows it, otherwise does nothing. Subclasses should override {@link #usesPresetMeasure()} if
	 * they want to allow preset measures and also may consider making the
	 * {@link #setPresetMeasure(DistanceMeasure)} method public.
	 *
	 * @param measure
	 *            the measure to be used as the preset
	 * @since 7.6
	 */
	protected void setPresetMeasure(DistanceMeasure measure) {
		if (!usesPresetMeasure()) {
			return;
		}
		presetMeasure = measure;
	}

	/**
	 * The default capabilities for clusterers. Allows by default any kind of label (none, numerical
	 * or nominal) and does not support weighted examples, missing values, formula provider or
	 * updatable. Allows nominal/numerical values depending on
	 * {@link #supportsNominalValues()}/{@link #supportsNumericalValues()}.
	 */
	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case BINOMINAL_ATTRIBUTES:
			case POLYNOMINAL_ATTRIBUTES:
				return supportsNominalValues();
			case NUMERICAL_ATTRIBUTES:
				return supportsNumericalValues();
			case NO_LABEL:
			case NUMERICAL_LABEL:
			case BINOMINAL_LABEL:
			case ONE_CLASS_LABEL:
			case POLYNOMINAL_LABEL:
				return true;
			case WEIGHTED_EXAMPLES:
			case MISSING_VALUES:
			case FORMULA_PROVIDER:
			case UPDATABLE:
			default:
				return false;

		}
	}

	/**
	 * Indicates whether the cluster algorithm supports nominal values in general. Defaults to
	 * {@link #usesDistanceMeasures()} and does not depend on the currently selected measure.
	 * Subclasses should override this method if they want to change the capabilities for nominal
	 * values.
	 *
	 * @see #supportsCapability(OperatorCapability)
	 * @since 7.6
	 */
	protected boolean supportsNominalValues() {
		return usesDistanceMeasures();
	}

	/**
	 * Indicates whether the cluster algorithm supports numerical values in general. Defaults to
	 * {@code true}. Subclasses should override this method if they want to change the capabilities
	 * for numerical values.
	 *
	 * @see #supportsCapability(OperatorCapability)
	 * @since 7.6
	 */
	protected boolean supportsNumericalValues() {
		return true;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeBoolean(PARAMETER_ADD_CLUSTER_ATTRIBUTE,
				"If enabled, a cluster id is generated as new special attribute directly in this operator, otherwise this operator does not add an id attribute. In the latter case you have to use the Apply Model operator to generate the cluster attribute.",
				true, false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_ADD_AS_LABEL,
				"If true, the cluster id is stored in an attribute with the special role 'label' instead of 'cluster'.",
				false);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_REMOVE_UNLABELED, "Delete the unlabeled examples.", false);
		type.setExpert(false);
		types.add(type);

		return types;
	}

	/**
	 * Returns the list of parameter types related to {@link DistanceMeasure}. If there are defaults
	 * defined in {@link #getMeasureParametersDefaults()}, these defaults will be applied.
	 * Subclasses should at some point add these parameters in their respective
	 * {@link #getParameterTypes()} method.
	 *
	 * @since 7.6
	 * @see DistanceMeasures#getParameterTypes(Operator)
	 */
	protected List<ParameterType> getMeasureParameterTypes() {
		List<ParameterType> types = DistanceMeasures.getParameterTypes(this);
		Map<String, Object> defaults = getMeasureParametersDefaults();
		if (defaults != null) {
			for (ParameterType type : types) {
				Object defaultValue = defaults.get(type.getKey());
				if (defaultValue != null) {
					type.setDefaultValue(defaultValue);
				}
			}
		}
		return types;
	}

	/**
	 * Returns a map of parameter keys to default values if this clusterer prefers a specific
	 * {@link DistanceMeasure} type and instance. Subclasses should override this method to provide
	 * their own defaults if necessary.
	 *
	 * @since 7.6
	 */
	protected Map<String, Object> getMeasureParametersDefaults() {
		return null;
	}

	/**
	 * Indicates whether the implementation was affected by the label fix connected to
	 * {@link #BEFORE_LABEL_CLUSTER_FIX}.
	 *
	 * @since 7.6
	 */
	protected boolean affectedByLabelFix() {
		return true;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		// no comp level or already taken care of
		if (!affectedByLabelFix() || affectedByEmptyCheck()) {
			return old;
		}
		OperatorVersion[] versions = Arrays.copyOf(old, old.length + 1);
		versions[old.length] = BEFORE_EMPTY_CHECKS;
		return versions;
	}

}

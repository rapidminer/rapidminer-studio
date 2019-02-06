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
package com.rapidminer.operator.preprocessing.normalization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.annotation.ResourceConsumptionEstimator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.preprocessing.PreprocessingOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorResourceConsumptionHandler;


/**
 * This operator performs a normalization. This can be done between a user defined minimum and
 * maximum value or by a z-transformation, i.e. on mean 0 and variance 1. or by a proportional
 * transformation as proportion of the total sum of the respective attribute.
 *
 * @author Ingo Mierswa, Sebastian Land
 */
public class Normalization extends PreprocessingOperator {

	public static final OperatorVersion BEFORE_NON_FINITE_VALUES_HANDLING = new OperatorVersion(7, 5, 3);

	private static final ArrayList<NormalizationMethod> METHODS = new ArrayList<NormalizationMethod>();

	static {
		registerNormalizationMethod(new ZTransformationNormalizationMethod());
		registerNormalizationMethod(new RangeNormalizationMethod());
		registerNormalizationMethod(new ProportionNormalizationMethod());
		registerNormalizationMethod(new IQRNormalizationMethod());
	}

	/**
	 * This must not be modified outside this class!
	 */
	public static String[] NORMALIZATION_METHODS;

	public static final int METHOD_Z_TRANSFORMATION = 0;

	public static final int METHOD_RANGE_TRANSFORMATION = 1;

	public static final int METHOD_PROPORTION_TRANSFORMATION = 2;

	public static final String PARAMETER_NORMALIZATION_METHOD = "method";

	/** Creates a new Normalization operator. */
	public Normalization(OperatorDescription description) {
		super(description);
	}

	@Override
	protected Collection<AttributeMetaData> modifyAttributeMetaData(ExampleSetMetaData emd, AttributeMetaData amd)
			throws UndefinedParameterError {
		if (amd.isNumerical()) {
			amd.setType(Ontology.REAL);
			int method = getParameterAsInt(PARAMETER_NORMALIZATION_METHOD);
			NormalizationMethod normalizationMethod = METHODS.get(method);
			return normalizationMethod.modifyAttributeMetaData(emd, amd, getExampleSetInputPort(), this);
		}
		return Collections.singleton(amd);
	}

	@Override
	public PreprocessingModel createPreprocessingModel(ExampleSet exampleSet) throws OperatorException {
		int method = getParameterAsInt(PARAMETER_NORMALIZATION_METHOD);
		NormalizationMethod normalizationMethod = METHODS.get(method);
		normalizationMethod.init();
		return normalizationMethod.getNormalizationModel(exampleSet, this);
	}

	@Override
	public Class<? extends PreprocessingModel> getPreprocessingModelClass() {
		return AbstractNormalizationModel.class;
	}

	/** Returns a list with all parameter types of this model. */
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_NORMALIZATION_METHOD, "Select the normalization method.",
				NORMALIZATION_METHODS, 0));
		int i = 0;
		for (NormalizationMethod method : METHODS) {
			for (ParameterType type : method.getParameterTypes(this)) {
				type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_NORMALIZATION_METHOD,
						NORMALIZATION_METHODS, true, new int[] { i }));
				types.add(type);
			}
			i++;
		}
		return types;
	}

	@Override
	protected int[] getFilterValueTypes() {
		return new int[] { Ontology.NUMERICAL };
	}

	@Override
	public boolean writesIntoExistingData() {
		return false;
	}

	@Override
	public ResourceConsumptionEstimator getResourceConsumptionEstimator() {
		return OperatorResourceConsumptionHandler.getResourceConsumptionEstimator(getInputPort(), Normalization.class,
				attributeSelector);
	}

	/**
	 * This method can be used for registering additional normalization methods.
	 */
	public static void registerNormalizationMethod(NormalizationMethod newMethod) {
		METHODS.add(newMethod);
		NORMALIZATION_METHODS = new String[METHODS.size()];
		int i = 0;
		for (NormalizationMethod method : METHODS) {
			NORMALIZATION_METHODS[i] = method.getName();
			i++;
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		OperatorVersion[] old = super.getIncompatibleVersionChanges();
		Set<OperatorVersion> allVersions = new HashSet<>();
		for (OperatorVersion ov : old) {
			allVersions.add(ov);
		}
		allVersions.add(BEFORE_NON_FINITE_VALUES_HANDLING);
		for (NormalizationMethod method : METHODS) {
			for (OperatorVersion version : method.getIncompatibleVersionChanges()) {
				allVersions.add(version);
			}
		}
		return allVersions.toArray(new OperatorVersion[allVersions.size()]);
	}

}

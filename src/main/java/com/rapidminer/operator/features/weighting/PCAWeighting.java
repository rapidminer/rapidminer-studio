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

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.example.AttributeWeights;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.transformation.PCA;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.OperatorService;


/**
 * Uses the factors of one of the principal components (default is the first) as feature weights.
 * Please note that the PCA weighting operator is currently the only one which also works on data
 * sets without a label, i.e. for unsupervised learning.
 * 
 * @author Ingo Mierswa
 * 
 */
public class PCAWeighting extends AbstractWeighting {

	public PCAWeighting(OperatorDescription description) {
		super(description, false);
	}

	@Override
	protected AttributeWeights calculateWeights(ExampleSet exampleSet) throws OperatorException {
		PCA pcaOperator = null;
		try {
			pcaOperator = OperatorService.createOperator(PCA.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "inner pca operator", e.getMessage());
		}
		pcaOperator.setCompatibilityLevel(this.getCompatibilityLevel());
		pcaOperator.setParameter(PCA.PARAMETER_REDUCTION_TYPE, PCA.REDUCTION_NONE + "");

		ComponentWeights weightOperator = null;
		try {
			weightOperator = OperatorService.createOperator(ComponentWeights.class);
		} catch (OperatorCreationException e) {
			throw new UserError(this, 904, "inner weight operator", e.getMessage());
		}
		weightOperator.setParameter(ComponentWeights.PARAMETER_COMPONENT_NUMBER,
				getParameterAsInt(ComponentWeights.PARAMETER_COMPONENT_NUMBER) + "");
		weightOperator.setParameter(AbstractWeighting.PARAMETER_NORMALIZE_WEIGHTS, false + "");
		weightOperator.setParameter(AbstractWeighting.PARAMETER_SORT_WEIGHTS, false + "");

		Model pcaModel = pcaOperator.doWork(exampleSet);
		AttributeWeights result = weightOperator.doWork(pcaModel, exampleSet);

		result.setSource(this.getName());
		return result;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(ComponentWeights.PARAMETER_COMPONENT_NUMBER,
				"Indicates the number of the component from which the weights should be calculated.", 1, Integer.MAX_VALUE,
				1));
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_ATTRIBUTES:
			case NO_LABEL:
				return true;
			default:
				return false;
		}
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		return (OperatorVersion[]) ArrayUtils.add(super.getIncompatibleVersionChanges(), PCA.EIGENVALUE_ALGORITHM_CHANGED);
	}
}

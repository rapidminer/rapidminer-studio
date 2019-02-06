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

import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.AbstractLearner;
import com.rapidminer.operator.learner.PredictionModel;


/**
 * This class is the common super class of all KernelModel producing learners. It returns the
 * appropriate model class for metaData Processing.
 * 
 * @author Sebastian Land
 */
public abstract class AbstractKernelBasedLearner extends AbstractLearner {

	public AbstractKernelBasedLearner(OperatorDescription description) {
		super(description);

	}

	@Override
	public Class<? extends PredictionModel> getModelClass() {
		return KernelModel.class;
	}
}

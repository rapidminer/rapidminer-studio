/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
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

import com.rapidminer.operator.learner.PredictionModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;


/**
 * @author Simon Fischer
 */
public class GeneratePredictionModelTransformationRule implements MDTransformationRule {

	private final OutputPort outputPort;
	private final InputPort exampleSetInput;
	private final Class<? extends PredictionModel> modelClass;

	public GeneratePredictionModelTransformationRule(InputPort exampleSetInput, OutputPort outputPort,
			Class<? extends PredictionModel> modelClass) {
		this.outputPort = outputPort;
		this.exampleSetInput = exampleSetInput;
		this.modelClass = modelClass;
	}

	@Override
	public void transformMD() {
		MetaData input = exampleSetInput.getMetaData();
		PredictionModelMetaData mmd;
		if (input != null && input instanceof ExampleSetMetaData) {
			mmd = new PredictionModelMetaData(modelClass, (ExampleSetMetaData) input);
			mmd.addToHistory(outputPort);
			outputPort.deliverMD(mmd);
			return;
		}
		outputPort.deliverMD(null);
		return;
	}

	/**
	 * @return the {@link OutputPort} the MD rule is for
	 */
	public OutputPort getOutputPort() {
		return outputPort;
	}
}

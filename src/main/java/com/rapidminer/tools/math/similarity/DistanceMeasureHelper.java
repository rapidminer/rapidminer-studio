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
package com.rapidminer.tools.math.similarity;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;


/**
 * This helper class installs and uninstalls additional ports at operators that operate on distance
 * measures. It registers itself as an observer of the operator's parameters and instantiates a new
 * {@link DistanceMeasure} whenever one of the parameters
 * {@link DistanceMeasures#PARAMETER_MEASURE_TYPES},
 * {@link DistanceMeasures#PARAMETER_MIXED_MEASURE},
 * {@link DistanceMeasures#PARAMETER_NUMERICAL_MEASURE},
 * {@link DistanceMeasures#PARAMETER_NOMINAL_MEASURE}, or
 * {@link DistanceMeasures#PARAMETER_DIVERGENCE} changes. If the chosen {@link DistanceMeasure}
 * overrides
 * {@link DistanceMeasure#installAdditionalPorts(com.rapidminer.operator.ports.InputPorts, com.rapidminer.parameter.ParameterHandler)}
 * this may install new ports at the operator.
 *
 * In its {@link Operator#doWork()} method, the operator may call
 * {@link #getInitializedMeasure(ExampleSet)} in order to initialize the distance measure and obtain
 * it. Call this method only once.
 *
 * @author Simon Fischer
 *
 */
public class DistanceMeasureHelper {

	private Operator operator;
	private DistanceMeasure measure;

	public DistanceMeasureHelper(Operator operator) {
		this.operator = operator;
		operator.getParameters().addObserver(new Observer<String>() {

			@Override
			public void update(Observable<String> observable, String arg) {
				if (DistanceMeasures.PARAMETER_MEASURE_TYPES.equals(arg)
						|| DistanceMeasures.PARAMETER_MIXED_MEASURE.equals(arg)
						|| DistanceMeasures.PARAMETER_NUMERICAL_MEASURE.equals(arg)
						|| DistanceMeasures.PARAMETER_NOMINAL_MEASURE.equals(arg)
						|| DistanceMeasures.PARAMETER_DIVERGENCE.equals(arg)) {
					updateMeasure();
				}
			}
		}, false);
		updateMeasure();
	}

	private void updateMeasure() {
		if (measure != null) {
			measure.uninstallAdditionalPorts(operator.getInputPorts());
		}
		try {
			measure = DistanceMeasures.createMeasure(operator);
		} catch (UndefinedParameterError e) {
			operator.getLogger().warning("While updating distance measure: " + e.toString());
		} catch (OperatorException e) {
			operator.getLogger().warning("While updating distance measure: " + e.toString());
		}
		if (measure != null) {
			measure.installAdditionalPorts(operator.getInputPorts(), operator);
		}
	}

	public DistanceMeasure getInitializedMeasure(ExampleSet exampleSet) throws OperatorException {
		updateMeasure();
		measure.init(exampleSet, operator);
		return measure;
	}

	public int getSelectedMeasureType() throws UndefinedParameterError {
		return DistanceMeasures.getSelectedMeasureType(operator);
	}
}

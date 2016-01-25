/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasures;


/**
 * This precondition must be added to the exampleset input port, if a distance measure might be
 * chosen by the user. It will check if the example set contains only compatible values.
 * 
 * @author Sebastian Land
 */
public class DistanceMeasurePrecondition extends AbstractPrecondition {

	private ParameterHandler parameterHandler;

	public DistanceMeasurePrecondition(InputPort inputPort, ParameterHandler handler) {
		super(inputPort);
		this.parameterHandler = handler;
	}

	@Override
	public void check(MetaData metaData) {
		if (metaData instanceof ExampleSetMetaData) {
			ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
			try {
				switch (this.parameterHandler.getParameterAsInt(DistanceMeasures.PARAMETER_MEASURE_TYPES)) {
					case DistanceMeasures.NOMINAL_MEASURES_TYPE:
						if (!containsOnlyType(emd, Ontology.NOMINAL, false)) {
							createError(Severity.ERROR, "measures.nominal",
									parameterHandler.getParameterAsString(DistanceMeasures.PARAMETER_NOMINAL_MEASURE));
						}
						break;
					case DistanceMeasures.NUMERICAL_MEASURES_TYPE:
						if (!containsOnlyType(emd, Ontology.NUMERICAL, false)) {
							createError(Severity.ERROR, "measures.numerical",
									parameterHandler.getParameterAsString(DistanceMeasures.PARAMETER_NUMERICAL_MEASURE));
						}
						break;
					case DistanceMeasures.DIVERGENCES_TYPE:
						if (!containsOnlyType(emd, Ontology.NUMERICAL, false)) {
							createError(Severity.ERROR, "measures.numerical",
									parameterHandler.getParameterAsString(DistanceMeasures.PARAMETER_DIVERGENCE));
						}
						break;
				}
			} catch (UndefinedParameterError e) {
			}
		}
	}

	private boolean containsOnlyType(ExampleSetMetaData emd, int type, boolean includeSpecial) {
		for (AttributeMetaData amd : emd.getAllAttributes()) {
			if (!amd.isSpecial() || includeSpecial) {
				if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(amd.getValueType(), type)) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void assumeSatisfied() {
		getInputPort().receiveMD(new ExampleSetMetaData());
	}

	@Override
	public String getDescription() {
		return "<em>expects:</em> ExampleSet";
	}

	@Override
	public boolean isCompatible(MetaData input, CompatibilityLevel level) {
		return ExampleSet.class.isAssignableFrom(input.getObjectClass());
	}

	@Override
	public MetaData getExpectedMetaData() {
		return new ExampleSetMetaData();
	}

}

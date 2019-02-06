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
package com.rapidminer.operator.ports.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
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

	private static String errorKeys[] = { null, "measures.nominal", "measures.numerical", "measures.numerical" };
	private static String paramNames[] = { null, DistanceMeasures.PARAMETER_NOMINAL_MEASURE,
			DistanceMeasures.PARAMETER_NUMERICAL_MEASURE, DistanceMeasures.PARAMETER_DIVERGENCE };

	@Override
	public void check(MetaData metaData) {
		if (metaData instanceof ExampleSetMetaData) {
			ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
			try {
				int distType = this.parameterHandler.getParameterAsInt(DistanceMeasures.PARAMETER_MEASURE_TYPES);
				if (distType == DistanceMeasures.MIXED_MEASURES_TYPE) {
					return;
				}
				boolean handlesOnlyNom = distType == DistanceMeasures.NOMINAL_MEASURES_TYPE;
				boolean containsOnlyNom = containsOnlyType(emd, Ontology.NOMINAL, false);
				boolean containsOnlyNum = containsOnlyType(emd, Ontology.NUMERICAL, false);
				if (!handlesOnlyNom && !containsOnlyNum || handlesOnlyNom && !containsOnlyNom) {
					createError(Severity.ERROR, getQuickFixes(containsOnlyNom != containsOnlyNum, containsOnlyNom),
							errorKeys[distType], parameterHandler.getParameterAsString(paramNames[distType]));
				}
			} catch (UndefinedParameterError e) {
			}
		}

	}

	/**
	 * Creates {@link QuickFix QuickFixes} for {@link DistanceMeasure} based errors. Will always
	 * suggest mixed measures, but also nominal/numeric measures if the meta data supports that.
	 *
	 * @param moreFixes
	 *            indicates whether the meta data represents a pure nominal/numerical example set
	 * @param nomFixes
	 *            if {@code true}, adds nominal measure fix, otherwise adds numerical measure fixes
	 * @return a list of appropriate quick fixes
	 *
	 * @since 7.6
	 */
	private List<QuickFix> getQuickFixes(boolean moreFixes, boolean nomFixes) {
		Operator operator;
		try {
			operator = getInputPort().getPorts().getOwner().getOperator();
		} catch (NullPointerException npe) {
			return Collections.emptyList();
		}
		List<QuickFix> fixes = new ArrayList<>();
		fixes.add(createFix(operator, DistanceMeasures.MIXED_MEASURES_TYPE));
		if (moreFixes) {
			if (nomFixes) {
				fixes.add(createFix(operator, DistanceMeasures.NOMINAL_MEASURES_TYPE));
			} else {
				fixes.add(createFix(operator, DistanceMeasures.NUMERICAL_MEASURES_TYPE));
				fixes.add(createFix(operator, DistanceMeasures.DIVERGENCES_TYPE));
			}
		}
		return fixes;
	}

	private QuickFix createFix(Operator operator, int type) {
		return new ParameterSettingQuickFix(operator, DistanceMeasures.PARAMETER_MEASURE_TYPES,
				DistanceMeasures.MEASURE_TYPES[type]);
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

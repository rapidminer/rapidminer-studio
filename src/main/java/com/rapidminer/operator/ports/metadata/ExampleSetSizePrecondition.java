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

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;

import java.util.Collections;


/**
 * This Precondition checks whether two example indices given by parameters lying in the size of the
 * example set. If only a starting Parameter is given and the end parameter is null, it will be
 * treated as single index.
 * 
 * @author Sebastian Land
 * 
 */
public class ExampleSetSizePrecondition extends AbstractPrecondition {

	private String startParameter;
	private String endParameter;
	private Operator operator;

	public ExampleSetSizePrecondition(InputPort inputPort, Operator operator, String startParameter) {
		this(inputPort, operator, startParameter, null);
	}

	public ExampleSetSizePrecondition(InputPort inputPort, Operator operator, String startParameter, String endParameter) {
		super(inputPort);

		this.startParameter = startParameter;
		this.endParameter = endParameter;
		this.operator = operator;
	}

	@Override
	public void check(MetaData metaData) {
		final InputPort inputPort = getInputPort();
		if (metaData == null) {
			inputPort.addError(new InputMissingMetaDataError(inputPort, ExampleSet.class, null));
		} else {
			if (metaData instanceof ExampleSetMetaData) {
				ExampleSetMetaData emd = (ExampleSetMetaData) metaData;

				try {
					int startIndex = operator.getParameterAsInt(startParameter);
					if (emd.getNumberOfExamples().isAtLeast(startIndex) == MetaDataInfo.NO) {
						createError(
								Severity.ERROR,
								Collections.singletonList(new ParameterSettingQuickFix(operator, startParameter, (emd
										.getNumberOfExamples().getValue() - ((endParameter == null) ? 0 : 1)) + "")),
								"exampleset.parameter_value_exceeds_exampleset_size", startParameter, startIndex + "");
					}
					if (endParameter != null) {
						int endIndex = operator.getParameterAsInt(endParameter);
						if (emd.getNumberOfExamples().isAtLeast(endIndex) == MetaDataInfo.NO) {
							createError(
									Severity.ERROR,
									Collections.singletonList(new ParameterSettingQuickFix(operator, endParameter, (emd
											.getNumberOfExamples().getValue()) + "")),
									"exampleset.parameter_value_exceeds_exampleset_size", endParameter, endIndex + "");
						}
						if (startIndex > endIndex) {
							operator.addError(new SimpleProcessSetupError(Severity.ERROR, operator.getPortOwner(),
									Collections.singletonList(new ParameterSettingQuickFix(operator, endParameter,
											(startIndex + 1) + "")), "parameter_combination_forbidden", startParameter,
									endParameter));

						}
					}
				} catch (Exception e) {
				}
			} else {
				inputPort.addError(new MetaDataUnderspecifiedError(inputPort));
			}
		}
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

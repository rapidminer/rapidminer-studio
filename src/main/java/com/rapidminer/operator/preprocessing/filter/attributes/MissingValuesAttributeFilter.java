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
package com.rapidminer.operator.preprocessing.filter.attributes;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeDouble;

import java.util.List;


/**
 * This condition checks whether an attribute contains less than a specified fraction of missing
 * values. If the maximal fraction is exceeded, the attribute is removed.
 * 
 * This condition needs a full data scan per attribute and hence might downspeed calculations.
 * 
 * @author Sebastian Land
 */
public class MissingValuesAttributeFilter extends AbstractAttributeFilterCondition {

	public static final String PARAMETER_MAX_FRACTION_MISSING = "max_fraction_of_missings";

	private double maxFraction;

	private int numberOfExamples = 0;
	private int numberOfMissings = 0;
	private Attribute lastAttribute = null;

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler parameterHandler)
			throws ConditionCreationException {
		// TODO: Implement meta data dependent handling
		return MetaDataInfo.UNKNOWN;
	}

	@Override
	public boolean isNeedingFullScan() {
		return true;
	}

	@Override
	public boolean isNeedingScan() {
		return true;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		return ScanResult.UNCHECKED;
	}

	@Override
	public ScanResult check(Attribute attribute, Example example) {
		if (attribute != lastAttribute) {
			numberOfExamples = 0;
			numberOfMissings = 0;
			lastAttribute = attribute;
		}
		numberOfExamples++;
		if (Double.isNaN(example.getValue(attribute))) {
			numberOfMissings++;
		}

		// returning unchecked, since counting not completed
		return ScanResult.UNCHECKED;
	}

	@Override
	public ScanResult checkAfterFullScan() {
		double fraction = numberOfExamples;
		if (numberOfMissings / fraction > maxFraction) {
			return ScanResult.REMOVE;
		}
		return ScanResult.KEEP;
	}

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {
		maxFraction = operator.getParameterAsDouble(PARAMETER_MAX_FRACTION_MISSING);
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes) {
		List<ParameterType> types = super.getParameterTypes(operator, inPort);
		types.add(new ParameterTypeDouble(
				PARAMETER_MAX_FRACTION_MISSING,
				"If the attribute contains missing values in more than this fraction of the total number of examples, it is removed.",
				0d, 1d, true));
		return types;
	}
}

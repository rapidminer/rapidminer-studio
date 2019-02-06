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
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;


/**
 * This class implements a no missing value filter for attributes. Attributes are filtered and hence
 * be removed from exampleSet if there are missing values in one of the examples in this attribute.
 * 
 * @author Sebastian Land, Ingo Mierswa
 */
public class NoMissingValuesAttributeFilter extends AbstractAttributeFilterCondition {

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler) {
		switch (attribute.containsMissingValues()) {
			case YES:
				return MetaDataInfo.YES;
			case NO:
				return MetaDataInfo.NO;
			default:
				return MetaDataInfo.UNKNOWN;
		}
	}

	@Override
	public boolean isNeedingScan() {
		return true;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) {
		return ScanResult.UNCHECKED;
	}

	@Override
	public ScanResult check(Attribute attribute, Example example) {
		if (Double.isNaN(example.getValue(attribute))) {
			return ScanResult.REMOVE;
		} else {
			return ScanResult.UNCHECKED;
		}
	}
}

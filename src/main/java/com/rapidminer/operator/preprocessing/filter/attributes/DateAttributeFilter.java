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
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.Ontology;

import java.util.LinkedList;
import java.util.List;


/**
 * This class implements the condition if an attribute is numeric. All non-numerical attributes will
 * be removed.
 * 
 * @author Ingo Mierswa, Sebastian Land
 */
public class DateAttributeFilter extends AbstractAttributeFilterCondition {

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler) {
		return Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME) ? MetaDataInfo.NO
				: MetaDataInfo.YES;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if (!Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.DATE_TIME)) {
			return ScanResult.REMOVE;
		} else {
			return ScanResult.KEEP;
		}
	}

	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		return new LinkedList<ParameterType>();
	}
}

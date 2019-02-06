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
import com.rapidminer.example.set.ConditionCreationException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


/**
 * @author Tobias Malbrecht
 */
public class ValueTypeAttributeFilter extends AbstractAttributeFilterCondition {

	public static final String PARAMETER_VALUE_TYPE = "value_type";
	public static final String PARAMETER_ADD_EXCEPTION = "use_value_type_exception";
	public static final String PARAMETER_EXCEPT_VALUE_TYPE = "except_value_type";

	private int valueType;
	private int exceptValueType;

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {
		valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(operator.getParameterAsString(PARAMETER_VALUE_TYPE));
		if (valueType < 0 || valueType >= Ontology.VALUE_TYPE_NAMES.length) {
			throw new ConditionCreationException("Unknown value type selected.");
		}
		String exceptValueTypeName = operator.getParameterAsString(PARAMETER_EXCEPT_VALUE_TYPE);
		if (operator.getParameterAsBoolean(PARAMETER_ADD_EXCEPTION)) {
			exceptValueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(exceptValueTypeName);
			if (valueType < 0 || valueType >= Ontology.VALUE_TYPE_NAMES.length) {
				throw new ConditionCreationException("Unknown value type selected.");
			}
		} else {
			exceptValueType = -1;
		}
	}

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler)
			throws ConditionCreationException {
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), valueType)) {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), exceptValueType)) {
				return MetaDataInfo.YES;
			} else {
				return MetaDataInfo.NO;
			}
		}
		return MetaDataInfo.YES;
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), valueType)) {
			if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), exceptValueType)) {
				return ScanResult.REMOVE;
			} else {
				return ScanResult.KEEP;
			}
		}
		return ScanResult.REMOVE;
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, InputPort inPort, int... valueTypes) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		Set<String> valueTypeSet = new LinkedHashSet<String>();
		for (String valueTypeName : Ontology.ATTRIBUTE_VALUE_TYPE.getNames()) {
			int valueType = Ontology.ATTRIBUTE_VALUE_TYPE.mapName(valueTypeName);
			for (int parent : valueTypes) {
				if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, parent)) {
					valueTypeSet.add(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(valueType));
				}
			}
		}
		String[] valueTypeNames = new String[valueTypeSet.size()];
		valueTypeNames = valueTypeSet.toArray(valueTypeNames);

		String[] exceptValueTypeNames = new String[valueTypeSet.size()];
		exceptValueTypeNames = valueTypeSet.toArray(valueTypeNames);

		types.add(new ParameterTypeCategory(PARAMETER_VALUE_TYPE, "The value type of the attributes.", valueTypeNames, 0,
				false));
		types.add(new ParameterTypeBoolean(
				PARAMETER_ADD_EXCEPTION,
				"If enabled, an exception to the specified value type might be specified. Attributes of this type will be filtered out, although matching the first specified type.",
				false, true));
		ParameterType type = new ParameterTypeCategory(PARAMETER_EXCEPT_VALUE_TYPE, "Except this value type.",
				exceptValueTypeNames, exceptValueTypeNames.length - 1, true);
		type.registerDependencyCondition(new BooleanParameterCondition(operator, PARAMETER_ADD_EXCEPTION, true, true));
		types.add(type);

		return types;
	}
}

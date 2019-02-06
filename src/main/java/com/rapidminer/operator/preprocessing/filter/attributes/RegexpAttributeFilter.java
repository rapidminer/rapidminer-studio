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
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataInfo;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeRegexp;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;


/**
 * This Attribute Filter removes every attribute, which name does not match the Regular expression
 * given by parameter. A data scan is not needed.
 * 
 * @author Sebastian Land, Ingo Mierswa, Tobias Malbrecht
 */
public class RegexpAttributeFilter extends AbstractAttributeFilterCondition {

	public static final String PARAMETER_REGULAR_EXPRESSION = "regular_expression";
	public static final String PARAMETER_ADD_EXCEPTION = "use_except_expression";
	public static final String PARAMETER_EXCEPT_REGULAR_EXPRESSION = "except_regular_expression";

	private String attributeNameRegexp;
	private String exceptRegexp = null;

	@Override
	public void init(ParameterHandler operator) throws UserError, ConditionCreationException {
		attributeNameRegexp = operator.getParameterAsString(PARAMETER_REGULAR_EXPRESSION);
		if ((attributeNameRegexp == null) || (attributeNameRegexp.length() == 0)) {
			throw new UserError((operator instanceof Operator) ? (Operator) operator : null, 904,
					"The condition for attribute names needs a parameter string.");
		}
		if (operator.isParameterSet(PARAMETER_EXCEPT_REGULAR_EXPRESSION)
				&& operator.getParameterAsBoolean(PARAMETER_ADD_EXCEPTION)) {
			exceptRegexp = operator.getParameterAsString(PARAMETER_EXCEPT_REGULAR_EXPRESSION);
		}
		if ((exceptRegexp != null) && (exceptRegexp.length() == 0)) {
			exceptRegexp = null;
		}
	}

	@Override
	public MetaDataInfo isFilteredOutMetaData(AttributeMetaData attribute, ParameterHandler handler)
			throws ConditionCreationException {
		try {
			if (attribute.getName().matches(attributeNameRegexp)) {
				if (exceptRegexp != null) {
					if (attribute.getName().matches(exceptRegexp)) {
						return MetaDataInfo.YES;
					} else {
						return MetaDataInfo.NO;
					}
				}
				return MetaDataInfo.NO;
			} else {
				return MetaDataInfo.YES;
			}
		} catch (PatternSyntaxException e) {
			return MetaDataInfo.UNKNOWN;
		}
	}

	@Override
	public ScanResult beforeScanCheck(Attribute attribute) throws UserError {
		if (attribute.getName().matches(attributeNameRegexp)) {
			if (exceptRegexp != null) {
				if (attribute.getName().matches(exceptRegexp)) {
					return ScanResult.REMOVE;
				}
			}
			return ScanResult.KEEP;
		} else {
			return ScanResult.REMOVE;
		}
	}

	private boolean isOfAllowedType(int valueType, int[] allowedValueTypes) {
		boolean isAllowed = false;
		for (int type : allowedValueTypes) {
			isAllowed |= Ontology.ATTRIBUTE_VALUE_TYPE.isA(valueType, type);
		}
		return isAllowed;
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler operator, final InputPort inPort, final int... valueTypes) {
		List<ParameterType> types = new LinkedList<ParameterType>();
		types.add(new ParameterTypeRegexp(PARAMETER_REGULAR_EXPRESSION,
				"A regular expression for the names of the attributes which should be kept.", true, false) {

			private static final long serialVersionUID = 8133149560984042644L;

			@Override
			public Collection<String> getPreviewList() {
				Collection<String> regExpPreviewList = new LinkedList<String>();
				if (inPort == null) {
					return null;
				}
				MetaData metaData = inPort.getMetaData();
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					for (AttributeMetaData amd : emd.getAllAttributes()) {
						if (isOfAllowedType(amd.getValueType(), valueTypes)) {
							regExpPreviewList.add(amd.getName());
						}
					}
				}
				return regExpPreviewList;
			}
		});
		types.add(new ParameterTypeBoolean(
				PARAMETER_ADD_EXCEPTION,
				"If enabled, an exception to the specified regular expression might be specified. Attributes of matching this will be filtered out, although matching the first expression.",
				false, true));

		ParameterType type = (new ParameterTypeRegexp(
				PARAMETER_EXCEPT_REGULAR_EXPRESSION,
				"A regular expression for the names of the attributes which should be filtered out although matching the above regular expression.",
				true, true) {

			private static final long serialVersionUID = 81331495609840426L;

			@Override
			public Collection<String> getPreviewList() {
				Collection<String> regExpPreviewList = new LinkedList<String>();
				if (inPort == null) {
					return null;
				}
				MetaData metaData = inPort.getMetaData();
				if (metaData instanceof ExampleSetMetaData) {
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					for (AttributeMetaData amd : emd.getAllAttributes()) {
						if (isOfAllowedType(amd.getValueType(), valueTypes)) {
							regExpPreviewList.add(amd.getName());
						}
					}
				}
				return regExpPreviewList;
			}
		});
		type.setExpert(true);
		type.registerDependencyCondition(new BooleanParameterCondition(operator, PARAMETER_ADD_EXCEPTION, true, true));
		types.add(type);

		return types;
	}
}

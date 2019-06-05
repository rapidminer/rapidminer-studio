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
package com.rapidminer.parameter;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.rapidminer.operator.Operator;
import com.rapidminer.tools.XMLException;


/**
 * A specialized {@link ParameterTypeTupel} that handles one {@link Operator} and one of its {@link ParameterType parameters}.
 * This will validate that an operator/parameter pair actually exists when
 * {@link #notifyOperatorReplacing(String, Operator, String, Operator, String) replacing an operator}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ParameterTypeOperatorParameterTupel extends ParameterTypeTupel {

	public static final String PARAMETER_OPERATOR = "operator_name";
	public static final String PARAMETER_PARAMETER = "parameter_name";

	public ParameterTypeOperatorParameterTupel(Element element) throws XMLException {
		super(element);
	}

	/**
	 * Same as {@link #ParameterTypeOperatorParameterTupel(String, String, String, String)
	 * ParameterTypeOperatorParameterTupel}(key, description, {@value #PARAMETER_OPERATOR}, {@value #PARAMETER_PARAMETER})
	 */
	public ParameterTypeOperatorParameterTupel(String key, String description) {
		this(key, description, PARAMETER_OPERATOR, PARAMETER_PARAMETER);
	}

	/**
	 * Creates a parameter type that can represent an operator and one of it's parameter keys.
	 * Will create sub types for the operator of type {@link ParameterTypeInnerOperator} and for the parameter of
	 * type {@link ParameterTypeString}. The sub types' keys are set according to the metod arguments.
	 *
	 * @param key
	 * 		the key for this parameter
	 * @param description
	 * 		the description of this parameter
	 * @param operatorKey
	 * 		the key for the operator sub type
	 * @param parameterKey
	 * 		the key for the parameter sub type
	 * @see ParameterTypeTupel#ParameterTypeTupel(String, String, ParameterType...)
	 */
	public ParameterTypeOperatorParameterTupel(String key, String description, String operatorKey, String parameterKey) {
		super(key, description, new ParameterTypeInnerOperator(operatorKey, "The operator."),
				new ParameterTypeString(parameterKey, "The parameter."));
	}

	@Override
	public String transformNewValue(String value) {
		String transformedValue = super.transformNewValue(value);
		String[] tupel = transformString2Tupel(transformedValue);
		if (tupel.length != 2 || StringUtils.isEmpty(tupel[0]) || StringUtils.isEmpty(tupel[1])) {
			return null;
		}
		return transformedValue;
	}

	/** @return the updated value if this parameter is affected and the new operator has the same parameter; empty string otherwise */
	@Override
	public String notifyOperatorReplacing(String oldName, Operator oldOp, String newName, Operator newOp, String parameterValue) {
		String[] tupel = transformString2Tupel(parameterValue);
		if (!oldName.equals(tupel[0])) {
			// parameter is not affected => just return old value
			return parameterValue;
		}
		if (!newOp.getParameters().getKeys().contains(tupel[1])) {
			// new operator does not have that parameter => irrelevant
			return null;
		}
		String opValue = getFirstParameterType().notifyOperatorRenaming(oldName, newName, tupel[0]);
		if (opValue.equals(tupel[0])) {
			// operator name did not change => just return old value
			return parameterValue;
		}
		tupel[0] = opValue;
		return transformTupel2String(tupel);
	}

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String getDefaultValueAsString() {
		return null;
	}
}

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
package com.rapidminer.operator.preprocessing.normalization;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.LogService;


/**
 * This is an abstract class for all normalization methods. It returns just an empty list of
 * {@link ParameterType}s and does not perform any init code.
 * 
 * @author Sebastian Land
 * 
 */
public abstract class AbstractNormalizationMethod implements NormalizationMethod {

	@Override
	public void init() {}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler handler) {
		return new LinkedList<ParameterType>();
	}

	/**
	 * Logs a warning and adds a {@link ProcessSetupError} to the given operator based on the i18n
	 * key and offending values.
	 *
	 * @param i18nKey
	 *            the key of the error
	 * @param operator
	 *            the offending operator
	 * @param attributeName
	 *            the offending attribute
	 * @param values
	 *            the offending value(s)
	 * @since 7.6
	 */
	protected void problematicValueWarning(String i18nKey, Operator operator, String attributeName, double... values) {
		String valueString = Arrays.toString(values);
		SimpleProcessSetupError setupError = new SimpleProcessSetupError(Severity.WARNING, operator.getPortOwner(), i18nKey,
				getName(), attributeName, valueString);
		operator.addError(setupError);
		LogService.getRoot().warning(setupError.getMessage());
	}

	/**
	 * Same as {@link #problematicValueWarning(String, Operator, String, double...)
	 * problematicValueWarning("normalization.nonfinite_values", Operator, String, double...)}
	 *
	 * @since 7.6
	 */
	protected void nonFiniteValueWarning(Operator operator, String attributeName, double... values) {
		problematicValueWarning("normalization.nonfinite_values", operator, attributeName, values);
	}

	/**
	 * Same as {@link #problematicValueWarning(String, Operator, String, double...)
	 * problematicValueWarning("normalization.negative_values", Operator, String, double...)}
	 *
	 * @since 7.6
	 */
	protected void negativeValueWarning(Operator operator, String attributeName, double... values) {
		problematicValueWarning("normalization.negative_values", operator, attributeName, values);
	}

	/**
	 * Same as {@link #problematicValueWarning(String, Operator, String, double...)
	 * problematicValueWarning("normalization.divisor", Operator, String, double...)}
	 *
	 * @since 7.6
	 */
	protected void divisorWarning(Operator operator, String attributeName, double... values) {
		problematicValueWarning("normalization.divisor", operator, attributeName, values);
	}

}

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
package com.rapidminer.operator.preprocessing.filter;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


/**
 * This operator switches off all features whose name matches the one given in the parameter
 * <code>skip_features_with_name</code>. The name can be defined as a regular expression.
 * 
 * @author Buelent Moeller, Ingo Mierswa
 */
public class FeatureNameFilter extends FeatureFilter {

	/**
	 * The parameter name for &quot;Remove attributes with a matching name (accepts regular
	 * expressions)&quot;
	 */
	public static final String PARAMETER_SKIP_FEATURES_WITH_NAME = "skip_features_with_name";

	/**
	 * The parameter name for &quot;Does not remove attributes if their name fulfills this matching
	 * criterion (accepts regular expressions)&quot;
	 */
	public static final String PARAMETER_EXCEPT_FEATURES_WITH_NAME = "except_features_with_name";

	private Pattern skipPattern;

	private Pattern exceptionPattern;

	public FeatureNameFilter(OperatorDescription description) {
		super(description);
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		// set some internal parameters as a side effect.
		// TODO: Can't we do that in some init method?
		String regex = getParameterAsString(PARAMETER_SKIP_FEATURES_WITH_NAME);
		try {
			skipPattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			throw new UserError(this, 206, regex, e.getMessage());
		}
		regex = getParameterAsString(PARAMETER_EXCEPT_FEATURES_WITH_NAME);
		if ((regex == null) || (regex.trim().length() == 0)) {
			exceptionPattern = null;
		} else {
			try {
				exceptionPattern = Pattern.compile(regex);
			} catch (PatternSyntaxException e) {
				throw new UserError(this, 206, regex, e.getMessage());
			}
		}
		return super.apply(exampleSet);
	}

	/**
	 * Implements the method required by the superclass. For features whose name matches the input
	 * name (regular expression). If the input name does not match the the input name (regular
	 * expression) will not be switched off. If no parameter was provided, FALSE is always returned,
	 * so no feature is switched off.
	 * 
	 * @param attributeRole
	 *            Feature to check.
	 * @return TRUE if this feature should <b>not</b> be active in the output example set of this
	 *         operator. FALSE otherwise.
	 */
	@Override
	public boolean switchOffFeature(AttributeRole attributeRole) throws OperatorException {
		Attribute attribute = attributeRole.getAttribute();
		Matcher nameSkipMatcher = skipPattern.matcher(attribute.getName());
		Matcher specialNameSkipMatcher = null;
		if (attributeRole.isSpecial()) {
			specialNameSkipMatcher = skipPattern.matcher(attributeRole.getSpecialName());
		}
		Matcher exceptionMatcher = exceptionPattern != null ? exceptionPattern.matcher(attribute.getName()) : null;
		Matcher specialExceptionMatcher = null;
		if (attributeRole.isSpecial()) {
			specialExceptionMatcher = exceptionPattern != null ? exceptionPattern.matcher(attributeRole.getSpecialName())
					: null;
		}
		return (nameSkipMatcher.matches() || ((specialNameSkipMatcher != null) && (specialNameSkipMatcher.matches())))
				&& ((exceptionMatcher == null) || (!exceptionMatcher.matches()))
				&& ((specialExceptionMatcher == null) || (!specialExceptionMatcher.matches()));
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_SKIP_FEATURES_WITH_NAME,
				"Remove attributes with a matching name (accepts regular expressions)", false));
		types.add(new ParameterTypeString(PARAMETER_EXCEPT_FEATURES_WITH_NAME,
				"Does not remove attributes if their name fulfills this matching criterion (accepts regular expressions)",
				true));
		return types;
	}
}

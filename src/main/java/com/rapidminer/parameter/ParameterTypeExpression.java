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

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.internal.function.process.ParameterValue;


/**
 * This attribute type supports the user by letting him define an expression with a user interface
 * known from calculators.
 *
 * For knowing attribute names before process execution a valid meta data transformation must be
 * performed. Otherwise the user might type in the name, instead of choosing.
 *
 * @author Ingo Mierswa
 */
public class ParameterTypeExpression extends ParameterTypeString {

	private static final long serialVersionUID = -1938925853519339382L;

	private static final String ATTRIBUTE_INPUT_PORT = "port-name";
	/** @since 9.3 */
	private static final String PARAMETER_VALUE_FUNCTION_NAME = new ParameterValue(null).getFunctionName();
	/** @since 9.3 */
	private static final String RENAMING_PATTERN = PARAMETER_VALUE_FUNCTION_NAME + " *\\( *\"(%s)\" *,.*?\\)";

	private transient InputPort inPort;

	private Callable<OperatorVersion> operatorVersion;

	/**
	 * A simple functional which allows to query the current operator compatibility level.
	 */
	public static final class OperatorVersionCallable implements Callable<OperatorVersion> {

		private final Operator op;

		/**
		 * Constructor for the {@link OperatorVersionCallable}.
		 *
		 * @param op
		 *            the operator. Must not be {@code null}
		 */
		public OperatorVersionCallable(Operator op) {
			if (op == null) {
				throw new IllegalArgumentException("Operator must not be null");
			}

			this.op = op;
		}

		@Override
		public OperatorVersion call() {
			return op.getCompatibilityLevel();
		}

	}

	public ParameterTypeExpression(Element element) throws XMLException {
		super(element);
	}

	/**
	 * This constructor will generate a ParameterType that does not use the {@link MetaData} of an
	 * associated {@link InputPort} to verify the expressions.
	 *
	 * @param key
	 * @param description
	 *
	 * @deprecated use {@link #ParameterTypeExpression(String, String, OperatorVersionCallable)} instead
	 */
	@Deprecated
	public ParameterTypeExpression(final String key, String description) {
		this(key, description, null, false);
	}

	/**
	 * This constructor will generate a ParameterType that does not use the {@link MetaData} of an
	 * associated {@link InputPort} to verify the expressions.
	 *
	 * @param key
	 * 		the parameter key
	 * @param description
	 * 		the parameter description
	 * @param operatorVersion
	 * 		a functional which allows to query the current operator version. Must not be
	 * 		{@code null} and must not return null
	 */
	public ParameterTypeExpression(final String key, String description, OperatorVersionCallable operatorVersion) {
		this(key, description, null, false, operatorVersion);
	}

	public ParameterTypeExpression(final String key, String description, InputPort inPort) {
		this(key, description, inPort, false);
	}

	public ParameterTypeExpression(final String key, String description, InputPort inPort, boolean optional, boolean expert) {
		this(key, description, inPort, optional);
		setExpert(expert);
	}

	public ParameterTypeExpression(final String key, String description, final InputPort inPort, boolean optional) {
		this(key, description, inPort, optional, getOperatorVersionProvider(inPort));
		if (inPort == null) {
			LogService.getRoot().log(Level.INFO, "com.rapidminer.parameter.ParameterTypeExpression.no_input_port_provided");
		}
	}

	/** @since 9.3 */
	private static Callable<OperatorVersion> getOperatorVersionProvider(InputPort inPort) {
		if (inPort != null) {
			Operator operator = inPort.getPorts().getOwner().getOperator();
			return operator::getCompatibilityLevel;
		}
		// callers that do not provide an input port are not able to use the
		// expression parser functions => use corresponding compatibility level
		return () -> ExpressionParserBuilder.OLD_EXPRESSION_PARSER_FUNCTIONS;
	}

	private ParameterTypeExpression(final String key, String description, InputPort inPort, boolean optional,
			Callable<OperatorVersion> operatorVersion) {
		super(key, description, optional);

		if (operatorVersion == null) {
			throw new IllegalArgumentException("Operator version parameter must not be null");
		}

		this.inPort = inPort;
		this.operatorVersion = operatorVersion;
	}

	@Override
	public Object getDefaultValue() {
		return "";
	}

	/**
	 * Returns the input port associated with this ParameterType. This might be null!
	 */
	public InputPort getInputPort() {
		return inPort;
	}

	@Override
	protected void writeDefinitionToXML(Element typeElement) {
		super.writeDefinitionToXML(typeElement);

		typeElement.setAttribute(ATTRIBUTE_INPUT_PORT, inPort.getName());
	}

	@Override
	public String substituteMacros(String parameterValue, MacroHandler mh) throws UndefinedParameterError {
		OperatorVersion version;
		try {
			version = operatorVersion.call();
		} catch (Exception e) {
			// cannot happen, throw error nevertheless
			throw new UndefinedParameterError(getKey());
		}

		if (version != null && version.isAtMost(ExpressionParserBuilder.OLD_EXPRESSION_PARSER_FUNCTIONS)) {
			// do not replace macros in case the compatibility level is at most 6.4
			return super.substituteMacros(parameterValue, mh);
		} else {
			return parameterValue;
		}

	}

	@Override
	public String substitutePredefinedMacros(String parameterValue, Operator operator) throws UndefinedParameterError {
		OperatorVersion version;
		try {
			version = operatorVersion.call();
		} catch (Exception e) {
			// cannot happen, throw error nevertheless
			throw new UndefinedParameterError(getKey());
		}

		if (version != null && version.isAtMost(ExpressionParserBuilder.OLD_EXPRESSION_PARSER_FUNCTIONS)) {
			// do not replace macros in case the compatibility level is at most 6.4
			return super.substitutePredefinedMacros(parameterValue, operator);
		} else {
			return parameterValue;
		}
	}

	@Override
	public String notifyOperatorRenaming(String oldName, String newName, String value) {
		if (!value.contains(PARAMETER_VALUE_FUNCTION_NAME) || !value.contains(oldName)) {
			return value;
		}
		Pattern pattern = Pattern.compile(String.format(RENAMING_PATTERN, Pattern.quote(oldName)));
		Matcher matcher = pattern.matcher(value);
		int pos = 0;
		StringBuilder newValue = new StringBuilder();
		while (matcher.find(pos)) {
			int start = matcher.start(1);
			// constant part before the first match or after previous match
			if (start > pos) {
				newValue.append(value.substring(pos, start));
			}
			newValue.append(newName);
			pos = matcher.end(1);
		}
		if (pos > 0 && pos < value.length()) {
			newValue.append(value.substring(pos));
		}
		return newValue.toString();
	}
}

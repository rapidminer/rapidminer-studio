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
package com.rapidminer.tools.expression.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.Expression;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.ExpressionParserBuilder;
import com.rapidminer.tools.expression.ExpressionRegistry;
import com.rapidminer.tools.expression.ExpressionType;
import com.rapidminer.tools.expression.MacroResolver;


/**
 *
 * A collections of utility functions for the expression parser.
 *
 * @author David Arnu, Nils Woehler
 * @since 6.5.0
 *
 */
public final class ExpressionParserUtils {

	private ExpressionParserUtils() {
		throw new UnsupportedOperationException("Static utility class");
	}

	/**
	 * Creates attribute meta data that represents the attribute that will be generated for the
	 * provided arguments.
	 *
	 * @return the {@link AttributeMetaData} for the provided arguments
	 */
	public static AttributeMetaData generateAttributeMetaData(ExampleSetMetaData emd, String name,
			ExpressionType expressionType) {

		AttributeMetaData newAttribute = null;
		AttributeMetaData existingAtt = emd.getAttributeByName(name);

		int ontology = expressionType.getAttributeType();

		if (ontology == Ontology.BINOMINAL) {
			newAttribute = new AttributeMetaData(name, Ontology.BINOMINAL);
			HashSet<String> values = new HashSet<>();
			values.add("false");
			values.add("true");
			newAttribute.setValueSet(values, SetRelation.EQUAL);
		} else {
			newAttribute = new AttributeMetaData(name, ontology);
		}

		// restore role if attribute existed already
		if (existingAtt != null) {
			newAttribute.setRole(existingAtt.getRole());
		}

		return newAttribute;
	}

	/**
	 * Creates attribute meta data that represents the attribute that will be generated for the
	 * provided arguments.
	 *
	 * @return the {@link AttributeMetaData} for the provided arguments
	 */
	public static AttributeMetaData generateAttributeMetaData(ExampleSet exampleSet, String name,
			ExpressionType expressionType) {

		AttributeMetaData newAttribute = null;
		Attribute existingAtt = exampleSet.getAttributes().get(name);

		int ontology = expressionType.getAttributeType();

		if (ontology == Ontology.BINOMINAL) {
			newAttribute = new AttributeMetaData(name, Ontology.BINOMINAL);
			HashSet<String> values = new HashSet<>();
			values.add("false");
			values.add("true");
			newAttribute.setValueSet(values, SetRelation.EQUAL);
		} else {
			newAttribute = new AttributeMetaData(name, ontology);
		}

		// restore role if attribute existed already
		if (existingAtt != null) {
			newAttribute.setRole(exampleSet.getAttributes().getRole(existingAtt).getSpecialName());
		}

		return newAttribute;
	}

	/**
	 * Parses the provided expression and iterates over the {@link ExampleSet}, interprets
	 * attributes as variables, evaluates the function and creates a new attribute with the given
	 * name that takes the expression's value. The type of the attribute depends on the expression
	 * type and is {@link Ontology#NOMINAL} for strings, {@link Ontology#INTEGER} for integers,
	 * {@link Ontology#REAL} for reals, {@link Ontology#DATE_TIME} for Dates, and
	 * {@link Ontology#BINOMINAL} with values &quot;true&quot; and &quot;false&quot; for booleans.
	 * If the executing operator is defined, there will be a check for stop before the calculation
	 * of each example.
	 *
	 * @param exampleSet
	 *            the example set to which the generated attribute is added
	 * @param name
	 *            the new attribute name
	 * @param expression
	 *            the expression used to generate attribute values
	 * @param parser
	 *            the expression parser used to parse the expression argument
	 * @param resolver
	 *            the example resolver which is used by the parser to resolve example values
	 * @param executingOperator
	 *            the operator calling this method. <code>null</code> is allowed. If not null the
	 *            operator will be used to check for stop
	 *
	 * @throws ProcessStoppedException
	 *             in case the process was stopped by the user
	 * @throws ExpressionException
	 *             in case parsing the expression fails
	 *
	 */
	public static Attribute addAttribute(ExampleSet exampleSet, String name, String expression, ExpressionParser parser,
			ExampleResolver resolver, Operator executingOperator) throws ProcessStoppedException, ExpressionException {

		// parse the expression
		Expression parsedExpression = parser.parse(expression);

		Attribute newAttribute = null;
		// if != null this needs to be overridden
		Attribute existingAttribute = exampleSet.getAttributes().get(name);
		StringBuffer appendix = new StringBuffer();
		String targetName = name;
		if (existingAttribute != null) {
			// If an existing attribute will be overridden, first a unique temporary name has to be
			// generated by appending a random string to the attribute's name until it's a unique
			// attribute name. After the new attribute is build, it's name is set the 'targetName'
			// at the end of this method.
			//
			RandomGenerator randomGenerator = RandomGenerator.getGlobalRandomGenerator();
			do {
				appendix.append(randomGenerator.nextString(5));
			} while (exampleSet.getAttributes().get(name + appendix.toString()) != null);
			name = name + appendix.toString();
		}

		ExpressionType resultType = parsedExpression.getExpressionType();
		int ontology = resultType.getAttributeType();
		if (ontology == Ontology.BINOMINAL) {
			newAttribute = AttributeFactory.createAttribute(name, Ontology.BINOMINAL);
			newAttribute.getMapping().mapString("false");
			newAttribute.getMapping().mapString("true");
		} else {
			newAttribute = AttributeFactory.createAttribute(name, ontology);
		}

		// set construction description
		newAttribute.setConstruction(expression);

		// add new attribute to table and example set
		exampleSet.getExampleTable().addAttribute(newAttribute);
		exampleSet.getAttributes().addRegular(newAttribute);

		// create attribute of correct type and all values
		for (Example example : exampleSet) {
			if (executingOperator != null) {
				executingOperator.checkForStop();
			}

			// bind example to resolver
			resolver.bind(example);

			// calculate result
			try {
				switch (resultType) {
					case DOUBLE:
					case INTEGER:
						example.setValue(newAttribute, parsedExpression.evaluateNumerical());
						break;
					case DATE:
						Date date = parsedExpression.evaluateDate();
						example.setValue(newAttribute, date == null ? Double.NaN : date.getTime());
						break;
					default:
						example.setValue(newAttribute, parsedExpression.evaluateNominal());
						break;
				}
			} finally {
				// avoid memory leaks
				resolver.unbind();
			}

		}

		// remove existing attribute (if necessary)
		if (existingAttribute != null) {
			AttributeRole oldRole = exampleSet.getAttributes().getRole(existingAttribute);
			exampleSet.getAttributes().remove(existingAttribute);
			newAttribute.setName(targetName);
			// restore role from old attribute to new attribute
			if (oldRole.isSpecial()) {
				exampleSet.getAttributes().setSpecialAttribute(newAttribute, oldRole.getSpecialName());
			}
		}

		// update example resolver after meta data change
		resolver.addAttributeMetaData(
				new AttributeMetaData(exampleSet.getAttributes().getRole(newAttribute), exampleSet, true));

		return newAttribute;
	}

	/**
	 * Adds the {@link ExpressionParserBuilder#OLD_EXPRESSION_PARSER_FUNCTIONS} operator version as
	 * incompatible version change by increasing the array size by one and adding the
	 * {@link OperatorVersion} at the end of the array.
	 *
	 * @param incompatibleVersions
	 *            all prior incompatible version changes
	 * @return an array which contains
	 *         {@link ExpressionParserBuilder#OLD_EXPRESSION_PARSER_FUNCTIONS} as last element
	 */
	public static OperatorVersion[] addIncompatibleExpressionParserChange(OperatorVersion... incompatibleVersions) {
		OperatorVersion[] extendedIncompatibleVersions = Arrays.copyOf(incompatibleVersions,
				incompatibleVersions.length + 1);
		extendedIncompatibleVersions[incompatibleVersions.length] = ExpressionParserBuilder.OLD_EXPRESSION_PARSER_FUNCTIONS;
		return extendedIncompatibleVersions;
	}

	/**
	 * Uses the {@link ExpressionParserBuilder} to create an {@link ExpressionParser} with all
	 * modules that are registered to the {@link ExpressionRegistry}.
	 *
	 * @param op
	 *            the operator to create the {@link ExpressionParser} for. Must not be {@code null}
	 * @param exampleResolver
	 *            the {@link ExampleResolver} which is used to lookup example values. Might be
	 *            {@code null} in case no {@link ExampleResolver} is available
	 * @return the build expression parser
	 */
	public static ExpressionParser createAllModulesParser(final Operator op, final ExampleResolver exampleResolver) {
		ExpressionParserBuilder builder = new ExpressionParserBuilder();

		// decide which functions should be available
		builder.withCompatibility(op.getCompatibilityLevel());

		if (op.getProcess() != null) {
			builder.withProcess(op.getProcess());
			builder.withScope(new MacroResolver(op.getProcess().getMacroHandler(), op));
		}
		if (exampleResolver != null) {
			builder.withDynamics(exampleResolver);
		}

		builder.withModules(ExpressionRegistry.INSTANCE.getAll());

		return builder.build();
	}

	/**
	 * Converts a {@link ExpressionException} into a {@link UserError}.
	 *
	 * @param op
	 *            the calling operator
	 * @param function
	 *            the entered function
	 * @param e
	 *            the exception
	 * @throws UserError
	 *             the converted {@link UserError}
	 */
	public static UserError convertToUserError(Operator op, String function, ExpressionException e) {

		// only show up to 15 characters of the function string
		String shortenedFunction = function;
		if (function.length() > 15) {
			shortenedFunction = function.substring(0, 15).concat(" (...)");
		}

		return new UserError(op, e, "expression_evaluation_failed", e.getShortMessage(), shortenedFunction);
	}

}

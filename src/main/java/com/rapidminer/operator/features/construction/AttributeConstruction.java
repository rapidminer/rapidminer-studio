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
package com.rapidminer.operator.features.construction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.preprocessing.filter.ChangeAttributeName;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeExpression;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.expression.ExampleResolver;
import com.rapidminer.tools.expression.ExpressionException;
import com.rapidminer.tools.expression.ExpressionParser;
import com.rapidminer.tools.expression.internal.ExpressionParserUtils;
import com.rapidminer.tools.expression.internal.UnknownResolverVariableException;


/**
 * <p>
 * This operator constructs new attributes from the attributes of the input example set. The names
 * of the new attributes and their construction description are defined in the parameter list
 * &quot;functions&quot;.
 * </p>
 *
 * <p>
 * The following <em>operators</em> are supported:
 * <ul>
 * <li>Addition: +</li>
 * <li>Subtraction: -</li>
 * <li>Multiplication: *</li>
 * <li>Division: /</li>
 * <li>Power: ^</li>
 * <li>Modulus: %</li>
 * <li>Less Than: &lt;</li>
 * <li>Greater Than: &gt;</li>
 * <li>Less or Equal: &lt;=</li>
 * <li>More or Equal: &gt;=</li>
 * <li>Equal: ==</li>
 * <li>Not Equal: !=</li>
 * <li>Boolean Not: !</li>
 * <li>Boolean And: &&</li>
 * <li>Boolean Or: ||</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>log and exponential functions</em> are supported:
 * <ul>
 * <li>Natural Logarithm: ln(x)</li>
 * <li>Logarithm Base 10: log(x)</li>
 * <li>Logarithm Dualis (Base 2): ld(x)</li>
 * <li>Exponential (e^x): exp(x)</li>
 * <li>Power: pow(x,y)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>trigonometric functions</em> are supported:
 * <ul>
 * <li>Sine: sin(x)</li>
 * <li>Cosine: cos(x)</li>
 * <li>Tangent: tan(x)</li>
 * <li>Arc Sine: asin(x)</li>
 * <li>Arc Cosine: acos(x)</li>
 * <li>Arc Tangent: atan(x)</li>
 * <li>Arc Tangent (with 2 parameters): atan2(x,y)</li>
 * <li>Hyperbolic Sine: sinh(x)</li>
 * <li>Hyperbolic Cosine: cosh(x)</li>
 * <li>Hyperbolic Tangent: tanh(x)</li>
 * <li>Inverse Hyperbolic Sine: asinh(x)</li></li>
 * <li>Inverse Hyperbolic Cosine: acosh(x)</li></li>
 * <li>Inverse Hyperbolic Tangent: atanh(x)</li></li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>statistical functions</em> are supported:
 * <ul>
 * <li>Round: round(x)</li>
 * <li>Round to p decimals: round(x,p)</li>
 * <li>Floor: floor(x)</li>
 * <li>Ceiling: ceil(x)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>aggregation functions</em> are supported:
 * <ul>
 * <li>Average: avg(x,y,z...)</li>
 * <li>Minimum: min(x,y,z...)</li>
 * <li>Maximum: max(x,y,z...)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>text functions</em> are supported:
 * <ul>
 * <li>Number to String: str(x)</li>
 * <li>String to Number: parse(text)</li>
 * <li>Substring: cut(text, start, length)</li>
 * <li>Concatenation (also possible by &quot+&quot;): concat(text1, text2, text3...)</li>
 * <li>Replace: replace(text, what, by)</li>
 * <li>Replace All: replaceAll(text, what, by)</li>
 * <li>To lower case: lower(text)</li>
 * <li>To upper case: upper(text)</li>
 * <li>First position of string in text: index(text, string)</li>
 * <li>Length: length(text)</li>
 * <li>Character at position pos in text: char(text, pos)</li>
 * <li>Compare: compare(text1, text2)</li>
 * <li>Contains string in text: contains(text, string)</li>
 * <li>Equals: equals(text1, text2)</li>
 * <li>Starts with string: starts(text, string)</li>
 * <li>Ends with string: ends(text, string)</li>
 * <li>Matches with regular expression exp: matches(text, exp)</li>
 * <li>Suffix of length: suffix(text, length)</li>
 * <li>Prefix of length: prefix(text, length)</li>
 * <li>Trim (remove leading and trailing whitespace): trim(text)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>miscellaneous functions</em> are supported:
 * <ul>
 * <li>If-Then-Else: if(cond,true-evaluation, false-evaluation)</li>
 * <li>Absolute: abs(x)</li>
 * <li>Constant: const(x)</li>
 * <li>Square Root: sqrt(x)</li>
 * <li>Signum (delivers the sign of a number): sgn(x)</li>
 * <li>Random Number (between 0 and 1): rand()</li>
 * <li>Modulus (x % y): mod(x,y)</li>
 * <li>Sum of k Numbers: sum(x,y,z...)</li>
 * <li>Binomial Coefficients: binom(n, i)</li>
 * <li>Retrieving parameter value: param(operator name, parameter name)</li>
 * </ul>
 * </p>
 *
 * <p>
 * The following <em>process related functions</em> are supported:
 * <ul>
 * <li>Retrieving a parameter value: param("operator", "parameter")</li>
 * </ul>
 * </p>
 *
 *
 * <p>
 * Beside those operators and functions, this operator also supports the constants pi and e if this
 * is indicated by the corresponding parameter (default: true). You can also use strings in formulas
 * (for example in a conditioned if-formula) but the string values have to be enclosed in double
 * quotes.
 * </p>
 *
 * <p>
 * Please note that there are some restrictions for the attribute names in order to let this
 * operator work properly:
 * <ul>
 * <li>If the standard constants are usable, attribute names with names like &quot;e&quot; or
 * &quot;pi&quot; are not allowed.</li>
 * <li>Attribute names with function or operator names are also not allowed.</li>
 * <li>Attribute names containing parentheses are not allowed.</li>
 * </ul>
 * If these conditions are not fulfilled, the names must be changed beforehand, for example with the
 * {@link ChangeAttributeName} operator.
 * </p>
 *
 * <p>
 * <br/>
 * <em>Examples:</em><br/>
 * a1+sin(a2*a3)<br/>
 * if (att1>5, att2*att3, -abs(att1))<br/>
 * </p>
 *
 * @author Ingo Mierswa
 */
public class AttributeConstruction extends AbstractFeatureConstruction {

	/** The parameter name for &quot;List of functions to generate.&quot; */
	public static final String PARAMETER_FUNCTIONS = "function_descriptions";

	/**
	 * The parameter name for &quot;If set to true, all the original attributes are kept, otherwise
	 * they are removed from the example set.&quot;
	 */
	public static final String PARAMETER_KEEP_ALL = "keep_all";

	public AttributeConstruction(OperatorDescription description) {
		super(description);
	}

	@Override
	protected MetaData modifyMetaData(ExampleSetMetaData metaData) {
		List<AttributeMetaData> originalAttributes = new LinkedList<>();
		for (AttributeMetaData attribute : metaData.getAllAttributes()) {
			originalAttributes.add(attribute);
		}

		List<String> newAttributeNames = new LinkedList<>();

		ExampleResolver resolver = new ExampleResolver(metaData);
		ExpressionParser parser = ExpressionParserUtils.createAllModulesParser(this, resolver);

		try {

			List<String[]> parameterList = getParameterList(PARAMETER_FUNCTIONS);
			for (String[] nameFunctionPair : parameterList) {
				String name = nameFunctionPair[0];
				String function = nameFunctionPair[1];

				try {
					AttributeMetaData amd = ExpressionParserUtils.generateAttributeMetaData(metaData, name,
							parser.parse(function).getExpressionType());

					newAttributeNames.add(name);
					metaData.addAttribute(amd);

					// update resolver meta data after meta data change
					// in case more than one attribute is generated
					if (parameterList.size() > 1) {
						resolver.addAttributeMetaData(amd);
					}

				} catch (ExpressionException e) {
					if (e.getCause() != null && e.getCause() instanceof UnknownResolverVariableException) {
						// in case a resolver variable cannot be resolved, return a new attribute
						// with nominal type
						metaData.addAttribute(new AttributeMetaData(name, Ontology.NOMINAL));
					} else {
						// in all other cases abort meta data generation, add an error and return
						// empty meta data
						getExampleSetOutputPort().addError(new SimpleMetaDataError(Severity.ERROR,
								this.getExampleSetOutputPort(), "cannot_create_exampleset_metadata", e.getShortMessage()));
						return new ExampleSetMetaData();
					}
				}
			}

			if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
				for (AttributeMetaData attribute : originalAttributes) {
					if (!newAttributeNames.contains(attribute.getName())) {
						metaData.removeAttribute(attribute);
					}
				}
			}
		} catch (UndefinedParameterError e) {
			// ignore
		}

		return metaData;
	}

	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		List<Attribute> originalAttributes = new ArrayList<>();
		for (Attribute attribute : exampleSet.getAttributes()) {
			originalAttributes.add(attribute);
		}

		// create resolver and parser
		ExampleResolver resolver = new ExampleResolver(exampleSet);
		ExpressionParser expParser = ExpressionParserUtils.createAllModulesParser(this, resolver);

		// iterate over new attributes and generate them
		List<String> newAttributeNames = new LinkedList<>();
		List<String[]> parameterList = getParameterList(PARAMETER_FUNCTIONS);
		for (String[] nameFunctionPair : parameterList) {
			String name = nameFunctionPair[0];
			String function = nameFunctionPair[1];
			try {
				Attribute newAttribute = ExpressionParserUtils.addAttribute(exampleSet, name, function, expParser, resolver,
						this);
				newAttributeNames.add(newAttribute.getName());
			} catch (ExpressionException e) {
				throw ExpressionParserUtils.convertToUserError(this, function, e);
			}
			checkForStop();
		}

		if (!getParameterAsBoolean(PARAMETER_KEEP_ALL)) {
			for (Attribute attribute : originalAttributes) {
				if (!newAttributeNames.contains(attribute.getName())) {
					exampleSet.getAttributes().remove(attribute);
				}
			}
		}

		return exampleSet;
	}

	@Override
	public OperatorVersion[] getIncompatibleVersionChanges() {
		// add expression parser version change to allow usage of old functions
		return ExpressionParserUtils.addIncompatibleExpressionParserChange(super.getIncompatibleVersionChanges());
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeList(PARAMETER_FUNCTIONS, "List of functions to generate.",
				new ParameterTypeString("attribute_name", "Specifies the name of the constructed attribute"),
				new ParameterTypeExpression("function_expressions", "Function and arguments to use for generation.",
						getInputPort()));
		type.setExpert(false);
		type.setPrimary(true);
		types.add(type);

		types.add(new ParameterTypeBoolean(PARAMETER_KEEP_ALL,
				"If set to true, all the original attributes are kept, otherwise they are removed from the example set.",
				true));

		return types;
	}

}

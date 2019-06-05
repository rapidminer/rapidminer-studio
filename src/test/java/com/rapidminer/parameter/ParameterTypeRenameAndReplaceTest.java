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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorVersion;
import com.rapidminer.operator.Value;
import com.rapidminer.operator.ValueDouble;
import com.rapidminer.operator.ValueString;
import com.rapidminer.parameter.ParameterTypeExpression.OperatorVersionCallable;
import com.rapidminer.parameter.ParameterTypeValue.OperatorValueSelection;
import com.rapidminer.tools.documentation.OperatorDocumentation;
import com.rapidminer.tools.expression.internal.function.process.ParameterValue;

/**
 * Tests for {@link ParameterType#notifyOperatorRenaming(String, String, String)} and
 * {@link ParameterType#notifyOperatorReplacing(String, Operator, String, Operator, String)}. Includes tests for relevant
 * {@link ParameterType} subclasses.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ParameterTypeRenameAndReplaceTest {

	private static final String PARAMETER_OPERATOR_VALUE = "operator_value";

	private interface Notification<N, V> {
		void notify(N notified, V oldValue, V newValue) throws UndefinedParameterError;
	}

	private static final String ORIGINAL_OPERATOR_NAME = "Operator A";
	private static final String NEW_OPERATOR_NAME = "Operator B";
	private static final String FIRST_PARAMETER_KEY = "param_key";
	private static final String SECOND_PARAMETER_KEY = "other_param_key";
	private static final String SIMPLE_VALUE = "this is a string";
	private static final String VALUE_NAME = "special_value";

	/** list of {@link ParameterType} generators that need testing for operator renaming */
	private static List<Supplier<ParameterType>> renamingSuppliers;

	/** list of {@link ParameterType} generators that need testing for operator replacing */
	private static List<Supplier<ParameterType>> replacingSuppliers;

	/** list of {@link ParameterType} generators that need testing for operator replacing where the new operator is missing a parameter */
	private static List<Supplier<ParameterType>> replacingSuppliersParameterSensitive;

	/** list of {@link ParameterType} generators that need testing for operator replacing where the new operator is missing a value */
	private static List<Supplier<ParameterType>> replacingSuppliersValueSensitive;

	private static Map<Class<? extends ParameterType>, Function<ParameterType, BiFunction<String[], String[], String>>> valueGenerator;

	@BeforeClass
	public static void setup() {
		Operator compProvider = mock(Operator.class);
		when(compProvider.getCompatibilityLevel()).thenReturn(new OperatorVersion(9,3,0));
		renamingSuppliers = Arrays.asList(() -> new ParameterTypeString("string", ""),
				() -> new ParameterTypeExpression("expression", "",
						new OperatorVersionCallable(compProvider)),
				() -> new ParameterTypeInnerOperator("inner_operator", ""),
				() -> new ParameterTypeValue(PARAMETER_OPERATOR_VALUE, ""),
				() -> new ParameterTypeValue("operator_parameter", ""),
				() -> new ParameterTypeEnumeration("enumeration", "",
						renamingSuppliers.get(1).get()),
				() -> new ParameterTypeList("list", "",
						renamingSuppliers.get(0).get(), renamingSuppliers.get(1).get()),
				() -> new ParameterTypeTupel("tupel", "",
						renamingSuppliers.get(2).get(), renamingSuppliers.get(0).get()));

		replacingSuppliers = Arrays.asList(renamingSuppliers.get(0),
				() -> new ParameterTypeOperatorParameterTupel("operator_parameter_tupel", ""),
				() -> new ParameterTypeValue(PARAMETER_OPERATOR_VALUE, ""),
				() -> new ParameterTypeValue("operator_parameter", ""),
				() -> new ParameterTypeEnumeration("enumeration", "",
						replacingSuppliers.get(1).get()),
				() -> new ParameterTypeList("list", "",
						replacingSuppliers.get(0).get(), replacingSuppliers.get(3).get()),
				() -> new ParameterTypeTupel("tupel", "",
						replacingSuppliers.get(0).get(), replacingSuppliers.get(3).get()));

		replacingSuppliersParameterSensitive = IntStream.of(1, 3, 4, 5)
				.mapToObj(replacingSuppliers::get).collect(Collectors.toList());
		replacingSuppliersValueSensitive = Collections.singletonList(replacingSuppliers.get(2));

		valueGenerator = new HashMap<>();
		valueGenerator.put(ParameterTypeString.class, t -> (os, ps) -> SIMPLE_VALUE);
		valueGenerator.put(ParameterTypeExpression.class, t-> (os, ps) ->
				new ParameterValue(null).getFunctionName() + "(\"" + os[0] + "\",\"" + ps[0] + "\")");
		valueGenerator.put(ParameterTypeInnerOperator.class, t -> (os, ps) -> os[0]);
		valueGenerator.put(ParameterTypeValue.class, t -> (os, ps) ->
				ParameterTypeValue.transformOperatorValueSelection2String(new OperatorValueSelection(os[0],
						t.getKey().contains("value"), ps[0])));

		valueGenerator.put(ParameterTypeEnumeration.class, t -> {
			ParameterType valueType = ((ParameterTypeEnumeration) t).getValueType();
			BiFunction<String[], String[], String> generator = valueGenerator.get(valueType.getClass()).apply(valueType);
			return (os, ps) -> ParameterTypeEnumeration.transformEnumeration2String(
				IntStream.range(0, os.length).mapToObj(i -> generator.apply(sub(os, i), sub(ps, i)))
						.collect(Collectors.toList()));
		});
		valueGenerator.put(ParameterTypeList.class, t -> {
			ParameterTypeList ptl = (ParameterTypeList) t;
			ParameterType keyType = ptl.getKeyType();
			BiFunction<String[], String[], String> keyGen = valueGenerator.get(keyType.getClass()).apply(keyType);
			ParameterType valueType = ptl.getValueType();
			BiFunction<String[], String[], String> valueGen = valueGenerator.get(valueType.getClass()).apply(valueType);
			return (os, ps) -> ParameterTypeList.transformList2String(IntStream.range(0, os.length)
							.mapToObj(i -> new String[]{keyGen.apply(sub(os, i), sub(ps, i)), valueGen.apply(sub(os, i), sub(ps, i))})
							.collect(Collectors.toList()));
		});
		valueGenerator.put(ParameterTypeTupel.class, t -> {
			ParameterTypeTupel ptt = (ParameterTypeTupel) t;
			List<BiFunction<String[], String[], String>> generators = Arrays.stream(ptt.getParameterTypes())
					.map(p -> valueGenerator.get(p.getClass()).apply(p))
					.collect(Collectors.toList());
			return (os, ps) -> ParameterTypeTupel.transformTupel2String(generators.stream()
					.map(g -> g.apply(sub(os, 0), sub(ps, 0))).toArray(String[]::new));
		});
		valueGenerator.put(ParameterTypeOperatorParameterTupel.class, t -> (os, ps) ->
				t.transformNewValue(ParameterTypeTupel.transformTupel2String(new String[]{os[0], ps[0]})));
	}

	private static String[] sub(String[] strings, int i) {
		return new String[]{strings[i]};
	}

	@AfterClass
	public static void tearDown() {
		renamingSuppliers = replacingSuppliers = replacingSuppliersParameterSensitive = replacingSuppliersValueSensitive = null;
		valueGenerator = null;
	}

	/** Test the parameters created through {@link #renamingSuppliers} with renaming */
	@Test
	public void testRenaming() throws UndefinedParameterError {
		testRenameReplace(renamingSuppliers, Parameters::notifyRenaming);
	}

	/** Test the parameters created through {@link #replacingSuppliers} with perfect replacing */
	@Test
	public void testReplacing() throws UndefinedParameterError, OperatorCreationException {
		Operator newOperator = getOperator(NEW_OPERATOR_NAME, Arrays.asList(new ParameterTypeString(FIRST_PARAMETER_KEY, ""),
				new ParameterTypeString(SECOND_PARAMETER_KEY, "")));
		addSpecialValue(newOperator, VALUE_NAME, true);
		testRenameReplace(replacingSuppliers, (p, o, n) -> p.notifyReplacing(o, null, n, newOperator));
	}

	/** Test the parameters created through {@link #replacingSuppliersParameterSensitive} with imperfect replacing */
	@Test
	public void testMissingParameterAfterReplace() throws UndefinedParameterError, OperatorCreationException {
		List<ParameterType> parameterTypes = replacingSuppliersParameterSensitive.stream().map(Supplier::get).collect(Collectors.toList());
		String[] ops = {ORIGINAL_OPERATOR_NAME, ORIGINAL_OPERATOR_NAME};
		String[] params = {FIRST_PARAMETER_KEY, SECOND_PARAMETER_KEY};
		Parameters parameters = createParametersInstance(parameterTypes, ops, params);

		String[] newOps = {NEW_OPERATOR_NAME, NEW_OPERATOR_NAME};
		String[] newParams = {null, SECOND_PARAMETER_KEY};
		Parameters expectedAfterChange = createParametersInstance(parameterTypes, newOps, newParams);

		Operator newOperator = getOperator(NEW_OPERATOR_NAME,
				Collections.singletonList(new ParameterTypeString(SECOND_PARAMETER_KEY, "")));
		addSpecialValue(newOperator, VALUE_NAME, true);
		Notification<Parameters, String> notification = (p, o, n) -> p.notifyReplacing(o, null, n, newOperator);
		testNotification(notification, parameters, expectedAfterChange);
	}

	/** Test the parameters created through {@link #replacingSuppliersValueSensitive} with imperfect replacing */
	@Test
	public void testMissingValueAfterReplace() throws UndefinedParameterError, OperatorCreationException {
		List<ParameterType> parameterTypes = replacingSuppliersValueSensitive.stream().map(Supplier::get).collect(Collectors.toList());
		String[] ops = {ORIGINAL_OPERATOR_NAME, ORIGINAL_OPERATOR_NAME};
		String[] params = {FIRST_PARAMETER_KEY, SECOND_PARAMETER_KEY};
		Parameters parameters = createParametersInstance(parameterTypes, ops, params);

		String[] newOps = {NEW_OPERATOR_NAME, NEW_OPERATOR_NAME};
		String[] newParams = {null, SECOND_PARAMETER_KEY};
		Parameters expectedAfterChange = createParametersInstance(parameterTypes, newOps, newParams);


		Operator newOperator = getOperator(NEW_OPERATOR_NAME, Arrays.asList(new ParameterTypeString(FIRST_PARAMETER_KEY, ""),
				new ParameterTypeString(SECOND_PARAMETER_KEY, "")));
		Notification<Parameters, String> notification = (p, o, n) -> p.notifyReplacing(o, null, n, newOperator);
		testNotification(notification, parameters, expectedAfterChange);
	}

	/**
	 * Testing helper method
	 * <ol>
	 *     <li>Create a list of parameters from the suppliers</li>
	 *     <li>Create two {@link Parameters} instances from that list</li>
	 *     <li>Set the values for the first one with references to {@value #ORIGINAL_OPERATOR_NAME} [actual values before notification]</li>
	 *     <li>Set the values for the second one with references to {@value #NEW_OPERATOR_NAME} [expected values]</li>
	 *     <li>Call the {@link Notification} on the first [actual values after notification]</li>
	 *     <li>Compare each parameter value between first and second</li>
	 * </ol>
	 */
	private void testRenameReplace(List<Supplier<ParameterType>> typeSuppliers, Notification<Parameters, String> notification) throws UndefinedParameterError {
		List<ParameterType> parameterTypes = typeSuppliers.stream().map(Supplier::get).collect(Collectors.toList());
		String[] ops = {ORIGINAL_OPERATOR_NAME, ORIGINAL_OPERATOR_NAME};
		String[] params = {FIRST_PARAMETER_KEY, SECOND_PARAMETER_KEY};
		Parameters parameters = createParametersInstance(parameterTypes, ops, params);
		String[] newOps = {NEW_OPERATOR_NAME, NEW_OPERATOR_NAME};
		Parameters expectedAfterChange = createParametersInstance(parameterTypes, newOps, params);
		testNotification(notification, parameters, expectedAfterChange);
	}

	private void testNotification(Notification<Parameters, String> notification, Parameters parameters, Parameters expectedAfterChange) throws UndefinedParameterError {
		notification.notify(parameters, ORIGINAL_OPERATOR_NAME, NEW_OPERATOR_NAME);
		for (ParameterType type : parameters.getParameterTypes()) {
			assertEquals(type.getKey(), expectedAfterChange.getParameter(type.getKey()), parameters.getParameter(type.getKey()));
		}
	}

	private Parameters createParametersInstance(List<ParameterType> parameterTypes, String[] ops, String[] params) {
		Parameters parameters = new Parameters(parameterTypes);
		for (ParameterType type : parameterTypes) {
			String value;
			if (type.getKey().equals(PARAMETER_OPERATOR_VALUE)) {
				String[] valueParams = Arrays.stream(params).map(v -> v == null ? null : VALUE_NAME).toArray(String[]::new);
				value = valueGenerator.get(type.getClass()).apply(type).apply(ops, valueParams);
			} else {
				value = valueGenerator.get(type.getClass()).apply(type).apply(ops, params);
			}
			parameters.setParameter(type.getKey(), value);
		}
		return parameters;
	}

	private static Operator getOperator(String name, List<ParameterType> parameters) throws OperatorCreationException {
		OperatorDocumentation documentation = mock(OperatorDocumentation.class);
		when(documentation.getShortName()).thenReturn(name);

		OperatorDescription description = mock(OperatorDescription.class);
		when(description.getOperatorDocumentation()).thenReturn(documentation);
		when(description.createOperatorInstance()).then((Answer<Operator>) invocation ->
				new Operator(description) {
					@Override
					public List<ParameterType> getParameterTypes() {
						return parameters;
					}
				});
		return description.createOperatorInstance();
	}

	private static void addSpecialValue(Operator op, String valName, boolean isDouble) {
		Value value;
		if (isDouble) {
			value = new ValueDouble(valName, "") {
				@Override
				public double getDoubleValue() {
					return 0;
				}
			};
		} else {
			value = new ValueString(valName, "") {
				@Override
				public String getStringValue() {
					return "";
				}
			};
		}
		op.addValue(value);
	}
}

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
package com.rapidminer.connection.valueprovider.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import com.rapidminer.MacroHandler;
import com.rapidminer.Process;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.operator.Operator;


/**
 * Test for the MacroValueProviderHandler
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class MacroValueProviderHandlerTest {

	private static final String PREFIX = "myPrefix";
	private static final Map<String, String> REQUESTED = Stream.of("first", "second").collect(Collectors.toMap(s -> s, s -> s));

	@Test
	public void testInjectValuesSuccessful() {
		MacroValueProviderHandler handler = MacroValueProviderHandler.getInstance();
		ValueProvider provider = handler.createNewProvider("provider", PREFIX);
		Operator operator = getOperatorWithMacros();
		Map<String, String> result = handler.injectValues(provider, REQUESTED, operator, null);

		Map<String, String> expected = new HashMap<>();
		expected.put("first", "first");
		expected.put("second", "other");

		// Test equals
		assertEquals(expected, result);
	}

	@Test
	public void testInjectValuesWrongType() {
		MacroValueProviderHandler handler = MacroValueProviderHandler.getInstance();
		// Test not matching case
		ValueProvider correctProvider = handler.createNewProvider("name", PREFIX);
		ValueProvider wrongProvider = new ValueProviderImpl("name", "wrong", correctProvider.getParameters());
		Operator operator = getOperatorWithMacros();
		Map<String, String> wrongResult = handler.injectValues(wrongProvider, REQUESTED, operator, null);
		// Test equals
		assertTrue(wrongResult.isEmpty());
	}

	@Test
	public void testInjectValuesWrongPrefix() {
		MacroValueProviderHandler handler = MacroValueProviderHandler.getInstance();
		// Test correct type, but no values
		ValueProvider wrongProvider2 = handler.createNewProvider("name");
		Operator operator = getOperatorWithMacros();
		Map<String, String> wrongResult = handler.injectValues(wrongProvider2, REQUESTED, operator, null);
		// Test equals
		assertTrue(wrongResult.isEmpty());
	}

	@Test
	public void testIsConfigurable() {
		// ensure that the macroValueProvider is always configurable
		assertTrue(MacroValueProviderHandler.getInstance().isConfigurable());
	}

	private static Operator getOperatorWithMacros() {
		String prefix = PREFIX + MacroValueProviderHandler.PARAMETER_PREFIX_SEPARATOR;
		Map<String, String> macroMap = new HashMap<>();
		macroMap.put(prefix + "first", "first");
		macroMap.put(prefix + "second", "other");
		return getOperatorWithMacros(macroMap);
	}

	static Operator getOperatorWithMacros(Map<String, String> macroMap) {
		Process process = mock(Process.class);
		Operator operator = mock(Operator.class);
		MacroHandler macros = mock(MacroHandler.class);
		when(macros.getMacro(anyString(), eq(operator))).then(in -> macroMap.get(in.<String>getArgument(0)));
		when(process.getMacroHandler()).thenReturn(macros);
		when(operator.getProcess()).thenReturn(process);
		return operator;
	}

}
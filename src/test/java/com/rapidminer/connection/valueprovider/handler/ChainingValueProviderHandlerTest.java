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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.operator.Operator;


/**
 * Test class for {@link ChainingValueProviderHandler}
 *
 * @since 9.3
 * @author Jan Czogalla
 */
public class ChainingValueProviderHandlerTest {

	private static final String PARAMETER_GROUP = "group";
	private static final String PARAMETER_NAME = "name";
	private static final String PARAMETER_VALUE = "value";

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String[] VP_NAMES = {"first", "second", "third"};

	private static final List<ValueProvider> VALUE_PROVIDERS = Arrays.asList(
			MacroValueProviderHandler.getInstance().createNewProvider(VP_NAMES[0], "normal"),
			MacroValueProviderHandler.getInstance().createNewProvider(VP_NAMES[1]),
			MacroValueProviderHandler.getInstance().createNewProvider(VP_NAMES[2])
	);
	/**
	 * First VP injects second VP's prefix, second VP injects third VP's prefix; third VP injects actual value
	 *
	 * The injection for this works as follows:
	 * <ol>
	 *     <li>The first VP has the prefix {@code normal} and thus will look for macros that start with {@code normal_}.
	 *     The second VP's prefix parameter is {@code null}, so the first VP looks for the macro {@code normal_prefix},
	 *     which in the best case is {@code other}.</li>
	 *     <li>The second VP injects the third VP's prefix parameter (looking for the macro {@code other_prefix} which
	 *     should resolve to {@code special}).</li>
	 *     <li>The third VP is now ready to inject a parameter with the name {@value #PARAMETER_NAME} by finding
	 *     the macro {@code special_name}</li>
	 * </ol>
	 * @see #createOperator() creation of operator and macro map
	 */
	private static final ValueProvider CHAINING = ChainingValueProviderHandler.getInstance().createNewProvider("chain",
			VALUE_PROVIDERS.stream().map(ValueProvider::getName).collect(Collectors.toList()));

	@Test
	public void testInjectProvidersSuccessful() {
		Map<String, String> injectedValues = ValueProviderHandlerRegistry.getInstance()
				.injectValues(createConnection(getVPListCopy(), CHAINING), createOperator(), true);
		assertEquals(Collections.singletonMap(PARAMETER_GROUP + "." + PARAMETER_NAME, PARAMETER_VALUE), injectedValues);
	}

	@Test
	public void testInjectProvidersLoop() {
		// one vp is referenced twice; this will result in no injection at all
		ValueProvider faultyChain = ChainingValueProviderHandler.getInstance().createNewProvider("faulty chain",
				Arrays.asList(VP_NAMES[0], VP_NAMES[1], VP_NAMES[2], VP_NAMES[0]));
		Map<String, String> injectValues = ValueProviderHandlerRegistry.getInstance()
				.injectValues(createConnection(getVPListCopy(), faultyChain), createOperator(), true);
		assertTrue(injectValues.isEmpty());
	}

	@Test
	public void testInjectProvidersWrongProviderInjection() {
		List<ValueProvider> faultyProviders = getVPListCopy();
		// the second VP actually already has a prefix, and it's not the correct one to inject the third
		faultyProviders.get(1).getParameterMap().get(MacroValueProviderHandler.PARAMETER_PREFIX).setValue("different");
		Map<String, String> injectValues = ValueProviderHandlerRegistry.getInstance()
				.injectValues(createConnection(faultyProviders, CHAINING), createOperator(), true);
		assertTrue(injectValues.isEmpty());
	}

	@Test
	public void testMissingHandler() {
		List<ValueProvider> faultyProviders = getVPListCopy();
		// the second VP actually has an unregistered type
		ValueProvider originalSecond = faultyProviders.get(1);
		faultyProviders.set(1, new ValueProviderImpl(originalSecond.getName(), "wrong"));
		Map<String, String> injectValues = ValueProviderHandlerRegistry.getInstance()
				.injectValues(createConnection(faultyProviders, CHAINING), createOperator(), true);
		assertTrue(injectValues.isEmpty());
	}

	@Test
	public void testSanityChecks() {
		ChainingValueProviderHandler cvph = ChainingValueProviderHandler.getInstance();
		List<ValueProvider> vps = Arrays.asList(null,
				new ValueProviderImpl("Wrong Type", "wrong"),
				new ValueProviderImpl("Missing Parameter", ChainingValueProviderHandler.TYPE),
				cvph.createNewProvider("Empty"),
				cvph.createNewProvider("Singleton", Collections.singletonList("unknown")));
		for (ValueProvider vp : vps) {
			Set<ValueProvider> chainedProviders = cvph.findChainedProviders(vp, Collections.emptyMap());
			assertTrue("Unexpected non-empty list for " + (vp == null ? "null" : vp.getName()),
					chainedProviders.isEmpty());
		}
	}

	@Test
	public void testDisableParameters() {
		// this test is similar to testInjectProvidersSuccessful() but with the parameter of the second vp disabled
		List<ValueProvider> vpListCopy = getVPListCopy();
		ValueProviderParameter secondPrefixParameter =
				vpListCopy.get(1).getParameterMap().get(MacroValueProviderHandler.PARAMETER_PREFIX);
		secondPrefixParameter.setEnabled(false);
		Map<String, String> injectedValues = ValueProviderHandlerRegistry.getInstance()
				.injectValues(createConnection(vpListCopy, CHAINING), createOperator(), true);
		assertTrue(injectedValues.isEmpty());
	}

	/** Create an operator with our basic macros */
	private static Operator createOperator() {
		Map<String, String> macroMap = new HashMap<>();
		macroMap.put("normal_prefix", "other");
		macroMap.put("other_prefix", "special");
		macroMap.put("special_" + PARAMETER_NAME, PARAMETER_VALUE);
		return MacroValueProviderHandlerTest.getOperatorWithMacros(macroMap);
	}

	/** Create a {@link ConnectionConfiguration} with an injected parameter {@value #PARAMETER_NAME} */
	private static ConnectionInformation createConnection(List<ValueProvider> valueProviders, ValueProvider chaining) {
		ConnectionConfigurationBuilder ccBuilder = new ConnectionConfigurationBuilder("chaining", "test");
		ConfigurationParameterImpl parameter = new ConfigurationParameterImpl(PARAMETER_NAME);
		parameter.setInjectorName("third");
		ccBuilder.withKeys(PARAMETER_GROUP, Collections.singletonList(parameter));
		ccBuilder.withValueProvider(chaining);
		valueProviders.forEach(ccBuilder::withValueProvider);
		return new ConnectionInformationBuilder(ccBuilder.build()).build();
	}

	/** Get a deep clone of the basic list of VPs */
	private static <T> List<T> getVPListCopy() {
		try {
			return MAPPER.readValue(MAPPER.writeValueAsString(ChainingValueProviderHandlerTest.VALUE_PROVIDERS),
					MAPPER.getTypeFactory().constructCollectionType(List.class, ValueProvider.class));
		} catch (IOException e) {
			return new ArrayList<>();
		}
	}
}
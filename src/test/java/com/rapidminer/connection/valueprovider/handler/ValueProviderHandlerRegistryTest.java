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

import static com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry.getInstance;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.wrapIntoPlaceholder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.configuration.PlaceholderParameter;
import com.rapidminer.connection.configuration.PlaceholderParameterImpl;
import com.rapidminer.connection.util.GenericHandlerRegistry.MissingHandlerException;
import com.rapidminer.connection.util.GenericRegistrationEventListener;
import com.rapidminer.connection.util.RegistrationEvent;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.operator.Operator;


/**
 * Testing the {@link ValueProviderHandlerRegistry}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ValueProviderHandlerRegistryTest {

	private static final String TYPE = "test type";
	private static final Map<String, String> INJECTED_VALUES = new HashMap<>();
	private static List<ValueProviderHandler> originalHandlers;

	@BeforeClass
	public static void preClass() {
		List<String> allTypes = getInstance().getAllTypes();
		originalHandlers = allTypes.stream().map(getInstance()::getHandler).collect(Collectors.toList());
	}

	@Before
	public void preRunCleanup() {
		INJECTED_VALUES.clear();
		getInstance().getAllTypes().stream().map(getInstance()::getHandler).forEach(getInstance()::unregisterHandler);
	}

	@AfterClass
	public static void postClass() {
		if (originalHandlers == null || originalHandlers.isEmpty()) {
			return;
		}
		originalHandlers.forEach(getInstance()::registerHandler);
	}

	@Test(expected = IllegalArgumentException.class)
	public void registerNull() {
		getInstance().registerHandler(null);
	}

	@Test
	public void injectToNull() {
		assertNull(getInstance().injectValues(null, null, true));
		assertNull(getInstance().injectValues(null, null, false));
	}

	@Test
	public void registerWithKeys() {
		assertTrue(getInstance().getAllTypes().isEmpty());
		assertFalse(getInstance().isTypeKnown(TYPE));

		final ValueProviderHandler valueProviderHandler = getValueProviderHandler(TYPE);
		getInstance().registerHandler(valueProviderHandler);
		ValueProvider valueProvider = valueProviderHandler.createNewProvider("a");

		final List<ConfigurationParameter> configParams = new ArrayList<>();
		configParams.add(new ConfigurationParameterImpl("rapid", wrapIntoPlaceholder("123"), false, valueProvider.getName(), true));
		configParams.add(new ConfigurationParameterImpl("pirad", "arrrr", false));
		configParams.add(new ConfigurationParameterImpl("foo", null, false, "injectorName", false));
		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		keys.put("keykey", configParams);
		final ConnectionConfiguration connectionConfiguration = new ConnectionConfigurationBuilder("test", "connection")
				.withValueProvider(valueProvider).withKeys(keys).build();
		ConnectionInformation connectionInformation = new ConnectionInformationBuilder(connectionConfiguration).build();

		Map<String, String> myInjectedValues = getInstance().injectValues(connectionInformation, null, false);
		checkStaticParameters(myInjectedValues);

		myInjectedValues = getInstance().injectValues(connectionInformation, null, true);
		assertTrue(myInjectedValues.isEmpty());

		INJECTED_VALUES.put("foo", "bar");
		INJECTED_VALUES.put("keykey", "valval");
		INJECTED_VALUES.put("rapid", "miner");
		// should not be injected!
		INJECTED_VALUES.put("pirad", "PIRAD");

		myInjectedValues = getInstance().injectValues(connectionInformation, null, false);
		checkStaticParameters(myInjectedValues);
		assertEquals("miner", myInjectedValues.get("keykey.rapid"));
		assertEquals("arrrr", myInjectedValues.get("keykey.pirad"));
		assertEquals(5, myInjectedValues.size());
	}

	@Test
	public void registerWithKeysAndPlaceholderKeys() {
		assertTrue(getInstance().getAllTypes().isEmpty());
		assertFalse(getInstance().isTypeKnown(TYPE));

		final ValueProviderHandler valueProviderHandler = getValueProviderHandler(TYPE);
		getInstance().registerHandler(valueProviderHandler);
		ValueProvider valueProvider = valueProviderHandler.createNewProvider("a");
		String vpName = valueProvider.getName();

		final List<ConfigurationParameter> configParams = new ArrayList<>();
		configParams.add(new ConfigurationParameterImpl("rapid", wrapIntoPlaceholder("123"), false, vpName, true));
		configParams.add(new ConfigurationParameterImpl("pirad", "arrrr", false));
		configParams.add(new ConfigurationParameterImpl("replaced", wrapIntoPlaceholder("keykey.rapid"), false));
		configParams.add(new ConfigurationParameterImpl("replaced_local", wrapIntoPlaceholder("rapid"), false));
		configParams.add(new ConfigurationParameterImpl("replaced_cross", wrapIntoPlaceholder("grupo_uno.placeholder1"), false));
		configParams.add(new ConfigurationParameterImpl("replaced_cross_inj", wrapIntoPlaceholder("grupo_uno.placeholder2"), false));
		configParams.add(new ConfigurationParameterImpl("combined", "rapid: " + wrapIntoPlaceholder("rapid") + " [value]", false));
		configParams.add(new ConfigurationParameterImpl("multiple", wrapIntoPlaceholder("rapid") + ":" + wrapIntoPlaceholder("grupo_uno.placeholder1") , false));
		configParams.add(new ConfigurationParameterImpl("nested", wrapIntoPlaceholder("test-" + wrapIntoPlaceholder("pirad")), false));
		configParams.add(new ConfigurationParameterImpl("disabled", wrapIntoPlaceholder("rapid"), false, "injectorName", false));

		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		keys.put("keykey", configParams);
		List<PlaceholderParameter> placeholders = new ArrayList<>();
		PlaceholderParameterImpl staticPlaceholder = new PlaceholderParameterImpl("placeholder1", "grupo_uno");
		staticPlaceholder.setValue("static ONE");
		placeholders.add(staticPlaceholder);
		placeholders.add(new PlaceholderParameterImpl("placeholder2", "grupo_uno", vpName));
		placeholders.add(new PlaceholderParameterImpl("placeholder3", "grupo_uno", vpName));
		final ConnectionConfiguration connectionConfiguration = new ConnectionConfigurationBuilder("test", "connection")
				.withValueProvider(valueProvider).withKeys(keys).withPlaceholders(placeholders).build();
		ConnectionInformation connectionInformation = new ConnectionInformationBuilder(connectionConfiguration).build();

		Map<String, String> myInjectedValues = getInstance().injectValues(connectionInformation, null, false);
		checkStaticParameters(myInjectedValues);

		myInjectedValues = getInstance().injectValues(connectionInformation, null, true);
		assertEquals("There should be five injected value here, but we got " + myInjectedValues.size() + ": " + myInjectedValues.keySet().toString(),
				7, myInjectedValues.size());
		assertEquals("rapid:  [value]", myInjectedValues.get("keykey.combined"));

		INJECTED_VALUES.put("foo", "bar");
		INJECTED_VALUES.put("keykey", "valval");
		INJECTED_VALUES.put("rapid", "miner");
		// should not be injected!
		INJECTED_VALUES.put("pirad", "PIRAD");
		INJECTED_VALUES.put("placeholder2", "INJECTED 2");

		myInjectedValues = getInstance().injectValues(connectionInformation, null, false);
		checkStaticParameters(myInjectedValues);
		assertEquals("miner", myInjectedValues.get("keykey.rapid"));
		assertEquals("miner", myInjectedValues.get("keykey.replaced"));
		assertEquals("miner", myInjectedValues.get("keykey.replaced_local"));
		assertEquals("arrrr", myInjectedValues.get("keykey.pirad"));
		assertEquals("static ONE", myInjectedValues.get("grupo_uno.placeholder1"));
		assertEquals("static ONE", myInjectedValues.get("keykey.replaced_cross"));
		assertEquals("INJECTED 2", myInjectedValues.get("grupo_uno.placeholder2"));
		assertEquals("INJECTED 2", myInjectedValues.get("keykey.replaced_cross_inj"));
		assertEquals("rapid: miner [value]", myInjectedValues.get("keykey.combined"));
		assertEquals("miner:static ONE", myInjectedValues.get("keykey.multiple"));
		assertEquals(wrapIntoPlaceholder("test-arrrr"), myInjectedValues.get("keykey.nested"));
		assertNull(myInjectedValues.get("grupo_uno.placeholder3"));
		assertEquals(14, myInjectedValues.size());
	}

	@Test
	public void registerWithPlaceholderSelfReference() {
		assertTrue(getInstance().getAllTypes().isEmpty());
		assertFalse(getInstance().isTypeKnown(TYPE));

		final ValueProviderHandler valueProviderHandler = getValueProviderHandler(TYPE);
		getInstance().registerHandler(valueProviderHandler);

		final List<ConfigurationParameter> configParams = new ArrayList<>();
		configParams.add(new ConfigurationParameterImpl("name", "value", false));
		configParams.add(new ConfigurationParameterImpl("normal", wrapIntoPlaceholder("name"), false));
		ConfigurationParameterImpl directOffender = new ConfigurationParameterImpl("direct", wrapIntoPlaceholder("direct"), false);
		configParams.add(directOffender);
		configParams.add(new ConfigurationParameterImpl("other", wrapIntoPlaceholder("direct"), false));
		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		keys.put("keykey", configParams);
		final ConnectionConfiguration connectionConfiguration = new ConnectionConfigurationBuilder("test", "connection").withValueProvider(valueProviderHandler.createNewProvider("a")).withKeys(keys).build();
		ConnectionInformation connectionInformation = new ConnectionInformationBuilder(connectionConfiguration).build();

		Map<String, String> myInjectedValues = getInstance().injectValues(connectionInformation, null, false);
		checkStaticParameters(myInjectedValues);

		myInjectedValues = getInstance().injectValues(connectionInformation, null, true);
		assertEquals("value", myInjectedValues.get("keykey.normal"));
		assertEquals("", myInjectedValues.get("keykey.direct"));
		assertEquals("", myInjectedValues.get("keykey.other"));

		directOffender.setValue("new value");
		myInjectedValues = getInstance().injectValues(connectionInformation, null, true);
		assertEquals(2, myInjectedValues.size());
		assertEquals(directOffender.getValue(), myInjectedValues.get("keykey.other"));
	}

	@Test
	public void checkEventsFired() {
		final ValueProviderHandlerRegistryListener firstListener = mock(ValueProviderHandlerRegistryListener.class);
		final ValueProviderHandlerRegistryListener secondListener = mock(ValueProviderHandlerRegistryListener.class);
		getInstance().addEventListener(firstListener);
		try {
			getInstance().registerHandler(null);
		} catch (IllegalArgumentException iae) {
			// supposed to happen without informing the listeners
		}

		final ArgumentMatcher<RegistrationEvent> registrationEventMatcher = arg -> arg.getType() == RegistrationEvent.RegistrationEventType.REGISTERED;
		final ArgumentMatcher<RegistrationEvent> unregistrationEventMatcher = arg -> arg.getType() == RegistrationEvent.RegistrationEventType.UNREGISTERED;

		verify(firstListener, times(0)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());

		final ValueProviderHandler valueProviderHandler = getValueProviderHandler(TYPE);
		getInstance().registerHandler(valueProviderHandler);

		verify(firstListener, times(1)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());

		getInstance().unregisterHandler(valueProviderHandler);

		verify(firstListener, times(1)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(1)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());

		getInstance().addEventListener(secondListener);
		getInstance().registerHandler(valueProviderHandler);

		verify(firstListener, times(2)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(1)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(1)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());

		getInstance().registerHandler(valueProviderHandler);

		verify(firstListener, times(2)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(1)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(1)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(0)).registrationChanged(argThat(unregistrationEventMatcher), any());

		getInstance().unregisterHandler(valueProviderHandler);

		verify(firstListener, times(2)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(2)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(1)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(1)).registrationChanged(argThat(unregistrationEventMatcher), any());

		// create a failing listener to see the other listeners will be called even if one fails
		final GenericRegistrationEventListener<ValueProviderHandler> failingListener = (event, changedObject) -> {
			throw new RuntimeException("Check failing the eventlistener handling");
		};

		// insert the failing listener in position one
		getInstance().removeEventListener(firstListener);
		getInstance().removeEventListener(secondListener);
		getInstance().addEventListener(failingListener);
		getInstance().addEventListener(firstListener);
		getInstance().addEventListener(secondListener);

		getInstance().registerHandler(valueProviderHandler);

		verify(firstListener, times(3)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(2)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(2)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(1)).registrationChanged(argThat(unregistrationEventMatcher), any());

		getInstance().unregisterHandler(valueProviderHandler);

		verify(firstListener, times(3)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(firstListener, times(3)).registrationChanged(argThat(unregistrationEventMatcher), any());
		verify(secondListener, times(2)).registrationChanged(argThat(registrationEventMatcher), any());
		verify(secondListener, times(2)).registrationChanged(argThat(unregistrationEventMatcher), any());


		getInstance().removeEventListener(firstListener);
		getInstance().removeEventListener(secondListener);
		getInstance().removeEventListener(failingListener);
	}

	@Test
	public void getUnregisteredHandler() {
		assertTrue(getInstance().getAllTypes().isEmpty());

		String coreMessage = null;
		try {
			getInstance().getHandler(TYPE);
			fail("Get handler did not throw an exception for an unknown handler type");
		} catch (MissingHandlerException e) {
			// ignore?
			coreMessage = e.getMessage().replace('\'' + TYPE + '\'', "");
		}
		String extType = "test:" + TYPE;
		String extMessage = null;
		try {
			getInstance().getHandler(extType);
			fail("Get handler did not throw an exception for an unknown handler type");
		} catch (MissingHandlerException e) {
			// ignore?
			extMessage = e.getMessage().replace('\'' + extType + '\'', "");
		}
		assertNotEquals("Exception messages should be different", coreMessage, extMessage);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addNullEventListener() {
		getInstance().addEventListener(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void remNullEventListener() {
		getInstance().removeEventListener(null);
	}

	private void checkStaticParameters(Map<String, String> injectedValues) {
		assertNotNull(injectedValues);
		assertTrue(injectedValues.containsKey("ID"));
		assertTrue(injectedValues.containsKey("NAME"));
		assertTrue(injectedValues.containsKey("TYPE"));
	}

	public static ValueProviderHandler getValueProviderHandler(String type) {
		return new BaseValueProviderHandler(type) {

			@Override
			public ValueProvider createNewProvider(String name) {
				return super.createNewProvider("VP-" + name);
			}

			@Override
			public Map<String, String> injectValues(ValueProvider vp, Map<String, String> injectables, Operator operator, ConnectionInformation connection) {
				return injectables.entrySet().stream().filter(e -> INJECTED_VALUES.containsKey(e.getValue()))
						.collect(Collectors.toMap(Entry::getKey, e -> INJECTED_VALUES.get(e.getValue())));
			}
		};
	}

}
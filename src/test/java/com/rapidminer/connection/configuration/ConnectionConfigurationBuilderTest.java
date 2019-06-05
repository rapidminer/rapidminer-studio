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
package com.rapidminer.connection.configuration;

import static com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry.getInstance;
import static com.rapidminer.connection.valueprovider.handler.ValueProviderUtils.wrapIntoPlaceholder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.connection.valueprovider.ValueProviderImpl;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandler;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistryTest;


/**
 * Testing the {@link ConnectionConfigurationBuilder}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionConfigurationBuilderTest {

	private static final String CONFIGURATION_TYPE_TEST = "test";

	public static ConnectionConfiguration getDefaultConfiguration() {
		return new ConnectionConfigurationBuilder("default configuration", CONFIGURATION_TYPE_TEST).build();
	}

	public static ConnectionConfiguration getLargeConfiguration() throws IOException {

		String vpTestType = "value provider test type";

		int cfgparam = 0;
		List<ConfigurationParameter> groupkeys = new ArrayList<>();
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "another"));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++,
				"brick", false));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++,
				"in ", false));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "the", false));
		groupkeys.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "wall", false, vpTestType, true));

		String group = "LG (large group)";
		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		final ArrayList<ConfigurationParameter> value = new ArrayList<>();
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++));
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "another"));
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++,
				"brick", false));
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++,
				"in ", false));
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "the", false));
		value.add(new ConfigurationParameterImpl("Config param " + cfgparam++, "wall", false, vpTestType, true));
		keys.put("key map", value);

		String description;
		try (final InputStream inputStream = ConnectionResources.ENCODING_TEST_RESOURCE.openStream()) {
			description = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		}

		String id = UUID.randomUUID().toString();

		List<String> tags = new ArrayList<>();
		tags.add("i");
		tags.add("dont");
		tags.add("know");
		tags.add("what");
		tags.add("u");
		tags.add("did");
		tags.add("last");
		tags.add("summer");

		PlaceholderParameter placeholder = new PlaceholderParameterImpl("placeholder param name", "placeholder param group");
		return new ConnectionConfigurationBuilder("large configuration", CONFIGURATION_TYPE_TEST).withKeys(keys).withKeys(group, groupkeys).withDescription(description).withTags(tags).withTag("afterwards added tag").withPlaceholder(placeholder).withValueProvider(new ValueProviderImpl("value provider test name", vpTestType)).build();
	}

	@Test
	public void testLargeConfigurationContent() throws IOException {
		ConnectionConfiguration conf = getLargeConfiguration();
		assertEquals(CONFIGURATION_TYPE_TEST, conf.getType());
		assertEquals("large configuration", conf.getName());
		assertEquals(2, conf.getKeys().size());
		assertTrue(conf.getKeys().stream().anyMatch(p -> p.getGroup().equals("LG (large group)") || p.getGroup().equals("key map")));
		for (ConfigurationParameterGroup parameterGroup : conf.getKeys()) {
			if (parameterGroup.getGroup().equals("key map")) {
				assertEquals(6, parameterGroup.getParameters().size());
			} else if (parameterGroup.getGroup().equals("LG (large group)")) {
				assertEquals(7, parameterGroup.getParameters().size());
			}
		}
		for (ConfigurationParameter parameterZero : conf.getKeys().get(0).getParameters()) {
			for (ConfigurationParameter parameterOne : conf.getKeys().get(1).getParameters()) {
				assertNotEquals(parameterZero.getName(), parameterOne.getName());
			}
		}
		assertEquals(9, conf.getTags().size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDotInGroup(){
		List<ConfigurationParameter> groupkeys = new ArrayList<>();
		groupkeys.add(new ConfigurationParameterImpl("Config param 1"));
		groupkeys.add(new ConfigurationParameterImpl("Config param 2"));
		new ConnectionConfigurationBuilder("configuration", CONFIGURATION_TYPE_TEST).withKeys("dot.group", groupkeys);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withoutName() {
		new ConnectionConfigurationBuilder((String) null, CONFIGURATION_TYPE_TEST);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withWrongName() {
		new ConnectionConfigurationBuilder("", CONFIGURATION_TYPE_TEST);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructFromNull() throws IOException {
		final ConnectionConfiguration build = new ConnectionConfigurationBuilder(null).build();
	}

	@Test(expected = IllegalArgumentException.class)
	public void withNullNewName() throws IOException {
		new ConnectionConfigurationBuilder(getDefaultConfiguration(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withEmptyNewName() throws IOException {
		new ConnectionConfigurationBuilder(getDefaultConfiguration(), "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void withPlaceholder() {
		new ConnectionConfigurationBuilder("wrong placeholder", CONFIGURATION_TYPE_TEST).withPlaceholder(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withValueProvider() {
		new ConnectionConfigurationBuilder("wrong placeholder", CONFIGURATION_TYPE_TEST).withValueProvider(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withTag() {
		new ConnectionConfigurationBuilder("wrong placeholder", CONFIGURATION_TYPE_TEST).withTag(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void withPlaceholderKeysWithoutValue() {
		final ConnectionConfiguration build = new ConnectionConfigurationBuilder("accept nulls", CONFIGURATION_TYPE_TEST)
				.withPlaceholder(null).build();
		fail("The placeholder should not support null but did: " + build);
	}

	@Test
	public void withKeysWithDuplicates() {
		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		final ArrayList<ConfigurationParameter> value = new ArrayList<>();
		value.add(new ConfigurationParameterImpl("1", "2"));
		value.add(new ConfigurationParameterImpl("1", "2"));
		keys.put("keen", value);
		try {
			final ConnectionConfiguration build = new ConnectionConfigurationBuilder("key with duplicates", CONFIGURATION_TYPE_TEST)
					.withKeys(keys).build();
			fail("Should not be able to build the configuration with duplicate parameter but got " + build);
		} catch (IllegalArgumentException iae) {
			assertEquals("Duplicates in list for \"parameters\" not allowed: 1: 2", iae.getMessage());
		}
	}

	@Test
	public void withPlaceholderKeysWithDuplicates() {
		List<PlaceholderParameter> value = new ArrayList<>();
		value.add(new PlaceholderParameterImpl("1", "group1"));
		value.add(new PlaceholderParameterImpl("1", "group1"));
		try {
			final ConnectionConfiguration build = new ConnectionConfigurationBuilder("placeholder with duplicates", CONFIGURATION_TYPE_TEST)
					.withPlaceholders(value).build();
			fail("Should not be able to build the configuration with duplicate parameter but got " + build);
		} catch (IllegalArgumentException iae) {
			assertEquals("Duplicates in list for \"placeholders\" not allowed: group1 : 1", iae.getMessage());
		}
	}

	@Test
	public void withPlaceholderKeysWithDot() {
		try {
			PlaceholderParameterImpl param = new PlaceholderParameterImpl("1", "group.1");
			fail("Should not be able to construct parameter with dot in group name but got " + param);
		} catch (IllegalArgumentException iae) {
			assertEquals("Strings containing dots for \"group\" not allowed", iae.getMessage());
		}
	}

	@Test
	public void acceptingNullInputs() {
		final ConnectionConfiguration accept = new ConnectionConfigurationBuilder("accept nulls", CONFIGURATION_TYPE_TEST)
				.withDescription(null)
				.withTags(null)
				.withValueProviders(null)
				.withKeys(null)
				.build();
		assertNotNull(accept);
	}


	@Test
	public void duplicateParamTest() {
		final ValueProviderHandler valueProviderHandler = ValueProviderHandlerRegistryTest.getValueProviderHandler(CONFIGURATION_TYPE_TEST);
		getInstance().registerHandler(valueProviderHandler);

		final List<ConfigurationParameter> configParams = new ArrayList<>();
		configParams.add(new ConfigurationParameterImpl("rapid", wrapIntoPlaceholder("123"), false, "none", true));
		configParams.add(new ConfigurationParameterImpl("rapid", wrapIntoPlaceholder("123"), false, "none", true));
		Map<String, List<ConfigurationParameter>> keys = new HashMap<>();
		keys.put("keykey", configParams);
		try {
			final ConnectionConfiguration connectionConfiguration = new ConnectionConfigurationBuilder("test", "connection").withValueProvider(valueProviderHandler.createNewProvider("a")).withKeys(keys).build();
			fail("Duplicate parameter for keys is not allowed");
		} catch (IllegalArgumentException iae) {
			assertEquals("Duplicates in list for \"parameters\" not allowed: rapid: null", iae.getMessage());
		}
	}


	@Test
	public void ensureIdIsKeptOnCopy() throws IOException {
		ConnectionConfiguration connection =  new ConnectionConfigurationBuilder("name", CONFIGURATION_TYPE_TEST).build();
		ConnectionConfiguration newConnection = new ConnectionConfigurationBuilder(connection).withDescription("bla").build();
		assertEquals(connection.getId(), newConnection.getId());
		assertNotNull("A connection configuration must have an id", connection.getId());
	}

	@Test
	public void ensureDifferentIds() throws IOException {
		ConnectionConfiguration connection =  new ConnectionConfigurationBuilder("name", CONFIGURATION_TYPE_TEST).build();
		ConnectionConfiguration newConnection =  new ConnectionConfigurationBuilder("name", CONFIGURATION_TYPE_TEST).build();
		assertNotEquals(connection.getId(), newConnection.getId());
	}

	@Test
	public void testCloneBuilder() throws IOException {
		final ConnectionConfiguration largeConfiguration = getLargeConfiguration();
		final ConnectionConfiguration clonedConfig = new ConnectionConfigurationBuilder(largeConfiguration).build();
		assertTrue(largeConfiguration.equals(clonedConfig));
	}


	@Test
	public void loadConfigWithGroupDots() throws IOException {
		Map<String, String> dotReplacement = new HashMap<>();
		dotReplacement.put("key map", "key.map");
		dotReplacement.put("LG (large group)", "LG (large.group)");
		dotReplacement.put("placeholder param group", "placeholder param.group");
		String serialized =
				new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(getLargeConfiguration());
		for (Map.Entry<String, String> entry : dotReplacement.entrySet()) {
			String replaced = serialized.replace(entry.getKey(), entry.getValue());
			//stream version
			try {
				ConnectionInformationSerializer.LOCAL.loadConfiguration(new ByteArrayInputStream(replaced.getBytes(StandardCharsets.UTF_8)));
				fail("Groups with dots should not be allowed");
			} catch (InvalidDefinitionException e) {
				assertEquals(IllegalArgumentException.class, e.getCause().getClass());
			}
			//reader version
			try {
				ConnectionInformationSerializer.LOCAL.loadConfiguration(new StringReader(replaced));
				fail("Groups with dots should not be allowed");
			} catch (InvalidDefinitionException e) {
				assertEquals(IllegalArgumentException.class, e.getCause().getClass());
			}
		}
	}
}
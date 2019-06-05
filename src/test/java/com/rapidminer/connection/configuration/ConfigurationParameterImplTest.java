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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.security.Key;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;


/**
 * Tests for {@link ConfigurationParameterImpl}. Includes Includes serialization/deserialization and manipulating
 * encryption and injection state.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConfigurationParameterImplTest {

	private static String name;
	private static String value;
	private static String injectorName;
	private static ConfigurationParameter[] parameters;
	private static ObjectMapper mapper;
	private static ObjectWriter writer;

	private static final Key USER_KEY;

	static {
		Key userKey = null;
		try {
			userKey = KeyGeneratorTool.getUserKey();
		} catch (IOException e) {
			// ignore
		}
		USER_KEY = userKey;
	}

	@BeforeClass
	public static void setup() throws KeyGenerationException {
		name = RandomStringUtils.randomAlphabetic(5);
		value = RandomStringUtils.randomAlphanumeric(10);
		injectorName = RandomStringUtils.randomAlphabetic(5);
		parameters = new ConfigurationParameter[8];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = new ConfigurationParameterImpl(name, value, (i & 1) == 1, (i & 2) == 2 ? injectorName : null, i > 4);
		}
		mapper = new ObjectMapper();
		writer = mapper.writerWithDefaultPrettyPrinter();
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
	}

	@AfterClass
	public static void restoreValues() {
		KeyGeneratorTool.setUserKey(USER_KEY);
	}

	@Test
	public void testSerialisation() throws IOException {
		for (ConfigurationParameter parameter : parameters) {
			String serialized = writer.writeValueAsString(parameter);
			System.out.println(serialized + "\n");
			ConfigurationParameter deserialized = mapper.readValue(serialized, ConfigurationParameter.class);
			assertEquals("encryption state not equal after serialization", parameter.isEncrypted(), deserialized.isEncrypted());
			assertEquals("injector name not equal after serialization", parameter.getInjectorName(), deserialized.getInjectorName());
			assertEquals("enabled state not equal after serialization", parameter.isEnabled(), deserialized.isEnabled());
			assertEquals("names not equal after serialization", parameter.getName(), deserialized.getName());
			assertEquals("values not equal after serialization", parameter.getValue(), deserialized.getValue());
			String doubleSerialized = writer.writeValueAsString(deserialized);
			assertEquals("double serialization breaks json", serialized, doubleSerialized);
		}
	}

	@Test
	public void testUnsetInjectionInSerialization() throws IOException {
		ConfigurationParameter parameter = new ConfigurationParameterImpl(name, value, false);
		parameter.setInjectorName(injectorName);
		String serialized = writer.writeValueAsString(parameter);
		serialized = serialized.replace("null", "\"" + value + "\"");
		serialized = serialized.replace("\"injectorName\" : \"" + injectorName + "\"", "\"injectorName\" : null");
		ConfigurationParameter deserialized = mapper.readValue(serialized, ConfigurationParameter.class);
		assertNotEquals("Values equal after disabling injection", parameter.getValue(), deserialized.getValue());
	}

	@Test
	public void testSetInjectionInSerialization() throws IOException {
		String otherValue = RandomStringUtils.randomAlphanumeric(8);
		ConfigurationParameter parameters[] = {new ConfigurationParameterImpl(name, value, true),
				new ConfigurationParameterImpl(name, otherValue, true),
				new ConfigurationParameterImpl(name, value, false),
				new ConfigurationParameterImpl(name, otherValue, false)};
		for (ConfigurationParameter parameter : parameters) {
			String serialized = writer.writeValueAsString(parameter);
			serialized = serialized.replace("\"injectorName\" : null", "\"injectorName\" : \"" + injectorName + "\"");
			ConfigurationParameter deserialized = mapper.readValue(serialized, ConfigurationParameter.class);
			assertNotEquals("Values equal after enabling injection", parameter.getValue(), deserialized.getValue());
			assertNull("Value not null after injection turned on", deserialized.getValue());
		}
	}

	@AfterClass
	public static void tearDown() {
		writer = null;
		mapper = null;
		parameters = null;
	}
}
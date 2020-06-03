/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.tools.encryption.EncryptionProvider;


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


	@BeforeClass
	public static void setup() {
		name = RandomStringUtils.randomAlphabetic(5);
		value = RandomStringUtils.randomAlphanumeric(10);
		injectorName = RandomStringUtils.randomAlphabetic(5);
		parameters = new ConfigurationParameter[8];
		for (int i = 0; i < parameters.length; i++) {
			parameters[i] = new ConfigurationParameterImpl(name, value, (i & 1) == 1, (i & 2) == 2 ? injectorName : null, i > 4);
		}
		EncryptionProvider.initialize();
	}

	@Test
	public void testSerialisation() throws IOException {
		for (ConfigurationParameter parameter : parameters) {
			String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
			ConfigurationParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ConfigurationParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
			assertEquals("encryption state not equal after serialization", parameter.isEncrypted(), deserialized.isEncrypted());
			assertEquals("injector name not equal after serialization", parameter.getInjectorName(), deserialized.getInjectorName());
			assertEquals("enabled state not equal after serialization", parameter.isEnabled(), deserialized.isEnabled());
			assertEquals("names not equal after serialization", parameter.getName(), deserialized.getName());
			assertEquals("values not equal after serialization", parameter.getValue(), deserialized.getValue());

			// can only check for equality if the parameter is not encrypted, as encrypting the same input multiple times yields different outputs each time for increased security
			if (!parameter.isEncrypted()) {
				String doubleSerialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(deserialized, EncryptionProvider.DEFAULT_CONTEXT);
				assertEquals("double serialization breaks json", serialized, doubleSerialized);
			}
		}
	}

	@Test
	public void testUnsetInjectionInSerialization() throws IOException {
		ConfigurationParameter parameter = new ConfigurationParameterImpl(name, value, false);
		parameter.setInjectorName(injectorName);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		serialized = serialized.replace("null", "\"" + value + "\"");
		serialized = serialized.replace("\"injectorName\" : \"" + injectorName + "\"", "\"injectorName\" : null");
		ConfigurationParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ConfigurationParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertNotEquals("Values equal after disabling injection", parameter.getValue(), deserialized.getValue());
	}

	@Test
	public void testSetInjectionInSerialization() throws IOException {
		String otherValue = RandomStringUtils.randomAlphanumeric(8);
		ConfigurationParameter[] parameters = {new ConfigurationParameterImpl(name, value, true),
				new ConfigurationParameterImpl(name, otherValue, true),
				new ConfigurationParameterImpl(name, value, false),
				new ConfigurationParameterImpl(name, otherValue, false)};
		for (ConfigurationParameter parameter : parameters) {
			String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
			serialized = serialized.replace("\"injectorName\" : null", "\"injectorName\" : \"" + injectorName + "\"");
			ConfigurationParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ConfigurationParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
			assertNotEquals("Values equal after enabling injection", parameter.getValue(), deserialized.getValue());
			assertNull("Value not null after injection turned on", deserialized.getValue());
		}
	}

}
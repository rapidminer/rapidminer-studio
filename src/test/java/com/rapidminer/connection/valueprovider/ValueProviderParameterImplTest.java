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
package com.rapidminer.connection.valueprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
 * Tests for {@link ValueProviderParameterImpl}. Includes serialization/deserialization, encryption and
 * altering the encryption state in the serialized string.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ValueProviderParameterImplTest {

	private static String name;
	private static String value;
	private static ValueProviderParameter[] parameters;
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
		parameters = new ValueProviderParameter[8];
		for (int i = 0; i < 8; i++) {
			parameters[i] = new ValueProviderParameterImpl(name, i % 2 == (i < 4 ? 0 : 1) ? value : null, i % 4 > 1,
					(i % 4) % 3 != 0);
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
		for (ValueProviderParameter parameter : parameters) {
			String serialized = writer.writeValueAsString(parameter);
			ValueProviderParameter deserialized = mapper.readValue(serialized, ValueProviderParameter.class);
			assertEquals("encryption state not equal after serialization", parameter.isEncrypted(), deserialized.isEncrypted());
			assertEquals("enabled state not equal after serialization", parameter.isEnabled(), deserialized.isEnabled());
			assertEquals("names not equal after serialization", parameter.getName(), deserialized.getName());
			assertEquals("values not equal after serialization", parameter.getValue(), deserialized.getValue());
			String doubleSerialized = writer.writeValueAsString(deserialized);
			assertEquals("double serialization breaks json", serialized, doubleSerialized);
		}
	}

	@Test
	public void testEncryption() throws IOException {
		for (ValueProviderParameter parameter : parameters) {
			String serialized = writer.writeValueAsString(parameter);
			boolean isPlainText = serialized.contains(String.valueOf(parameter.getValue()));
			boolean isNull = parameter.getValue() == null;
			boolean isEncrypted = parameter.isEncrypted();
			assertEquals("Value was not correctly written", !isEncrypted || isNull, isPlainText);
		}
	}

	@Test
	public void testUnsetEncryptionInSerialization() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(name, value, true);
		String serialized = writer.writeValueAsString(parameter);
		serialized = serialized.replace("true", "false");
		ValueProviderParameter deserialized = mapper.readValue(serialized, ValueProviderParameter.class);
		assertNotEquals("Values equal after disabling encryption", parameter.getValue(), deserialized.getValue());
	}

	@Test
	public void testSetEncryptionInSerialization() throws IOException {
		String otherValue = RandomStringUtils.randomAlphanumeric(8);
		ValueProviderParameter parameters[] = {new ValueProviderParameterImpl(name, value, false),
				new ValueProviderParameterImpl(name, otherValue, false)};
		for (ValueProviderParameter parameter : parameters) {
			String serialized = writer.writeValueAsString(parameter);
			serialized = serialized.replace("false", "true");
			ValueProviderParameter deserialized = mapper.readValue(serialized, ValueProviderParameter.class);
			assertNotEquals("Values equal after enabling encryption", parameter.getValue(), deserialized.getValue());
			assertNull("Value not null after decryption error", deserialized.getValue());
		}
	}

	@Test
	public void testMissingEnabledInSerialization() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(name, value, false, false);
		String serialized = writer.writeValueAsString(parameter);
		serialized = serialized.replace("  \"enabled\" : false," + System.lineSeparator(), "");
		ValueProviderParameter deserialized = mapper.readValue(serialized, ValueProviderParameter.class);
		assertTrue("Missing enabled is not desirialized to true", deserialized.isEnabled());
	}

	@AfterClass
	public static void tearDown() {
		writer = null;
		mapper = null;
		parameters = null;
	}
}
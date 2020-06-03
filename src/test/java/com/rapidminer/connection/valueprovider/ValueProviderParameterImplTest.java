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
package com.rapidminer.connection.valueprovider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.tools.encryption.EncryptionProvider;


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


	@BeforeClass
	public static void setup() {
		name = RandomStringUtils.randomAlphabetic(5);
		value = RandomStringUtils.randomAlphanumeric(10);
		parameters = new ValueProviderParameter[8];
		for (int i = 0; i < 8; i++) {
			parameters[i] = new ValueProviderParameterImpl(name, i % 2 == (i < 4 ? 0 : 1) ? value : null, i % 4 > 1,
					(i % 4) % 3 != 0);
		}
		EncryptionProvider.initialize();
	}

	@Test
	public void testSerialisation() throws IOException {
		for (ValueProviderParameter parameter : parameters) {
			String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
			ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
			assertEquals("encryption state not equal after serialization", parameter.isEncrypted(), deserialized.isEncrypted());
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
	public void testEncryption() throws IOException {
		for (ValueProviderParameter parameter : parameters) {
			String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
			boolean isPlainText = serialized.contains(String.valueOf(parameter.getValue()));
			boolean isNull = parameter.getValue() == null;
			boolean isEncrypted = parameter.isEncrypted();
			assertEquals("Value was not correctly written", !isEncrypted || isNull, isPlainText);
		}
	}

	@Test
	public void testUnsetEncryptionInSerialization() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(name, value, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		serialized = serialized.replace("true", "false");
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertNotEquals("Values equal after disabling encryption", parameter.getValue(), deserialized.getValue());
	}

	@Test
	public void testSetEncryptionInSerialization() throws IOException {
		String otherValue = RandomStringUtils.randomAlphanumeric(8);
		ValueProviderParameter[] parameters = {new ValueProviderParameterImpl(name, value, false),
				new ValueProviderParameterImpl(name, otherValue, false)};
		for (ValueProviderParameter parameter : parameters) {
			String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
			serialized = serialized.replace("false", "true");
			ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
			assertNotEquals("Values equal after enabling encryption", parameter.getValue(), deserialized.getValue());
			assertNull("Value not null after decryption error", deserialized.getValue());
		}
	}

	@Test
	public void testMissingEnabledInSerialization() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(name, value, false, false);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		serialized = serialized.replace("  \"enabled\" : false," + System.lineSeparator(), "");
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertTrue("Missing enabled is not desirialized to true", deserialized.isEnabled());
	}
}
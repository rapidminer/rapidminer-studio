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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionInformationSerializer;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * Testing the {@link ValueProviderParameterImpl}.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ValueProviderParameterTest {

	private static final Random RANDOM = new Random();
	private static final String NAME = RandomStringUtils.randomAlphabetic(2 + RANDOM.nextInt(18));
	private static final String VALUE = RandomStringUtils.randomAlphabetic(5 + RANDOM.nextInt(15));
	private static final String VALUE2 = RandomStringUtils.randomAlphabetic(2 + RANDOM.nextInt(18));


	@BeforeClass
	public static void setup() {
		EncryptionProvider.initialize();
	}

	@Test
	public void testGetValue() {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		assertEquals(VALUE, parameter.getValue());
	}

	@Test
	public void testGetName() {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, null, true);
		assertEquals(NAME, parameter.getName());
	}

	@Test
	public void testIsEncrypted() {
		ValueProviderParameter encrypted = new ValueProviderParameterImpl(NAME, VALUE, true);
		assertTrue(encrypted.isEncrypted());
		ValueProviderParameter notEncrypted = new ValueProviderParameterImpl(NAME, VALUE);
		assertFalse(notEncrypted.isEncrypted());
	}

	@Test
	public void testIsEnabled() {
		ValueProviderParameter disabled = new ValueProviderParameterImpl(NAME, VALUE, false, false);
		assertFalse(disabled.isEnabled());
		ValueProviderParameter enabled = new ValueProviderParameterImpl(NAME, VALUE);
		assertTrue(enabled.isEnabled());
	}

	@Test
	public void testSetValue() {
		ValueProviderParameter impl = new ValueProviderParameterImpl(NAME, VALUE, true);
		impl.setValue(VALUE2);
		assertEquals(VALUE2, impl.getValue());
	}

	@Test
	public void testSetEnabled() {
		ValueProviderParameter impl = new ValueProviderParameterImpl(NAME, VALUE, true);
		impl.setEnabled(false);
		assertFalse(impl.isEnabled());
		impl.setEnabled(true);
		assertTrue(impl.isEnabled());
	}

	@Test
	public void testToString() {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		assertTrue(parameter.toString().contains(NAME));
		assertFalse(parameter.toString().contains(VALUE));
	}

	@Test
	public void testSerialization() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		// Check that serialized version does not contain the plain value
		assertTrue(serialized.contains(NAME));
		assertFalse(serialized.contains(VALUE));
		// Read again
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertEquals(VALUE, deserialized.getValue());
		assertEquals(NAME, deserialized.getName());
		assertTrue(deserialized.isEncrypted());
	}

	@Test
	public void testSerializationNotEnabled() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true, false);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		// Read again
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertEquals(VALUE, deserialized.getValue());
		assertEquals(NAME, deserialized.getName());
		assertFalse(deserialized.isEnabled());
	}

	@Test
	public void testSerializationOfNull() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, null, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertNull(deserialized.getValue());
	}

	@Test
	public void testSerializationWithNullContext() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, null);
		assertTrue(serialized.contains(VALUE));
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, null);
		assertEquals("Deserialized encrypted value different from input value", VALUE, deserialized.getValue());
	}

	@Test
	public void testSerializationWithMissingContext() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, "---does-not-exist---");
		assertFalse(serialized.contains(VALUE));
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, "---does-not-exist---");
		assertNull(deserialized.getValue());
	}

	@Test
	public void testDeserializationWithMissingContext() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, "---does-not-exist---");
		assertFalse(serialized.contains(VALUE));
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, "---does-not-exist---");
		assertNull(deserialized.getValue());
	}

	@Test
	public void testDeserializationWithNullContext() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, null);
		assertTrue(serialized.contains(VALUE));
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, null);
		assertEquals("Deserialized encrypted value different from input value", VALUE, deserialized.getValue());
	}

	@Test
	public void testWrongOrder() throws IOException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl("name", "value", true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, EncryptionProvider.DEFAULT_CONTEXT);
		assertFalse(serialized.contains(VALUE));
		String trimmed = serialized.substring(1, serialized.length() - 1);
		List<String> data = Arrays.asList(trimmed.split(",", 3));
		Collections.swap(data, 1, 2);
		String wrongOrder = "{" + String.join(",", data) + "}";
		ValueProviderParameter deserialized = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(wrongOrder, ValueProviderParameter.class, null, EncryptionProvider.DEFAULT_CONTEXT);
		assertEquals("value", deserialized.getValue());
	}

	@Test
	public void testEquals() {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ValueProviderParameter parameterDuplicate = new ValueProviderParameterImpl(NAME, VALUE, true);
		ValueProviderParameter different = new ValueProviderParameterImpl(NAME, VALUE);
		assertEquals(parameter, parameter);
		assertEquals(parameter, parameterDuplicate);
		assertNotEquals(parameter, different);
		assertNotEquals(parameter, parameter.toString());
		assertEquals(parameter.hashCode(), parameterDuplicate.hashCode());
		assertNotEquals(parameter.hashCode(), different.hashCode());
	}

	@Test
	public void testServerSerialization() throws Exception {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		String serialized = ConnectionInformationSerializer.INSTANCE.createJsonFromObject(parameter, null);
		assertTrue(serialized.contains(VALUE));
		ValueProviderParameter read = ConnectionInformationSerializer.INSTANCE.createObjectFromJson(serialized, ValueProviderParameter.class, null, null);
		assertEquals(parameter, read);
	}
}
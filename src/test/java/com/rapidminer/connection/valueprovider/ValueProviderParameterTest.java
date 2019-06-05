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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.crypto.KeyGenerator;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidminer.tools.cipher.KeyGenerationException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;


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

	private static final String RAPID_MINER_HOME = System.getProperty("rapidminer.user-home");
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

	@After
	public void restoreValues() {
		if (RAPID_MINER_HOME == null) {
			System.clearProperty("rapidminer.user-home");
		} else {
			System.setProperty("rapidminer.user-home", RAPID_MINER_HOME);
		}
		KeyGeneratorTool.setUserKey(USER_KEY);
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
	public void testSerialization() throws IOException, NoSuchAlgorithmException, KeyGenerationException {
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		// Check that serialized version does not contain the plain value
		assertTrue(serialized.contains(NAME));
		assertFalse(serialized.contains(VALUE));
		// Read again
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertEquals(VALUE, deserialized.getValue());
		assertEquals(NAME, deserialized.getName());
		assertTrue(deserialized.isEncrypted());
	}

	@Test
	public void testSerializationNotEnabled() throws IOException, NoSuchAlgorithmException, KeyGenerationException {
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true, false);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		// Read again
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertEquals(VALUE, deserialized.getValue());
		assertEquals(NAME, deserialized.getName());
		assertFalse(deserialized.isEnabled());
	}

	@Test
	public void testSerializationOfNull() throws IOException, NoSuchAlgorithmException, KeyGenerationException {
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, null, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertNull(deserialized.getValue());
	}

	@Test
	public void testSerializationWithoutKey() throws IOException, NoSuchAlgorithmException {
		System.setProperty("rapidminer.user-home", "/dev/null");
		KeyGeneratorTool.setUserKey(null);
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		assertFalse(serialized.contains(VALUE));
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertNull(deserialized.getValue());
	}

	@Test
	public void testDeserializationWithoutKey() throws IOException, NoSuchAlgorithmException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		assertFalse(serialized.contains(VALUE));
		System.setProperty("rapidminer.user-home", "/dev/null");
		KeyGeneratorTool.setUserKey(null);
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertNull(deserialized.getValue());
	}


	@Test
	public void testDeserializationWithInvalidKey() throws IOException, NoSuchAlgorithmException {
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		assertFalse(serialized.contains(VALUE));
		System.setProperty("rapidminer.user-home", "/dev/null");
		KeyGeneratorTool.setUserKey(KeyGenerator.getInstance("AES").generateKey());
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertNull(deserialized.getValue());
	}


	@Test
	public void testSerializationWithInvalidKey() throws IOException, NoSuchAlgorithmException {
		System.setProperty("rapidminer.user-home", "/dev/null");
		KeyGeneratorTool.setUserKey(KeyGenerator.getInstance("AES").generateKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		assertFalse(serialized.contains(VALUE));
		ValueProviderParameter deserialized = om.readValue(serialized, ValueProviderParameter.class);
		assertNull(deserialized.getValue());
	}

	@Test
	public void testWrongOrder() throws IOException, NoSuchAlgorithmException, KeyGenerationException, NoSuchFieldException {
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl("name", "value", true);
		ObjectMapper om = new ObjectMapper();
		String serialized = om.writeValueAsString(parameter);
		assertFalse(serialized.contains(VALUE));
		String trimmed = serialized.substring(1, serialized.length() - 1);
		List<String> data = Arrays.asList(trimmed.split(",", 3));
		Collections.swap(data, 1, 2);
		String wrongOrder = "{" + String.join(",", data) + "}";
		ValueProviderParameter deserialized = om.readValue(wrongOrder, ValueProviderParameter.class);
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
		KeyGeneratorTool.setUserKey(KeyGeneratorTool.createSecretKey());
		ValueProviderParameter parameter = new ValueProviderParameterImpl(NAME, VALUE, true);
		ObjectMapper om = new ObjectMapper();
		om.addMixIn(ValueProviderParameterImpl.class, ValueProviderParameterImpl.UnencryptedValueMixIn.class);
		String serialized = om.writeValueAsString(parameter);
		assertTrue(serialized.contains(VALUE));
		ValueProviderParameter read = om.readValue(serialized, ValueProviderParameter.class);
		assertEquals(parameter, read);
	}
}
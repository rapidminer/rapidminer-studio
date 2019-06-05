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
package com.rapidminer.operator.ports.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConfigurationParameterImpl;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;


/**
 * Tests for {@link ConnectionInformationMetaData}
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class ConnectionInformationMetaDataTest {

	private static final Random RANDOM = new Random();
	private static final String CONNECTION_NAME = RandomStringUtils.randomAlphabetic(2 + RANDOM.nextInt(18));
	private static final String CONNECTION_TYPE = RandomStringUtils.randomAlphabetic(3 + RANDOM.nextInt(10));
	private static final String PARAMETER_NAME = RandomStringUtils.randomAlphabetic(3 + RANDOM.nextInt(10));
	private static final String CONFIGURATION_GROUP_NAME = RandomStringUtils.randomAlphabetic(2 + RANDOM.nextInt(18));
	private static final String PARAMETER_VALUE = RandomStringUtils.randomAlphabetic(5 + RANDOM.nextInt(15));
	private static final String DESCRIPTION = RandomStringUtils.randomAlphabetic(ConnectionInformationMetaData.DESCRIPTION_PREVIEW_LENGTH + 1 + RANDOM.nextInt(100));
	private static final String TAG = RandomStringUtils.randomAlphabetic(3 + RANDOM.nextInt(5));
	private static final String ANNOTATION_KEY = RandomStringUtils.randomAlphabetic(3 + RANDOM.nextInt(5));
	private static final String ANNOTATION_VALUE = RandomStringUtils.randomAlphabetic(3 + RANDOM.nextInt(5));

	@Test
	public void testSerializedObjectDoesNotContainEncryptedParameter() throws IOException {
		ConnectionConfigurationBuilder original = new ConnectionConfigurationBuilder(CONNECTION_NAME, CONNECTION_TYPE);
		ConfigurationParameter parameter = new ConfigurationParameterImpl(PARAMETER_NAME, PARAMETER_VALUE, true);
		original.withKeys(Collections.singletonMap(CONFIGURATION_GROUP_NAME, Collections.singletonList(parameter)));
		ConnectionInformationMetaData metaData = new ConnectionInformationMetaData(original.build());
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(output);
		out.writeObject(metaData);
		out.close();
		String result = output.toString();
		assertTrue(result.contains(PARAMETER_NAME));
		assertFalse(result.contains(PARAMETER_VALUE));
	}

	@Test
	public void testGetDescription() {
		String description = getMetaData().getDescription();
		assertTrue(description.contains(CONNECTION_TYPE));
		assertTrue(description.contains(CONNECTION_NAME));
		assertTrue(description.contains(TAG));
		assertTrue(description.contains(ANNOTATION_KEY));
		assertTrue(description.contains(ANNOTATION_VALUE));
		assertFalse(description.contains(DESCRIPTION));
		int maxLength = ConnectionInformationMetaData.DESCRIPTION_PREVIEW_LENGTH - "...".length();
		assertTrue(description.contains(DESCRIPTION.substring(0, maxLength)));
	}

	@Test
	public void testClone() {
		ConnectionInformationMetaData metaData = getMetaData();
		ConnectionInformationMetaData clone = metaData.clone();
		assertNotSame(metaData.getConfiguration(), clone.getConfiguration());
		assertEquals(metaData.getConfiguration(), clone.getConfiguration());
	}

	@Test
	public void testMissingMetaData() {
		ConnectionInformationMetaData metaData = new ConnectionInformationMetaData();
		assertTrue(metaData.getDescription().contains(ConnectionInformationMetaData.UNKNOWN_CONNECTION.getName()));
		assertNull(metaData.getConfiguration());
		assertEquals(metaData.getConfiguration(), metaData.clone().getConfiguration());
	}

	@Test
	public void testGetConnectionType() {
		assertEquals(CONNECTION_TYPE, getMetaData().getConnectionType());
	}

	private ConnectionInformationMetaData getMetaData() {
		ConnectionConfigurationBuilder configurationBuilder = new ConnectionConfigurationBuilder(CONNECTION_NAME, CONNECTION_TYPE);
		configurationBuilder.withDescription(DESCRIPTION);
		configurationBuilder.withTag(TAG);
		ConfigurationParameter parameter = new ConfigurationParameterImpl(PARAMETER_NAME, PARAMETER_VALUE, true);
		configurationBuilder.withKeys(Collections.singletonMap(CONFIGURATION_GROUP_NAME, Collections.singletonList(parameter)));
		ConnectionInformationMetaData metaData = new ConnectionInformationMetaData(configurationBuilder.build());
		metaData.getAnnotations().put(ANNOTATION_KEY, ANNOTATION_VALUE);
		return metaData;
	}
}
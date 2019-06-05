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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.util.TestExecutionContextImpl;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.ValueProviderParameter;
import com.rapidminer.connection.valueprovider.ValueProviderParameterImpl;
import com.rapidminer.operator.Operator;


/**
 * Test the abstract class {@link BaseValueProviderHandler}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class BaseValueProviderHandlerTest {

	private static final String TEST_TYPE = "TEST";

	private class TestBaseValueProviderHandler extends BaseValueProviderHandler {

		protected TestBaseValueProviderHandler(String type) {
			super(type);
		}

		protected TestBaseValueProviderHandler(String type, List<ValueProviderParameter> parameters) {
			super(type, parameters);
		}

		@Override
		public Map<String, String> injectValues(ValueProvider vp, Map<String, String> injectables, Operator operator, ConnectionInformation connection) {
			return null;
		}
	}

	@Test
	public void checkBaseImpl() {
		TestBaseValueProviderHandler teba = new TestBaseValueProviderHandler(TEST_TYPE);
		assertEquals(TEST_TYPE, teba.getType());
		teba.initialize();
		assertTrue(teba.isInitialized());
		assertFalse(teba.isConfigurable());
		assertEquals(ValidationResult.nullable(), teba.validate(null));
		assertEquals(TestResult.nullable(), teba.test(null));
		final ValueProvider valueProvider = ValueProviderHandlerRegistryTest.getValueProviderHandler(TEST_TYPE).createNewProvider(TEST_TYPE);

		assertEquals(TestResult.ResultType.SUCCESS, teba.validate(valueProvider).getType());
		assertEquals(TestResult.ResultType.NOT_SUPPORTED, teba.test(new TestExecutionContextImpl<>(valueProvider)).getType());
		assertTrue(teba.getParameters().isEmpty());
		final ValueProvider newProvider = teba.createNewProvider("new provider");
		assertNotNull(newProvider);
		assertEquals("new provider", newProvider.getName());
		assertEquals(TEST_TYPE, newProvider.getType());
		assertTrue(newProvider.getParameters().isEmpty());
	}

	@Test
	public void checkParameterClone() {
		TestBaseValueProviderHandler valueProviderHandler = new TestBaseValueProviderHandler(TEST_TYPE);
		assertTrue(valueProviderHandler.getParameters().isEmpty());
		// cannot just change the content
		try {
			valueProviderHandler.getParameters().add(new ValueProviderParameterImpl(TEST_TYPE));
		} catch (UnsupportedOperationException uoe) {
			// this should happen but it is not necessary
		}
		// this is the expected behaviour, do not change the list afterwards
		assertTrue(valueProviderHandler.getParameters().isEmpty());

		List<ValueProviderParameter> valueProviderList = new ArrayList<>();
		valueProviderList.add(new ValueProviderParameterImpl("VP1"));
		valueProviderList.add(new ValueProviderParameterImpl("VP2"));
		valueProviderList.add(new ValueProviderParameterImpl("VP1"));
		valueProviderList.add(new ValueProviderParameterImpl("VP2"));
		valueProviderList.add(new ValueProviderParameterImpl("VP3"));
		valueProviderList.add(new ValueProviderParameterImpl("VP4"));
		valueProviderHandler = new TestBaseValueProviderHandler(TEST_TYPE, valueProviderList);
		assertArrayEquals(valueProviderList.toArray(), valueProviderHandler.getParameters().toArray());
	}

	@Test
	public void checkNullName() {
		expectIAE("type", () -> new TestBaseValueProviderHandler(null));
		expectIAE("type", () -> new TestBaseValueProviderHandler(""));
		expectIAE("type", () -> new TestBaseValueProviderHandler(null, null));
		expectIAE("type", () -> new TestBaseValueProviderHandler("", Collections.emptyList()));

		expectIAE("parameters", () -> new TestBaseValueProviderHandler(TEST_TYPE, null));
		expectIAE("parameters", () -> new TestBaseValueProviderHandler(TEST_TYPE, Collections.emptyList()));
		expectIAE("parameters", () -> new TestBaseValueProviderHandler(TEST_TYPE, Arrays.asList(null, null, null, null, null)));
	}

	private void expectIAE(String type, Runnable r) {
		try {
			r.run();
			fail("Call succeeded without expected IllegalArgumentException");
		} catch (IllegalArgumentException iae) {
			assertTrue(iae.getMessage().endsWith(" for \"" + type + "\" not allowed"));
		}
	}
}
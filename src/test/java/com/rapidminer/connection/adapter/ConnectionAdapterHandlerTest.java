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
package com.rapidminer.connection.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.ProcessToolsTest;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/**
 * Tests for {@link ConnectionAdapterHandler}.
 *
 * @author Jan Czogalla
 * @since 9.3
 */
public class ConnectionAdapterHandlerTest {

	/**
	 * Simple {@link Operator} that implements {@link ConnectionSelectionProvider} and holds a
	 * {@link ConnectionInformationSelector}.
	 *
	 * @author Jan Czogalla
	 * @since 9.6
	 */
	private static class ConnectionOperator extends Operator implements ConnectionSelectionProvider {

		private ConnectionInformationSelector selectorReference;

		private ConnectionOperator(OperatorDescription description) {
			super(description);
		}

		@Override
		public ConnectionInformationSelector getConnectionSelector() {
			return selectorReference;
		}

		@Override
		public void setConnectionSelector(ConnectionInformationSelector selector) {
			selectorReference = selector;
		}

	}

	/**
	 * Minimalistic {@link ConnectionAdapter} that has a {@link #cleanUp()} method whose effect can be checked.
	 * Must be public so it can be created automatically.
	 *
	 * @author Jan Czogalla
	 * @since 9.6
	 */
	public static class TestConnectionAdapter extends ConnectionAdapter {

		private boolean initialized = true;

		@Override
		public String getTypeId() {
			return TEST_TYPE;
		}

		@Override
		public void cleanUp() {
			initialized = false;
		}

		private boolean isInitialized() {
			return initialized;
		}
	}

	/**
	 * Wrapper {@link ConnectionInformationSelector} to create a test connection and provide a fake repository location
	 *
	 * @author Jan Czogalla
	 * @since 9.6
	 */
	private static class TestCIS extends ConnectionInformationSelector {

		private final ConnectionInformationSelector original;

		private TestCIS(ConnectionInformationSelector original) {
			super(original.getInput(), original.getOutput(), original.getHandler(), original.getConnectionType());
			this.original = original;
		}

		@Override
		public InputPort getInput() {
			return original.getInput();
		}

		@Override
		public String getConnectionType() {
			return original.getConnectionType();
		}

		@Override
		public ConnectionInformation getConnection() throws UserError {
			ConnectionAdapterHandler<ConnectionAdapter> handler = ConnectionAdapterHandler.getHandler(getConnectionType());
			return handler.createNewConnectionInformation(TEST_NAME);
		}

		@Override
		public RepositoryLocation getConnectionLocation() {
			return original.getConnectionLocation();
		}

		@Override
		public void passDataThrough() {
			original.passDataThrough();
		}
	}

	private static final String TEST_TYPE = "test";
	private static final String TEST_NAME = "Test Connection";
	private static final String TEST_LOCATION = "//test/test";
	private static final String PARAMETER_TEST_INTEGER = "int_key";
	private static OperatorDescription desc;
	private static ParameterTypeConfigurable configurableParam;
	private static ConnectionAdapterHandler<TestConnectionAdapter> handler;

	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setup() throws ConfigurationException, OperatorCreationException, ConnectionAdapterException {
		OperatorDocumentation docu = new OperatorDocumentation("Test");
		desc = mock(OperatorDescription.class);
		doReturn(docu).when(desc).getOperatorDocumentation();
		when(desc.createOperatorInstance()).then(invocation -> new ConnectionOperator(desc));

		Repository repo = mock(Repository.class);
		when(repo.getName()).thenReturn(TEST_TYPE);

		handler = new ConnectionAdapterHandler<TestConnectionAdapter>(null) {

			@Override
			public Class<TestConnectionAdapter> getConfigurableClass() {
				return TestConnectionAdapter.class;
			}

			@Override
			public String getTypeId() {
				return TEST_TYPE;
			}

			@Override
			public String getType() {
				return getTypeId();
			}

			@Override
			public TestConnectionAdapter getAdapter(ConnectionInformation connection, Operator operator) throws ConnectionAdapterException, ConfigurationException {
				try {
					connection = new ConnectionInformationBuilder(connection).inRepository(repo).build();
					ConfigurationParameter parameter = connection.getConfiguration().getParameter(ConnectionAdapterHandler.BASE_GROUP + "." + PARAMETER_TEST_INTEGER);
					if (parameter != null) {
						String value = operator.getParameters().getParameterOrNull(PARAMETER_TEST_INTEGER);
						if (value == null) {
							value = "0";
						}
						parameter.setValue(value);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return super.getAdapter(connection, operator);
			}

			@Override
			public String getI18NBaseKey() {
				return null;
			}

			@Override
			public List<ParameterType> getParameterTypes(ParameterHandler parameterHandler) {
				return Collections.singletonList(new ParameterTypeInt(PARAMETER_TEST_INTEGER, "", 0, 20));
			}
		};

		ConnectionAdapterHandler.registerHandler(handler);

		configurableParam = new ParameterTypeConfigurable("parameter", "", TEST_TYPE);

		// register a fake process root operator
		ProcessToolsTest.setup();
	}

	@AfterClass
	@SuppressWarnings("unchecked")
	public static void tearDown() throws NoSuchFieldException, IllegalAccessException {
		ConnectionHandlerRegistry.getInstance().unregisterHandler(handler);
		assertNull(ConnectionAdapterHandler.getHandler(handler.getType()));
		Field configurators = ConfigurationManager.class.getDeclaredField("configurators");
		Field configurables = ConfigurationManager.class.getDeclaredField("configurables");
		configurators.setAccessible(true);
		configurables.setAccessible(true);
		((Map<String, Object>) configurators.get(ConfigurationManager.getInstance())).remove(handler.getType());
		((Map<String, Object>) configurables.get(ConfigurationManager.getInstance())).remove(handler.getType());
		desc = null;
		configurableParam = null;
		handler = null;
		ProcessToolsTest.tearDown();
	}

	@Test
	public void testGetHandlerWithFullAndPartialTypeID() {
		ConnectionAdapterHandler<ConnectionAdapter> partial = ConnectionAdapterHandler.getHandler(TEST_TYPE);
		assertNotNull("Handler not found for partial type", partial);
		ConnectionAdapterHandler<ConnectionAdapter> full = ConnectionAdapterHandler.getHandler("extension:" + TEST_TYPE);
		assertNotNull("Handler not found for full type", full);
		assertEquals(partial, full);
	}

	@Test
	public void testGettingParameters() {
		Operator normalOperator = new Operator(desc){};
		List<ParameterType> parameters = ConnectionAdapterHandler.getConnectionParameters(normalOperator, TEST_TYPE, configurableParam);
		assertEquals("Mismatched parameters for non-connection operator", Collections.singletonList(configurableParam), parameters);

		ConnectionOperator connectionOperator = new ConnectionOperator(desc);

		parameters = ConnectionAdapterHandler.getConnectionParameters(connectionOperator, TEST_TYPE, configurableParam);
		assertTrue(parameters.contains(configurableParam));
		assertTrue(configurableParam.isOptionalWithoutConditions());
		assertEquals(3, parameters.size());
		assertNotNull("Connection selector was not set", connectionOperator.getConnectionSelector());
		assertNotNull("Input port was not set", connectionOperator.getConnectionSelector().getInput());
		assertNotNull("Output port was not set", connectionOperator.getConnectionSelector().getOutput());
	}
}
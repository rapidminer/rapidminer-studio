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
package com.rapidminer.connection.adapter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.impl.InputPortsImpl;
import com.rapidminer.operator.ports.impl.OutputPortsImpl;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorService;
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

	private static final String TEST_TYPE = "test";
	private static final String TEST_NAME = "Test Connection";

	@BeforeClass
	@SuppressWarnings("unchecked")
	public static void setup() {
		ConnectionAdapterHandler<ConnectionAdapter> handler = mock(ConnectionAdapterHandler.class);
		when(handler.getTypeId()).thenReturn(TEST_TYPE);
		when(handler.getType()).thenReturn(TEST_TYPE);
		when(handler.getName()).thenReturn(TEST_NAME);
		ConnectionAdapterHandler.registerHandler(handler);
	}

	@AfterClass
	@SuppressWarnings("unchecked")
	public static void tearDown() throws NoSuchFieldException, IllegalAccessException {
		Field handlerMap = ConnectionAdapterHandler.class.getDeclaredField("handlerMap");
		handlerMap.setAccessible(true);
		((Map<String, Object>) handlerMap.get(null)).remove(TEST_TYPE);
		ConnectionHandlerRegistry.getInstance().unregisterHandler(ConnectionHandlerRegistry.getInstance().getHandler(TEST_TYPE));
		Field configurators = ConfigurationManager.class.getDeclaredField("configurators");
		Field configurables = ConfigurationManager.class.getDeclaredField("configurables");
		configurators.setAccessible(true);
		configurables.setAccessible(true);
		((Map<String, Object>) configurators.get(ConfigurationManager.getInstance())).remove(TEST_TYPE);
		((Map<String, Object>) configurables.get(ConfigurationManager.getInstance())).remove(TEST_TYPE);
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
		OperatorDocumentation docu = mock(OperatorDocumentation.class);
		OperatorDescription desc = mock(OperatorDescription.class);
		doReturn(docu).when(desc).getOperatorDocumentation();
		Operator normalOperator = new Operator(desc){};
		ParameterTypeConfigurable configurableParam = new ParameterTypeConfigurable("parameter", "", TEST_TYPE);
		List<ParameterType> parameters = ConnectionAdapterHandler.getConnectionParameters(normalOperator, TEST_TYPE, configurableParam);
		assertEquals("Mismatched parameters for non-connection operator", Collections.singletonList(configurableParam), parameters);

		AtomicReference<ConnectionInformationSelector> selectorReference = new AtomicReference<>();
		class ConnectionOperator extends Operator implements ConnectionSelectionProvider {
			private ConnectionOperator(OperatorDescription description) {
				super(description);
			}

			@Override
			public ConnectionInformationSelector getConnectionSelector() {
				return selectorReference.get();
			}

			@Override
			public void setConnectionSelector(ConnectionInformationSelector selector) {
				selectorReference.set(selector);
			}
		}

		ConnectionOperator connectionOperator = new ConnectionOperator(desc);

		parameters = ConnectionAdapterHandler.getConnectionParameters(connectionOperator, TEST_TYPE, configurableParam);
		assertTrue(parameters.contains(configurableParam));
		assertTrue(configurableParam.isOptionalWithoutConditions());
		assertEquals(3, parameters.size());
		assertNotNull("Connection selector was not set", selectorReference.get());
		assertNotNull("Input port was not set", selectorReference.get().getInput());
		assertNotNull("Output port was not set", selectorReference.get().getOutput());
	}
}
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
package com.rapidminer.tools;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.tools.documentation.OperatorDocumentation;


/**
 * Tests for the {@link ProcessTools} utility class.
 *
 * @author Jan Czogalla
 * @since 9.6
 */
public class ProcessToolsTest {

	private static OperatorDescription desc;

	@BeforeClass
	public static void setup() throws OperatorCreationException {
		OperatorDocumentation doc = new OperatorDocumentation("Process");
		desc = mock(OperatorDescription.class);
		when(desc.getGroup()).thenReturn("");
		when(desc.getKey()).thenReturn("process");
		when(desc.getKeyWithoutPrefix()).thenReturn("process");
		Class<? extends Operator> rootClass = ProcessRootOperator.class;
		when(desc.getOperatorClass()).then(invocation -> rootClass);
		when(desc.createOperatorInstance()).then(invocation -> new ProcessRootOperator(desc));
		when(desc.getOperatorDocumentation()).thenReturn(doc);

		OperatorService.registerOperator(desc, null);
	}

	@AfterClass
	public static void tearDown() {
		OperatorService.unregisterOperator(desc);
		desc = null;
	}

	/**
	 * Test that parent process propagation works as expected
	 */
	@Test
	public void testProcessParent() {
		Process process = new Process();
		assertSame(process, ProcessTools.getParentProcess(process));
		Process child = new Process();
		assertNotSame(process, child);
		ProcessTools.setParentProcess(child, process);
		assertSame(process, ProcessTools.getParentProcess(child));
		Process grandChild = new Process();
		assertNotSame(process, grandChild);
		assertNotSame(child, grandChild);
		ProcessTools.setParentProcess(grandChild, child);
		assertSame(process, ProcessTools.getParentProcess(grandChild));
	}
}
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
package com.rapidminer.test;

import static junit.framework.Assert.*;

import org.junit.Test;

import com.rapidminer.operator.OperatorVersion;

/**
 * 
 * @author Simon Fischer
 *
 */
public class OperatorVersionTest {

	@Test
	public void testParse() {
		OperatorVersion reference = new OperatorVersion(5,1,2);
		assertEquals(reference, new OperatorVersion("5.1.2"));		
	}

//  
//	@Test
//	public void testBeta() {
//		OperatorVersion reference = new OperatorVersion("5.1.2");
//		assertEquals(reference, new OperatorVersion("5.1.2beta"));		
//	}
	
	@Test
	public void testZero() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertEquals(reference, new OperatorVersion("5.1.002"));
		assertEquals(reference, new OperatorVersion("5.01.2"));
		assertEquals(reference, new OperatorVersion("05.1.2"));
	}
	
	@Test
	public void testCompare1() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.0")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.0.3")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("4.9.0")) > 0);
	}
	
	@Test
	public void testCompare2() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.2")) == 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.1.3")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.2.2")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("6.0.0")) < 0);
	}
	
	@Test
	public void testComparator1() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.0")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.0.3")) > 0);
		assertTrue(reference.compareTo(new OperatorVersion("4.9.0")) > 0);
	}
	
	@Test
	public void testComparator2() {
		OperatorVersion reference = new OperatorVersion("5.1.2");
		assertTrue(reference.compareTo(new OperatorVersion("5.1.2")) == 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.1.3")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("5.2.2")) < 0);
		assertTrue(reference.compareTo(new OperatorVersion("6.0.0")) < 0);
	}
}

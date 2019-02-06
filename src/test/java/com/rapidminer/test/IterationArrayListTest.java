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

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import com.rapidminer.tools.IterationArrayList;

/**
 * A test for the  {@link IterationArrayList}.
 * 
 * @author Michael Wurst
 */
public class IterationArrayListTest {

	@Test
	public void testAccess() {

		ArrayList<String> l = new ArrayList<String>();
		l.add("a");
		l.add("b");
		l.add("c");

		ArrayList<String> l2 = new IterationArrayList<String>(l.iterator());

		for (int i = 0; i < l.size(); i++)
			assertEquals(l2.get(i), l.get(i));

	}

}

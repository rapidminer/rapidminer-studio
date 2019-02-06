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

package com.rapidminer.operator.learner.associations;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import junit.framework.TestCase;


/**
 * Test for Ticket <a href="https://jira.rapidminer.com/browse/RM-2941">RM-2941</a><br/>
 * Calling toString with active Iterator failed
 *
 * @author Jonas Wilms-Pfau
 *
 */
public class AssociationRulesTest extends TestCase {

	@Test
	public void testToStringwithIteratorFunction() {
		AssociationRules mockRules = new AssociationRules();
		for (int i = 0; i < 100; i++) {
			mockRules.addItemRule(new AssociationRule(new ArrayList<Item>(), new ArrayList<Item>(), 0.3d));
		}
		mockRules.sort();
		try {
			Iterator<AssociationRule> it = mockRules.iterator();
			mockRules.toString();
			// This would still not work
			/*
			 * mockRules.addItemRule(new AssociationRule(new ArrayList<Item>(), new
			 * ArrayList<Item>(), 0.3d));
			 */
			it.next();
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}

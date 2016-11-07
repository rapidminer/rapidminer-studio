/*
 * Copyright (C) 2001-2016 RapidMiner GmbH
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

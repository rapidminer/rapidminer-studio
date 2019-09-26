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
package com.rapidminer.test_utils;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.test.asserter.AsserterFactory;


/**
 * @author Marius Helf
 *
 */
public class AsserterRegistry {

	private List<Asserter> registeredAsserters = new LinkedList<>();

	public void registerAsserter(Asserter asserter) {
		registeredAsserters.add(asserter);
	}

	public void registerAllAsserters(AsserterFactory factory) {
		for (Asserter asserter : factory.createAsserters()) {
			registerAsserter(asserter);
		}
	}

	public List<Asserter> getAsserterForObject(Object object) {
		List<Asserter> availableAsserters = new LinkedList<>();
		for (Asserter asserter : registeredAsserters) {
			Class<?> assertable = asserter.getAssertable();
			if (assertableCovered(assertable, object)) {
				availableAsserters.add(asserter);
			}
		}
		if (availableAsserters.isEmpty()) {
			return null;
		} else {
			return availableAsserters;
		}
	}


	public List<Asserter> getAsserterForObjects(Object o1, Object o2) {
		List<Asserter> availableAsserters = new LinkedList<>();
		for (Asserter asserter : registeredAsserters) {
			Class<?> clazz = asserter.getAssertable();
			if (assertableCovered(clazz, o1) && assertableCovered(clazz, o2)) {
				availableAsserters.add(asserter);
			}
		}
		if (availableAsserters.isEmpty()) {
			return null;
		} else {
			return availableAsserters;
		}
	}

	public List<Asserter> getAsserterForClass(Class<?> clazz) {
		List<Asserter> availableAsserters = new LinkedList<>();
		for (Asserter asserter : registeredAsserters) {
			if (assertableCovered(asserter.getAssertable(), clazz)) {
				availableAsserters.add(asserter);
			}
		}
		if (availableAsserters.isEmpty()) {
			return null;
		} else {
			return availableAsserters;
		}
	}

	private boolean assertableCovered(Class<?> assertable, Object object) {
		return assertable.isInstance(object) || (ExampleSet.class.equals(assertable) && (object instanceof IOTable));
	}

	private boolean assertableCovered(Class<?> assertable, Class<?> clazz) {
		return assertable.isAssignableFrom(clazz) || (ExampleSet.class.equals(assertable) && IOTable.class.isAssignableFrom(clazz));
	}
}

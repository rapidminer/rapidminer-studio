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

import com.rapidminer.Process;
import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 
 * @author Marcin Skirzynski
 * 
 */
public class Util {

	/**
	 * Token between the process name and the number for the expected results.
	 */
	public static final String EXPECTED_TOKEN = "-expected-port-";

	/**
	 * Returns all expected results for the specified process. These are all ioobjects which are
	 * directly contained in the folder of the process.
	 * 
	 * @throws RepositoryException
	 */
	public static List<IOObject> getExpectedResult(Process process) throws RepositoryException {

		Map<Integer, IOObject> results = new HashMap<Integer, IOObject>();

		Folder folder = (Folder) process.getRepositoryLocation().parent().locateEntry();

		for (DataEntry entry : folder.getDataEntries()) {
			if (entry instanceof IOObjectEntry) {
				IOObjectEntry ioo = (IOObjectEntry) entry;
				String name = ioo.getLocation().getName();
				// All expected results begin with port and the number of the port
				String expectedPrefix = process.getRepositoryLocation().getName() + EXPECTED_TOKEN;
				if (name.startsWith(expectedPrefix)) {
					String number = name.substring(expectedPrefix.length());
					try {
						int i = Integer.parseInt(number);
						results.put(i, ((IOObjectEntry) entry).retrieveData(null));
					} catch (NumberFormatException e) {
						// Can not parse so this is not a valid ioobject for the test and we will
						// skip this
					}
				}
			}
		}

		List<Integer> keys = new ArrayList<Integer>(results.keySet());
		Collections.sort(keys);

		List<IOObject> sortedResults = new ArrayList<IOObject>();
		for (Integer key : keys) {
			sortedResults.add(results.get(key));
		}
		return sortedResults;
	}

	/**
	 * Removes all stores expected results for the specified process.
	 * 
	 * @param process
	 * @throws RepositoryException
	 */
	public static void removeExpectedResults(Process process) throws RepositoryException {
		Folder folder = process.getRepositoryLocation().locateEntry().getContainingFolder();

		Collection<IOObjectEntry> toDelete = new ArrayList<>();
		for (DataEntry entry : folder.getDataEntries()) {
			if (entry instanceof IOObjectEntry) {
				IOObjectEntry ioo = (IOObjectEntry) entry;
				String name = ioo.getLocation().getName();
				// All expected results begin with port and the number of the port
				String expectedPrefix = process.getRepositoryLocation().getName() + EXPECTED_TOKEN;
				if (name.startsWith(expectedPrefix)) {
					toDelete.add(ioo);
				}
			}
		}

		for (IOObjectEntry entry : toDelete) {
			entry.delete();
		}
	}

}

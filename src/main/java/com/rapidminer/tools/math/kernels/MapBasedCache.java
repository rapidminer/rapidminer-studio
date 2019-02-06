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
package com.rapidminer.tools.math.kernels;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Stores all distances in a map. Uses only a fixed maximum amount of entries for this map (default:
 * 10,000,000, enough for about 3000 examples).
 * 
 * @author Ingo Mierswa
 */
public class MapBasedCache implements KernelCache {

	private int maxSize = 10000000;

	private int exampleSetSize;

	private int accessCounter = 0;

	private Map<Integer, Integer> accessMap;

	private Map<Integer, Double> entries;

	public MapBasedCache(int exampleSetSize) {
		this(10000000, exampleSetSize);
	}

	public MapBasedCache(int maxSize, int exampleSetSize) {
		this.maxSize = maxSize;
		this.exampleSetSize = exampleSetSize;
		this.accessMap = new HashMap<Integer, Integer>(maxSize);
		this.entries = new HashMap<Integer, Double>(maxSize);
	}

	@Override
	public double get(int i, int j) {
		accessCounter++;
		Double result = entries.get(i * exampleSetSize + j);
		if (result == null) {
			return Double.NaN;
		} else {
			accessMap.put(i * exampleSetSize + j, accessCounter);
			return result;
		}
	}

	@Override
	public void store(int i, int j, double value) {
		if (accessMap.size() > this.maxSize) {
			int oldestKey = -1;
			int oldestAcess = Integer.MAX_VALUE;
			Iterator<Map.Entry<Integer, Integer>> it = accessMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Integer> entry = it.next();
				int access = entry.getValue();
				if (access < oldestAcess) {
					oldestKey = entry.getKey();
					oldestAcess = access;
				}
			}

			if (oldestKey != -1) {
				accessMap.remove(oldestKey);
				entries.remove(oldestKey);
			}
		}

		accessMap.put(i * exampleSetSize + j, accessCounter);
		entries.put(i * exampleSetSize + j, value);
	}
}

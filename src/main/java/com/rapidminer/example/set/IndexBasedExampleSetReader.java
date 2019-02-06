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
package com.rapidminer.example.set;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;


/**
 * Returns only a subset of an example set specified by an instance of {@link Partition}.
 * 
 * @author Simon Fischer, Ingo Mierswa ingomierswa Exp $
 */
public class IndexBasedExampleSetReader extends AbstractExampleReader {

	/** Index of the current example. */
	private int current;

	private ExampleSet parent;

	/** The next example that will be returned. */
	private Example next;

	private int size;

	public IndexBasedExampleSetReader(ExampleSet parent) {
		this.parent = parent;
		this.size = parent.size();
		current = -1;
		hasNext();
	}

	@Override
	public boolean hasNext() {
		while (next == null) {
			current++;

			if (current >= size) {
				return false;
			}

			next = parent.getExample(current);
		}
		return true;
	}

	@Override
	public Example next() {
		if (!hasNext()) {
			return null;
		} else {
			Example dummy = next;
			next = null;
			return dummy;
		}
	}

}

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

import java.util.Iterator;


/**
 * This reader simply uses all examples from the parent and all available attributes.
 * 
 * @author Ingo Mierswa
 */
public class AttributesExampleReader extends AbstractExampleReader {

	/** The parent example reader. */
	private Iterator<Example> parent;

	/** The used attributes are described in this example set. */
	private ExampleSet exampleSet;

	/** Creates a simple example reader. */
	public AttributesExampleReader(Iterator<Example> parent, ExampleSet exampleSet) {
		this.parent = parent;
		this.exampleSet = exampleSet;
	}

	/** Returns true if there are more data rows. */
	@Override
	public boolean hasNext() {
		return this.parent.hasNext();
	}

	/** Returns a new example based on the current data row. */
	@Override
	public Example next() {
		if (!hasNext()) {
			return null;
		}
		Example example = this.parent.next();
		if (example == null) {
			return null;
		}
		return new Example(example.getDataRow(), exampleSet);
	}
}

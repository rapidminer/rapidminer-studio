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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;

import java.util.Iterator;


/**
 * This ExampleReader skips all examples containing attribute values that are not a number.
 * 
 * @author Ingo Mierswa, Simon Fischer Exp $
 */
public class SkipNANExampleReader extends AbstractExampleReader {

	private Iterator<Example> reader;

	private Example currentExample;

	public SkipNANExampleReader(Iterator<Example> reader) {
		this.reader = reader;
		this.currentExample = null;
	}

	@Override
	public boolean hasNext() {
		if (currentExample == null) {
			while (reader.hasNext()) {
				Example e = reader.next();
				if (!containsNAN(e)) {
					currentExample = e;
					return true;
				}
			}
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Example next() {
		hasNext();
		Example dummy = currentExample;
		currentExample = null;
		return dummy;
	}

	private boolean containsNAN(Example e) {
		for (Attribute attribute : e.getAttributes()) {
			if (Double.isNaN(e.getValue(attribute))) {
				return true;
			}
		}
		return false;
	}

}

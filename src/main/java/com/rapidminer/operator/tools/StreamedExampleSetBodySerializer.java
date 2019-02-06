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
package com.rapidminer.operator.tools;

import com.rapidminer.example.ExampleSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class StreamedExampleSetBodySerializer implements BodySerializer {

	private int version;

	protected StreamedExampleSetBodySerializer(int version) {
		this.version = version;
	}

	@Override
	public Object deserialize(InputStream in) throws IOException {
		return new ExampleSetToStream(version).read(in);
	}

	@Override
	public void serialize(Object object, OutputStream out) throws IOException {
		if (object instanceof ExampleSet) {
			new ExampleSetToStream(version).write((ExampleSet) object, out);
		} else {
			throw new IOException("Serialization type " + SerializationType.STREAMED_EXAMPLE_SET_DENSE
					+ " only available for ExampleSets.");
		}
	}

	public int getVersion() {
		return version;
	}
}

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
package com.rapidminer.tools;

import java.io.IOException;
import java.io.Writer;


/**
 * By using this multiplier all written content is multiplied to the given writers.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class WriterMultiplier extends Writer {

	/** The writers to write the contents to. */
	private Writer[] writer;

	/** Creates a new writer multiplier. */
	public WriterMultiplier(Writer[] writer) {
		this.writer = writer;
	}

	/** Implements the abstract method of the superclass. */
	@Override
	public void write(char[] bytes, int offset, int length) throws IOException {
		for (int i = 0; i < writer.length; i++) {
			writer[i].write(bytes, offset, length);
		}
	}

	/** Closes all writers. */
	@Override
	public void close() throws IOException {
		for (int i = 0; i < writer.length; i++) {
			writer[i].close();
		}
	}

	/** Flushes all writers. */
	@Override
	public void flush() throws IOException {
		for (int i = 0; i < writer.length; i++) {
			writer[i].flush();
		}
	}
}

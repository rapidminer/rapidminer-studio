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
package com.rapidminer.gui.renderer;

import com.rapidminer.report.Readable;


/**
 * A simple default readable just build from a given text.
 * 
 * @author Ingo Mierswa
 */
public class DefaultReadable implements Readable {

	private String text;
	private boolean isInTargetEncoding = false;

	public DefaultReadable(String text) {
		this.text = text;
	}

	public DefaultReadable(String text, boolean isInTargetEncoding) {
		this.text = text;
		this.isInTargetEncoding = isInTargetEncoding;
	}

	@Override
	public String toString() {
		return text;
	}

	@Override
	public boolean isInTargetEncoding() {
		return isInTargetEncoding;
	}
}

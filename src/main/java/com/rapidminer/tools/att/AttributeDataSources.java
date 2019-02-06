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
package com.rapidminer.tools.att;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;


/**
 * A container class for all attribute data sources and a default source file.
 * 
 * @author Ingo Mierswa, Simon Fischer ingomierswa Exp $
 */
public class AttributeDataSources {

	/** A list of attribute data sources. */
	private List<AttributeDataSource> attributeDataSources;

	/** The default source file. */
	private File defaultSource;

	/** The charset of the default source */
	private Charset encoding;

	public AttributeDataSources(List<AttributeDataSource> attributeDataSources, File defaultSource, Charset encoding) {
		this.attributeDataSources = attributeDataSources;
		this.defaultSource = defaultSource;
		this.encoding = encoding;
	}

	public List<AttributeDataSource> getDataSources() {
		return attributeDataSources;
	}

	public File getDefaultSource() {
		return defaultSource;
	}

	public Charset getEncoding() {
		return encoding;
	}

	@Override
	public String toString() {
		return attributeDataSources.toString() + ", default file: " + defaultSource;
	}
}

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
package com.rapidminer.repository.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;


/**
 * Class for a process that is associated with a {@link ZipStreamResource}.
 *
 * @author Gisa Schaefer, Marcel Michel, Jan Czogalla
 * @since 7.0.0
 */
public class ZipResourceProcessEntry extends ResourceProcessEntry {

	private final ZipStreamResource zipStream;

	protected ZipResourceProcessEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository,
			ZipStreamResource zipStream) {
		super(parent, name, resource, repository);
		this.zipStream = zipStream;
	}

	@Override
	protected InputStream getResourceStream(String suffix) throws RepositoryException {
		return zipStream.getStream(getName(), getResource(), suffix);
	}

	@Override
	public String getDescription() {
		return zipStream.getDescription();
	}

}

/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ResourceProcessEntry extends ResourceDataEntry implements ProcessEntry {

	protected ResourceProcessEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public String retrieveXML() throws RepositoryException {
		InputStream in = null;
		try {
			in = Tools.getResourceInputStream(getResource() + ".rmp");
		} catch (IOException e1) {
			throw new RepositoryException("Missing resource: " + getResource() + ".rmp", e1);
		}
		try {
			return Tools.readTextFile(new InputStreamReader(in));
		} catch (IOException e) {
			throw new RepositoryException("IO error reading " + getResource() + ": " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}

	}

	@Override
	public void storeXML(String xml) throws RepositoryException {
		throw new RepositoryException("Repository is read only.");
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getType() {
		return ProcessEntry.TYPE_NAME;
	}
}

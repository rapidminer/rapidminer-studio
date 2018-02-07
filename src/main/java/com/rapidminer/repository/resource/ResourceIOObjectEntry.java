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

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;


/**
 * 
 * @author Simon Fischer
 * 
 */
public class ResourceIOObjectEntry extends ResourceDataEntry implements IOObjectEntry {

	private MetaData metaData;

	protected ResourceIOObjectEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getType() {
		return IOObjectEntry.TYPE_NAME;
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}

		InputStream in;
		try {
			in = Tools.getResourceInputStream(getResource() + ".ioo");
		} catch (IOException e1) {
			throw new RepositoryException("Resource '" + getResource() + ".ioo does not exist'.", e1);
		}
		try {
			return (IOObject) IOObjectSerializer.getInstance().deserialize(in);
		} catch (Exception e) {
			throw new RepositoryException("Cannot load data from '" + getResource() + ".ioo': " + e, e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData == null) {
			String mdResource = getResource() + ".md";
			InputStream in;
			try {
				in = Tools.getResourceInputStream(mdResource);
			} catch (IOException e1) {
				throw new RepositoryException("Meta data resource '" + mdResource + " does not exist'.");
			}
			ObjectInputStream objectIn = null;
			try {
				objectIn = new ObjectInputStream(in);
				this.metaData = (MetaData) objectIn.readObject();
				if (this.metaData instanceof ExampleSetMetaData) {
					for (AttributeMetaData amd : ((ExampleSetMetaData) metaData).getAllAttributes()) {
						if (amd.isNominal()) {
							amd.shrinkValueSet();
						}
					}
				}
				objectIn.close();
			} catch (Exception e) {
				throw new RepositoryException("Cannot load meta data from '" + mdResource + "': " + e, e);
			} finally {
				if (objectIn != null) {
					try {
						objectIn.close();
					} catch (IOException e) {
					}
				}
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
		return metaData;
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		throw new RepositoryException("This is a read-only sample data entry. Cannot store data here.");
	}

	@Override
	public boolean willBlock() {
		return metaData == null;
	}

	@Override
	public Class<? extends IOObject> getObjectClass() {
		try {
			return retrieveMetaData().getObjectClass();
		} catch (RepositoryException e) {
			return null;
		}
	}
}

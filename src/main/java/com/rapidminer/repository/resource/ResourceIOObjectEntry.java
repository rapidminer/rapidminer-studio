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
import java.io.ObjectInputStream;
import java.util.logging.Level;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.MetaDataFactory;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;


/**
 * @author Simon Fischer, Jan Czogalla
 */
public class ResourceIOObjectEntry extends ResourceDataEntry implements IOObjectEntry {

	protected MetaData metaData;

	protected ResourceIOObjectEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository) {
		super(parent, name, resource, repository);
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		try (InputStream in = getResourceStream(getSuffix())) {
			return readDataObject(in);
		} catch (IOException e) {
			throw new RepositoryException("Cannot load data from '" + getResource() + getSuffix() + "': " + e, e);
		}
	}

	/**
	 * Read the actual IOObject from the given {@link InputStream}.
	 *
	 * @param in the input stream to read from
	 * @throws IOException
	 * 		if an error occurs
	 * @since 9.3
	 */
	protected IOObject readDataObject(InputStream in) throws IOException {
		return (IOObject) IOObjectSerializer.getInstance().deserialize(in);
	}

	/**
	 * Get the suffix for this resource object.
	 *
	 * @return the resource file suffix with a leading dot
	 * @since 9.3
	 */
	protected String getSuffix() {
		return IOO_SUFFIX;
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData == null) {
			try (InputStream in = getResourceStream(getMetaDataSuffix())) {
				this.metaData = readMetaDataObject(in);
				MetaData.shrinkValues(metaData);
			} catch (RepositoryException e) {
				// in case meta data cannot be loaded (e.g. missing .md files), we try to create the meta data now
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.resource.ResourceIOObjectEntry.missing_metadata", getName());
				metaData = MetaDataFactory.getInstance().createMetaDataforIOObject(retrieveData(null), false);
			} catch (IOException | ClassNotFoundException e) {
				throw new RepositoryException("Cannot load meta data from '" + getResource() + getMetaDataSuffix() + "': " + e, e);
			}
		}
		return metaData;
	}

	/**
	 * File ending for the resource that contains the {@link MetaData}
	 *
	 * @return the suffix with a leading dot
	 * @since 9.3
	 */
	protected String getMetaDataSuffix() {
		return MD_SUFFIX;
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

	/**
	 * Read the {@link MetaData} object.
	 *
	 * @param in
	 * 		the stream to read the meta data from
	 * @return the {@link MetaData} for this {@link ResourceIOObjectEntry}
	 * @throws IOException
	 * 		in case reading did not work
	 * @throws ClassNotFoundException
	 * 		if deserialization is not possible due to the missing class
	 * @since 9.3
	 */
	protected MetaData readMetaDataObject(InputStream in) throws IOException, ClassNotFoundException {
		try (ObjectInputStream objectIn = new ObjectInputStream(in)) {
			return (MetaData) objectIn.readObject();
		}
	}
}

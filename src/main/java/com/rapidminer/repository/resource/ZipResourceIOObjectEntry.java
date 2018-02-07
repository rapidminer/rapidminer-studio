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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;


/**
 * Class for a IOObject that is associated with a {@link ZipStreamResource}.
 *
 * @author Gisa Schaefer, Marcel Michel
 * @since 7.0.0
 */
public class ZipResourceIOObjectEntry extends ResourceIOObjectEntry {

	private MetaData metaData;

	private final ZipStreamResource zipStream;

	protected ZipResourceIOObjectEntry(ResourceFolder parent, String name, String resource, ResourceRepository repository,
			ZipStreamResource zipStream) {
		super(parent, name, resource, repository);
		this.zipStream = zipStream;
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}

		try (ZipInputStream zip = zipStream.getStream()) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.isDirectory() || entry.getName().replaceFirst("/", "").contains("/")) {
					continue;
				}
				if (zipStream.getStreamPath() != null && !entry.getName().startsWith(zipStream.getStreamPath())) {
					continue;
				}
				String entryName;
				if (zipStream.getStreamPath() != null) {
					entryName = entry.getName().replaceFirst(zipStream.getStreamPath(), "");
				} else {
					entryName = entry.getName();
				}
				if ((getName() + ".ioo").equals(entryName)) {
					return (IOObject) IOObjectSerializer.getInstance().deserialize(zip);
				}
			}
			throw new RepositoryException("Missing resource: " + getResource() + ".ioo");
		} catch (IOException e) {
			throw new RepositoryException("Cannot load data from '" + getResource() + ".ioo': " + e, e);
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData == null) {
			try (ZipInputStream zip = zipStream.getStream()) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					if (entry.isDirectory() || entry.getName().replaceFirst("/", "").contains("/")) {
						continue;
					}
					if (zipStream.getStreamPath() != null && !entry.getName().startsWith(zipStream.getStreamPath())) {
						continue;
					}
					String entryName;
					if (zipStream.getStreamPath() != null) {
						entryName = entry.getName().replaceFirst(zipStream.getStreamPath(), "");
					} else {
						entryName = entry.getName();
					}
					if ((getName() + ".md").equals(entryName)) {
						try (ObjectInputStream objectIn = new ObjectInputStream(zip)) {
							this.metaData = (MetaData) objectIn.readObject();
							if (this.metaData instanceof ExampleSetMetaData) {
								for (AttributeMetaData amd : ((ExampleSetMetaData) metaData).getAllAttributes()) {
									if (amd.isNominal()) {
										amd.shrinkValueSet();
									}
								}
							}
							return metaData;
						} catch (ClassNotFoundException e) {
							throw new RepositoryException("Cannot load meta data from '" + getResource() + ".md" + "': " + e,
									e);
						}
					}
				}
				throw new RepositoryException("Missing resource: " + getResource() + ".md");
			} catch (IOException e1) {
				throw new RepositoryException("Cannot load meta data from '" + getResource() + ".md" + "': " + e1, e1);
			}

		}
		return metaData;
	}

	@Override
	public boolean willBlock() {
		return metaData == null;
	}

}

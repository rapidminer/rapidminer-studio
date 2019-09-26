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
package com.rapidminer.repository.local;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.tools.ExampleSetToStream;
import com.rapidminer.operator.tools.IOObjectSerializer;
import com.rapidminer.operator.tools.RMObjectInputStream;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.IOObjectEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.plugin.Plugin;


/**
 * Stores IOObject in a file. Either as IOO serialized files using {@link ExampleSetToStream} where
 * appropriate.
 *
 * @author Simon Fischer, Jan Czogalla
 */
public class SimpleIOObjectEntry extends SimpleDataEntry implements IOObjectEntry {

	private static final String PROPERTY_IOOBJECT_CLASS = "ioobject-class";

	private WeakReference<MetaData> metaData = null;
	private Class<? extends IOObject> dataObjectClass = null;

	public SimpleIOObjectEntry(String name, SimpleFolder containingFolder, LocalRepository repository) {
		super(name, containingFolder, repository);
	}

	@Override
	public String getSuffix() {
		return IOO_SUFFIX;
	}


	/**
	 * Suffix for the specialized {@link MetaData} of this type, like {@value #MD_SUFFIX}.
	 *
	 * @since 9.3
	 */
	protected String getMetaDataSuffix() {
		return MD_SUFFIX;
	}

	/**
	 * Returns the file associated with this entry's {@link MetaData}.
	 *
	 * @see #getMetaDataSuffix()
	 */
	protected File getMetaDataFile() {
		return getFile(getMetaDataSuffix());
	}

	@Override
	public IOObject retrieveData(ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		File dataFile = getDataFile();
		if (dataFile.exists()) {
			try (FileInputStream fis = new FileInputStream(dataFile)) {
				return readDataFromFile(fis);
			} catch (Exception e) {
				throw new RepositoryException("Cannot load data from '" + dataFile + "': " + e, e);
			}
		} else {
			throw new RepositoryException("File '" + dataFile + " does not exist'.");
		}
	}

	/**
	 * Read the actual IOObject from the given {@link FileInputStream}.
	 *
	 * @throws IOException
	 * 		if an error occurs
	 * @since 9.3
	 */
	protected IOObject readDataFromFile(FileInputStream fis) throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(fis)) {
			return (IOObject) IOObjectSerializer.getInstance().deserialize(in);
		}
	}

	@Override
	public MetaData retrieveMetaData() throws RepositoryException {
		if (metaData != null) {
			MetaData storedData = metaData.get();
			if (storedData != null) {
				return storedData;
			}
		}
		// otherwise metaData == null OR get() == null -> re-read
		MetaData readObject;
		checkMetaDataFile();
		File metaDataFile = getMetaDataFile();
		if (!metaDataFile.exists()) {
			throw new RepositoryException("Meta data file '" + metaDataFile + " does not exist'.");
		}
		try {
			readObject = readMetaDataObject(metaDataFile);
			MetaData.shrinkValues(readObject);
			this.metaData = new WeakReference<>(readObject);
		} catch (Exception e) {
			throw new RepositoryException("Cannot load meta data from '" + metaDataFile + "': " + e, e);
		}
		return readObject;
	}

	/**
	 * Before handing out the metadata from a file this method is invoked to perform a check on the filesystem level, for
	 * instance if creation of a missing metadata file should be done.
	 *
	 * @since 9.3
	 */
	protected void checkMetaDataFile() {
		// noop
	}

	/**
	 * Re-usability for {@link MetaData} retrieval by overriding this method that returns the {@link MetaData} which
	 * should be contained in the given file.
	 *
	 * @param metaDataFile
	 * 		{@link File} to load that contains previously stored {@link MetaData}
	 * @return the {@link MetaData} object loaded from the metaDataFile
	 * @throws IOException
	 * 		if reading failed
	 * @throws ClassNotFoundException
	 * 		if reading failed
	 * @since 9.3
	 */
	protected MetaData readMetaDataObject(File metaDataFile) throws IOException, ClassNotFoundException {
		try (FileInputStream fis = new FileInputStream(metaDataFile);
			 ObjectInputStream objectIn = new RMObjectInputStream(fis)) {
			return (MetaData) objectIn.readObject();
		}
	}

	@Override
	public void storeData(IOObject data, Operator callingOperator, ProgressListener l) throws RepositoryException {
		if (l != null) {
			l.setTotal(100);
			l.setCompleted(10);
		}
		boolean existed = getDataFile().exists();
		MetaData md = MetaData.forIOObject(data);
		// Serialize Non-ExampleSets as IOO
		try (FileOutputStream fos = new FileOutputStream(getDataFile())) {
			writeDataToFile(data, fos);
			if (l != null) {
				l.setCompleted(75);
			}
		} catch (Exception e) {
			throw new RepositoryException("Cannot store data at '" + getDataFile() + "': " + e, e);
		}
		// Save MetaData
		try (FileOutputStream fos = new FileOutputStream(getMetaDataFile())) {
			writeMetaDataToFile(md, fos);
			if (l != null) {
				l.setCompleted(90);
			}
		} catch (Exception e) {
			throw new RepositoryException("Cannot store data at '" + getMetaDataFile() + "': " + e, e);
		} finally {
			if (l != null) {
				l.setCompleted(100);
				l.complete();
			}
		}
		this.metaData = new WeakReference<>(md);
		putProperty(PROPERTY_IOOBJECT_CLASS, data.getClass().getName());

		if (existed) {
			getRepository().fireEntryChanged(this);
		}
	}

	/**
	 * Takes care of the actual storing of the {@link IOObject} in a file.
	 * @since 9.3
	 */
	protected void writeDataToFile(IOObject data, FileOutputStream fos) throws IOException, RepositoryException {
		try (OutputStream out = new BufferedOutputStream(fos)) {
			IOObjectSerializer.getInstance().serialize(out, data);
		}
	}

	/**
	 * Takes care of the actual storing of the {@link MetaData} in a file.
	 * @since 9.3
	 */
	protected void writeMetaDataToFile(MetaData md, FileOutputStream fos) throws IOException {
		try (ObjectOutputStream mdOut = new ObjectOutputStream(fos)) {
			mdOut.writeObject(md);
		}
	}

	@Override
	public String getDescription() {
		if (metaData != null) {
			MetaData md = metaData.get();
			if (md != null) {
				return md.getDescription();
			}
		}
		return getDefaultDescription();
	}

	/**
	 * Get a description for this entry.
	 *
	 * @return very short description, basically the name of this entry type
	 * @since 9.3
	 */
	protected String getDefaultDescription() {
		return "Simple entry.";
	}

	@Override
	public void delete() throws RepositoryException {
		if (getMetaDataFile().exists()) {
			getMetaDataFile().delete();
		}
		super.delete();
	}

	@Override
	protected void handleRename(String newName) throws RepositoryException {
		super.handleRename(newName);
		renameFile(getMetaDataFile(), newName);
	}

	@Override
	protected void handleMove(Folder newParent, String newName) throws RepositoryException {
		super.handleMove(newParent, newName);
		moveFile(getMetaDataFile(), ((SimpleFolder) newParent).getFile(), newName, getMetaDataSuffix());
	}

	@Override
	public boolean willBlock() {
		return metaData == null || metaData.get() == null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends IOObject> getObjectClass() {
		if (dataObjectClass != null) {
			return dataObjectClass;
		}
		// first try from properties file
		String className = getProperty(PROPERTY_IOOBJECT_CLASS);
		if (className != null) {
			try {
				dataObjectClass = (Class<? extends IOObject>) Class.forName(className);
				return dataObjectClass;
			} catch (ClassNotFoundException e) {
				try {
					dataObjectClass = (Class<? extends IOObject>) Class.forName(className, false,
							Plugin.getMajorClassLoader());
					return dataObjectClass;
				} catch (ClassNotFoundException e1) {
					return null;
				}
			}
		}
		// if not yet defined, retrieve it from meta data and store in properties
		try {
			dataObjectClass = retrieveMetaData().getObjectClass();
			if (dataObjectClass != null) {
				putProperty(PROPERTY_IOOBJECT_CLASS, dataObjectClass.getName());
			}
			return dataObjectClass;
		} catch (RepositoryException e) {
			return null;
		}
	}
}

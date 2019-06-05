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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import javax.swing.Action;

import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryConnectionsFolderDuplicateException;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryNotConnectionsFolderException;
import com.rapidminer.repository.RepositoryStoreOtherInConnectionsFolderException;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * @author Simon Fischer, Jan Czogalla
 */
public abstract class SimpleEntry implements Entry {

	protected static final String DOT = ".";

	private Properties properties;

	private String name;
	private LocalRepository repository;
	private SimpleFolder containingFolder;

	public SimpleEntry(String name, SimpleFolder containingFolder, LocalRepository repository) {
		this.name = name;
		this.repository = repository;
		this.containingFolder = containingFolder;
	}

	protected LocalRepository getRepository() {
		return repository;
	}

	protected void setRepository(LocalRepository repository) {
		this.repository = repository;
	}

	/** Sets the name but does not fire any events. */
	void setName(String name) {
		this.name = name;
	}

	@Override
	public Folder getContainingFolder() {
		return containingFolder;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean rename(String newName) throws RepositoryException {
		Folder formerParent = getContainingFolder();
		String formerName = getName();
		checkRename(formerParent, newName);
		handleRename(newName);
		renameFile(getPropertiesFile(), newName);
		this.name = newName;
		getRepository().fireEntryMoved(this, formerParent, formerName);
		return true;
	}

	protected abstract void handleRename(String newName) throws RepositoryException;

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean willBlock() {
		return false;
	}

	@Override
	public String getOwner() {
		return getProperty("owner");
	}

	@Override
	public RepositoryLocation getLocation() {
		try {
			if (getContainingFolder() != null) {
				return new RepositoryLocation(getContainingFolder().getLocation(), getName());
			} else {
				return new RepositoryLocation(getRepository().getName(), new String[] { getName() });
			}
		} catch (MalformedRepositoryLocationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Renames the file, keeping the extension and directory unchanged. If the file does not exist,
	 * returns silently.
	 */
	void renameFile(File file, String newBaseName) throws RepositoryException {
		renameFile(file, newBaseName, null, null);
	}

	/**
	 * Renames a file, keeping the extension, and moves it to the selected target directory. If the
	 * file does not exist, returns silently.
	 * 
	 * @param newBaseName
	 *            The new name without extension (e.g. 'file1' for a file called 'file.dat' before).
	 * @param extensionSuffix
	 *            The extension suffix of file without the dot (e.g. 'dat' for 'file.dat'). If null,
	 *            it is extracted from the current file.
	 * @param targetDirectory
	 *            The target directory for the renamed file, if <code>null</code> the current
	 *            directory of the file will be used.
	 * */
	boolean renameFile(File file, String newBaseName, String extensionSuffix, File targetDirectory)
			throws RepositoryException {

		if (!file.exists()) {
			LogService.getRoot()
					.log(Level.WARNING, "com.rapidminer.repository.local.SimpleEntry.renaming_file2_error", file);
			return false;
		}

		// if no target directory is provided, use the current one
		if (targetDirectory == null) {
			targetDirectory = file.getParentFile();
		}

		File dest;

		// no extension provided, extract it automatically from original file name
		boolean isDirectory = file.isDirectory();
		if (extensionSuffix == null) {
			String fileName = file.getName();
			int dot = fileName.lastIndexOf(DOT);

			// if file is directory or has no extension
			if (isDirectory || dot == -1) {
				// just use new name without extension
				dest = new File(targetDirectory, newBaseName);
			} else {
				// otherwise keep the old extension
				extensionSuffix = fileName.substring(dot + 1);
				dest = new File(targetDirectory, newBaseName + DOT + extensionSuffix);
			}
		} else {
			if (isDirectory) {
				// directories do not have an extension
				dest = new File(targetDirectory, newBaseName);
			} else {
				// file extension is provided, so use it
				dest = new File(targetDirectory, newBaseName + DOT + extensionSuffix);
			}
		}

		// otherwise rename file
		return file.renameTo(dest);
	}

	/**
	 * Checks if renaming or moving the entry is possible. If it is not possible, a
	 * {@link RepositoryException} will be thrown.
	 */
	private void checkRename(Folder newParent, String newName) throws RepositoryException {
		// only connection entries can be moved inside special folder, folders need special handling
		if (RepositoryTools.isInSpecialConnectionsFolder(newParent) && !(this instanceof ConnectionEntry) && !(this instanceof Folder)) {
			throw new RepositoryStoreOtherInConnectionsFolderException(Folder.MESSAGE_CONNECTION_FOLDER);
		} else if (!newParent.isSpecialConnectionsFolder() && (this instanceof ConnectionEntry)) {
			throw new RepositoryNotConnectionsFolderException(Folder.MESSAGE_CONNECTION_CREATION);
		} else if (newParent instanceof Repository && Folder.isConnectionsFolderName(newName, false)) {
			throw new RepositoryConnectionsFolderDuplicateException(Folder.MESSAGE_CONNECTION_FOLDER_DUPLICATE);
		}
		if (!RepositoryLocation.isNameValid(newName)) {
			throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(), "repository.illegal_entry_name", newName,
					getLocation()));
		}

		if (containingFolder != null) {
			List<DataEntry> dataEntries = newParent.getDataEntries();
			for (Entry entry : dataEntries) {
				if (entry.getName().equalsIgnoreCase(newName)) {
					throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(),
							"repository.repository_entry_with_same_name_already_exists", newName));
				}
			}
			List<Folder> subfolders = newParent.getSubfolders();
			for (Folder folder : subfolders) {
				if (folder.getName().equalsIgnoreCase(newName)) {
					throw new RepositoryException(I18N.getMessage(I18N.getErrorBundle(),
							"repository.repository_entry_with_same_name_already_exists", newName));
				}
			}
		}
	}

	private Pair<String, String> extractNameAndSuffix(File file) {
		String name;
		String suffix = null;
		String fileName = file.getName();
		int dot = fileName.lastIndexOf(DOT);
		if (file.isDirectory() || dot == -1) {
			// in case of directory or no extension, keep filename
			name = fileName;
		} else {
			// otherwise split
			name = fileName.substring(0, dot);
			suffix = fileName.substring(dot + 1);
		}
		return new Pair<>(name, suffix);
	}

	/**
	 * Moves a file to the new target directory without renaming it.
	 */
	boolean moveFile(File file, File targetDirectory) throws RepositoryException {
		Pair<String, String> nameAndSuffix = extractNameAndSuffix(file);
		return renameFile(file, nameAndSuffix.getFirst(), nameAndSuffix.getSecond(), targetDirectory);
	}

	/**
	 * Moves the file to a new location.
	 * 
	 * @param newEntryName
	 *            The {@link Entry}'s new name (without file extension). If newEntryName is null the
	 *            old name will be used.
	 * @param extensionSuffix
	 *            The {@link Entry}'s extension suffix with the dot (e.g. '.dat'). Will be used if
	 *            newEntryName is not null to keep the correct suffix.
	 */
	boolean moveFile(File file, File targetDirectory, String newEntryName, String extensionSuffix)
			throws RepositoryException {
		String name;
		String suffix;
		if (newEntryName == null) {
			Pair<String, String> nameAndSuffix = extractNameAndSuffix(file);
			name = nameAndSuffix.getFirst();
			suffix = nameAndSuffix.getSecond();
		} else {
			name = newEntryName;
			suffix = extensionSuffix.contains(".") ? extensionSuffix.substring(1) : null; // get rid
																							// of
																							// the
																							// dot
		}
		return renameFile(file, name, suffix, targetDirectory);
	}

	@Override
	public boolean move(Folder newParent) throws RepositoryException {
		checkRename(newParent, getName());
		handleMove(newParent, getName());
		moveFile(getPropertiesFile(), ((SimpleFolder) newParent).getFile());
		this.containingFolder.removeChild(this);
		this.containingFolder = (SimpleFolder) newParent;
		this.containingFolder.addChild(this);
		return true;
	}

	@Override
	public boolean move(Folder newParent, String newName) throws RepositoryException {
		checkRename(newParent, newName);
		handleMove(newParent, newName);
		moveFile(getPropertiesFile(), ((SimpleFolder) newParent).getFile(), newName, PROPERTIES_SUFFIX);

		this.containingFolder.removeChild(this);

		if (newName != null) {
			this.name = newName;
		}

		this.containingFolder = (SimpleFolder) newParent;
		this.containingFolder.addChild(this);
		return true;
	}

	protected abstract void handleMove(Folder newParent, String newName) throws RepositoryException;

	/*
	 * Properties We store the owner in a properties file because there is no system independent way
	 * of determining the user. TODO: Check if Java 7 has such a feature.
	 */
	private void loadProperties() {
		File propertiesFile = getPropertiesFile();
		if ((propertiesFile != null) && propertiesFile.exists()) {
			InputStream in;
			try {
				in = new FileInputStream(propertiesFile);
			} catch (FileNotFoundException e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.local.SimpleEntry.loading_repository_entry_properties_error",
								propertiesFile, e), e);
				return;
			}
			try {
				this.properties.loadFromXML(in);
			} catch (Exception e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.local.SimpleEntry.loading_repository_entry_properties_error",
								propertiesFile, e), e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void storeProperties() {
		File propertiesFile = getPropertiesFile();
		if (propertiesFile != null) {
			FileOutputStream os;
			try {
				os = new FileOutputStream(propertiesFile);
			} catch (FileNotFoundException e1) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.local.SimpleEntry.storing_repository_entry_properties_error",
								propertiesFile, e1), e1);
				return;
			}
			try {
				properties.storeToXML(os, "Properties of repository entry " + getName());
			} catch (IOException e) {
				LogService.getRoot().log(
						Level.WARNING,
						I18N.getMessage(LogService.getRoot().getResourceBundle(),
								"com.rapidminer.repository.local.SimpleEntry.storing_repository_entry_properties_error",
								propertiesFile, e), e);
			} finally {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private synchronized Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			loadProperties();
			if (properties.getProperty("owner") == null) {
				putProperty("owner", System.getProperty("user.name"));
			}
		}
		return properties;
	}

	protected void putProperty(String key, String value) {
		if (value != null) {
			getProperties().setProperty(key, value);
			storeProperties();
		}
	}

	protected String getProperty(String key) {
		return getProperties().getProperty(key);
	}

	/**
	 * Get a file associated with this {@link Entry}, specified by the given suffix.
	 * The returned file is located in the {@link #getContainingFolder() containing folder} and it's name is
	 * a concatenation of {@link #getName()} and the {@code suffix}.
	 *
	 * @since 9.3
	 */
	protected File getFile(String suffix) {
		return new File(((SimpleFolder) getContainingFolder()).getFile(), getName() + suffix);
	}

	private File getPropertiesFile() {
		return getFile(PROPERTIES_SUFFIX);
	}

	@Override
	public void delete() throws RepositoryException {
		File propFile = getPropertiesFile();
		if (propFile.exists()) {
			propFile.delete();
		}
		SimpleFolder parent = (SimpleFolder) getContainingFolder();
		if (parent != null) {
			parent.removeChild(this);
		}
	}

	@Override
	public Collection<Action> getCustomActions() {
		return null;
	}
}

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
package com.rapidminer.io.remote;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;


/**
 * A class for files that are not on the local hard drive. It is used to hold information about
 * files that can be accessed remotely. The file names are assumed to be of the following form
 * "/NameOfRemoteFileSystem/Folder/Subfolder(s)/Filename", for example
 * "/Dropbox/Photos/Sample Album" or "/Dropbox/Photos/Sample Album/sample.png".
 *
 * <p>
 * Most of the methods of {@link File} cannot be used for {@link RemoteFile} since they access the
 * hard drive; thus they are overwritten by methods doing nothing. By default all remote files are
 * read-only and cannot be renamed or deleted. Usual properties as the size, the last modified time
 * and if the file is a directory cannot be obtained from the hard drive; hence they have to be set
 * to archive the expected behavior.
 *
 * @author Gisa Schaefer
 * @since 6.1.0
 */
public class RemoteFile extends File {

	private static final long serialVersionUID = -3955270339797739703L;

	private Boolean directoryProperty = null;
	private long modifiedLast = 0;
	private long sizeInBytes = 0;

	/**
	 * Calls the constructor of {@link File}.
	 *
	 * @param pathname
	 *            a path of the following form
	 *            "/NameOfRemoteFileSystem/Folder/Subfolder(s)/Filename"
	 */
	public RemoteFile(String pathname) {
		super(pathname);
	}

	/**
	 * Calls the constructor of {@link File}.
	 *
	 * @param parent
	 *            a folder
	 * @param fileName
	 *            name of the file that is to be constructed inside the folder
	 */
	public RemoteFile(File parent, String fileName) {
		super(parent, fileName);
	}

	/**
	 * Calls the constructor of {@link File} and sets the directoryProperty.
	 *
	 * @param pathname
	 *            a path of the following form
	 *            "/NameOfRemoteFileSystem/Folder/Subfolder(s)/Filename"
	 * @param directoryProperty
	 *            <code>true</code> if the constructed file is a directory
	 */
	public RemoteFile(String pathname, Boolean directoryProperty) {
		super(pathname);
		this.directoryProperty = directoryProperty;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == this.getClass()) {
			return ((RemoteFile) obj).getPath().equals(this.getPath());
		}
		return false;
	}

	/**
	 * Sets the flag which decides if the file is a directory. If this flag is not set or set to
	 * <code> null </code> then {@link #isDirectory()} returns <code>false</code>, otherwise it
	 * returns this flag.
	 *
	 * @param directoryProperty
	 */
	public void setDirectoryProperty(Boolean directoryProperty) {
		this.directoryProperty = directoryProperty;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns the remote file with the parent
	 * path name if it is not just "/" and <code>null</code> otherwise.
	 */
	@Override
	public File getParentFile() {
		RemoteFile file = new RemoteFile(getParent());
		if (new RemoteFile("/").equals(file)) {
			return null;
		}
		return file;
	}

	/**
	 * Returns the last time the file was modified (in milliseconds since the epoch) if it has been
	 * set by {@link #setLastModified}, <code>0</code> otherwise.
	 */
	@Override
	public long lastModified() {
		return modifiedLast;
	}

	/**
	 * Sets the last time the file was modified (in milliseconds since the epoch) - needed since
	 * remote files cannot get this information from the hard drive.
	 *
	 * @param time
	 *            non-negative number representing the milliseconds since the epoch
	 */
	@Override
	public boolean setLastModified(long time) {
		if (time < 0) {
			throw new IllegalArgumentException("Negative time");
		}
		modifiedLast = time;
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>true</code>.
	 */
	@Override
	public boolean canRead() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns the file itself.
	 */
	@Override
	public File getCanonicalFile() throws IOException {
		return this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>true</code>.
	 */
	@Override
	public boolean exists() {
		return true;
	}

	/**
	 * If the flag directoryProperty is not set via {@link #setDirectoryProperty()} or set to
	 * <code> null </code> then this method returns <code>false</code>, otherwise it returns the
	 * flag directoryProperty.
	 */
	@Override
	public boolean isDirectory() {
		if (directoryProperty == null) {
			return false;
		} else {
			return directoryProperty;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean renameTo(File dest) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean delete() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} checks if the path starts with a separator.
	 */
	@Override
	public boolean isAbsolute() {
		return getPath().startsWith(File.separator);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns {@link #getPath}.
	 */
	@Override
	public String getAbsolutePath() {
		return getPath();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns {@link #getPath}.
	 */
	@Override
	public String getCanonicalPath() throws IOException {
		return getPath();
	}

	/**
	 * Returns the size in bytes if it has been set by {@link #setSizeInBytes}, <code>0</code>
	 * otherwise.
	 */
	@Override
	public long length() {
		return sizeInBytes;
	}

	/**
	 * Sets the size (in bytes) of a file - needed since remote files cannot get this information
	 * from the hard drive.
	 *
	 * @param sizeInBytes
	 *            non-negative number representing the size of the file in bytes
	 */
	public void setSizeInBytes(long sizeInBytes) {
		if (sizeInBytes < 0) {
			throw new IllegalArgumentException("Negative size");
		}
		this.sizeInBytes = sizeInBytes;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns the file itself.
	 */
	@Override
	public File getAbsoluteFile() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>false</code>.
	 */
	@Override
	public boolean canWrite() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} returns <code>true</code> if the file is
	 * not a directory.
	 */
	@Override
	public boolean isFile() {
		return !isDirectory();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>false</code>.
	 */
	@Override
	public boolean isHidden() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean createNewFile() throws IOException {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} throws an UnsupportedOperationException.
	 */
	@Override
	public void deleteOnExit() {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>null</code>.
	 */
	@Override
	public String[] list() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>null</code>.
	 */
	@Override
	public String[] list(FilenameFilter filter) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>null</code>.
	 */
	@Override
	public File[] listFiles() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>null</code>.
	 */
	@Override
	public File[] listFiles(FilenameFilter filter) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>null</code>.
	 */
	@Override
	public File[] listFiles(FileFilter filter) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean mkdir() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean mkdirs() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>false</code>.
	 */
	@Override
	public boolean canExecute() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>0L</code>.
	 */
	@Override
	public long getTotalSpace() {
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>0L</code>.
	 */
	@Override
	public long getFreeSpace() {
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} always returns <code>0L</code>.
	 */
	@Override
	public long getUsableSpace() {
		return 0L;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setReadOnly() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setWritable(boolean writable) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setReadable(boolean readable) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The default implementation for {@link RemoteFile} does nothing and always returns
	 * <code>false</code>.
	 */
	@Override
	public boolean setExecutable(boolean executable) {
		return false;
	}

}

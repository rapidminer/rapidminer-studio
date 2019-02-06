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
import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import com.rapidminer.gui.properties.celleditors.value.RemoteFileValueCellEditor;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.UserError;


/**
 * This file system view is used for files that are not on the hard drive. It works with
 * {@link RemoteFile}s and overwrites the methods of {@link FileSystemView} that access the hard
 * drive. It can be used with a {@link JFileChooser} to select {@link RemoteFile}s as if they were
 * normal files.
 *
 * @author Gisa Schaefer
 * @since 6.1.0
 */
public abstract class RemoteFileSystemView extends FileSystemView {

	/** flag which controls that an error while getting files is only shown once */
	private boolean errorShown = false;

	/**
	 * Determines if the given file is a root in the navigable tree(s). Examples: Windows 98 has one
	 * root, the Desktop folder. DOS has one root per drive letter, <code>C:\</code>,
	 * <code>D:\</code>, etc. Unix has one root, the <code>"/"</code> directory.
	 *
	 * The default implementation checks if the given file agrees with the one specified by
	 * {@link #getRootDirectory()}.
	 *
	 */
	@Override
	public boolean isRoot(File f) {
		return getRootDirectory().equals(f);
	}

	/**
	 * Name of a file, directory, or folder as it would be displayed in a system file browser.
	 * Example from Windows: the "M:\" directory displays as "CD-ROM (M:)"
	 *
	 * The default implementation returns the name of the file.
	 *
	 */
	@Override
	public String getSystemDisplayName(File f) {
		return f == null ? null : f.getName();
	}

	/**
	 * Type description for a file, directory, or folder as it would be displayed in a system file
	 * browser. Example from Windows: the "Desktop" folder is described as "Desktop".
	 *
	 * The default implementation returns either "Folder" for a directory or "ABC-File" for a file
	 * with name "somefile.abc".
	 */
	@Override
	public String getSystemTypeDescription(File f) {
		if (f.isDirectory()) {
			return "Folder";
		}
		int separatorPosition = f.getName().lastIndexOf(".");
		String fileending = "";
		if (separatorPosition > -1) {
			fileending = f.getName().substring(separatorPosition + 1).toUpperCase() + "-";
		}
		return fileending + "File";
	}

	/**
	 * Icon for a file, directory, or folder as it would be displayed in a system file browser. The
	 * default implementation returns the standard icon of either a folder or a file.
	 *
	 */
	@Override
	public Icon getSystemIcon(File f) {
		return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
	}

	/**
	 * Checks whether folder is the same as file.getParentFile().
	 */
	@Override
	public boolean isParent(File folder, File file) {
		return folder.equals(file.getParentFile());
	}

	/**
	 * @return a File object constructed with
	 *         <code>{@link #createFileObject}(parent, fileName)</code>.
	 */
	@Override
	public File getChild(File parent, String fileName) {
		return createFileObject(parent, fileName);
	}

	/**
	 * {@inheritDoc} The default implementation always returns <code>true</code>.
	 */
	@Override
	public boolean isFileSystem(File f) {
		return true;
	}

	/**
	 * {@inheritDoc} The default implementation always returns <code>null</code>.
	 */
	@Override
	public File createNewFolder(File containingDir) throws IOException {
		return null;
	}

	/**
	 * {@inheritDoc} The default implementation always returns <code>false</code>.
	 */
	@Override
	public boolean isHiddenFile(File f) {
		return false;
	}

	/**
	 * {@inheritDoc} The default implementation checks if the given file agrees with the one
	 * specified by {@link #getRootDirectory()}.
	 */
	@Override
	public boolean isFileSystemRoot(File dir) {
		return getRootDirectory().equals(dir);
	}

	@Override
	public boolean isComputerNode(File dir) {
		return false;
	}

	/**
	 * {@inheritDoc} The default implementation returns the directory specified by
	 * {@link #getRootDirectory()}.
	 */
	@Override
	public File[] getRoots() {
		return new File[] { getRootDirectory() };
	}

	/**
	 * Returns the directory specified by {@link #getRootDirectory()}.
	 */
	@Override
	public File getHomeDirectory() {
		return getRootDirectory();
	}

	/**
	 * {@inheritDoc} The default implementation returns the directory specified by
	 * {@link #getRootDirectory()}.
	 */
	@Override
	public File getDefaultDirectory() {
		return getRootDirectory();
	}

	@Override
	public File createFileObject(File dir, String filename) {
		if (dir == null) {
			return new RemoteFile(filename);
		} else {
			return new RemoteFile(dir, filename);
		}
	}

	@Override
	public File createFileObject(String path) {
		return new RemoteFile(path);
	}

	/**
	 * {@inheritDoc} The default implementation does this via {@link #getRemoteFiles}.
	 */
	@Override
	public File[] getFiles(File dir, boolean useFileHiding) {
		try {
			RemoteFile directory = new RemoteFile(dir.getPath());
			final RemoteFile[] remoteFiles = getRemoteFiles(directory);
			errorShown = false;
			return remoteFiles;
		} catch (UserError | IOException e) {
			// needed since getFiles is called twice in a row by the file chooser
			if (!errorShown) {
				SwingTools.showSimpleErrorMessage("cannot_open_remote_folder", e);
				errorShown = true;
			}
			return new File[0];
		}
	}

	/**
	 * @return the parent directory of <code>dir</code>, or <code>null</code> if <code>dir</code> is
	 *         <code>null</code> or the directory specified by {@link #getRootDirectory()}.
	 */
	@Override
	public File getParentDirectory(File dir) {
		if (dir == null || dir.equals(getRootDirectory()) || dir.getParentFile() == null
				|| dir.getParentFile().getPath() == null) {
			return null;
		}
		return new RemoteFile(dir.getParentFile().getPath(), true);
	}

	/**
	 * Returns the root directory of the remote file system.
	 *
	 * @return the remote file that is the root of the remote file system
	 */
	public abstract RemoteFile getRootDirectory();

	/**
	 * Gets the list of {@link RemoteFile}s inside the directory dir.
	 *
	 * @param dir
	 *            the directory
	 * @return the files in the directory dir
	 * @throws IOException
	 *             when fetching the content of the remote directory fails
	 * @throws UserError
	 *             when fetching the content of the remote directory fails
	 */
	public abstract RemoteFile[] getRemoteFiles(RemoteFile dir) throws IOException, UserError;

	/**
	 * Returns a flag for whether new folders can be created, <code>false</code> by default. This
	 * flag is used by the {@link FileChooserUI} to decide if the control elements for creating a
	 * new folder are shown.
	 *
	 */
	public boolean isCreatingNewFolderEnabled() {
		return false;
	}

	/**
	 * Returns a flag for whether files can be renamed, <code>false</code> by default. This flag is
	 * used by the {@link FileChooserUI} to decide if the control elements for renaming files are
	 * shown.
	 *
	 */
	public boolean isRenamingEnabled() {
		return false;
	}

	/**
	 * Checks whether the parent of the given file exists.
	 *
	 * Used to open the folder in the {@link JFileChooser} if it exists. The default implementation
	 * returns false, which leads to opening the root folder in the {@link JFileChooser}. Is not
	 * related to {@link RemoteFile#exists()}
	 *
	 * @param file
	 *            which should be checked
	 * @return if the parent of the given file exists
	 */
	public boolean parentExists(String file) {
		return false;
	}

	/**
	 * Returns a flag for whether files can be deleted, <code>false</code> by default. This flag is
	 * used by the {@link FileChooserUI} to decide if the control elements for deleting files are
	 * shown.
	 *
	 */
	public boolean isDeletingEnabled() {
		return false;
	}

	/**
	 * Method for checking the remote file system before the file chooser is opened. Used in
	 * {@link RemoteFileValueCellEditor} before the file chooser is created.
	 */
	public abstract void checkFileSystemAvailability() throws UserError;

	/**
	 * Returns the normalized path name of the file. The default implementation returns just
	 * <code>file.getPath()</code>. This should be overwritten if the path of a file resulting from
	 * the file chooser should not depend on the local file system.
	 *
	 */
	public String getNormalizedPathName(File file) {
		return file.getPath();
	}
}

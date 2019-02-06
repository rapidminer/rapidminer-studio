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
package com.rapidminer.parameter;

import javax.swing.JFileChooser;

import com.rapidminer.gui.properties.celleditors.value.RemoteFileValueCellEditor;
import com.rapidminer.io.remote.RemoteFileSystemView;


/**
 * A parameter type for remote files, for example files coming from Dropbox. Requires a
 * {@link RemoteFileSystemView} for opening a file chooser in the {@link RemoteFileValueCellEditor}.
 *
 * @author Gisa Schaefer
 * @since 6.1.0
 *
 */
public class ParameterTypeRemoteFile extends ParameterTypeString {

	private static final long serialVersionUID = -691704467412002399L;

	private RemoteFileSystemView remoteFileSystemView;

	private int fileSelectionMode = JFileChooser.FILES_ONLY;

	public ParameterTypeRemoteFile(String key, String description, RemoteFileSystemView remoteFileSystemView) {
		super(key, description);
		this.remoteFileSystemView = remoteFileSystemView;
	}

	public ParameterTypeRemoteFile(String key, String description, boolean optional,
			RemoteFileSystemView remoteFileSystemView) {
		this(key, description, remoteFileSystemView);
		setOptional(optional);
	}

	public ParameterTypeRemoteFile(String key, String description, boolean optional, boolean expert,
			RemoteFileSystemView remoteFileSystemView) {
		this(key, description, remoteFileSystemView);
		setExpert(expert);
		setOptional(optional);
	}

	public RemoteFileSystemView getRemoteFileSystemView() {
		return remoteFileSystemView;
	}

	/**
	 * Returns the file selection mode for the file chooser. See
	 * {@link JFileChooser#getFileSelectionMode()}. Default is {@link JFileChooser#FILES_ONLY}, to
	 * use a diffent mode, call {@link #setFileSelectionMode(int)}.
	 *
	 * @return one of {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#FILES_AND_DIRECTORIES},
	 *         and {@link JFileChooser#DIRECTORIES_ONLY}
	 * @since 6.5.0
	 */
	public int getFileSelectionMode() {
		return fileSelectionMode;
	}

	/**
	 * Sets the file selection mode for the file chooser. See
	 * {@link JFileChooser#getFileSelectionMode()}. Default is {@link JFileChooser#FILES_ONLY}.
	 *
	 * @param fileSelectionMode
	 *            one of {@link JFileChooser#FILES_ONLY}, {@link JFileChooser#FILES_AND_DIRECTORIES}
	 *            , and {@link JFileChooser#DIRECTORIES_ONLY}
	 * @since 6.5.0
	 */
	public void setFileSelectionMode(int fileSelectionMode) {
		this.fileSelectionMode = fileSelectionMode;
	}
}

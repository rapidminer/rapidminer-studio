/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.encryption.EncryptionProvider;


/**
 * A file process location. Note that a file process is always using the {@link EncryptionProvider#DEFAULT_CONTEXT} for
 * encryption/decryption.
 *
 * @author Simon Fischer
 */
public class FileProcessLocation implements ProcessLocation {

	private static final String FILE_PROCESS_ICON = "hard_drive.png";

	private final File file;

	public FileProcessLocation(File file) {
		this.file = file;
	}

	@Override
	public Process load(ProgressListener l) throws IOException, XMLException {
		if (!file.exists()) {
			throw new IOException("Process file '" + file + "' does not exist.");
		}
		if (!file.canRead()) {
			throw new IOException("Process file '" + file + "' is not readable.");
		}
		return new Process(file, EncryptionProvider.DEFAULT_CONTEXT, l);
	}

	@Override
	public String toHistoryFileString() {
		return "file " + file.getAbsolutePath();
	}

	@Override
	public String getRawXML() throws IOException {
		return Tools.readOutput(new BufferedReader(new FileReader(file)));
	}

	@Override
	public void store(Process process, ProgressListener listener) throws IOException {
		try {
			if (listener != null) {
				listener.setCompleted(33);
			}
			process.getRootOperator().writeXML(Files.newBufferedWriter(file.toPath()), false, EncryptionProvider.DEFAULT_CONTEXT);
			if (listener != null) {
				listener.setCompleted(100);
			}
			LogService.getRoot().log(Level.INFO, "com.rapidminer.FileProcessLocation.saved_process_definition_file", file);
		} catch (IOException e) {
			throw new IOException("Cannot save process: " + e, e);
		} finally {
			if (listener != null) {
				listener.setCompleted(100);
				listener.complete();
			}
		}
	}

	public File getFile() {
		return file;
	}

	@Override
	public String toMenuString() {
		return file.getName();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FileProcessLocation)) {
			return false;
		} else {
			return ((FileProcessLocation) o).file.equals(this.file);
		}
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public String toString() {
		return file.toString();
	}

	@Override
	public String getShortName() {
		return file.getName();
	}

	@Override
	public String getIconName() {
		return FILE_PROCESS_ICON;
	}
}

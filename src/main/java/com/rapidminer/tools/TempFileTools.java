/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import com.rapidminer.RapidMiner;


/**
 * Util methods to create temp files and have them removed automatically
 *
 * @author Andreas Timm
 * @since 9.7
 */
public enum TempFileTools {
	;

	/**
	 * Create a temporary file that will be removed during {@link RapidMiner#cleanup()} and on shutdown.
	 *
	 * @param prefix the prefix string to be used in generating the file's name; may be {@code null}
	 * @param suffix the suffix string to be used in generating the file's name; may be {@code null}, in which case
	 *               "{@code .tmp}" is used
	 * @return the path to the newly created file that did not exist before this method was invoked
	 * @throws IllegalArgumentException      if the prefix or suffix parameters cannot be used to generate a candidate
	 *                                       file name
	 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically when
	 *                                       creating the directory
	 * @throws IOException                   if an I/O error occurs or the temporary-file directory does not exist
	 * @throws SecurityException             In the case of the default provider, and a security manager is installed,
	 *                                       the {@link SecurityManager#checkWrite(String) checkWrite} method is invoked
	 *                                       to check write access to the file.
	 */
	public static Path createTempFile(String prefix, String suffix) throws IOException {
		return createTempFile(prefix, suffix, true);
	}

	/**
	 * Create a temporary file that will be removed during {@link RapidMiner#cleanup()} if removeOnCleanup is set to
	 * {@code true} and on shutdown.
	 *
	 * @param prefix          the prefix string to be used in generating the file's name; may be {@code null}
	 * @param suffix          the suffix string to be used in generating the file's name; may be {@code null}, in which
	 *                        case "{@code .tmp}" is used
	 * @param removeOnCleanup flag to remove the temporary file during {@link RapidMiner#cleanup()}
	 * @return the path to the newly created file that did not exist before this method was invoked
	 * @throws IllegalArgumentException      if the prefix or suffix parameters cannot be used to generate a candidate
	 *                                       file name
	 * @throws UnsupportedOperationException if the array contains an attribute that cannot be set atomically when
	 *                                       creating the directory
	 * @throws IOException                   if an I/O error occurs or the temporary-file directory does not exist
	 * @throws SecurityException             In the case of the default provider, and a security manager is installed,
	 *                                       the {@link SecurityManager#checkWrite(String) checkWrite} method is invoked
	 *                                       to check write access to the file.
	 */
	public static Path createTempFile(String prefix, String suffix, boolean removeOnCleanup) throws IOException {
		Path tempFile = Files.createTempFile(prefix, suffix);
		if (removeOnCleanup) {
			registerCleanup(tempFile);
		} else {
			registerShutdowncleanup(tempFile);
		}
		return tempFile;
	}

	/**
	 * Register deletion of this path during {@link RapidMiner#cleanup() cleanup} and, to make sure this happens, on
	 * shutdown of RapidMiner.
	 *
	 * @param path that is not required for execution of different processes and may be deleted during cleanup
	 */
	public static void registerCleanup(Path path) {
		if (path != null) {
			registerCleanup(path.toFile());
		}
	}

	/**
	 * Register deletion of this file during {@link RapidMiner#cleanup() cleanup} and, to make sure this happens, on
	 * shutdown of RapidMiner.
	 *
	 * @param file that is not required for execution of different processes and may be deleted during cleanup
	 */
	public static void registerCleanup(File file) {
		if (file != null) {
			RapidMiner.registerCleanupHook(() -> FileUtils.deleteQuietly(file));
			registerShutdowncleanup(file);
		}
	}

	/**
	 * Register deletion of this path on shutdown of RapidMiner.
	 *
	 * @param path that is not required for the next run of RapidMiner
	 */
	public static void registerShutdowncleanup(Path path) {
		if (path != null) {
			registerShutdowncleanup(path.toFile());
		}
	}

	/**
	 * Register deletion of this file on shutdown of RapidMiner.
	 *
	 * @param file that is not required for the next run of RapidMiner
	 */
	public static void registerShutdowncleanup(File file) {
		if (file != null) {
			RapidMiner.addShutdownHook(() -> FileUtils.deleteQuietly(file));
		}
	}

}

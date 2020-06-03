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
package com.rapidminer.repository.versioned;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.operator.IOObject;
import com.rapidminer.repository.BinaryEntry;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.DataEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.ValidationUtil;


/**
 * Some utility methods for the new filesystem based repository.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public enum FilesystemRepositoryUtils {

	INSTANCE;


	private static final Set<String> TEXT_DIFF_SUFFIXES = Collections.synchronizedSet(new HashSet<>());
	static {
		TEXT_DIFF_SUFFIXES.add(FilesystemRepositoryUtils.normalizeSuffix(ProcessEntry.RMP_SUFFIX));
		TEXT_DIFF_SUFFIXES.add("txt"); //regular text
		TEXT_DIFF_SUFFIXES.add("py"); //python scripts
		TEXT_DIFF_SUFFIXES.add("ipynb"); //jupyter notebooks
		TEXT_DIFF_SUFFIXES.add("r"); //r scripts
		TEXT_DIFF_SUFFIXES.add("md"); // markdown
		TEXT_DIFF_SUFFIXES.add("csv"); // CSV formatted file
		TEXT_DIFF_SUFFIXES.add("xml"); // XML format
		TEXT_DIFF_SUFFIXES.add("json"); // JSON format
	}


	/**
	 * Gets the {@link DataEntry} (sub-)class for the given suffix as it is stored in the {@link
	 * NewFilesystemRepository}.
	 *
	 * @param suffix the suffix, will be normalized
	 * @return the {@link DataEntry} (sub-)class, never {@code null}
	 */
	public Class<? extends DataEntry> getDataEntryClassForSuffix(String suffix) {
		suffix = normalizeSuffix(suffix);
		if (FilesystemRepositoryAdapter.getInternalSuffixes().contains(suffix)) {
			if (normalizeSuffix(ProcessEntry.RMP_SUFFIX).equals(suffix)) {
				return ProcessEntry.class;
			} else if (normalizeSuffix(ConnectionEntry.CON_SUFFIX).equals(suffix)) {
				return ConnectionEntry.class;
			} else {
				if (IOObjectSuffixRegistry.getRegisteredSuffixes().contains(suffix)) {
					Class<? extends IOObject> ioObjectClass = IOObjectSuffixRegistry.getIOObjectClass(suffix);
					return IOObjectEntryTypeRegistry.getEntryClassForIOObjectClass(ioObjectClass);
				} else {
					// apparently known but not here
					LogService.getRoot().log(Level.WARNING, "com.rapidminer.repository.versioned.FilesystemRepositoryUtils.unexpected_suffix", suffix);
					return DataEntry.class;
				}
			}
		} else {
			return BinaryEntry.class;
		}
	}

	/**
	 * Normalize a file suffix to a lowercase and without leading dot version.
	 *
	 * @param suffix the suffix, can be {@code null} in which case it gets converted to an empty string
	 * @return the normalized, lowercase suffix without leading dot OR an empty string if the input was null, never
	 * {@code null}
	 */
	public static String normalizeSuffix(String suffix) {
		return suffix == null ? "" : suffix.trim().toLowerCase(Locale.ENGLISH).replaceFirst("^\\.", "");
	}

	/**
	 * Registers a file suffix as viable for a textual diff. Will be used e.g. in the conflict UI dialog.
	 *
	 * @param suffix the suffix, must not be {@code null} or empty
	 * @since 9.7
	 */
	public static void addTextualDiffSuffix(String suffix) {
		TEXT_DIFF_SUFFIXES.add(normalizeSuffix(ValidationUtil.requireNonNull(StringUtils.stripToNull(suffix), "suffix")));
	}

	/**
	 * Returns whether the file of a given path is diffable as a text.
	 *
	 * @param path the path, must not be {@code null}
	 * @return {@code true} if this can be diffed as a text; {@code false} otherwise
	 */
	public static boolean isPathTextuallyDiffable(Path path) {
		return TEXT_DIFF_SUFFIXES.contains(normalizeSuffix(RepositoryTools.getSuffixFromFilename(
				ValidationUtil.requireNonNull(path, "path").getFileName().toString())));
	}
}

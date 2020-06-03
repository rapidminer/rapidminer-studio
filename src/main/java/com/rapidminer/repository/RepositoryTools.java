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
package com.rapidminer.repository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import com.rapidminer.RapidMiner;
import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.external.alphanum.AlphanumComparator.AlphanumCaseSensitivity;
import com.rapidminer.operator.IOObject;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.repository.RepositoryManager.RepositoryType;
import com.rapidminer.repository.versioned.IOObjectEntryTypeRegistry;
import com.rapidminer.repository.versioned.IOObjectSuffixRegistry;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.ValidationUtil;


/**
 * This class contains utility methods to compare {@link Repository}s, {@link Entry}s and
 * {@link Folder}s.
 *
 * @author Denis Schernov, Marcel Seifert
 *
 * @since 7.3
 */
public final class RepositoryTools {

	private static final AlphanumComparator ALPHANUMERIC_COMPARATOR = new AlphanumComparator(
			AlphanumCaseSensitivity.INSENSITIVE);

	private static final String PARAMETER_CUSTOM_FILE_BROWSER = "rapidminer.system.files.file_browser";
	private static final String PARAMETER_CUSTOM_OPEN_FILE_COMMANDS = "rapidminer.system.files.open_file_commands";

	private static final String SUFFIX_SEPARATOR = ".";

	/**
	 * Shows only repos that support {@link com.rapidminer.repository.BinaryEntry}, omits Connections folder but shows
	 * everything else.
	 *
	 * @since 9.7
	 */
	public static final Predicate<Entry> ONLY_REPOSITORIES_WITH_BINARY_ENTRY_SUPPORT = e -> {
		if (e instanceof Repository) {
			return ((Repository) e).isSupportingBinaryEntries();
		} else if (e instanceof Folder) {
			return !((Folder) e).isSpecialConnectionsFolder();
		} else {
			return true;
		}
	};

	/**
	 * Shows only {@link BinaryEntry} and {@link BlobEntry}.
	 *
	 * @since 9.7
	 */
	public static final Predicate<Entry> ONLY_BLOB_AND_BINARY_ENTRIES = e -> {
		if (e instanceof Folder) {
			return !((Folder) e).isSpecialConnectionsFolder();
		} else {
			return e instanceof BinaryEntry || e instanceof BlobEntry;
		}
	};

	/**
	 * A predicate that only allows {@link ProcessEntry} objects.
	 *
	 * @since 9.7
	 */
	public static final Predicate<DataEntry> ONLY_PROCESS_ENTRIES = dataEntry -> dataEntry instanceof ProcessEntry;

	/**
	 * A predicate that only allows {@link ConnectionEntry} objects.
	 *
	 * @since 9.7
	 */
	public static final Predicate<DataEntry> ONLY_CONNECTION_ENTRIES = dataEntry -> dataEntry instanceof ConnectionEntry;

	/**
	 * A predicate that only allows {@link IOObjectEntry} objects.
	 *
	 * @since 9.7
	 */
	public static final Predicate<DataEntry> ONLY_IOOBJECT_ENTRIES = dataEntry -> dataEntry instanceof IOObjectEntry;

	/**
	 * A predicate that only allows {@link BlobEntry} objects.
	 *
	 * @since 9.7
	 */
	public static final Predicate<DataEntry> ONLY_BLOB_ENTRIES = dataEntry -> dataEntry instanceof BlobEntry;

	/**
	 * A name based filter for {@link Entry Entries}.
	 *
	 * @since 9.7
	 */
	public static final BiPredicate<Entry, String> NAME_FILTER = (e, n) -> e.getName().equals(n);


	/**
	 * Private constructor which throws if called.
	 */
	private RepositoryTools() {
		throw new AssertionError("Utility class must not be instatiated!");
	}

	/**
	 * Compares an {@link Entry} or {@link Folder} to another by comparing their names as
	 * {@link String} using simple java comparison.
	 *
	 * @param entry1
	 *            The {@link Entry} or {@link Folder} which should be compared to the second
	 *            {@link Entry} or {@link Folder}.
	 * @param entry2
	 *            The {@link Entry} or {@link Folder} which should be compared to the first
	 *            {@link Entry} or {@link Folder}.
	 * @return one of -1, 0, or 1 according to whether first {@link Entry}s or {@link Folder}s name
	 *         as a {@link String} is less, equal or higher than the second {@link Entry}s or
	 *         {@link Folder}s name.
	 */
	public static final Comparator<Entry> SIMPLE_NAME_COMPARATOR = (entry1, entry2) -> {
		Integer nullComparison = compareForNull(entry1, entry2);
		if (nullComparison != null) {
			return nullComparison;
		}
		return entry1.getName().compareTo(entry2.getName());
	};

	/**
	 * Compares an {@link Entry} to another by comparing their names as {@link String} using
	 * alphanumeric comparison.
	 *
	 * @param entry1
	 *            The {@link Entry} which should be compared to the second {@link Entry}.
	 * @param entry2
	 *            The {@link Entry} which should be compared to the first {@link Entry}.
	 * @return one of -1, 0, or 1 according to whether {@link Entry}1s name as a {@link String} is
	 *         less, equal or higher than {@link Entry}2s name.
	 */
	public static final Comparator<Entry> ENTRY_COMPARATOR = (entry1, entry2) -> {
		int specialFolderSorting = specialFolderFirst(entry1, entry2);
		if (specialFolderSorting != 0) {
			return specialFolderSorting;
		}
		Integer nullComparison = compareForNull(entry1, entry2);
		if (nullComparison != null) {
			return nullComparison;
		}
		return ALPHANUMERIC_COMPARATOR.compare(entry1.getName(), entry2.getName());
	};

	/**
	 * Compares an {@link Entry} to another by comparing their last modified dates {@link String}
	 * descending from new to old.
	 *
	 * @param entry1
	 *            The {@link Entry} which should be compared to the second {@link Entry}.
	 * @param entry2
	 *            The {@link Entry} which should be compared to the first {@link Entry}.
	 * @return one of -1, 0, or 1 according to whether {@link Entry}1s date as a {@link String} is
	 *         less, equal or higher than {@link Entry}2s date.
	 * @since 7.4
	 */
	public static final Comparator<Entry> ENTRY_COMPARATOR_LAST_MODIFIED = (entry1, entry2) -> {
		int specialFolderSorting = specialFolderFirst(entry1, entry2);
		if (specialFolderSorting != 0) {
			return specialFolderSorting;
		}
		boolean entry1HasDate = entry1 instanceof DateEntry;
		boolean entry2HasDate = entry2 instanceof DateEntry;
		// sort entries without modification date to front (i.e. usually folder)
		if (entry1HasDate != entry2HasDate) {
			return entry1HasDate ? 1 : -1;
		}
		// same type; sort by date if possible
		if (entry1HasDate) {
			DateEntry dataEntry1 = (DateEntry) entry1;
			DateEntry dataEntry2 = (DateEntry) entry2;
			int compareValue = Long.compare(dataEntry2.getDate(), dataEntry1.getDate());
			if (compareValue != 0) {
				return compareValue;
			}
		}
		// same date or no date at all => sort by name or null
		return ENTRY_COMPARATOR.compare(entry1, entry2);
	};

	/**
	 * Sort special folders before all other folders.
	 */
	private static int specialFolderFirst(Entry entry1, Entry entry2) {
		boolean firstIsSpecial = entry1 instanceof Folder && ((Folder) entry1).isSpecialConnectionsFolder();
		boolean secondIsSpecial = entry2 instanceof Folder && ((Folder) entry2).isSpecialConnectionsFolder();
		if (firstIsSpecial) {
			return secondIsSpecial ? 0 : -1;
		} else {
			return secondIsSpecial ? 1 : 0;
		}
	}

	/**
	 * Compares two repositories, ordered by {@link RepositoryType} (Samples, DB, Local
	 * Repositories, Remote Repositories, Others). If the {@link RepositoryType} of the
	 * {@link Repository}s is identical then their names as {@link String} will be compared by
	 * alphanumeric comparison.
	 *
	 * @param repository1
	 *            The {@link Repository} which should be compared to the second {@link Repository}.
	 * @param repository2
	 *            The {@link Repository} which should be compared to the first {@link Repository}.
	 * @return one of -1, 0, or 1 according to whether {@link Repository}1 {@link RepositoryType} is
	 *         higher prioritized than the {@link RepositoryType} of {@link Repository}2. If both
	 *         have the same {@link RepositoryType} it will return an {@link Integer} depending to
	 *         whether {@link Repository}1 name as a {@link String} is less, equal or higher than
	 *         {@link Repository}2 name.
	 */
	public static final Comparator<Repository> REPOSITORY_COMPARATOR = (repository1, repository2) -> {
		Integer nullComparison = compareForNull(repository1, repository2);
		if (nullComparison != null) {
			return nullComparison;
		}
		RepositoryType repositoryType = RepositoryType.getRepositoryType(repository1);
		RepositoryType theirRepositoryType = RepositoryType.getRepositoryType(repository2);
		// treat local and "filesystem new" equally
		repositoryType = repositoryType == RepositoryType.LOCAL ? RepositoryType.FILESYSTEM_NEW : repositoryType;
		theirRepositoryType = theirRepositoryType == RepositoryType.LOCAL ? RepositoryType.FILESYSTEM_NEW : theirRepositoryType;
		int compareValue = repositoryType.compareTo(theirRepositoryType);
		if (compareValue == 0) { // same repository type
			if (repositoryType == RepositoryType.RESOURCES) { // special resource repositories
				compareValue = compareResourceRepositoryNames(repository1.getName(), repository2.getName());
				if (compareValue != 0) {
					return compareValue;
				}
			}
			return ALPHANUMERIC_COMPARATOR.compare(repository1.getName(), repository2.getName());
		} else {
			return compareValue;
		}
	};

	/** @since 9.0 */
	private static Integer compareForNull(Entry entry1, Entry entry2) {
		boolean entry1IsNull = entry1 == null || entry1.getName() == null;
		boolean entry2IsNull = entry2 == null || entry2.getName() == null;
		if (entry1IsNull) {
			return entry2IsNull ? 0 : -1;
		}
		if (entry2IsNull) {
			return 1;
		}
		return null;
	}

	/**
	 * Compares names of resource repositories. Uses {@link RepositoryManager#SPECIAL_RESOURCE_REPOSITORY_NAMES} as
	 * ordering, orders everything else after that.
	 *
	 * @since 9.0
	 */
	private static int compareResourceRepositoryNames(String name1, String name2) {
		if (name1.equals(name2)) {
			return 0;
		}
		int index1 = RepositoryManager.SPECIAL_RESOURCE_REPOSITORY_NAMES.indexOf(name1);
		int index2 = RepositoryManager.SPECIAL_RESOURCE_REPOSITORY_NAMES.indexOf(name2);
		if (index1 == -1) {
			return index2 == -1 ? 0 : 1;
		}
		return index2 == -1 ? -1 : index1 - index2;
	}

	/**
	 * Checks if the folder is a special connections folder or inside one.
	 *
	 * @param folder
	 * 		the folder to check
	 * @return whether the folder is or is in a special folder
	 */
	public static boolean isInSpecialConnectionsFolder(Folder folder) {
		// no parent -> repository
		if (folder.getContainingFolder() == null) {
			return false;
		}
		// find super-folder with its super-folder the repository
		Folder nextFolder = folder;
		while (nextFolder.getContainingFolder().getContainingFolder() != null) {
			nextFolder = nextFolder.getContainingFolder();
		}
		// check for the special name
		return nextFolder.isSpecialConnectionsFolder();
	}

	/**
	 * Returns the special connections folder for the repository or {@code null}.
	 *
	 * @param repository
	 * 		the repository for which to retrieve the Connections folder
	 * @return the connections folder or {@code null}
	 * @throws RepositoryException
	 * 		if a repository exception happens while inspecting the subfolders
	 */
	public static Folder getConnectionFolder(Repository repository) throws RepositoryException {
		return repository.getSubfolders().stream()
				.filter(Folder::isSpecialConnectionsFolder).findAny().orElse(null);
	}

	/**
	 * Returns all connections defined for the repository.
	 *
	 * @param repository
	 * 		the repository for which to retrieve the connections
	 * @return all {@link ConnectionEntry}s in this repository
	 * @throws RepositoryException
	 * 		if accessing the connections folder fails
	 */
	public static List<ConnectionEntry> getConnections(Repository repository) throws RepositoryException {
		Folder connectionFolder = getConnectionFolder(repository);
		if (connectionFolder != null) {
			return connectionFolder.getDataEntries().stream()
					.filter(e -> ConnectionEntry.TYPE_NAME.equals(e.getType()))
					.map(e -> (ConnectionEntry) e)
					.collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Initialize the {@link ParameterType} for the {@value #PARAMETER_CUSTOM_FILE_BROWSER} setting. Does nothing on Windows because that's too dangerous and not needed there.
	 *
	 * @since 9.7
	 */
	public static void initFileBrowser() {
		if (SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS) {
			return;
		}

		ParameterType fileBrowser = new ParameterTypeFile(PARAMETER_CUSTOM_FILE_BROWSER, "", null, true);
		RapidMiner.registerParameter(fileBrowser, "system");
	}

	/**
	 * Returns the custom file browser entered by the user in the preferences.
	 *
	 * @return the path to the custom file browser, or {@code null}
	 * @since 9.7
	 */
	public static String getCustomFileBrowser() {
		return StringUtils.trimToNull(ParameterService.getParameterValue(PARAMETER_CUSTOM_FILE_BROWSER));
	}

	/**
	 * Initialize the {@link ParameterType} for the {@value #PARAMETER_CUSTOM_OPEN_FILE_COMMANDS} setting.
	 *
	 * @since 9.7
	 */
	public static void initOpenFileInOperatorSystem() {
		ParameterType fileBinding = new ParameterTypeList(PARAMETER_CUSTOM_OPEN_FILE_COMMANDS, "",
				new ParameterTypeString("file_suffix", ""),
				new ParameterTypeFile("open_with", "", null, false));
		RapidMiner.registerParameter(fileBinding, "system");
	}

	/**
	 * Returns the custom open command entered by the user in the preferences for a given file suffix. Has precedence
	 * over the {@link com.rapidminer.repository.gui.OpenBinaryEntryActionRegistry}.
	 *
	 * @param suffix the suffix (including the leading '.'), must not be {@code null}
	 * @return the command or {@code null} if none has been entered for the given suffix
	 * @since 9.7
	 */
	public static String getOpenCommandForSuffix(String suffix) {
		if (SUFFIX_SEPARATOR.equals(suffix.trim())) {
			return null;
		}
		String fileBindings = ParameterService.getParameterValue(PARAMETER_CUSTOM_OPEN_FILE_COMMANDS);
		if (fileBindings == null || StringUtils.trimToNull(suffix) == null) {
			return null;
		}
		if (!suffix.startsWith(SUFFIX_SEPARATOR)) {
			suffix = SUFFIX_SEPARATOR + suffix;
		}

		for (String[] entry : ParameterTypeList.transformString2List(fileBindings)) {
			String registeredSuffix = entry[0];
			// we need to account for users registering the suffix without the leading dot anyway
			if (suffix.equals(registeredSuffix) || suffix.substring(1).equals(registeredSuffix)) {
				return StringUtils.trimToNull(entry[1]);
			}
		}

		return null;
	}

	/**
	 * Get the suffix from a filename, will return "bar" for "foo.bar" and "gitignore" for ".gitignore".
	 *
	 * @param name the filename, not the path, to get the suffix from, must not be null or empty
	 * @return the suffix of the file, can be empty if no suffix exists, never {@code null}. Always lower case.
	 * @since 9.7
	 */
	public static String getSuffixFromFilename(String name) {
		if (StringUtils.stripToNull(name) == null) {
			throw new IllegalArgumentException("filename must not be null or empty!");
		}

		return FilenameUtils.getExtension(StringUtils.stripToNull(name)).toLowerCase(Locale.ENGLISH);
	}


	/**
	 * Tries to remove a file suffix from the given filename. The name can contain path separators. If no dot is found,
	 * nothing happens. Likewise, if the dot is the last character, nothing happens.
	 *
	 * @param filename the file name
	 * @return the filename without the suffix or the original filename
	 * @since 9.7
	 */
	public static String removeSuffix(String filename) {
		if (filename == null) {
			return null;
		}

		int lastDotIndex = filename.lastIndexOf('.');
		if (lastDotIndex > 0 && filename.length() > lastDotIndex + 1) {
			filename = filename.substring(0, lastDotIndex);
		}

		return filename;
	}

	/**
	 * Returns the full path of a location, including suffix, if possible and necessary. The suffix depends on the given
	 * data entry (sub-)type. Results could be like "//Repo/folder/test.ioo" (data) or "//Repo/folder/test.rmhdf5table"
	 * (example set in new filesystem repo= or "//Repo/folder/test.rmpf" (process).
	 *
	 * @param location the location to append the suffix to, must not be {@code null}
	 * @return the path with the suffix, never {@code null}
	 * @since 9.7
	 */
	public static String getPathWithSuffix(RepositoryLocation location) {
		return ValidationUtil.requireNonNull(location, "location").getPath() + findSuffix(location.getLocationType(), location.getExpectedDataEntryType());
	}

	/**
	 * Finds the suffix for the given {@link RepositoryLocationType} and {@link DataEntry} type.
	 * Will return an empty string for folders, unknown and others, as well as for binary data entries
	 * or if the entry type is not specified for a data entry.
	 * <p>
	 * For data entries, returns the corresponding suffix, for {@link IOObjectEntry} types, will find
	 * the actual suffix with {@link #findIOOSuffix(Class)}.
	 *
	 * @param locationType	the location type to find the suffix for; must not be {@code null}
	 * @param dataEntryType	the entry type to find the suffix for; may be {@code null}
	 * @return the suffix foir the given parameters; may be empty, but never {@code null}
	 * @since 9.7
	 */
	@SuppressWarnings({"unchecked", "deprecation"})
	private static String findSuffix(RepositoryLocationType locationType, Class<? extends DataEntry> dataEntryType) {
		switch (locationType) {
			case DATA_ENTRY:
				if (dataEntryType == null || dataEntryType == DataEntry.class) {
					return "";
				}
				if (ProcessEntry.class.isAssignableFrom(dataEntryType)) {
					return ProcessEntry.RMP_SUFFIX;
				}
				if (BlobEntry.class.isAssignableFrom(dataEntryType)) {
					return BlobEntry.BLOB_SUFFIX;
				}
				if (ConnectionEntry.class.isAssignableFrom(dataEntryType)) {
					return ConnectionEntry.CON_SUFFIX;
				}
				if (IOObjectEntry.class.isAssignableFrom(dataEntryType)) {
					return findIOOSuffix((Class<? extends IOObjectEntry>) dataEntryType);
				}
				return "";
			case FOLDER:
			case UNKNOWN:
			default:
				return "";
		}
	}

	/**
	 * Finds the suffix for the given IOO entry type if possible. Will return a suffix including a leading dot.
	 * If no specific suffix can be found, the default suffix {@value IOObjectEntry#IOO_SUFFIX} is returned.
	 *
	 * @param dataEntryType	the entry type to find a suffix for
	 * @return the specific suffix for the given entry type or the default; never {@code null}
	 * @since 9.7
	 * @see IOObjectEntryTypeRegistry
	 * @see IOObjectSuffixRegistry
	 */
	private static String findIOOSuffix(Class<? extends IOObjectEntry> dataEntryType) {
		Class<? extends IOObject> iooClass = IOObjectEntryTypeRegistry.getIOObjectClassForEntryClass(dataEntryType);
		String suffix = IOObjectSuffixRegistry.getSuffix(iooClass);
		return StringUtils.isBlank(suffix) ? IOObjectEntry.IOO_SUFFIX : '.' + suffix;
	}
}

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
package com.rapidminer.repository;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.external.alphanum.AlphanumComparator.AlphanumCaseSensitivity;
import com.rapidminer.repository.RepositoryManager.RepositoryType;


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
		int compareValue = repositoryType.compareTo(RepositoryType.getRepositoryType(repository2));
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
}

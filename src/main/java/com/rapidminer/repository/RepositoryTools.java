/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.util.Comparator;

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
	public static final Comparator<Entry> SIMPLE_NAME_COMPARATOR = new Comparator<Entry>() {

		@Override
		public int compare(Entry entry1, Entry entry2) {
			if ((entry1 == null || entry1.getName() == null) && (entry2 == null || entry2.getName() == null)) {
				return 0;
			} else if (entry1 == null || entry1.getName() == null) {
				return -1;
			} else if (entry2 == null || entry2.getName() == null) {
				return 1;
			} else {
				return entry1.getName().compareTo(entry2.getName());
			}
		}

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
	public static final Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>() {

		@Override
		public int compare(Entry entry1, Entry entry2) {
			if ((entry1 == null || entry1.getName() == null) && (entry2 == null || entry2.getName() == null)) {
				return 0;
			} else if (entry1 == null || entry1.getName() == null) {
				return -1;
			} else if (entry2 == null || entry2.getName() == null) {
				return 1;
			} else {
				return ALPHANUMERIC_COMPARATOR.compare(entry1.getName(), entry2.getName());
			}
		}

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
	public static final Comparator<Entry> ENTRY_COMPARATOR_LAST_MODIFIED = new Comparator<Entry>() {

		@Override
		public int compare(Entry entry1, Entry entry2) {
			if (!(entry1 instanceof DateEntry) && !(entry2 instanceof DateEntry)) {
				return ALPHANUMERIC_COMPARATOR.compare(entry1.getName(), entry2.getName());
			} else if (!(entry1 instanceof DateEntry)) {
				return -1;
			} else if (!(entry2 instanceof DateEntry)) {
				return 1;
			}
			DateEntry dataEntry1 = (DateEntry) entry1;
			DateEntry dataEntry2 = (DateEntry) entry2;
			int compareValue = Long.compare(dataEntry2.getDate(), dataEntry1.getDate());
			if (compareValue == 0) { // same date
				return ALPHANUMERIC_COMPARATOR.compare(dataEntry1.getName(), dataEntry2.getName());
			} else {
				return compareValue;
			}
		}

	};

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
	public static final Comparator<Repository> REPOSITORY_COMPARATOR = new Comparator<Repository>() {

		@Override
		public int compare(Repository repository1, Repository repository2) {
			if ((repository1 == null || repository1.getName() == null)
					&& (repository2 == null || repository2.getName() == null)) {
				return 0;
			} else if (repository1 == null || repository1.getName() == null) {
				return -1;
			} else if (repository2 == null || repository2.getName() == null) {
				return 1;
			}
			int compareValue = RepositoryType.getRepositoryType(repository1)
					.compareTo(RepositoryType.getRepositoryType(repository2));
			if (compareValue == 0) { // same repository type
				return ALPHANUMERIC_COMPARATOR.compare(repository1.getName(), repository2.getName());
			} else {
				return compareValue;
			}
		}
	};
}

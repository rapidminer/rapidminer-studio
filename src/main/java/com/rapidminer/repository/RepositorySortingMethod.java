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

import java.util.Comparator;

import com.rapidminer.repository.gui.RepositoryBrowser;


/**
 * These are the sorting methods, which are supported by the {@link RepositoryBrowser}.
 *
 * @author Marcel Seifert
 * @since 7.4
 *
 */
public enum RepositorySortingMethod implements Comparator<Entry> {
	NAME_ASC(RepositoryTools.ENTRY_COMPARATOR) {
	}, LAST_MODIFIED_DATE_DESC(RepositoryTools.ENTRY_COMPARATOR_LAST_MODIFIED);

	private final Comparator<Entry> comparator;

	RepositorySortingMethod(Comparator<Entry> comparator){
		this.comparator = comparator;
	}

	public int compare(Entry o1, Entry o2) {
		return comparator.compare(o1, o2);
	}
}

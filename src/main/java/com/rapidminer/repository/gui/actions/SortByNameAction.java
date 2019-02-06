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

package com.rapidminer.repository.gui.actions;

import com.rapidminer.repository.RepositorySortingMethod;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * Action for sorting by names in the repository browser.
 *
 * @author Marcel Seifert
 * @since 7.4
 *
 */
public class SortByNameAction extends SortByAction {

	private static final long serialVersionUID = 1L;

	private static final RepositorySortingMethod REPOSITORY_SORTING_METHOD = RepositorySortingMethod.NAME_ASC;

	private static final String I18N = "repository_sort_alphanumeric";

	public SortByNameAction(RepositoryTree tree) {
		super(I18N, tree, REPOSITORY_SORTING_METHOD);
	}

}

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

import java.util.EventListener;

import com.rapidminer.repository.gui.RepositoryTree;
import com.rapidminer.repository.gui.actions.SortByAction;


/**
 * A listener listening to changes of the {@link RepositorySortingMethod} of the
 * {@link RepositoryTree}
 *
 * @author Marcel Seifert
 * @since 7.4
 */
public interface RepositorySortingMethodListener extends EventListener {

	/**
	 * This should be called when the {@link RepositorySortingMethod} was changed and can be
	 * implemented e.g. by {@link SortByAction}s to indicate which {@link RepositorySortingMethod}
	 * is currently selected.
	 *
	 * @param method
	 *            The {@link RepositorySortingMethod}
	 */
	public void changedRepositorySortingMethod(RepositorySortingMethod method);

}

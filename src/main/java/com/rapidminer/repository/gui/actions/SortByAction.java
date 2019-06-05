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

import java.awt.event.ActionEvent;

import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.repository.RepositorySortingMethod;
import com.rapidminer.repository.RepositorySortingMethodListener;
import com.rapidminer.repository.gui.RepositoryTree;


/**
 * Abstract action for sorting by a {@link RepositorySortingMethod} in the repository browser.
 *
 * @author Marcel Seifert
 * @since 7.4
 *
 */
public abstract class SortByAction extends ToggleAction {

	private static final long serialVersionUID = 1L;

	private final RepositoryTree tree;
	private final RepositorySortingMethod method;

	/**
	 * Creates a new SortByAction
	 *
	 * @param i18n the i18n key
	 * @param tree the repository tree on which the sorting method is applied
	 * @param method the sorting method represented by this action
	 */
	public SortByAction(String i18n, RepositoryTree tree, RepositorySortingMethod method) {
		super(true, i18n);
		this.tree = tree;
		this.method = method;
		tree.addRepostorySortingMethodListener(this::updateSelectedStatus);
		updateSelectedStatus(tree.getSortingMethod());
	}

	/**
	 * Updates the selected status
	 *
	 * @param selectedMethod the currently selected method
	 */
	private void updateSelectedStatus(RepositorySortingMethod selectedMethod){
		setSelected(selectedMethod == method);
	}

	@Override
	public void actionToggled(ActionEvent e) {
		tree.setSortingMethod(method);
	}

}

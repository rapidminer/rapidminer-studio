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
package com.rapidminer.gui.actions.search;

import com.rapidminer.search.GlobalSearchIndexer;
import com.rapidminer.search.GlobalSearchManager;
import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchable;


/**
 * Responsible for implementing Global Search functionality for actions that should be found with it. See {@link GlobalSearchRegistry}
 * for more information.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class ActionsGlobalSearch implements GlobalSearchable {

	public static final String CATEGORY_ID = "actions";

	private final ActionsGlobalSearchManager manager;


	public ActionsGlobalSearch() {
		manager = new ActionsGlobalSearchManager();

		if (GlobalSearchIndexer.INSTANCE.isInitialized()) {
			GlobalSearchRegistry.INSTANCE.registerSearchCategory(this);
		}
	}

	@Override
	public GlobalSearchManager getSearchManager() {
		return manager;
	}
}

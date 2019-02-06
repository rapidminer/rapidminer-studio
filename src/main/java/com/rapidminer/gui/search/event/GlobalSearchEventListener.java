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
package com.rapidminer.gui.search.event;

import java.util.EventListener;

import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchResult;


/**
 * An object listening for {@link GlobalSearchModelEvent}s and {@link GlobalSearchCategoryEvent}s on the {@link com.rapidminer.gui.search.model.GlobalSearchModel}.
 *
 * @author Marco Boeck
 * @since 8.1
 *
 */
public interface GlobalSearchEventListener extends EventListener {

	/**
	 * Called when something globally in the model has changed.
	 *
	 * @param e
	 *            the event instance
	 */
	void modelChanged(final GlobalSearchModelEvent e);

	/**
	 * Called when a result for a search category in the model has changed.
	 *
	 * @param categoryId
	 * 		the id of the search category that has changed. See {@link GlobalSearchRegistry} for details
	 * @param e
	 * 		the event instance
	 * 	@param result the search result that caused the change, or {code null} if it was a pending state change
	 */
	void categoryChanged(final String categoryId, final GlobalSearchCategoryEvent e, final GlobalSearchResult result);

}

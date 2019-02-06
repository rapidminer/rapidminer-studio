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
package com.rapidminer.search.event;

import java.util.EventListener;

import com.rapidminer.search.GlobalSearchRegistry;
import com.rapidminer.search.GlobalSearchable;
import com.rapidminer.search.GlobalSearchCategory;


/**
 * An object listening for {@link GlobalSearchRegistryEvent}s on the {@link GlobalSearchRegistry}.
 *
 * @author Marco Boeck
 * @since 8.1
 *
 */
public interface GlobalSearchRegistryEventListener extends EventListener {

	/**
	 * Called when a new {@link GlobalSearchable} has been registered to the {@link
	 * GlobalSearchRegistry}.
	 *
	 * @param e
	 * 		the event instance, never {@code null}
	 * @param category
	 * 		the search category that triggered the event, never {@code null}
	 */
	void searchCategoryRegistrationChanged(final GlobalSearchRegistryEvent e, final GlobalSearchCategory category);

}

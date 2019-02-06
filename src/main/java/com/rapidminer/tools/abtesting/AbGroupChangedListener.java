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
package com.rapidminer.tools.abtesting;

/**
 * A listener that gets informed if the selected A/B Group has changed
 *
 * <p>
 *     Register with {@link AbGroupProvider#registerAbGroupChangedListener}
 * </p>
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
public interface AbGroupChangedListener{

	/**
	 * Notifies that the group might have changed
	 *
	 * @param newGroup the new group [{@code 0}, {@code numberOfGroups})
	 * @param oldGroup the previous group [{@code 0}, {@code numberOfGroups})
	 * @param numberOfGroups the requested group bounds
	 */
	void groupChanged(int newGroup, int oldGroup, int numberOfGroups);
}

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
package com.rapidminer.search;

/**
 * A search category pojo. Contains only the manager instance, category id and visibility information.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public class GlobalSearchCategory {

	private final String categoryId;

	private final GlobalSearchManager manager;

	private final boolean visible;

	/**
	 * Use {@link #GlobalSearchCategory(GlobalSearchManager)} instead
	 *
	 * @param categoryId
	 * 		unused
	 * @param manager
	 * 		the global search manager
	 * @deprecated since 9.3
	 */
	@Deprecated
	GlobalSearchCategory(String categoryId, GlobalSearchManager manager) {
		this(manager);
	}

	/**
	 * Creates a new GlobalSearchCategory that is visible in the UI
	 *
	 * @param manager
	 * 		the global search manager
	 * @since 9.3
	 */
	public GlobalSearchCategory(GlobalSearchManager manager) {
		this(manager, true);
	}

	/**
	 * Creates a new GlobalSearchCategory
	 *
	 * @param manager
	 * 		the global search manager
	 * @param showInUI
	 * 		if the category should be shown in global search UI
	 * @since 9.3
	 */
	public GlobalSearchCategory(GlobalSearchManager manager, boolean showInUI) {
		this.categoryId = manager.getSearchCategoryId();
		this.manager = manager;
		this.visible = showInUI;
	}

	/**
	 * The manager instance.
	 *
	 * @return the instance, never {@code null}
	 */
	public GlobalSearchManager getManager() {
		return manager;
	}

	/**
	 * The unique id of the category.
	 *
	 * @return the id, never {@code null}
	 */
	public String getCategoryId() {
		return categoryId;
	}

	/**
	 * Indicates if the given Search category is visible
	 *
	 * @return {@code true} if the category should be shown in the global search ui
	 * @since 9.3
	 */
	public boolean isVisible() {
		return visible;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GlobalSearchCategory that = (GlobalSearchCategory) o;

		return categoryId.equals(that.categoryId);
	}

	@Override
	public int hashCode() {
		return categoryId.hashCode();
	}

}

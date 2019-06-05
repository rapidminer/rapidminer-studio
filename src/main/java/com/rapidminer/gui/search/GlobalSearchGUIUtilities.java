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
package com.rapidminer.gui.search;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jsoup.Jsoup;

import com.rapidminer.search.GlobalSearchCategory;
import com.rapidminer.search.GlobalSearchRegistry;


/**
 * Utility class for using Global Search GUI features.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public enum GlobalSearchGUIUtilities {

	INSTANCE;


	/** the max height of a GUI component that the Global Search GUI will accept */
	public static final int MAX_HEIGHT = 50;

	public static final String HTML_TAG_OPEN = "<html>";
	public static final String HTML_TAG_CLOSE = "</html>";

	private static final String CATEGORY_ID_ACTIONS = "actions";
	private static final String CATEGORY_ID_OPERATOR = "operator";
	private static final String CATEGORY_ID_REPOSITORY = "repository";
	private static final String CATEGORY_ID_MARKETPLACE = "marketplace";
	private static final List<String> PREDEFINED_CATEGORY_ORDER;

	static {
		// we need this to ensure our own searchables have a well-defined order in the UI
		PREDEFINED_CATEGORY_ORDER = new ArrayList<>(4);
		PREDEFINED_CATEGORY_ORDER.add(CATEGORY_ID_ACTIONS);
		PREDEFINED_CATEGORY_ORDER.add(CATEGORY_ID_OPERATOR);
		PREDEFINED_CATEGORY_ORDER.add(CATEGORY_ID_MARKETPLACE);
		PREDEFINED_CATEGORY_ORDER.add(CATEGORY_ID_REPOSITORY);
	}

	/**
	 * Adds highlights to strings for search result hits.
	 *
	 * @param text
	 * 		the original text where highlights should be added
	 * @param bestFragments
	 * 		the best fragments as provided by {@link GlobalSearchableGUIProvider#getGUIListComponentForDocument}. If {@code null}, only HTML tags are added.
	 * @return the HTML-styled string
	 */
	public String createHTMLHighlightFromString(final String text, final String[] bestFragments) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("text must not be null or empty!");
		}

		StringBuilder sb = new StringBuilder();
		sb.append(HTML_TAG_OPEN);
		if (bestFragments != null && bestFragments.length > 0) {
			for (String bestFragment : bestFragments) {
				if (bestFragment == null) {
					continue;
				}
				// best fragment may only be a part of the original content. So replace original substring match with highlighted fragment
				sb.append(text.replace(createTextFromHTML(bestFragment), bestFragment));
			}
		} else {
			sb.append(text);
		}
		sb.append(HTML_TAG_CLOSE);

		return sb.toString();
	}

	/**
	 * Converts HTML to plain text. Does not care if HTML is well formatted at all.
	 *
	 * @param html
	 * 		the html to convert to plain text
	 * @return the plain text, never {@code null}
	 */
	public String createTextFromHTML(final String html) {
		if (html == null) {
			throw new IllegalArgumentException("html must not be null!");
		}
		if (html.trim().isEmpty()) {
			return html;
		}

		return Jsoup.parse(html).text();
	}

	/**
	 * Sorts the search categories in a sort of predefined order - first our own categories, then user provided ones.
	 *
	 * @param categories
	 * 		the input categories list
	 * @return the sorted list
	 */
	public List<GlobalSearchCategory> sortCategories(final List<GlobalSearchCategory> categories) {
		if (categories == null) {
			throw new IllegalArgumentException("categories must not be null!");
		}

		List<GlobalSearchCategory> clone = new ArrayList<>(categories.size());
		for (String predefinedCategory : PREDEFINED_CATEGORY_ORDER) {
			GlobalSearchCategory cat = GlobalSearchRegistry.INSTANCE.getSearchCategoryById(predefinedCategory);
			if (cat != null) {
				clone.add(cat);
				// remove predefined cats from given list
				categories.remove(cat);
			}
		}

		// sort remaining categories by id
		if (!categories.isEmpty()) {
			categories.sort(Comparator.comparing(GlobalSearchCategory::getCategoryId));
		}

		// now add them as well
		clone.addAll(categories);

		return clone;
	}
}

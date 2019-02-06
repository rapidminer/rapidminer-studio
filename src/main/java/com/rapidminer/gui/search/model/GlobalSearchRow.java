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
package com.rapidminer.gui.search.model;

import org.apache.lucene.document.Document;

import com.rapidminer.search.GlobalSearchUtilities;


/**
 * POJO for each row in the search results.
 *
 * @author Marco Boeck
 * @since 8.1
 */
public final class GlobalSearchRow {

	private final Document doc;
	private final String[] bestFragments;

	public GlobalSearchRow(final Document doc, final String[] bestFragments) {
		this.doc = doc;
		this.bestFragments = bestFragments;
	}

	/**
	 * The document for this row.
	 *
	 * @return the document, never {@code null}
	 */
	public Document getDoc() {
		return doc;
	}

	/**
	 * The best matching fragments of the name field for this row. Can be {@code null}.
	 *
	 * @return the best fragment of the {@link GlobalSearchUtilities#FIELD_NAME} or {@code null}
	 */
	public String[] getBestFragments() {
		return bestFragments;
	}
}

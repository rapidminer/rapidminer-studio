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
package com.rapidminer.gui.tools;

import com.rapidminer.parameter.TextType;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.LanguageAwareCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


/**
 * A generic SQL editor.
 * 
 * @author Ingo Mierswa
 */
public class SQLEditor extends RSyntaxTextArea {

	private static final long serialVersionUID = 6062150929521199578L;

	private String[] BASIC_SQL_DIALECT = new String[] {

			// basics select expressions
			"SELECT", "SELECT DISTINCT", "WHERE", "FROM", "AND", "OR", "ORDER BY", "GROUP BY", "AS", "NULL", "DISTINCT",

			// other expressions
			"UPDATE", "DELETE", "INSERT INTO",

			// management
			"CREATE DATABASE", "ALTER DATABASE", "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX", "DROP INDEX",
			"CREATE VIEW", "DROP VIEW",

			// JOINs
			"INNER JOIN", "LEFT JOIN", "RIGHT JOIN", "FULL JOIN", "UNION", "LEFT OUTER JOIN", "RIGHT OUTER JOIN", "ON",

			// aggregations
			"COUNT (", "SUM (", "AVG (", "MAX (", "MIN (",

			// condition expressions
			"IN (", "NOT IN (", "EXISTS (", "NOT EXISTS (", "ALL (", "ANY (", "SOME (", "HAVING", "IS NULL", "IS NOT NULL"

	};

	public SQLEditor() {
		super(new RSyntaxDocument(TextType.SQL.getSyntaxIdentifier()));
		setAnimateBracketMatching(true);
		setAutoIndentEnabled(true);
		setAutoscrolls(true);

		// A CompletionProvider is what knows of all possible completions, and
		// analyzes the contents of the text area at the caret position to
		// determine what completion choices should be presented. Most instances
		// of CompletionProvider (such as DefaultCompletionProvider) are designed
		// so that they can be shared among multiple text components.
		CompletionProvider provider = new LanguageAwareCompletionProvider(new DefaultCompletionProvider(BASIC_SQL_DIALECT));

		// An AutoCompletion acts as a "middle-man" between a text component
		// and a CompletionProvider. It manages any options associated with
		// the auto-completion (the popup trigger key, whether to display a
		// documentation window along with completion choices, etc.). Unlike
		// CompletionProviders, instances of AutoCompletion cannot be shared
		// among multiple text components.
		AutoCompletion ac = new AutoCompletion(provider);
		ac.install(this);

		requestFocusInWindow();
	}
}

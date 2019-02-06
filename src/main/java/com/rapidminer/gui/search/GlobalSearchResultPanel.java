/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.search;

import java.awt.BorderLayout;
import javax.swing.Action;
import javax.swing.JPanel;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.search.model.GlobalSearchRow;


/**
 * This panel contains one result of the global search and can get an action to be executed if the result gets activated.
 *
 * @author Andreas Timm
 * @since 9.0.0
 */
public class GlobalSearchResultPanel extends JPanel {

	/**
	 * Remember the row of this result.
	 */
	private GlobalSearchRow row;

	/**
	 * Potential {@link Action} that could be activated via doActivate().
	 */
	private Action activationAction;

	public GlobalSearchResultPanel(GlobalSearchRow row) {
		this.row = row;
		setOpaque(false);
		setBackground(Colors.TEXT_HIGHLIGHT_BACKGROUND);
		setLayout(new BorderLayout());
	}

	/**
	 * Show this entry as highlighted.
	 *
	 * @param b
	 * 		if true do hightlight, else remove highlight.
	 */
	public void highlightPanel(boolean b) {
		setOpaque(b);
		repaint();
	}

	/**
	 * Activate the {@link Action} if there is any
	 */
	public void doActivate() {
		if (activationAction != null) {
			activationAction.actionPerformed(null);
		}
	}

	/**
	 * Change the {@link Action} to be activated on doActivate().
	 *
	 * @param activationAction
	 * 		Some action that should be activated on doActivate().
	 */
	public void setActivationAction(Action activationAction) {
		this.activationAction = activationAction;
	}

	/**
	 * Return the row that this result was made for. Can be null.
	 *
	 * @return the row that this result was made for. Can be null.
	 */
	public GlobalSearchRow getRow() {
		return row;
	}
}

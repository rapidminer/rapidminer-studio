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
package com.rapidminer.operator.ports.quickfix;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JOptionPane;


/**
 * @author Simon Fischer
 */
public abstract class AbstractQuickFix implements QuickFix {

	private Action action;
	private int rating;
	private boolean isInteractive;

	/**
	 * @param i18nKey
	 *            is a key referencing an entry in a properties file that defines the action's text
	 *            and icon.
	 * @param i18nArgs
	 *            arguments to pass to the text formatter
	 */
	public AbstractQuickFix(int rating, boolean isInteractive, String i18nKey, Object... i18nArgs) {
		this.isInteractive = isInteractive;
		this.rating = rating;
		seti18nKey(i18nKey, i18nArgs);
	}

	protected void seti18nKey(String i18nKey, Object... i18nArgs) {
		this.action = new LoggedAbstractAction(I18N.getMessage(I18N.getErrorBundle(), "metadata.quickfix." + i18nKey, i18nArgs)) {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent arg0) {
				try {
					apply();
				} catch (Exception e) {
					// TODO: Handle exception by GUI properly
					JOptionPane.showMessageDialog(null, e.toString(), "Cannot apply quick fix", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		};
		String iconName = I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.quickfix." + i18nKey + ".icon");
		if (iconName != null) {
			this.action.putValue(Action.SMALL_ICON, SwingTools.createIcon("16/" + iconName));
		}
	}

	@Override
	public Action getAction() {
		return action;
	}

	@Override
	public int getRating() {
		return rating;
	}

	@Override
	public boolean isInteractive() {
		return isInteractive;
	}

	@Override
	public int compareTo(QuickFix arg0) {
		return arg0.getRating() - this.rating;
	}

	@Override
	public String toString() {
		return (String) action.getValue(Action.NAME);
	}
}

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
package com.rapidminer.gui.look;

import java.awt.event.ActionEvent;

import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;


/**
 * The basic actions of the look and feel components for text components.
 * 
 * @author Ingo Mierswa
 */
public final class TextActions {

	public static final String CLEAR_ALL_ACTION = "Clear All";

	public static final String SELECT_ALL_ACTION = "Select All";

	public static final String DELETE_TEXT_ACTION = "Delete";

	public static class SelectAllAction extends TextAction {

		private static final long serialVersionUID = 7009424030828006069L;

		public SelectAllAction() {
			super(SELECT_ALL_ACTION);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			if (target != null) {
				Document doc = target.getDocument();
				target.setCaretPosition(0);
				target.moveCaretPosition(doc.getLength());
			}
		}
	}

	public static class DeleteTextAction extends TextAction {

		private static final long serialVersionUID = 6412033584635198994L;

		public DeleteTextAction() {
			super(DELETE_TEXT_ACTION);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			boolean beep = true;
			if ((target != null) && (target.isEditable())) {
				try {
					Document doc = target.getDocument();
					int ss = target.getSelectionStart();
					int se = target.getSelectionEnd();

					if (ss != se) {
						doc.remove(ss, se - ss);
						beep = false;
					}
				} catch (BadLocationException bl) {
				}
			}
			if (beep) {
				UIManager.getLookAndFeel().provideErrorFeedback(target);
			}
		}
	}

	public static class ClearAction extends TextAction {

		private static final long serialVersionUID = -8717559424063560794L;

		public ClearAction() {
			super(CLEAR_ALL_ACTION);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JTextComponent target = getTextComponent(e);
			boolean beep = true;
			if ((target != null) && (target.isEditable())) {
				try {
					Document doc = target.getDocument();
					int ss = 0;
					int se = doc.getLength();

					if (ss != se) {
						doc.remove(ss, se - ss);
						beep = false;
					}
				} catch (BadLocationException bl) {
				}
			}
			if (beep) {
				UIManager.getLookAndFeel().provideErrorFeedback(target);
			}
		}
	}
}

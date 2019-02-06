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
package com.rapidminer.gui;

import com.rapidminer.InputHandler;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;


/**
 * An input handler using a JOptionPane.
 * 
 * @author Simon Fischer, Ingo Mierswa
 */
public class GUIInputHandler implements InputHandler {

	@Override
	public String inputPassword(String messageText) {
		final JPasswordField passwordField = new JPasswordField();
		JOptionPane jop = new JOptionPane(new Object[] { messageText, passwordField }, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION);
		JDialog dialog = jop.createDialog("Authentication required");
		dialog.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentShown(ComponentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						passwordField.requestFocusInWindow();
						passwordField.requestFocus();
					}
				});
			}
		});
		dialog.setVisible(true);
		int result = (Integer) jop.getValue();
		if (result == JOptionPane.OK_OPTION) {
			return new String(passwordField.getPassword());
		} else {
			return null;
		}
	}
}

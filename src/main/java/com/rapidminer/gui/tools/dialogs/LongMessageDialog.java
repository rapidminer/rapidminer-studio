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
package com.rapidminer.gui.tools.dialogs;

import java.awt.Window;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.ExtendedJScrollPane;


/**
 *
 * @author Tobias Malbrecht
 */
public class LongMessageDialog extends ButtonDialog {

	private static final long serialVersionUID = -2647548699663292273L;

	/**
	 * Creates a dialog to display a long message.
	 *
	 * @param i18nKey
	 *            the i18n key
	 * @param message
	 *            the actual message
	 * @param i18nArgs
	 *            additional i18n arguments
	 * @deprecated use {@link #LongMessageDialog(Window, String, String, Object...)} instead
	 */
	@Deprecated
	public LongMessageDialog(String i18nKey, String message, Object... i18nArgs) {
		this(ApplicationFrame.getApplicationFrame(), i18nKey, message, i18nArgs);
	}

	/**
	 * Creates a dialog to display a long message.
	 *
	 * @param owner
	 *            the owner window in which the dialog is displayed
	 * @param i18nKey
	 *            the i18n key
	 * @param message
	 *            the actual message
	 * @param i18nArgs
	 *            additional i18n arguments
	 * @since 6.5.0
	 */
	public LongMessageDialog(Window owner, String i18nKey, String message, Object... i18nArgs) {
		super(owner, i18nKey, ModalityType.APPLICATION_MODAL, i18nArgs);
		JEditorPane textComponent = new ExtendedHTMLJEditorPane("text/html", message);
		// so it does not steel ENTER from the default button
		textComponent.setFocusable(false);
		StyleSheet css = ((HTMLEditorKit) textComponent.getEditorKit()).getStyleSheet();
		css.addRule("body { margin : 0; font-family : sans-serif; font-size : 9px; font-style : normal; }");
		css.addRule(".error { font-weight:bold; color:red; font-style:plain; }");
		css.addRule("code { font-weight:bold; color:#000088; }");
		textComponent.setEditable(false);
		JScrollPane scrollPane = new ExtendedJScrollPane(textComponent);
		scrollPane.setBorder(createBorder());
		layoutDefault(scrollPane, NORMAL, makeCloseButton());
	}
}

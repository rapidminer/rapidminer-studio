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

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

import com.rapidminer.gui.tools.ResourceActionTransmitter;


/**
 * The popup menu for all text components.
 *
 * @author Ingo Mierswa, Tobias Malbrecht
 */
public class ClipboardActionsPopup extends JPopupMenu {

	private static final long serialVersionUID = -6304527692064490218L;

	private JTextComponent parent;

	private JMenuItem cutMenuItem;
	private JMenuItem copyMenuItem;
	private JMenuItem pasteMenuItem;
	private JMenuItem deleteMenuItem;
	// private JMenuItem clearMenuItem;
	private JMenuItem selectAllMenuItem;

	public ClipboardActionsPopup(JTextComponent parent) {
		this.parent = parent;

		this.cutMenuItem = new JMenuItem(new ResourceActionTransmitter("editor.cut",
				new javax.swing.text.DefaultEditorKit.CutAction()));
		this.copyMenuItem = new JMenuItem(new ResourceActionTransmitter("editor.copy",
				new javax.swing.text.DefaultEditorKit.CopyAction()));
		this.pasteMenuItem = new JMenuItem(new ResourceActionTransmitter("editor.paste",
				new javax.swing.text.DefaultEditorKit.PasteAction()));
		this.deleteMenuItem = new JMenuItem(new ResourceActionTransmitter("editor.delete",
				new TextActions.DeleteTextAction()));

		// this.clearMenuItem = new JMenuItem(new TextActions.ClearAction());

		this.selectAllMenuItem = new JMenuItem(new ResourceActionTransmitter("editor.select_all",
				new TextActions.SelectAllAction()));

		this.add(this.cutMenuItem);
		this.add(this.copyMenuItem);
		this.add(this.pasteMenuItem);
		this.add(this.deleteMenuItem);
		this.addSeparator();
		// this.add(this.clearMenuItem);
		this.add(this.selectAllMenuItem);

		// this.setPreferredSize(new Dimension((int) this.getPreferredSize().getWidth() + 30, (int)
		// this.getPreferredSize().getHeight()));
	}

	protected final JTextComponent getTextComponent(Component c) {
		if (c != null) {
			if (c instanceof JTextComponent) {
				return (JTextComponent) c;
			}
		}
		return null;
	}

	@Override
	public void show(Component invoker, int x, int y) {
		JTextComponent target = getTextComponent(invoker);
		if (target == null) {
			return;
		}
		if (target instanceof JPasswordField) {
			boolean enabled = false;
			Object copyClientProp = target.getClientProperty("JPasswordField.cutCopyAllowed");
			if (copyClientProp != null && copyClientProp instanceof Boolean && (Boolean) copyClientProp == true) {
				enabled = true;
			}
			this.copyMenuItem.setEnabled(enabled);
			this.cutMenuItem.setEnabled(enabled);
		} else {
			if (target.getSelectionStart() != target.getSelectionEnd()) {
				this.copyMenuItem.setEnabled(true);
			} else {
				this.copyMenuItem.setEnabled(false);
			}
			if (target.getSelectionStart() != target.getSelectionEnd() && target.isEditable()) {
				this.cutMenuItem.setEnabled(true);
			} else {
				this.cutMenuItem.setEnabled(false);
			}
		}

		if (target.isEditable()) {
			this.pasteMenuItem.setEnabled(true);
		} else {
			this.pasteMenuItem.setEnabled(false);
		}
		if (target.isEditable() && target.getSelectionStart() != target.getSelectionEnd()) {
			this.deleteMenuItem.setEnabled(true);
		} else {
			this.deleteMenuItem.setEnabled(false);
		}
		if (getTextLength(target) > 0) {
			this.selectAllMenuItem.setEnabled(true);
		} else {
			this.selectAllMenuItem.setEnabled(false);
		}
		// if ((getTextLength(target) > 0) && target.isEditable()) {
		// this.clearMenuItem.setEnabled(true);
		// } else {
		// this.clearMenuItem.setEnabled(false);
		// }
		super.show(invoker, x, y);
		this.parent.requestFocus();
	}

	private int getTextLength(JTextComponent c) {
		if (c == null) {
			return 0;
		}
		if (c.getText() == null) {
			return 0;
		}
		return c.getText().length();
	}

	@Override
	public void setVisible(boolean val) {
		try {
			super.setVisible(val);
			this.parent.requestFocus();
		} catch (Exception exp) {
			// do nothing
		}
	}
}

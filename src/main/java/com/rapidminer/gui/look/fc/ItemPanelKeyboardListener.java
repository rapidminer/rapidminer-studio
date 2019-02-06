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
package com.rapidminer.gui.look.fc;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;


/**
 * A keyboard listener used for the item panel.
 * 
 * @author Ingo Mierswa
 */
public class ItemPanelKeyboardListener implements java.awt.event.KeyListener {

	private ItemPanel pane;

	@Override
	public void keyPressed(KeyEvent e) {
		if (this.pane == null) {
			if (e.getSource() instanceof ItemPanel) {
				this.pane = ((ItemPanel) e.getSource());
			} else if (e.getSource() instanceof Item) {
				this.pane = ((ItemPanel) ((JComponent) e.getSource()).getParent());
			}
		}

		if (e.getKeyCode() == KeyEvent.VK_DELETE) {
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			if ((this.pane != null) && (this.pane.getFilePane().lastSelected != null)) {
				this.pane.getFilePane().filechooserUI.setCurrentDirectoryOfFileChooser(this.pane.getFilePane().lastSelected
						.getFile());
			}
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			this.pane.useKeyMoves("SPACE", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			this.pane.useKeyMoves("RIGHT", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			this.pane.useKeyMoves("LEFT", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			this.pane.useKeyMoves("UP", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			this.pane.useKeyMoves("DOWN", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_HOME) {
			this.pane.useKeyMoves("HOME", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_END) {
			this.pane.useKeyMoves("END", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			// ignore enter here!
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
			this.pane.useKeyMoves("PAGE_UP", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
			this.pane.useKeyMoves("PAGE_DOWN", e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK);
		} else {
			if (KeyEvent.getKeyText(e.getKeyCode()).toLowerCase().equals("a")
					&& (e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK)) {
				this.pane.getFilePane().selectAll();
			}
			if (e.getModifiersEx() == 0) {
				this.pane.forwardToNearestFor(String.valueOf(e.getKeyChar()).toLowerCase());
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {}
}

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
package com.rapidminer.gui.look.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.InternalFrameTitlePane;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.painters.CachedPainter;


/**
 * The UI for menus.
 *
 * @author Ingo Mierswa
 */
public class MenuUI extends BasicMenuUI {

	private class MenuMouseHandler extends MouseAdapter {

		@Override
		public void mouseEntered(MouseEvent me) {
			boolean res = true;
			try {
				if (MenuUI.this.menuItem.getParent().getParent() != null) {
					if (((JMenu) MenuUI.this.menuItem).getParent().getParent() instanceof InternalFrameTitlePane) {
						res = false;
					}
				}
			} catch (Exception exp) {
				// do nothing
			}
			if (res && ((AbstractButton) me.getSource()).isRolloverEnabled()) {
				((AbstractButton) me.getSource()).getModel().setRollover(true);
				((JComponent) me.getSource()).repaint();
			}
		}

		@Override
		public void mouseExited(MouseEvent me) {
			boolean res = true;
			try {
				if (MenuUI.this.menuItem.getParent().getParent() != null) {
					if (((JMenu) MenuUI.this.menuItem).getParent().getParent() instanceof InternalFrameTitlePane) {
						res = false;
					}
				}
			} catch (Exception exp) {
				// do nothing
			}

			if (res && ((AbstractButton) me.getSource()).isRolloverEnabled()) {
				((AbstractButton) me.getSource()).getModel().setRollover(false);
				((JComponent) me.getSource()).repaint();
			}
		}
	}

	private MouseListener mouseRolloverListener;

	public static ComponentUI createUI(JComponent x) {
		return new MenuUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		this.menuItem.setRolloverEnabled(true);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		this.mouseRolloverListener = new MenuMouseHandler();
		this.menuItem.addMouseListener(this.mouseRolloverListener);
	}

	@Override
	protected void uninstallListeners() {
		super.uninstallListeners();
		if (this.mouseRolloverListener != null) {
			this.menuItem.removeMouseListener(this.mouseRolloverListener);
			this.mouseRolloverListener = null;
		}
	}

	@Override
	protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
		super.paintText(g, menuItem, textRect, text);
	}

	@Override
	protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
		JMenu menu = (JMenu) menuItem;
		ButtonModel buttonmodel = menu.getModel();
		int w = menu.getWidth();
		int h = menu.getHeight();
		Color oldColor = g.getColor();
		if (!menu.isContentAreaFilled() || !menu.isOpaque()) {
			// do nothing
		} else {
			if (menu.isTopLevelMenu()) {
				if (buttonmodel.isSelected()) {
					CachedPainter.drawMenuBackground(menuItem, g, 0, 0, w, h);
				} else if (buttonmodel.isRollover() && buttonmodel.isEnabled()) {
					g.setColor(Colors.MENUBAR_BACKGROUND_HIGHLIGHT);
					g.fillRect(0, 0, w, h);
				} else {
					if (menuItem.getParent() instanceof JMenuBar) {
						((MenuBarUI) ((JMenuBar) menuItem.getParent()).getUI()).update(g, menuItem);
					}
				}
			} else {
				if (!menuItem.getModel().isSelected()) {
					RapidLookTools.drawMenuItemFading(menuItem, g);
				} else {
					RapidLookTools.drawMenuItemBackground(g, menuItem);
				}
			}
		}
		g.setColor(oldColor);
	}
}

/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.look.ui;

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.InternalFrameTitlePane;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.painters.CashedPainter;

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
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicMenuUI;


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
		if (menu.isTopLevelMenu()) {
			this.selectionForeground = Colors.getBlack();
		} else {
			this.selectionForeground = Colors.getWhite();
		}
		int w = menu.getWidth();
		int h = menu.getHeight();
		Color oldColor = g.getColor();
		if (!menu.isContentAreaFilled() || !menu.isOpaque()) {
			// do nothing
		} else {
			if (menu.isTopLevelMenu()) {
				if (buttonmodel.isSelected()) {
					CashedPainter.drawMenuBackground(menuItem, g, 0, 0, w, h);
				} else if (buttonmodel.isRollover() && buttonmodel.isEnabled()) {
					g.setColor(new ColorUIResource(252, 252, 252));
					g.fillRect(1, 1, w - 2, h - 2);
					g.setColor(RapidLookTools.getColors().getToolbarButtonColors()[0]);
					g.drawLine(2, 0, w - 3, 0);
					g.drawLine(2, h - 1, w - 3, h - 1);
					g.drawLine(0, 2, 0, h - 3);
					g.drawLine(w - 1, 2, w - 1, h - 3);
					g.drawLine(1, 1, 1, 1);
					g.drawLine(w - 2, 1, w - 2, 1);
					g.drawLine(1, h - 2, 1, h - 2);
					g.drawLine(w - 2, h - 2, w - 2, h - 2);
					g.setColor(RapidLookTools.getColors().getToolbarButtonColors()[8]);
					g.drawLine(0, 1, 1, 0);
					g.drawLine(w - 1, 1, w - 2, 0);
					g.drawLine(1, h - 1, 0, h - 2);
					g.drawLine(w - 1, h - 2, w - 2, h - 1);
					g.setColor(RapidLookTools.getColors().getToolbarButtonColors()[7]);
					g.drawLine(1, 2, 2, 1);
					g.drawLine(w - 2, 2, w - 3, 1);
					g.setColor(RapidLookTools.getColors().getToolbarButtonColors()[6]);
					g.drawLine(2, h - 2, 1, h - 3);
					g.drawLine(w - 2, h - 3, w - 3, h - 2);
					g.setColor(new ColorUIResource(252, 252, 252));
					g.drawLine(0, 0, 0, 0);
					g.drawLine(w - 1, 0, w - 1, 0);
					g.setColor(new ColorUIResource(232, 232, 232));
					g.drawLine(0, h - 1, 0, h - 1);
					g.drawLine(w - 1, h - 1, w - 1, h - 1);
				} else {
					if (menuItem.getParent() instanceof JMenuBar) {
						((MenuBarUI) ((JMenuBar) menuItem.getParent()).getUI()).update(g, menuItem);
					}
				}
			} else {
				if (!menuItem.getModel().isSelected()) {
					CashedPainter.drawMenuItemFading(menuItem, g);
				} else {
					RapidLookTools.drawMenuItemBackground(g, menuItem);
				}
			}
		}
		g.setColor(oldColor);
	}
}

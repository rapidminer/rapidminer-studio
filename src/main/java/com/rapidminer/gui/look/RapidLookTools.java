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
package com.rapidminer.gui.look;

import com.rapidminer.gui.look.painters.CashedPainter;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.components.ToggleDropDownButton;
import com.vlsolutions.swing.toolbars.VLToolBar;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;


/**
 * Some tools methods.
 * 
 * @author Ingo Mierswa
 */
public final class RapidLookTools {

	private static boolean vlDockingAvailable = true;

	static {
		try {
			VLToolBar.class.getName();
		} catch (NoClassDefFoundError e) {
			vlDockingAvailable = false;
		}
	}

	public static void clearMenuCache() {
		CashedPainter.clearMenuCache();
	}

	public static void drawRoundRectBorder(Graphics g, int w, int h, String action, boolean down) {
		Color c1 = new ColorUIResource(0);
		Color c2 = new ColorUIResource(0);
		Color c3 = new ColorUIResource(0);
		Color c4 = new ColorUIResource(0);
		Color c5 = new ColorUIResource(0);
		Color c6 = new ColorUIResource(0);
		Color c7 = new ColorUIResource(0);

		if (action.equals("NORMAL")) {
			c1 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][0];
			c2 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][1];
			c3 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][2];
			c4 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][3];
			c5 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][4];
			c6 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][5];
			c7 = RapidLookAndFeel.getColors().getButtonBorderColors()[0][6];
		} else if (action.equals("ROLLOVER")) {
			c1 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][0];
			c2 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][1];
			c3 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][2];
			c4 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][3];
			c5 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][4];
			c6 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][5];
			c7 = RapidLookAndFeel.getColors().getButtonBorderColors()[1][6];
		} else if (action.equals("FOCUS")) {
			c1 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][0];
			c2 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][1];
			c3 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][2];
			c4 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][3];
			c5 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][4];
			c6 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][5];
			c7 = RapidLookAndFeel.getColors().getButtonBorderColors()[2][6];
		} else if (action.equals("DISABLE")) {
			c1 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][0];
			c2 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][1];
			c3 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][2];
			c4 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][3];
			c5 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][4];
			c6 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][5];
			c7 = RapidLookAndFeel.getColors().getButtonBorderColors()[3][6];

		}

		g.setColor(c1);
		g.drawLine(5, 0, w - 7, 0);
		g.drawLine(0, 6, 0, h - 7);
		g.drawLine(w - 1, 6, w - 1, h - 7);
		g.setColor(c2);
		g.drawLine(6, h - 1, w - 7, h - 1);

		g.setColor(c5);
		g.drawLine(0, 5, 1, 5);
		g.drawLine(5, 1, 5, 0);

		g.setColor(c7);
		g.drawLine(1, 4, 1, 3);
		g.drawLine(4, 1, 3, 1);
		g.drawLine(3, 1, 1, 3);

		g.setColor(c4);
		g.drawLine(4, 0, 5, 1);
		g.drawLine(0, 4, 1, 5);
		g.drawLine(2, 3, 3, 2);

		// top right
		g.setColor(c5);
		g.drawLine(w - 1, 5, w - 2, 5);
		g.drawLine(w - 6, 0, w - 6, 1);

		g.setColor(c7);
		g.drawLine(w - 2, 4, w - 2, 3);
		g.drawLine(w - 5, 1, w - 4, 1);
		g.drawLine(w - 4, 1, w - 2, 3);

		g.setColor(c4);
		g.drawLine(w - 5, 0, w - 6, 1);
		g.drawLine(w - 1, 4, w - 2, 5);
		g.drawLine(w - 4, 2, w - 3, 3);

		g.setColor(c3);
		g.drawLine(2, h - 4, 3, h - 3);
		g.drawLine(5, h - 2, 5, h - 1);

		g.setColor(c5);
		g.drawLine(0, h - 6, 1, h - 6);

		g.setColor(c6);
		g.drawLine(1, h - 5, 1, h - 4);
		g.drawLine(4, h - 2, 3, h - 2);
		g.drawLine(3, h - 2, 1, h - 4);

		g.setColor(c3);
		g.drawLine(w - 3, h - 4, w - 4, h - 3);
		g.drawLine(w - 6, h - 2, w - 6, h - 1);

		g.setColor(c5);
		g.drawLine(w - 1, h - 6, w - 2, h - 6);

		g.setColor(c6);
		g.drawLine(w - 2, h - 5, w - 2, h - 4);
		g.drawLine(w - 5, h - 2, w - 4, h - 2);
		g.drawLine(w - 4, h - 2, w - 2, h - 4);

		if (down) {
			g.setColor(new ColorUIResource(240, 240, 240));
		} else {
			g.setColor(new ColorUIResource(210, 210, 210));
		}

		g.drawLine(8, h, w - 9, h);
	}

	public static void drawMenuItemBackground(Graphics g, JMenuItem menuItem) {
		Color oldColor = g.getColor();
		ButtonModel model = menuItem.getModel();
		int w = menuItem.getWidth();
		int h = menuItem.getHeight();

		if (model.isArmed() || (model.isSelected() && (menuItem instanceof JMenu))) {
			Color c1 = UIManager.getColor("MenuItem.selectionBackground");
			g.setColor(c1);
			g.fillRect(0, 0, w, h);

			g.setColor(c1.brighter());
			g.drawLine(2, 0, w - 3, 0);
			g.drawLine(2, h - 1, w - 3, h - 1);
			g.drawLine(0, 2, 0, h - 3);
			g.drawLine(w - 1, 2, w - 1, h - 3);

			g.setColor(c1.brighter().brighter());
			g.drawLine(0, 1, 1, 0);
			g.drawLine(w - 1, 1, w - 2, 0);
			g.drawLine(0, h - 2, 1, h - 1);
			g.drawLine(w - 1, h - 2, w - 2, h - 1);

			g.setColor(c1.brighter().brighter().brighter());
			g.drawLine(0, 0, 0, 0);
			g.drawLine(0, h - 1, 0, h - 1);
			g.drawLine(w - 1, 0, w - 1, 0);
			g.drawLine(w - 1, h - 1, w - 1, h - 1);
		} else if (!((menuItem instanceof JMenu) && ((JMenu) menuItem).isTopLevelMenu())) {
			g.setColor(Color.white);
			g.fillRect(0, 0, w, h);

			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(new GradientPaint((w > 50 ? w - 50 : 0), 0, new ColorUIResource(Color.white), w, 0,
					new ColorUIResource(235, 242, 255)));
			g2.fillRect((w > 50 ? w - 50 : 0), 0, 50, h);
		}
		g.setColor(oldColor);
	}

	public static boolean isToolbarButton(JComponent b) {
		return (b.getParent() instanceof JToolBar) || (vlDockingAvailable && isVLToolbarButton(b));
	}

	public static boolean isVLToolbarButton(JComponent b) {
		return (b.getParent() instanceof VLToolBar);
	}

	public static void drawToolbarButton(Graphics g, JComponent c) {
		int w = c.getWidth();
		int h = c.getHeight();

		AbstractButton b = (AbstractButton) c;
		if (c.getParent() instanceof JToolBar || c.getParent() instanceof VLToolBar) {
			g.setColor(c.getParent().getBackground());
		}

		if (b.isOpaque()) {
			g.fillRect(0, 0, b.getWidth(), b.getHeight());
		}
		if (b.getModel().isSelected() && b.isRolloverEnabled()
				|| (b.getModel().isPressed() && b.getModel().isArmed() && b.isRolloverEnabled())) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setPaint(new GradientPaint(0, 0, RapidLookAndFeel.getColors().getToolbarColors()[0], 0, h, Color.white));

			g2.fillRect(1, 1, w - 2, h - 2);

			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[0]);
			g.drawLine(2, 0, w - 3, 0);
			g.drawLine(0, 2, 0, h - 3);
			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[1]);
			g.drawLine(w - 1, 2, w - 1, h - 3);
			g.drawLine(2, h - 1, w - 3, h - 1);

			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[2]);
			g.drawLine(0, 1, 1, 0);
			g.drawLine(w - 1, 1, w - 2, 0);
			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[3]);
			g.drawLine(1, h - 1, 0, h - 2);
			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[1]);
			g.drawLine(w - 1, h - 2, w - 2, h - 1);

			g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[4]);
			g.drawLine(0, 0, 0, 0);
			g.drawLine(w - 1, 0, w - 1, 0);
		} else if (b.getModel().isRollover() && b.isRolloverEnabled()) {
			if (b instanceof DropDownButton || b instanceof ToggleDropDownButton) {
				// for drop down buttons
				// do not print top left and bottom left corners
				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[0]);
				g.drawLine(2, 0, w - 1, 0);
				g.drawLine(2, h - 1, w - 1, h - 1);
				g.drawLine(0, 2, 0, h - 3);

				g.drawLine(1, 1, 1, 1);
				g.drawLine(1, h - 2, 1, h - 2);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[8]);
				g.drawLine(0, 1, 1, 0);
				g.drawLine(1, h - 1, 0, h - 2);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[7]);
				g.drawLine(1, 2, 2, 1);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[6]);
				g.drawLine(2, h - 2, 1, h - 3);
			} else if (b instanceof DropDownButton.DropDownArrowButton
					|| b instanceof ToggleDropDownButton.DropDownArrowButton) {
				// for arrow part of drop down buttons
				// do not print top right and bottom right corners
				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[0]);
				g.drawLine(0, 0, w - 3, 0);
				g.drawLine(0, h - 1, w - 3, h - 1);
				g.drawLine(0, 0, 0, h - 1);
				g.drawLine(w - 1, 1, w - 1, h - 3);

				g.drawLine(w - 2, 1, w - 2, 1);
				g.drawLine(w - 2, h - 2, w - 2, h - 2);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[8]);
				g.drawLine(w - 1, 1, w - 2, 0);
				g.drawLine(w - 1, h - 2, w - 2, h - 1);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[7]);
				g.drawLine(w - 2, 2, w - 3, 1);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[6]);
				g.drawLine(w - 2, h - 3, w - 3, h - 2);
			} else {
				// for all other buttons
				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[0]);
				g.drawLine(2, 0, w - 3, 0);
				g.drawLine(2, h - 1, w - 3, h - 1);
				g.drawLine(0, 2, 0, h - 3);
				g.drawLine(w - 1, 2, w - 1, h - 3);

				g.drawLine(1, 1, 1, 1);
				g.drawLine(w - 2, 1, w - 2, 1);
				g.drawLine(1, h - 2, 1, h - 2);
				g.drawLine(w - 2, h - 2, w - 2, h - 2);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[8]);
				g.drawLine(0, 1, 1, 0);
				g.drawLine(w - 1, 1, w - 2, 0);
				g.drawLine(1, h - 1, 0, h - 2);
				g.drawLine(w - 1, h - 2, w - 2, h - 1);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[7]);
				g.drawLine(1, 2, 2, 1);
				g.drawLine(w - 2, 2, w - 3, 1);

				g.setColor(RapidLookAndFeel.getColors().getToolbarButtonColors()[6]);
				g.drawLine(2, h - 2, 1, h - 3);
				g.drawLine(w - 2, h - 3, w - 3, h - 2);
			}
		}
	}

	public static boolean isLeftToRight(Component c) {
		return c.getComponentOrientation().isLeftToRight();
	}

	public static void drawGradient(Graphics g, int x1, int y1, int x2, int y2, Color c1, Color c2) {
		int w = x2 - x1;
		// int h = y2 - y1;
		if (w > 0) {
			BufferedImage bi = new BufferedImage(w, 1, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g2 = (Graphics2D) bi.getGraphics();
			g2.setPaint(new GradientPaint(0, 0, c1, w, 0, c2));
			g2.fillRect(0, 0, w, 1);
			g.drawImage(bi, x1, 0, w, y2, 0, 0, x2, 1, null);
		}
		// else if (h > 0) {
		// // do nothing
		// }
	}

	public static Colors getColors() {
		return RapidLookAndFeel.getColors();
	}
}

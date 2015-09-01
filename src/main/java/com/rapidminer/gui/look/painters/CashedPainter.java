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
package com.rapidminer.gui.look.painters;

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;


/**
 * This class provides static methods for cached painting of GUI elements.
 * 
 * @author Ingo Mierswa
 */
public class CashedPainter {

	public static void clearMenuCache() {
		AbstractCachedPainter.clearCache();
	}

	public static void clearCashedImages() {
		AbstractCachedPainter.clearCache();
	}

	public static boolean drawMenuBackground(Component c, Graphics g, int x, int y, int w, int h) {
		if ((h < 0) || (w < 0)) {
			return true;
		}
		MenuBackgroundPainter.SINGLETON.paint(c, g, x, y, w, h);
		return true;
	}

	public static boolean drawMenuBarBackground(Component c, Graphics g, int x, int y, int w, int h) {
		if ((h < 0) || (w < 0)) {
			return true;
		}
		MenuBarBackgroundPainter.SINGLETON.paint(c, g, x, y, w, h);
		return true;
	}

	public static boolean drawProgressBar(Component c, Graphics g, boolean vertical, boolean indeterminate, int x, int y,
			int w, int h) {
		if ((h < 0) || (w < 0)) {
			return true;
		}
		if (!indeterminate) {
			DeterminateProgressBarPainter.SINGLETON.paint(c, g, x, y, w, h, new Object[] { Boolean.valueOf(vertical) });
		} else {
			InDeterminateProgressBarPainter.SINGLETON.paint(c, g, x, y, w, h, new Object[] { Boolean.valueOf(vertical) });
		}
		return true;
	}

	public static boolean drawRadioButton(Component c, Graphics g) {
		JRadioButton radioButton = (JRadioButton) c;
		ButtonModel bm = radioButton.getModel();
		int w = c.getWidth();
		int h = c.getHeight();
		if ((h < 0) || (w < 0)) {
			return true;
		}
		RadioButtonPainter.SINGLETON.paint(
				c,
				g,
				c.getX(),
				c.getY(),
				w,
				h,
				new Object[] { Boolean.valueOf(bm.isSelected()), Boolean.valueOf(bm.isEnabled()),
						Boolean.valueOf(bm.isPressed() || bm.isSelected()), Boolean.valueOf(bm.isArmed()),
						Boolean.valueOf(bm.isRollover() && radioButton.isRolloverEnabled()), });
		return true;
	}

	public static boolean drawMenuSeparator(Component c, Graphics g) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((h < 0) || (w < 0)) {
			return true;
		}
		MenuSeparatorPainter.SINGLETON.paint(c, g, c.getX(), c.getY(), w, h);
		return true;
	}

	public static boolean drawCheckBoxIcon(Component c, Graphics g) {
		JCheckBox checkbox = (JCheckBox) c;
		ButtonModel bm = checkbox.getModel();
		int w = c.getWidth();
		int h = c.getHeight();
		if ((h < 0) || (w < 0)) {
			return true;
		}
		CheckboxPainter.SINGLETON.paint(
				c,
				g,
				c.getX(),
				c.getY(),
				w,
				h,
				new Object[] { Boolean.valueOf(bm.isSelected()), Boolean.valueOf(bm.isEnabled()),
						Boolean.valueOf(bm.isPressed()), Boolean.valueOf(bm.isArmed()),
						Boolean.valueOf(bm.isRollover() && checkbox.isRolloverEnabled()), });
		return true;
	}

	public static boolean drawMenuItemFading(Component c, Graphics g) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((h < 0) || (w < 0)) {
			return true;
		}

		g.setColor(RapidLookTools.getColors().getMenuItemBackground());
		g.fillRect(0, 0, c.getWidth(), c.getHeight());
		if (!RapidLookTools.getColors().getMenuItemFadingColor().equals(RapidLookTools.getColors().getMenuItemBackground())) {
			MenuItemFadingPainter.SINGLETON.paint(c, g, c.getX(), c.getY(), w, h);
		}
		return true;
	}

	public static boolean drawComboBox(Component c, Graphics g, boolean down) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((w <= 0) || (h <= 0)) {
			return true;
		}
		JComboBox comboBox = (JComboBox) c;
		ComboBoxPainter.SINGLETON.paint(c, g, 0, 0, w, h,
				new Object[] { Boolean.valueOf(down), Boolean.valueOf(comboBox.isEnabled()) });
		return true;
	}

	public static boolean drawComboBoxBorder(Component c, Graphics g, boolean down, boolean round) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((w <= 0) || (h <= 0)) {
			return true;
		}
		JComboBox comboBox = (JComboBox) c;
		String type = null;
		if (comboBox.isEnabled()) {
			if (comboBox.hasFocus()) {
				type = "ROLLOVER";
			} else {
				type = "NORMAL";
			}
		} else {
			type = "DISABLE";
		}
		ButtonBorderPainter.SINGLETON.paint(c, g, 0, 0, w, h, new Object[] { Boolean.valueOf(down), Boolean.valueOf(round),
				type });
		return true;
	}

	public static boolean drawButtonBorder(Component c, Graphics g, String prefix) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((w <= 0) || (h <= 0)) {
			return true;
		}
		AbstractButton b = ((AbstractButton) c);
		ButtonModel bm = ((AbstractButton) c).getModel();
		boolean down = false;
		boolean draw = false;
		String type = "";
		if (b.isContentAreaFilled() && !(b.getParent() instanceof JToolBar)) {
			draw = true;
			down = (bm.isArmed() && bm.isPressed()) || bm.isSelected();
			if (bm.isEnabled()) {
				if (bm.isRollover() && b.isRolloverEnabled()) {
					type = "ROLLOVER";
				} else if (b.hasFocus() && b.isFocusPainted()) {
					type = "FOCUS";
				} else {
					type = "NORMAL";
				}
			} else {
				type = "DISABLE";
			}
		}
		if (draw) {
			// boolean isDefault = false;
			if (b instanceof JButton) {
				if (((JButton) b).isDefaultButton()) {
					if (type.equals("NORMAL")) {
						type = "DEFAULT";
					}
				}
			}
			ButtonBorderPainter.SINGLETON.paint(c, g, 0, 0, w, h,
					new Object[] { Boolean.valueOf(down), Boolean.valueOf(false), type });
		}
		return true;
	}

	public static boolean drawButton(Component c, Graphics g) {
		int w = c.getWidth();
		int h = c.getHeight();
		if ((w <= 0) || (h <= 0)) {
			return true;
		}
		ButtonModel bm = ((AbstractButton) c).getModel();
		ButtonPainter.SINGLETON.paint(
				c,
				g,
				0,
				0,
				w,
				h,
				new Object[] { Boolean.valueOf(bm.isEnabled()), Boolean.valueOf(bm.isPressed()),
						Boolean.valueOf(bm.isSelected()), Boolean.valueOf(((AbstractButton) c).isBorderPainted()) });
		return true;
	}
}

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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicButtonUI;

import com.rapidminer.gui.look.ButtonListener;
import com.rapidminer.gui.look.RapidLookTools;


/**
 * The UI for the basic button.
 *
 * @author Ingo Mierswa
 */
public class ButtonUI extends BasicButtonUI {

	private final static ButtonUI BUTTON_UI = new ButtonUI();

	public static ComponentUI createUI(JComponent c) {
		return BUTTON_UI;
	}

	@Override
	protected void installDefaults(AbstractButton b) {
		super.installDefaults(b);
		b.setRolloverEnabled(true);
	}

	@Override
	protected void uninstallDefaults(AbstractButton b) {
		super.uninstallDefaults(b);
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
	}

	@Override
	public void uninstallUI(JComponent c) {
		super.uninstallUI(c);
	}

	@Override
	protected BasicButtonListener createButtonListener(AbstractButton b) {
		return new ButtonListener(b);
	}

	@Override
	protected void paintText(Graphics g, AbstractButton c, Rectangle textRect, String text) {
		// otherwise the tabs text would not have AA for some reason even though the rest of the
		// components has AA without this
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		super.paintText(g, c, textRect, text);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		AbstractButton b = (AbstractButton) c;
		if (RapidLookTools.isToolbarButton(b)) {
			RapidLookTools.drawToolbarButton(g, b);
			super.paint(g, c);
			return;
		}

		int w = c.getWidth();
		int h = c.getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}

		if (b.isContentAreaFilled()) {
			RapidLookTools.drawButton(b, g, RapidLookTools.createShapeForButton(b));
		}
		if (b.isBorderPainted()) {
			RapidLookTools.drawButtonBorder(b, g, RapidLookTools.createBorderShapeForButton(b));
		}
		super.paint(g, c);
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		if (c.getParent() instanceof JToolBar) {
			return new Dimension((int) super.getPreferredSize(c).getWidth() + 6,
					(int) super.getPreferredSize(c).getHeight() + 6);
		} else {
			return new Dimension((int) super.getPreferredSize(c).getWidth() + 10, (int) super.getPreferredSize(c)
					.getHeight() + 6);
		}
	}

	@Override
	protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
		if (b.isBorderPainted()) {
			RapidLookTools.drawButtonBorder(b, g, RapidLookTools.createBorderShapeForButton(b));
		}
	}

	@Override
	protected void paintButtonPressed(Graphics g, AbstractButton b) {
		setTextShiftOffset();
	}

}

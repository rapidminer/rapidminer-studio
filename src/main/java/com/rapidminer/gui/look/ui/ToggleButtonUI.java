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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import javax.swing.text.View;

import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.ToggleButtonListener;


/**
 * The UI for toggle buttons.
 *
 * @author Ingo Mierswa
 */
public class ToggleButtonUI extends BasicToggleButtonUI {

	private final static ToggleButtonUI TOGGLE_BUTTON_UI = new ToggleButtonUI();

	public static ComponentUI createUI(JComponent c) {
		return TOGGLE_BUTTON_UI;
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
	protected void paintText(Graphics g, AbstractButton c, Rectangle textRect, String text) {
		super.paintText(g, c, textRect, text);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		AbstractButton b = (AbstractButton) c;

		Dimension size = b.getSize();
		FontMetrics fm = g.getFontMetrics();

		Insets i = c.getInsets();

		Rectangle viewRect = new Rectangle(size);

		viewRect.x += i.left;
		viewRect.y += i.top;
		viewRect.width -= i.right + viewRect.x;
		viewRect.height -= i.bottom + viewRect.y;

		Rectangle iconRect = new Rectangle();
		Rectangle textRect = new Rectangle();

		Font f = c.getFont();
		g.setFont(f);

		String text = SwingUtilities.layoutCompoundLabel(c, fm, b.getText(), b.getIcon(), b.getVerticalAlignment(),
				b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect,
				textRect, b.getText() == null ? 0 : b.getIconTextGap());

		g.setColor(b.getBackground());

		if (b.isContentAreaFilled()) {
			if (RapidLookTools.isToolbarButton(b)) {
				RapidLookTools.drawToolbarButton(g, b);
			} else {
				RapidLookTools.drawButton(b, g, RapidLookTools.createShapeForButton(b));
			}
		}

		if (b.getIcon() != null) {
			paintIcon(g, b, iconRect);
		}

		if (text != null && !text.equals("")) {
			View v = (View) c.getClientProperty(BasicHTML.propertyKey);
			if (v != null) {
				v.paint(g, textRect);
			} else {
				paintText(g, b, textRect, text);
			}
		}

		if (b.isFocusPainted() && b.hasFocus()) {
			paintFocus(g, b, viewRect, textRect, iconRect);
		}

		if (!RapidLookTools.isToolbarButton(b)) {
			if (b.isBorderPainted()) {
				RapidLookTools.drawButtonBorder(b, g, RapidLookTools.createBorderShapeForButton(b));
			}
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
		if (b.isContentAreaFilled()) {
			if (RapidLookTools.isToolbarButton(b)) {
				RapidLookTools.drawToolbarButton(g, b);
			} else {
				RapidLookTools.drawButton(b, g, RapidLookTools.createShapeForButton(b));
			}
		}
		setTextShiftOffset();
	}

	@Override
	protected BasicButtonListener createButtonListener(AbstractButton b) {
		return new ToggleButtonListener(b);
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
}

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

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRadioButtonUI;

import com.rapidminer.gui.look.Colors;


/**
 * The UI for radio buttons.
 *
 * @author Ingo Mierswa
 */
public class RadioButtonUI extends BasicRadioButtonUI {

	private static final RadioButtonUI radioButtonUI = new RadioButtonUI();

	private final static BasicStroke DASHED_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
			6.0f, new float[] { 2.0f }, 2.0f);

	private boolean initialized = false;

	public RadioButtonUI() {}

	@Override
	protected void paintText(Graphics g, AbstractButton c, Rectangle textRect, String text) {
		super.paintText(g, c, textRect, text);
	}

	@Override
	public void installDefaults(AbstractButton abstractbutton) {
		super.installDefaults(abstractbutton);
		if (!this.initialized) {
			this.icon = UIManager.getIcon(getPropertyPrefix() + "icon");
			this.initialized = true;
		}
		abstractbutton.setRolloverEnabled(true);
	}

	@Override
	protected void uninstallDefaults(AbstractButton abstractbutton) {
		super.uninstallDefaults(abstractbutton);
		this.initialized = false;
	}

	public static ComponentUI createUI(JComponent jcomponent) {
		return radioButtonUI;
	}

	@Override
	public Dimension getMinimumSize(JComponent c) {
		return new Dimension(30, 22);
	}

	@Override
	public synchronized void paint(Graphics g, JComponent c) {
		// without this, checkboxes and radio buttons have a fixed bg color instead of using the bg
		// color of the container they are in
		if (c.getParent() != null) {
			c.setBackground(c.getParent().getBackground());
		}
		super.paint(g, c);
	}

	@Override
	protected void paintFocus(Graphics g, Rectangle textRect, Dimension d) {
		int x = 0, y = 0, w = 0, h = 0;
		x = (int) textRect.getX() - 2;
		y = (int) textRect.getY();
		w = (int) textRect.getWidth() + 4;
		h = (int) textRect.getHeight();

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2.setColor(Colors.RADIOBUTTON_BORDER_FOCUS);
		g2.setStroke(DASHED_STROKE);
		g2.drawRect(x, y, w, h);
	}
}

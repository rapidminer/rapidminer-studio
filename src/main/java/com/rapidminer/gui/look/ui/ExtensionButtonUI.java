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
import java.awt.Rectangle;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
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
public class ExtensionButtonUI extends BasicButtonUI {

	private final static ExtensionButtonUI BUTTON_UI = new ExtensionButtonUI();

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
		super.paintText(g, c, textRect, text);
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		AbstractButton b = (AbstractButton) c;
		RapidLookTools.drawToolbarButton(g, b);
		super.paint(g, c);
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return new Dimension(25, 25);
	}

	@Override
	protected void paintButtonPressed(Graphics g, AbstractButton b) {
		setTextShiftOffset();
	}
}

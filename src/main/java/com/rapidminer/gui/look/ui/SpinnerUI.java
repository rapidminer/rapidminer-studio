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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicSpinnerUI;


/**
 * The UI for spinners.
 *
 * @author Ingo Mierswa
 */
public class SpinnerUI extends BasicSpinnerUI {

	public static ComponentUI createUI(JComponent c) {
		return new SpinnerUI();
	}

	@Override
	protected void installDefaults() {
		super.installDefaults();
	}

	@Override
	protected void uninstallDefaults() {
		super.installDefaults();
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		super.paint(g, c);
	}

	@Override
	protected Component createPreviousButton() {
		AbstractButton ab = (AbstractButton) super.createPreviousButton();
		JButton b = new SpinnerButton("down");
		b.addActionListener((ActionListener) getUIResource(ab.getActionListeners()));
		b.addMouseListener((MouseListener) getUIResource(ab.getMouseListeners()));
		return b;
	}

	@Override
	protected Component createNextButton() {
		AbstractButton ab = (AbstractButton) super.createNextButton();
		JButton b = new SpinnerButton("up");
		b.setRequestFocusEnabled(false);
		b.addActionListener((ActionListener) getUIResource(ab.getActionListeners()));
		b.addMouseListener((MouseListener) getUIResource(ab.getMouseListeners()));
		return b;
	}

	private UIResource getUIResource(Object[] listeners) {
		for (Object element : listeners) {
			if (element instanceof UIResource) {
				return (UIResource) element;
			}
		}
		return null;
	}

	@Override
	public Dimension getPreferredSize(JComponent c) {
		return new Dimension(50, 20);
	}
}

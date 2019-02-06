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
package com.rapidminer.gui.look;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.plaf.basic.BasicButtonListener;


/**
 * The button listener used for buttons.
 * 
 * @author Ingo Mierswa
 */
public class ButtonListener extends BasicButtonListener {

	public ButtonListener(AbstractButton b) {
		super(b);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		super.mouseEntered(e);
		AbstractButton button = (AbstractButton) e.getSource();
		button.getModel().setRollover(true);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		super.mouseExited(e);
		AbstractButton button = (AbstractButton) e.getSource();
		button.getModel().setRollover(false);
		button.setSelected(false);
	}

	@Override
	public void focusGained(FocusEvent e) {
		Component c = (Component) e.getSource();
		c.repaint();
	}

	@Override
	public void focusLost(FocusEvent e) {
		super.focusLost(e);
		AbstractButton b = (AbstractButton) e.getSource();
		b.getModel().setArmed(false);
		b.repaint();
	}
}

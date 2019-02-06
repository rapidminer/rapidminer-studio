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
package com.rapidminer.gui.look.fc;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;


/**
 * A label for the file table.
 * 
 * @author Ingo Mierswa
 */
public class FileTableLabel extends JLabel {

	private static final long serialVersionUID = -2522723592207989524L;

	private boolean selected = false;

	private Color oldColor;

	private static Border emptyBorder = BorderFactory.createEmptyBorder(0, 2, 0, 3);

	public FileTableLabel() {
		super();
		setOpaque(false);
	}

	public FileTableLabel(String s, ImageIcon i, int n) {
		super(s, i, n);
		setBorder(emptyBorder);
		setOpaque(false);
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return this.selected;
	}

	@Override
	public void paint(Graphics g) {
		this.oldColor = g.getColor();
		if (this.selected) {
			g.setColor(UIManager.getColor("textHighlight"));
			g.fillRect(0, 0, (int) this.getPreferredSize().getWidth(), (int) this.getSize().getHeight());
			g.setColor(this.oldColor);
		}
		super.paint(g);
	}
}

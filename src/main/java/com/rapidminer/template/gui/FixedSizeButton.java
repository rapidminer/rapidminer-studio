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
package com.rapidminer.template.gui;

import java.awt.Dimension;
import java.beans.Transient;

import javax.swing.Action;
import javax.swing.JButton;


/**
 * Square button to use in the data area.
 * 
 * @author Simon Fischer
 * 
 */
public class FixedSizeButton extends JButton {

	private static final long serialVersionUID = 1L;

	private static final Dimension SIZE = new Dimension(150, 60);

	private final Dimension size;

	public FixedSizeButton(Action a) {
		this(a, SIZE);
	}

	public FixedSizeButton(Action a, Dimension size) {
		super(a);
		this.size = size;
	}

	@Override
	@Transient
	public Dimension getPreferredSize() {
		return size;
	}

	@Override
	@Transient
	public Dimension getMinimumSize() {
		return size;
	}

	@Override
	@Transient
	public Dimension getMaximumSize() {
		return size;
	}
}

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
package com.rapidminer.gui.look.borders;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JProgressBar;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.UIResource;

import com.rapidminer.gui.look.RapidLookTools;


/**
 * The UIResource for progress bar borders.
 *
 * @author Ingo Mierswa
 */
public class ProgressBarBorder extends AbstractBorder implements UIResource {

	private static final long serialVersionUID = 4150602481439529878L;

	@Override
	public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
		// border is not wanted
	}

	@Override
	public Insets getBorderInsets(Component c) {
		boolean compressed = Boolean.parseBoolean(String.valueOf(((JProgressBar) c)
				.getClientProperty(RapidLookTools.PROPERTY_PROGRESSBAR_COMPRESSED)));
		if (compressed) {
			return new Insets(3, 3, 3, 3);

		} else {
			return new Insets(0, 3, 10, 3);
		}
	}

	@Override
	public Insets getBorderInsets(Component c, Insets insets) {
		boolean compressed = Boolean.parseBoolean(String.valueOf(((JProgressBar) c)
				.getClientProperty(RapidLookTools.PROPERTY_PROGRESSBAR_COMPRESSED)));
		if (compressed) {
			insets.top = insets.left = insets.bottom = insets.right = 3;
		} else {
			insets.top = 0;
			insets.left = insets.right = 3;
			insets.bottom = 10;
		}
		return insets;

	}
}

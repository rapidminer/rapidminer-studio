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
package com.rapidminer.gui.tools.components;

import javax.swing.Icon;
import javax.swing.JLabel;


/**
 * A label with fixed width in pixels breaking lines accordingly.
 * 
 * @author Simon Fischer, Tobias Malbrecht
 * 
 */
public class FixedWidthLabel extends JLabel {

	private static final long serialVersionUID = -1970369698347783237L;

	private int width;

	private String rootlessHTML;

	public FixedWidthLabel(int width, String rootlessHTML) {
		this(width, rootlessHTML, null);
	}

	public FixedWidthLabel(int width, String rootlessHTML, Icon icon) {
		this.width = width;
		this.rootlessHTML = rootlessHTML;
		setIcon(icon);
		updateLabel();
	}

	@Override
	public void setText(String text) {
		this.rootlessHTML = text;
		updateLabel();
	}

	public void setWidth(int width) {
		this.width = width;
		updateLabel();
	}

	public void updateLabel() {
		super.setText("<html><body><div style=\"width:" + width + "pt\">" + rootlessHTML + "</div></body></html>");
	}

	/**
	 * @return the rootless HTML content w/o the formatting code. Can be {@code null}
	 * @since 9.3.0
	 */
	public String getPlaintext() {
		return rootlessHTML;
	}
}

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
package com.rapidminer.gui.tools;

import javax.swing.JSlider;


/**
 * This extended version of the JSlider shows the currently selected value as tool tip.
 * 
 * @author Ingo Mierswa
 */
public class ExtendedJSlider extends JSlider {

	private static final long serialVersionUID = -3411054850324165365L;

	public ExtendedJSlider(int orientation, int min, int max, int value, boolean showValueAsToolTip) {
		super(orientation, min, max, value);

		if (showValueAsToolTip) {
			ExtendedJSliderToolTips.enableSliderToolTips(this);
		}
	}
}

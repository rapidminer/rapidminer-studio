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
package com.rapidminer.gui.plotter.settings;

import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;

import javax.swing.JCheckBox;


/**
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class ListeningJCheckBox extends JCheckBox implements PlotterSettingsChangedListener {

	private static final long serialVersionUID = -3145893699784702675L;
	private String generalKey;

	public ListeningJCheckBox(String generalKey, String text, boolean defaultValue) {
		super(text, defaultValue);
		if (generalKey.startsWith("_")) {
			this.generalKey = generalKey;
		} else {
			this.generalKey = "_" + generalKey;
		}
	}

	@Override
	public void settingChanged(String generalKey, String specificKey, String value) {
		if (generalKey.equals(this.generalKey)) {
			setSelected(Boolean.parseBoolean(value));
		}
	}
}

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

import javax.swing.DefaultListSelectionModel;
import javax.swing.JList;
import javax.swing.ListModel;

import com.rapidminer.gui.plotter.PlotterConfigurationModel.PlotterSettingsChangedListener;
import com.rapidminer.parameter.ParameterTypeEnumeration;


/**
 * @author Sebastian Land
 * @deprecated since 9.2.0
 */
@Deprecated
public class ListeningListSelectionModel extends DefaultListSelectionModel implements PlotterSettingsChangedListener {

	private static final long serialVersionUID = -3145893699784702675L;
	private String generalKey;
	private JList<String> list;

	public ListeningListSelectionModel(String generalKey, JList<String> list) {
		if (generalKey.startsWith("_")) {
			this.generalKey = generalKey;
		} else {
			this.generalKey = "_" + generalKey;
		}
		this.list = list;
	}

	@Override
	public void settingChanged(String generalKey, String specificKey, String value) {
		if (generalKey.equals(this.generalKey)) {
			ListModel<String> listModel = list.getModel();
			// searching indices of selected dimensions
			String names[] = ParameterTypeEnumeration.transformString2Enumeration(value);
			boolean[] selectedDimensions = new boolean[listModel.getSize()];
			for (int i = 0; i < names.length; i++) {
				String name = names[i].trim();
				for (int j = 0; j < listModel.getSize(); j++) {
					if (listModel.getElementAt(j).equals(name)) {
						selectedDimensions[j] = true;
						break;
					}
				}
			}

			// switching all differing dimensions
			setValueIsAdjusting(true);
			for (int i = 0; i < listModel.getSize(); i++) {
				if (selectedDimensions[i]) {
					addSelectionInterval(i, i);
				} else {
					removeSelectionInterval(i, i);
				}
			}
			setValueIsAdjusting(false);
		}

	}
}

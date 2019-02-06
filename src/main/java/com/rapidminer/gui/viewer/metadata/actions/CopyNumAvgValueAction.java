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
package com.rapidminer.gui.viewer.metadata.actions;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.viewer.metadata.AttributeStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.NumericalAttributeStatisticsModel;
import com.rapidminer.tools.Tools;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;


/**
 * This action is only to be used by the {@link AttributePopupMenu}.
 * 
 * @author Marco Boeck
 * 
 */
public class CopyNumAvgValueAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link CopyNumAvgValueAction} instance.
	 */
	public CopyNumAvgValueAction() {
		super(true, "meta_data_stats.copy_numeric_avg");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (!(((JComponent) e.getSource()).getParent() instanceof AttributePopupMenu)) {
			return;
		}

		AttributeStatisticsPanel asp = ((AttributePopupMenu) ((JComponent) e.getSource()).getParent())
				.getAttributeStatisticsPanel();
		NumericalAttributeStatisticsModel model = (NumericalAttributeStatisticsModel) asp.getModel();
		Tools.copyStringToClipboard(String.valueOf(model.getAverage()));
	}

}

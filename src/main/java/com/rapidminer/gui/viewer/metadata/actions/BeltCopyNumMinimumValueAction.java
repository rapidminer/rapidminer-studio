/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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

import java.awt.event.ActionEvent;
import java.util.Objects;
import javax.swing.JComponent;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.viewer.metadata.BeltColumnStatisticsPanel;
import com.rapidminer.gui.viewer.metadata.model.AbstractBeltColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltNumericalColumnStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.BeltTimeColumnStatisticsModel;
import com.rapidminer.tools.Tools;


/**
 * This action is only to be used by the {@link BeltColumnPopupMenu}.
 * 
 * @author Marco Boeck, Gisa Meier
 * @since 9.7.0
 */
public class BeltCopyNumMinimumValueAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link BeltCopyNumMinimumValueAction} instance.
	 */
	public BeltCopyNumMinimumValueAction() {
		super(true, "meta_data_stats.copy_numeric_min");
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		if (!(((JComponent) e.getSource()).getParent() instanceof BeltColumnPopupMenu)) {
			return;
		}

		BeltColumnStatisticsPanel asp = ((BeltColumnPopupMenu) ((JComponent) e.getSource()).getParent())
				.getColumnStatisticsPanel();
		AbstractBeltColumnStatisticsModel model = asp.getModel();
		if (model.getType() == AbstractBeltColumnStatisticsModel.Type.TIME) {
			Tools.copyStringToClipboard(Objects.toString(((BeltTimeColumnStatisticsModel) model).getMinimum(), "?"));
		} else {
			Tools.copyStringToClipboard(String.valueOf(((BeltNumericalColumnStatisticsModel) model).getMinimum()));
		}
	}

}

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
import com.rapidminer.gui.viewer.metadata.model.AbstractAttributeStatisticsModel;
import com.rapidminer.gui.viewer.metadata.model.MetaDataStatisticsModel;

import java.awt.event.ActionEvent;


/**
 * The action shows the construction value for all {@link AbstractAttributeStatisticsModel}s.
 * 
 * @author Marco Boeck
 * 
 */
public class ShowConstructionValueAction extends ResourceAction {

	private static final long serialVersionUID = 6979404131032484600L;

	/** the {@link MetaDataStatisticsModel} model */
	MetaDataStatisticsModel model;

	/**
	 * Creates a new {@link ShowConstructionValueAction} instance.
	 * 
	 * @param model
	 */
	public ShowConstructionValueAction(MetaDataStatisticsModel model) {
		super(true, "meta_data_stats.toggle_show_construction");
		this.model = model;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		for (AbstractAttributeStatisticsModel statModel : model.getOrderedAttributeStatisticsModels()) {
			statModel.setShowConstruction(!statModel.isShowConstruction());
		}
	}

}

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
package com.rapidminer.gui.tour;

import com.rapidminer.gui.tools.dialogs.MessageDialog;


/**
 * This {@link Step} calls a MessageDialog with the complete_Tour.message from GUIBundle and quits
 * your Tour.
 * 
 * @author Thilo Kamradt
 * 
 */
public class FinalStep extends Step {

	protected String key, insert;

	/**
	 * This {@link Step} calls a MessageDialog with the complete_Tour.message from GUIBundle and
	 * quits your Tour.
	 * 
	 * @param explicitTour
	 *            Name of the Tour you have designed to show in the Dialog
	 */
	public FinalStep(String explicitTour) {
		this.key = "complete_Tour";
		this.insert = explicitTour;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.rapidminer.gui.tour.Step#createBubble()
	 */
	@Override
	boolean createBubble() {
		return false;
	}

	@Override
	public void start() {
		MessageDialog tourComplete = new MessageDialog(key, insert);
		tourComplete.setVisible(true);
		this.writeStateToFile();
		this.notifyTourListeners();
	}

	@Override
	protected void stepCanceled() {

	}

	@Override
	public Step[] getPreconditions() {
		return new Step[] {};
	}
}

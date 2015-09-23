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
package com.rapidminer.gui.actions;

import com.rapidminer.gui.OperatorDocViewer;

import java.awt.event.ActionEvent;


/**
 * 
 * @author Miguel Buescher
 * 
 */
public class ShowHelpTextAction extends ToggleAction {

	private final OperatorDocViewer operatorDocViewer;
	private static final long serialVersionUID = -8604443336707110762L;

	public ShowHelpTextAction(OperatorDocViewer operatorDocViewer) {
		super(true, "rapid_doc_bot_importer_offline");
		this.operatorDocViewer = operatorDocViewer;
		setSelected(true);
	}

	@Override
	public void actionToggled(ActionEvent e) {
		this.operatorDocViewer.refresh();
	}
}

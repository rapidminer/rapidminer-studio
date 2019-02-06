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
package com.rapidminer.gui.tools.logging.actions;

import java.awt.event.ActionEvent;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.logging.LogModel;
import com.rapidminer.gui.tools.logging.LogModelRegistry;
import com.rapidminer.gui.tools.logging.LogViewer;


/**
 * Closes the current {@link LogModel} in the {@link LogViewer} if it is closable.
 * 
 * @author Marco Boeck
 */
public class LogCloseAction extends ResourceAction {

	private static final long serialVersionUID = 1L;

	private LogViewer logviewer;

	public LogCloseAction(LogViewer logviewer) {
		super(true, "close_log");
		this.logviewer = logviewer;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		LogModel currentLogModel = this.logviewer.getLogSelectionModel().getCurrentLogModel();
		if (currentLogModel.isClosable()) {
			LogModelRegistry.INSTANCE.unregister(currentLogModel);
		}
	}
}

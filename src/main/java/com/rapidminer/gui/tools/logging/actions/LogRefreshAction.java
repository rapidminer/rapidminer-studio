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
import com.rapidminer.gui.tools.logging.LogViewer;


/**
 * Start the corresponding action.
 * 
 * @author Sabrina Kirstein
 */
public class LogRefreshAction extends ResourceAction {

	private static final long serialVersionUID = -8380073257252178693L;

	private LogViewer loggingViewer;

	public LogRefreshAction(LogViewer loggingViewer) {
		super(true, "refresh_log");
		this.loggingViewer = loggingViewer;
	}

	@Override
	public void loggedActionPerformed(ActionEvent e) {

		this.loggingViewer.performRefresh();
	}
}

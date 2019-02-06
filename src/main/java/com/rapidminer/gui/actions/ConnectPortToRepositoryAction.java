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
package com.rapidminer.gui.actions;

import com.rapidminer.Process;
import com.rapidminer.ProcessContext;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.gui.RepositoryLocationChooser;

import java.awt.event.ActionEvent;


/**
 * Connects a port to a user selected repository location via the {@link ProcessContext}
 * 
 * @author Simon Fischer
 * 
 */
public class ConnectPortToRepositoryAction extends ResourceAction {

	private Port port;

	public ConnectPortToRepositoryAction(Port port) {
		super("connect_port_to_repository_location", port.getName());
		this.port = port;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		Process process = port.getPorts().getOwner().getOperator().getProcess();
		RepositoryLocation processLoc = process.getRepositoryLocation();
		if (processLoc != null) {
			processLoc = processLoc.parent();
		}
		String location;
		if (port instanceof OutputPort) {
			location = RepositoryLocationChooser.selectLocation(processLoc, null, RapidMinerGUI.getMainFrame().getProcessPanel(), true, false,
					false, false, false);
		} else {
			location = RepositoryLocationChooser.selectLocation(processLoc, null, RapidMinerGUI.getMainFrame().getProcessPanel(), true, false,
					false, true, true);
		}
		if (location != null) {
			if (port instanceof OutputPort) {
				int index = process.getRootOperator().getSubprocess(0).getInnerSources().getAllPorts().indexOf(port);
				if (index != -1) {
					process.getContext().setInputRepositoryLocation(index, location);
				}
			} else {
				int index = process.getRootOperator().getSubprocess(0).getInnerSinks().getAllPorts().indexOf(port);
				if (index != -1) {
					process.getContext().setOutputRepositoryLocation(index, location);
				}
			}
		}
	}
}

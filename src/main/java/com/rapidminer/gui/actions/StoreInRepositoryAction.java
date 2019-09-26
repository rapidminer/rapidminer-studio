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

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObject;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.gui.RepositoryLocationChooser;

import java.awt.event.ActionEvent;


/**
 * An action to store IOObjects in the repository.
 * 
 * @author Simon Fischer
 * 
 */
public class StoreInRepositoryAction extends ResourceAction {

	private final IOObject object;
	private RepositoryLocation lastLocation;

	public StoreInRepositoryAction(IOObject object) {
		super(true, "store_in_repository", getI18nName(object));
		this.object = object;
	}

	public StoreInRepositoryAction(IOObject object, RepositoryLocation initialLocation) {
		super(true, "store_in_repository", (getI18nName(object)));
		this.object = object;
		this.lastLocation = initialLocation;
	}

	private static final long serialVersionUID = 1L;

	@Override
	public void loggedActionPerformed(ActionEvent e) {
		String loc = RepositoryLocationChooser.selectLocation(lastLocation, "", RapidMinerGUI.getMainFrame().getExtensionsMenu(), true, false,
				true, true, true, object instanceof ConnectionInformationContainerIOObject ? RepositoryLocationChooser.ONLY_CONNECTIONS : RepositoryLocationChooser.NO_CONNECTIONS);
		if (loc != null) {
			RepositoryLocation location;
			try {
				location = new RepositoryLocation(loc);
			} catch (Exception ex) {
				SwingTools.showSimpleErrorMessage("malformed_rep_location", ex, loc);
				return;
			}
			try {
				// check for overwrite
				if (location.locateEntry() != null && SwingTools.showConfirmDialog("overwrite", ConfirmDialog.YES_NO_OPTION, location) != ConfirmDialog.YES_OPTION) {
					return;
				}
				ProgressThread storePT = new ProgressThread("store_ioobject", false, getI18nName(object)) {

					@Override
					public void run() {
						try {
							RepositoryManager.getInstance(null).store(object, location, null);
							lastLocation = location;
						} catch (RepositoryException ex) {
							SwingTools.showSimpleErrorMessage("cannot_store_obj_at_location", ex, loc);
						}
					}
				};
				storePT.setIndeterminate(true);
				storePT.start();
			} catch (RepositoryException ex) {
				SwingTools.showSimpleErrorMessage("cannot_store_obj_at_location", ex, loc);
			}
		}
	}

	/** @since 9.2.0 */
	private static String getI18nName(IOObject object) {
		return (object instanceof ResultObject) ? ((ResultObject) object).getName() : "result";
	}
}

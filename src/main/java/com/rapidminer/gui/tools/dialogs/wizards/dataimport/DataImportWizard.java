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
package com.rapidminer.gui.tools.dialogs.wizards.dataimport;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.gui.tools.dialogs.wizards.AbstractWizard;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.io.AbstractExampleSource;
import com.rapidminer.repository.Entry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.local.SimpleIOObjectEntry;
import com.rapidminer.tools.ProgressListener;


/**
 * @author Tobias Malbrecht
 * @deprecated was replaced by the {@link com.rapidminer.studio.io.gui.internal.DataImportWizard}
 */
@Deprecated
public class DataImportWizard extends AbstractWizard {

	private static final long serialVersionUID = 6361602131820283501L;

	public DataImportWizard(String key, Object... arguments) {
		super(RapidMinerGUI.getMainFrame(), key, arguments);
	}

	protected boolean transferData(final AbstractExampleSource reader, final String repositoryLocationPath) {
		if (repositoryLocationPath == null) {
			return false;
		}
		final RepositoryLocation location;
		try {
			location = new RepositoryLocation(repositoryLocationPath);
			Entry entry = location.locateEntry();
			if (entry != null) {
				if (entry instanceof SimpleIOObjectEntry) {
					// could overwrite, ask for permission
					if (SwingTools.showConfirmDialog(DataImportWizard.this, "overwrite", ConfirmDialog.YES_NO_OPTION,
							entry.getLocation()) == ConfirmDialog.NO_OPTION) {
						return false;
					}
				} else {
					// cannot overwrite, inform user
					SwingTools.showSimpleErrorMessage(DataImportWizard.this, "cannot_save_data_no_dataentry", "",
							entry.getName());
					return false;
				}
			}
		} catch (Exception e) {
			SwingTools.showSimpleErrorMessage(DataImportWizard.this, "malformed_rep_location", e, repositoryLocationPath);
			return false;
		}

		ProgressThread t = new ProgressThread("import_data", true) {

			@Override
			public void run() {
				ProgressListener l = getProgressListener();
				l.setTotal(100);
				l.setCompleted(10);
				ExampleSet exampleSet;
				try {
					exampleSet = reader.createExampleSet();
				} catch (OperatorException e) {
					SwingTools.showSimpleErrorMessage(DataImportWizard.this, "could not read from access file", e);
					return;
				}

				l.setCompleted(55);
				try {
					RepositoryManager.getInstance(null).store(exampleSet, location, null);
					l.setCompleted(95);
				} catch (RepositoryException ex) {
					SwingTools.showSimpleErrorMessage(DataImportWizard.this, "cannot_store_obj_at_location", ex,
							repositoryLocationPath);
					return;
				}
				l.setCompleted(100);
			}
		};
		t.start();
		return true;
	}

}

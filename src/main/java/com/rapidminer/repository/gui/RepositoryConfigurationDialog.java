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
package com.rapidminer.repository.gui;

import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteRepository;


/**
 * Dialog to configure an existing repository.
 *
 * @author Simon Fischer, Jan Czogalla
 *
 */
public class RepositoryConfigurationDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	private RepositoryConfigurationPanel configurationPanel;
	private Repository repository;

	public RepositoryConfigurationDialog(Repository repository) {
		super(ApplicationFrame.getApplicationFrame(), (repository instanceof RemoteRepository ? "remote" : "")
				+ "repositoryconfigdialog", ModalityType.APPLICATION_MODAL, new Object[0]);
		this.repository = repository;
		JButton okButton = makeOkButton("repository_configuration_dialog.save");
		configurationPanel = repository.makeConfigurationPanel();
		configurationPanel.setOkButton(okButton);
		configurationPanel.configureUIElementsFrom(repository);

		List<AbstractButton> buttons = new LinkedList<>();
		buttons.addAll(configurationPanel.getAdditionalButtons());
		buttons.add(okButton);
		buttons.add(makeCancelButton());

		layoutDefault(configurationPanel.getComponent(), DEFAULT_SIZE, buttons);
	}

	@Override
	protected void ok() {
		ProgressThread repoConfigurePT = new ProgressThread("configure_repository", false, repository.getName()) {

			@Override
			public void run() {
				if (!configurationPanel.configure(repository)) {
					return;
				}
				try {
					repository.refresh();
				} catch (RepositoryException e) {
					SwingTools.showSimpleErrorMessage(RepositoryConfigurationDialog.this, "repository_configuration_dialog.cannot_refresh_folder", e);
				}
			}
		};
		repoConfigurePT.setIndeterminate(true);
		repoConfigurePT.start();
		super.ok();
	}

}

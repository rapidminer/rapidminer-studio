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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;

import com.rapidminer.Process;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.ProgressListener;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;
import com.rapidminer.tools.config.gui.ConfigurableDialog;
import com.rapidminer.tools.container.ComparablePair;
import com.rapidminer.tools.container.Pair;


/**
 * Provides a selection field for {@link Configurable}s.
 *
 * @author Marco Boeck, Sabrina Kirstein
 *
 */
public class ConfigurableValueCellEditor extends AbstractSuggestionBoxValueCellEditor {

	private static final long serialVersionUID = -771727412083431607L;

	/** the type id for the Configurable */
	private String typeId;

	/** the button to open the {@link Configurable} config dialog */
	private JButton configButton;

	/** the flag iff the process is stored */
	private boolean hasSaveDestination = false;

	/** the name of the source ({@link RemoteRepository}) of the process */
	private String source;

	/**
	 * Creates a new {@link ConfigurableValueCellEditor} instance.
	 */
	public ConfigurableValueCellEditor(final ParameterTypeConfigurable type) {
		super(type);
		this.typeId = type.getTypeId();
		if (!ConfigurationManager.getInstance().hasTypeId(typeId)) {
			throw new IllegalArgumentException("Unknown configurable type: " + typeId);
		}

		Process openProcess = RapidMinerGUI.getMainFrame().getProcess();
		hasSaveDestination = openProcess.hasSaveDestination();
		// mark that the source is not defined, because the process is not saved
		if (!hasSaveDestination) {
			source = null;
		} else {

			/** the repository of the process */
			Repository processRepository = null;
			try {
				processRepository = openProcess.getRepositoryLocation() != null
						? openProcess.getRepositoryLocation().getRepository() : null;
			} catch (RepositoryException e1) {
				// nothing to do
			}

			// check the type of the remote repository
			if (processRepository != null && processRepository instanceof RemoteRepository) {
				// remember the source of the process location
				source = processRepository.getName();
			} else {
				// for processes saved in local repositories,cloud repositories or individual
				// repositories
				// show all local connections
				source = ConfigurationManager.RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL;
			}

		}

		configButton = new JButton(new ResourceActionAdapter(true, "configurable_editor") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				ConfigurableDialog dialog = new ConfigurableDialog();
				dialog.selectConfigurable(String.valueOf(getCellEditorValue()), typeId);
				dialog.setVisible(true);
			}
		});
		configButton.setIcon(SwingTools
				.createIcon("16/" + ConfigurationManager.getInstance().getAbstractConfigurator(typeId).getIconName()));
		addConfigureButton(configButton);
	}

	@Override
	public List<Object> getSuggestions(Operator operator, ProgressListener progressListener) {
		List<ComparablePair<String, String>> allConfigurableNamesAndSources = ConfigurationManager.getInstance()
				.getAllConfigurableNamesAndSources(typeId);
		List<Object> list = new LinkedList<>();
		for (Pair<String, String> namesAndSources : allConfigurableNamesAndSources) {
			// if the process is not stored yet, show all configurations
			if (source == null || source.equals(ConfigurationManager.RM_SERVER_CONFIGURATION_SOURCE_NAME_LOCAL)) {
				if (!list.contains(namesAndSources.getFirst())) {
					list.add(namesAndSources.getFirst());
				}
			} else {
				// show only the configurations of the source
				if (source.equals(namesAndSources.getSecond())) {
					list.add(namesAndSources.getFirst());
				}
			}
		}
		return list;
	}

}

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
package com.rapidminer.tools.config.gui;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.io.process.XMLTools;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.internal.remote.ResponseContainer;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.XMLException;
import com.rapidminer.tools.cipher.KeyGeneratorTool;
import com.rapidminer.tools.config.AbstractConfigurable;
import com.rapidminer.tools.config.AbstractConfigurator;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.actions.ActionResult;
import com.rapidminer.tools.config.actions.ActionResult.Result;
import com.rapidminer.tools.config.actions.ConfigurableAction;
import com.rapidminer.tools.config.actions.SimpleActionResult;
import com.rapidminer.tools.config.gui.model.ConfigurableModel;
import com.rapidminer.tools.container.Pair;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


/**
 * The controller for the {@link ConfigurableDialog} view.
 *
 * @author Marco Boeck, Sabrina Kirstein
 *
 */
public class ConfigurableController {

	/** the view this controller manages */
	private ConfigurableDialog view;

	/** the model behind the view */
	private ConfigurableModel model;

	/** key used by server */
	private static final Key serverPublicKey = new Key(){

		@Override
		public String getAlgorithm() {
			return "PLAIN";
		}

		@Override
		public String getFormat() {
			return null;
		}

		@Override
		public byte[] getEncoded() {
			return null;
		}
	};
	/**
	 * Creates a new {@link ConfigurableController} instance.
	 *
	 * @param view
	 * @param model
	 */
	public ConfigurableController(ConfigurableDialog view, ConfigurableModel model) {
		this.view = view;
		this.model = model;
	}

	/**
	 * Checks if the given {@link Configurable} is in a valid state. If it is not, returns an error
	 * {@link ActionResult}.
	 *
	 * @return
	 */
	protected ActionResult checkConfigurableValidState(Configurable configurable) {

		// Get parameter types based on Configurator implementation
		AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
		        .getAbstractConfigurator(configurable.getTypeId());
		ParameterHandler parameterHandler = configurator.getParameterHandler(configurable);
		List<ParameterType> parameterTypes = configurator.getParameterTypes(parameterHandler);

		for (ParameterType type : parameterTypes) {
			if (!type.isOptional()) {
				String value = configurable.getParameter(type.getKey());
				if ((value == null || "".equals(value.trim())) && type.getDefaultValue() == null) {
					return new SimpleActionResult(
					        I18N.getGUIMessage("gui.dialog.configurable_dialog.error.missing_value.label", type.getKey()),
					        Result.FAILURE);
				}
			}
		}

		return new SimpleActionResult(null, Result.SUCCESS);
	}

	/**
	 * Checks if the given name is unique for the specified type.
	 *
	 * @param typeId
	 * @param name
	 * @return <code>true</code> if the name is unique; <code>false</code> otherwise
	 */
	protected boolean isNameUniqueForType(String typeId, String name) {
		return !model.getListOfUniqueNamesForType(typeId).contains(name);
	}

	/**
	 * Removes the specified {@link Configurable}.
	 *
	 * @param configurable
	 */
	protected void removeConfigurable(Configurable configurable) {
		model.removeConfigurable(configurable);

		// Update ParameterHandler
		ConfigurationManager.getInstance().getAbstractConfigurator(configurable.getTypeId())
		        .removeCachedParameterHandler(configurable);
	}

	/**
	 * Creates a {@link Configurable} of the specified type with the given name and the given remote
	 * repository.
	 *
	 * @param typeId
	 *            type of the configurable
	 * @param name
	 *            name of the configurable
	 * @param source
	 *            {@link RemoteRepository} (source) of the configurable, can be null in case of
	 *            local connections
	 */
	protected void addConfigurable(String typeId, String name, RemoteRepository source) {
		try {

			Configurable newConfigurable = ConfigurationManager.getInstance().createWithoutRegistering(typeId, name, source);
			model.addConfigurable(newConfigurable);

		} catch (ConfigurationException e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.config.gui.ConfigurableController.unknown_type",
			        typeId);
		}
	}

	/**
	 * Renames the specified {@link Configurable} to the new name.
	 *
	 * @param configurable
	 * @param newName
	 */
	protected void renameConfigurable(Configurable configurable, String newName) {
		String oldConfigurableName = configurable.getName();

		//Update permitted groups
		if (configurable.getSource() != null) {
			Set<String> permittedGroups = ConfigurationManager.getInstance().getPermittedGroupsForConfigurable(configurable);
			//Clear old entries
			ConfigurationManager.getInstance().setPermittedGroupsForConfigurable(configurable, Collections.emptySet());
			configurable.setName(newName);
			ConfigurationManager.getInstance().setPermittedGroupsForConfigurable(configurable, permittedGroups);
		} else {
			configurable.setName(newName);
		}

		// Update ParameterHandler
		ConfigurationManager.getInstance().getAbstractConfigurator(configurable.getTypeId())
		        .reregisterCachedParameterHandler(configurable, oldConfigurableName);
	}

	/**
	 * Executes the specified {@link ConfigurableAction} in a separate {@link ProgressThread} and
	 * displays the {@link ActionResult} in the view.
	 *
	 * @param action
	 */
	protected void executeConfigurableAction(final ConfigurableAction action) {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null!");
		}

		// execute action in own ProgressThread to avoid GUI blocking
		ProgressThread actionThread = new ProgressThread("configurable_action") {

			@Override
			public void run() {
				// show user we are doing something
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						view.displayActionIsRunning();
					}
				});

				ActionResult result = null;
				try {
					result = action.doWork();
				} catch (Exception e) {
					result = new SimpleActionResult(e.getMessage(), Result.FAILURE);
				}
				final ActionResult finalResult = result;

				// show user results
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						view.displayResult(finalResult);
					}
				});

			}
		};
		actionThread.start();
	}

	/**
	 * Saves the {@link Parameters} for the given {@link Configurable}.
	 *
	 * @param configurable
	 * @param parameters
	 */
	protected void saveConfigurable(Configurable configurable, Parameters parameters) {
		for (String key : parameters.getKeys()) {
			try {
				String value = parameters.getParameter(key);

				// If we are an AbstractConfigurable, toString must be invoked. This is necessary
				// e.g. to save passwords encrypted.
				if (configurable instanceof AbstractConfigurable) {
					AbstractConfigurator<? extends Configurable> configurator = ConfigurationManager.getInstance()
							.getAbstractConfigurator(configurable.getTypeId());
					for (ParameterType type : configurator
							.getParameterTypes(configurator.getParameterHandler(configurable))) {
						if (type.getKey().equals(key)) {
							value = type.toString(value);
							break;
						}
					}
				}
				configurable.setParameter(key, value);
			} catch (UndefinedParameterError e) {
				LogService.getRoot().log(Level.FINE,
				        "com.rapidminer.tools.config.gui.ConfigurableController.save_undef_param",
				        new Object[] { key, configurable.getName() });
			}
		}
	}

	/**
	 * Save the changes the user made to the configurables.
	 */
	protected void save() {

		RemoteRepository repository = model.getSource();

		// if this is locally changed, save it in a file
		if (repository == null) {
			ConfigurationManager.getInstance().replaceConfigurables(model.getConfigurables(), null);
			try {
				ConfigurationManager.getInstance().saveConfiguration();
			} catch (ConfigurationException e) {
				LogService.getRoot().log(Level.SEVERE,
				        "com.rapidminer.tools.config.gui.ConfigurableController.error_on_save", e);
				view.displaySaveErrorDialog();
			}
		} else {
			// else send the configurables as an XML to the server

			// check if the repository has been connected before
			if (repository.isConnected()) {

				// check if the repository is still connected
				try {
					repository.resetContentManager();
				} catch (RepositoryException | PasswordInputCanceledException e) {
					// revert parameter and name changes
					try {
						model.resetConfigurables();
						model.resetConnection();
					} catch (RepositoryException e1) {
						// could not connect to repository
					}
					view.collapseRemoteTaskPane(repository.getName());
					view.displayConnectionErrorDialog(repository.getName(), repository.getBaseUrl().toString());
					return;
				}

				if (model.isEditingPossible() && model.hasAdminRights()) {

					// upload the configurables for each typeId
					for (String typeId : ConfigurationManager.getInstance().getAllTypeIds()) {

						try {
							Document xml = ConfigurationManager.getInstance().getConfigurablesAsXMLAndChangeEncryption(typeId,
									model.getConfigurables(), repository.getName(), KeyGeneratorTool.getUserKey(), serverPublicKey);

							ResponseContainer response = repository.getClient().storeConfigurationType(typeId, xml);

							// check response code for result
							if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
								// something went wrong

								if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
									LogService.getRoot().log(Level.INFO, () -> typeId + ": " + response.getResponseMessage());

								} else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD) {
									LogService.log(LogService.getRoot(), Level.WARNING, null,
											"com.rapidminer.tools.config.gui.ConfigurableController.uploading_configuration_error_server_not_up_to_date",
											typeId, repository.getName(), response.getResponseMessage());
									view.displaySaveUploadErrorDialogServerNotUpToDate(repository.getName());
									// revert parameter and name changes
									revertChanges();
									break;
								} else {

									LogService.log(LogService.getRoot(), Level.WARNING,
											new Exception(response.getResponseMessage()),
											"com.rapidminer.tools.config.gui.ConfigurableController.uploading_configuration_error",
											typeId, repository.getName(), response.getResponseMessage());
									view.displaySaveUploadErrorDialog(typeId, repository.getName(),
											repository.getBaseUrl().toString());
									// revert parameter and name changes
									revertChanges();
									break;
								}
							} else {

								// all should be fine
								Document newIdsDoc = XMLTools.parse(response.getInputStream());
								List<Pair<Integer, String>> newIds = ConfigurationManager.newIdsFromXML(newIdsDoc);

								// replace ids of new created configurables (with id -1)
								for (Configurable config : model.getConfigurables()) {
									if (config.getTypeId().equals(typeId) && config.getId() == -1) {
										for (Pair<Integer, String> pair : newIds) {
											if (pair.getSecond().equals(config.getName())) {
												// set the new id (given by the server)
												// instead of -1
												config.setId(pair.getFirst());
											}
										}
									}
								}

								// store the configurables permanently in the
								// ConfigurationManager
								ConfigurationManager.getInstance().replaceConfigurables(model.getConfigurables(),
										repository.getName());

								LogService.getRoot().log(Level.INFO,
										"com.rapidminer.tools.config.gui.ConfigurableController.uploading_configuration",
										typeId);
							}
						} catch (IOException | RepositoryException | ConfigurationException | SAXException | XMLException e) {
							// revert parameter and name changes
							revertChanges();
							LogService.log(LogService.getRoot(), Level.WARNING, e,
									"com.rapidminer.tools.config.gui.ConfigurableController.uploading_configuration_error",
									typeId, repository.getName(), e.toString());
							view.displaySaveUploadErrorDialog(typeId, repository.getName(),
									repository.getBaseUrl().toString());
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Reverts all parameter changes the user made to the {@link Configurable}s.
	 */
	protected void revertChanges() {
		// revert parameters
		Map<Configurable, Map<String, String>> backupParameters = model.getBackupParameters();
		for (Configurable configurable : backupParameters.keySet()) {
			// find the same Configurable and revert its parameters (which may have been altered
			// by the user) to their original values
			Map<String, String> backupMap = backupParameters.get(configurable);
			for (String key : backupMap.keySet()) {
				configurable.setParameter(key, backupMap.get(key));
			}
		}

		// revert names
		Map<Configurable, String> backupNames = model.getBackupNames();
		for (Configurable configurable : backupNames.keySet()) {
			configurable.setName(backupNames.get(configurable));
		}

		// revert permitted user groups
		Map<Configurable, Set<String>> backupPermittedUserGroups = model.getBackupPermittedUserGroups();
		for (Configurable configurable : backupPermittedUserGroups.keySet()) {
			ConfigurationManager.getInstance().setPermittedGroupsForConfigurable(configurable,
			        backupPermittedUserGroups.get(configurable));
		}
	}

	/**
	 * @return the model for the controller.
	 */
	protected ConfigurableModel getModel() {
		return model;
	}

	/**
	 * @return the view for the controller
	 */
	protected ConfigurableDialog getView() {
		return view;
	}

}

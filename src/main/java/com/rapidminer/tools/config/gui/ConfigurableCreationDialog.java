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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.TransparentGlassPanePanel;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.config.AbstractConfigurator;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.gui.event.ConfigurableEvent;
import com.rapidminer.tools.config.gui.event.ConfigurableModelEventListener;
import com.rapidminer.tools.config.gui.renderer.ConfigurationRenderer;
import com.rapidminer.tools.container.Pair;


/**
 * This dialog can be used to create new {@link Configurable}s by specifying name and type.
 *
 * @author Marcel Michel, Sabrina Kirstein
 *
 */
public class ConfigurableCreationDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	/** dimension for fields */
	private static final Dimension FIELD_SIZE = new Dimension(250, 30);

	/** dimension for server combo */
	private static final Dimension SERVER_COMBO_SIZE = new Dimension(150, 30);

	/** dimension for text areas */
	private static final Dimension AREA_SIZE = new Dimension(250, 60);

	/** icon displayed in case of fetching configuration types */
	private static final ImageIcon WAITING_ICON = SwingTools.createIcon("48/rm_logo_loading.gif");

	/** the controller behind the creation dialog */
	private ConfigurableController controller;

	/** the controller behind the local configurables */
	private ConfigurableController localController;

	/** the controller behind the remote configurables */
	private Map<String, ConfigurableController> remoteControllers;

	/** button to create a new configurable */
	private JButton addButton;

	/** action to add a configurable as admin */
	private ResourceAction addAction = new ResourceActionAdapter(false, "configurable_creation_dialog.ok") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			if (validateInput()) {

				source = localRadioButton.isSelected() ? null : remoteControllers
						.get(serverCombo.getSelectedItem().toString()).getModel().getSource();

				controller.addConfigurable(((AbstractConfigurator<?>) configuratorCombo.getSelectedItem()).getTypeId(),
						configurationName.getText(), source);

				ConfigurableDialog.isAddingDialogOpened = false;
				ConfigurableCreationDialog.this.dispose();

			}
		}

	};

	/**
	 * action to login as admin and create a new configurable
	 */
	private ResourceAction loginAndAddAction = new ResourceActionAdapter(false, "configurable_creation_dialog_not_admin.ok") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {

			if (validateInput()) {
				source = localRadioButton.isSelected() ? null : remoteControllers
						.get(serverCombo.getSelectedItem().toString()).getModel().getSource();

				if (source != null) {
					// check admin credentials
					ConfigurableAdminPasswordDialog passwordDialog = new ConfigurableAdminPasswordDialog(
							ConfigurableCreationDialog.this, source);
					passwordDialog.setVisible(true);

					if (passwordDialog.wasConfirmed()) {
						String username = passwordDialog.getUserName();
						char[] password = passwordDialog.getPassword();
						// set the new connection as admin
						remoteControllers.get(source.getName()).getModel().getSource().setUsername(username);
						remoteControllers.get(source.getName()).getModel().getSource().setPassword(password);
						// check if the credentials are correct
						remoteControllers.get(source.getName()).getModel().checkForAdminRights();
						// and refresh the configurables
						if (remoteControllers.get(source.getName()).getModel().hasAdminRights()) {
							// save the add request
							requestedToAddConfigurable.put(source.getName(), new Pair<>(
									((AbstractConfigurator<?>) configuratorCombo.getSelectedItem()).getTypeId(),
									configurationName.getText()));
							// refresh the configurables
							owner.refreshConfigurables(source.getName());

							// show loading icon and wait for the refreshed connections
							loadingGlassPane.setVisible(true);
							outerPanel.setEnabled(false);
						}
					}
				}
			}
		}
	};

	/** storing the request to add a new configurable, if the connections need to be refreshed */
	private Map<String, Pair<String, String>> requestedToAddConfigurable;

	/** textfield for the configuration name */
	private JTextField configurationName;

	/** combobox for the available configurators */
	private JComboBox<AbstractConfigurator<?>> configuratorCombo;

	/** radio button for remote connections */
	private JRadioButton serverRadioButton;

	/** radio button for local connections */
	private JRadioButton localRadioButton;

	/** combobox for the available servers */
	private JComboBox<String> serverCombo;

	/** label for error reporting */
	private JLabel errorLabel;

	/** repository manager used to get {@link RemoteRepository}s */
	private RepositoryManager repoManager = RepositoryManager.getInstance(null);

	/**
	 * if a {@link Configurable} was selected in the {@link ConfigurableDialog} when this dialog is
	 * opened, this is the pre-selected source in this dialog
	 */
	private RemoteRepository source;

	/**
	 * if the {@link ConfigurableDialog} was opened from within the Parameters View of an operator,
	 * this is the pre-selected type id in this {@link ConfigurableCreationDialog}
	 */
	private String preferredTypeId;

	private JTextArea descriptionArea;

	/** glass pane showing a loading icon if connection to servers needs to be established */
	private JPanel loadingGlassPane;

	private JPanel outerPanel;

	/** owner of the dialog, necessary to update configurables */
	private ConfigurableDialog owner;

	/** listening to events, when configurables have been reloaded */
	private final ConfigurableModelEventListener modelEventListener = new ConfigurableModelEventListener() {

		@Override
		public void modelChanged(ConfigurableEvent e) {

			if (e.getEventType().equals(ConfigurableEvent.EventType.LOADED_FROM_REPOSITORY)) {

				// check the add request
				for (String serverName : requestedToAddConfigurable.keySet()) {

					RemoteRepository eventSource = e.getConfigurable().getSource();
					if (eventSource != null) {

						if (serverName.equals(eventSource.getName())) {

							Pair<String, String> request = requestedToAddConfigurable.get(serverName);
							String typeId = request.getFirst();
							String name = request.getSecond();

							// check if the input is valid for the admin connections
							boolean configurableChecked = validateInput();

							if (configurableChecked) {
								// if the configurable is valid, add it
								remoteControllers.get(eventSource.getName()).addConfigurable(typeId, name, eventSource);
								ConfigurableDialog.isAddingDialogOpened = false;
								ConfigurableCreationDialog.this.dispose();
							} else {
								// if the configurable is invalid, show the dialog again
								requestedToAddConfigurable.clear();
								loadingGlassPane.setVisible(false);
								outerPanel.setEnabled(true);
								// and set the action to the simple add action, as we are admin here
								addButton.setAction(addAction);
								addButton.setEnabled(false);
								outerPanel.repaint();
								return;
							}
						}
					}
				}
				requestedToAddConfigurable.clear();
			}
		}
	};

	/**
	 * Creates a new {@link ConfigurableCreationDialog} instance.
	 */
	public ConfigurableCreationDialog(ConfigurableDialog owner, ConfigurableController localController,
			Map<String, ConfigurableController> remoteControllers, RemoteRepository source, String preferredTypeId) {
		super(owner);
		this.owner = owner;
		this.source = source;
		this.localController = localController;
		this.remoteControllers = remoteControllers;
		for (ConfigurableController controller : remoteControllers.values()) {
			controller.getModel().registerEventListener(modelEventListener);
		}
		this.preferredTypeId = preferredTypeId;

		initGUI();
		fillGUI();
	}

	/**
	 * Inits the GUI.
	 */
	private void initGUI() {
		outerPanel = new JPanel();

		// disable the dialog and show the loading symbol
		JLayer<JPanel> layer = new JLayer<JPanel>(outerPanel);

		loadingGlassPane = new TransparentGlassPanePanel(WAITING_ICON,
				I18N.getGUILabel("configurable_creation_dialog.connection_to_remotes"), getBackground(), 0.9f);
		layer.setGlassPane(loadingGlassPane);
		loadingGlassPane.setVisible(true);
		outerPanel.setEnabled(false);

		outerPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;

		List<AbstractConfigurator<?>> configurators = new LinkedList<>();

		// Configuration name label
		JLabel configurationNameLabel = new ResourceLabel("configurable_creation_dialog.configurable_name");

		gbc.gridy += 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.insets = new Insets(15, 10, 10, 5);
		gbc.anchor = GridBagConstraints.WEST;
		outerPanel.add(configurationNameLabel, gbc);

		// Configuration name textField
		configurationName = new JTextField();
		configurationNameLabel.setLabelFor(configurationName);
		configurationName.setMinimumSize(FIELD_SIZE);
		configurationName.setPreferredSize(FIELD_SIZE);
		configurationName.setText("NewConnection");
		configurationName.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.configurable_creation_dialog.configurable_name.tip"));
		configurationName.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				validateInput();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				validateInput();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				validateInput();
			}
		});
		gbc.gridx += 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridwidth = 3;
		outerPanel.add(configurationName, gbc);

		// Choice between local and remote connection
		// Configuration location label
		JLabel configurationLocationLabel = new ResourceLabel("configurable_creation_dialog.configurable_location");
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(15, 10, 10, 5);
		outerPanel.add(configurationLocationLabel, gbc);

		localRadioButton = new JRadioButton(I18N.getGUILabel("configurable_creation_dialog.local_source.label"), true);
		localRadioButton.setEnabled(true);
		localRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				if (localRadioButton.isSelected()) {
					controller = localController;
					source = null;
					addButton.setAction(addAction);
					updateConfiguratorComboBox();
					validateInput();
				}
			}
		});
		serverRadioButton = new JRadioButton();
		serverRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// change the controller and actions if necessary
				if (serverRadioButton.isSelected()) {
					controller = remoteControllers.get(serverCombo.getSelectedItem().toString());
					try {
						source = (RemoteRepository) repoManager.getRepository(serverCombo.getSelectedItem().toString());
					} catch (RepositoryException e1) {
					}
					if (!controller.getModel().hasAdminRights()) {
						addButton.setAction(loginAndAddAction);
					} else {
						addButton.setAction(addAction);
					}
					// update and validate
					updateConfiguratorComboBox();
					validateInput();
				}
			}
		});

		ButtonGroup group = new ButtonGroup();
		group.add(localRadioButton);
		group.add(serverRadioButton);
		gbc.gridx += 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0.4;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 0);
		outerPanel.add(localRadioButton, gbc);

		gbc.gridx += 1;
		gbc.weightx = 0.1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 0, 5, 0);
		outerPanel.add(serverRadioButton, gbc);

		serverCombo = new JComboBox<String>(new String[0]);
		serverCombo.setMinimumSize(SERVER_COMBO_SIZE);
		serverCombo.setPreferredSize(SERVER_COMBO_SIZE);
		serverCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				// change the controller and actions if necessary
				if (!serverRadioButton.isSelected()) {
					serverRadioButton.setSelected(true);
				}

				controller = remoteControllers.get(serverCombo.getSelectedItem().toString());
				try {
					source = (RemoteRepository) repoManager.getRepository(serverCombo.getSelectedItem().toString());
				} catch (RepositoryException e1) {
				}
				if (!controller.getModel().hasAdminRights()) {
					addButton.setAction(loginAndAddAction);
				} else {
					addButton.setAction(addAction);
				}
				// update and validate
				updateConfiguratorComboBox();
				validateInput();
			}
		});

		gbc.gridx += 1;
		gbc.weightx = 0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 0, 5, 10);
		outerPanel.add(serverCombo, gbc);

		// Type selection label
		JLabel configuratorLabel = new ResourceLabel("configurable_creation_dialog.configurators");
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridwidth = 1;
		outerPanel.add(configuratorLabel, gbc);

		// Type comboBox
		configuratorCombo = new JComboBox<>(new Vector<>(configurators));
		configuratorLabel.setLabelFor(configuratorCombo);
		configuratorCombo.setRenderer(new ConfigurationRenderer());
		configuratorCombo.setMinimumSize(FIELD_SIZE);
		configuratorCombo.setPreferredSize(FIELD_SIZE);
		configuratorCombo.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.label.configurable_creation_dialog.configurators.tip"));
		gbc.gridx += 1;
		gbc.gridwidth = 3;
		outerPanel.add(configuratorCombo, gbc);

		descriptionArea = new JTextArea();
		descriptionArea.setLineWrap(true);
		descriptionArea.setEditable(false);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setBorder(null);
		// getBackground does not work
		descriptionArea.setBackground(UIManager.getColor("Panel.background"));
		gbc.gridy += 1;
		gbc.weightx = 1.0;
		gbc.weighty = 0.7;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.CENTER;

		// Listener for the comboBox to update the description
		configuratorCombo.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					descriptionArea.setText(((AbstractConfigurator<?>) e.getItem()).getDescription());
					validateInput();
				}

			}
		});

		AbstractConfigurator<?> currentConfigurator = (AbstractConfigurator<?>) configuratorCombo.getSelectedItem();
		if (currentConfigurator != null) {
			descriptionArea.setText(currentConfigurator.getDescription());
		}
		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(descriptionArea);
		scrollPane.setMinimumSize(AREA_SIZE);
		scrollPane.setPreferredSize(AREA_SIZE);
		scrollPane.setMaximumSize(AREA_SIZE);
		scrollPane.setBorder(null);
		outerPanel.add(scrollPane, gbc);

		// Error reporting
		errorLabel = new JLabel();
		errorLabel.setForeground(Color.RED);
		errorLabel.setMinimumSize(FIELD_SIZE);
		errorLabel.setPreferredSize(FIELD_SIZE);
		errorLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		errorLabel.setHorizontalAlignment(SwingConstants.LEFT);

		gbc.gridwidth = 4;
		gbc.gridy += 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.weighty = 0.3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.SOUTH;
		gbc.insets = new Insets(5, 10, 5, 10);
		outerPanel.add(errorLabel, gbc);

		// Ok and Cancel button
		gbc.gridy += 1;
		gbc.gridx = 1;
		gbc.gridwidth = 3;
		gbc.insets = new Insets(0, 0, 5, 5);
		outerPanel.add(createButtons(), gbc);

		// dialog set up
		add(layer);
		setTitle(I18N.getMessage(I18N.getGUIBundle(), "gui.action.configurable_creation_dialog.title"));
		// setPreferredSize(DIALOG_SIZE);
		setResizable(false);
		setModal(true);
		pack();
		setLocationRelativeTo(ApplicationFrame.getApplicationFrame());
	}

	/**
	 * fill the initialized UI in the background (connection to servers could be necessary)
	 */
	private void fillGUI() {

		ProgressThread pt = new ProgressThread("loading_information") {

			@Override
			public void run() {

				getProgressListener().setTotal(100);
				getProgressListener().setCompleted(10);

				// check if the source can be edited
				if (source != null) {
					if (source.isConnected() && remoteControllers.get(source.getName()).getModel().isEditingPossible()) {
						controller = remoteControllers.get(source.getName());
					} else {
						controller = localController;
						source = null;
					}
				} else {
					controller = localController;
				}

				List<RemoteRepository> remotes = repoManager.getRemoteRepositories();

				// fill list of servers
				final List<String> serverNames = new LinkedList<>();
				for (RemoteRepository repo : remotes) {
					if (repo.isConnected() && remoteControllers.get(repo.getName()).getModel().isEditingPossible()
							&& repo.getTypeIds() != null && !repo.getTypeIds().isEmpty()) {
						serverNames.add(repo.getName());
					}
				}
				final String[] serverNamesArray = new String[serverNames.size()];
				serverNames.toArray(serverNamesArray);

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						AbstractConfigurator<?> currentConfigurator = (AbstractConfigurator<?>) configuratorCombo
								.getSelectedItem();
						if (currentConfigurator != null) {
							descriptionArea.setText(currentConfigurator.getDescription());
						}

						serverCombo.setModel(new DefaultComboBoxModel<String>(serverNamesArray));

						// check what needs to be enabled / disabled
						if (serverNames.isEmpty()) {

							serverRadioButton.setEnabled(false);
							serverCombo.setEnabled(false);
							controller = localController;
							source = null;
						} else {

							if (source != null) {
								serverRadioButton.setEnabled(true);
								serverRadioButton.setSelected(true);
								serverCombo.setEnabled(true);
								int newSelection = 0;
								// preselect the given source
								for (int i = 0; i < serverCombo.getItemCount(); i++) {
									String serverName = serverCombo.getItemAt(i);
									if (serverName.equals(source.getName())) {
										newSelection = i;
										break;
									}
								}
								serverCombo.setSelectedIndex(newSelection);

								if (!remoteControllers.get(source.getName()).getModel().hasAdminRights()) {
									// if the user has no admin rights, show the login and add
									// button
									addButton.setAction(loginAndAddAction);
								}
							}
						}

						if (source == null) {
							localRadioButton.setSelected(true);
						}

						// update configuration types
						updateConfiguratorComboBox();

						// preselect the given preferred type id
						if (preferredTypeId != null) {
							for (int i = 0; i < configuratorCombo.getItemCount(); i++) {
								if (configuratorCombo.getItemAt(i) != null) {
									if (configuratorCombo.getItemAt(i).getTypeId().equals(preferredTypeId)) {
										configuratorCombo.setSelectedIndex(i);
										break;
									}
								}
							}
						}

						// initial check
						validateInput();

						// show the complete dialog
						loadingGlassPane.setVisible(false);
						outerPanel.setEnabled(true);
						outerPanel.repaint();
					}
				});

				getProgressListener().setCompleted(100);
				getProgressListener().complete();

			}
		};
		pt.start();
	}

	/**
	 * Validates the {@link #configurationName} text field and displays errors inside the
	 * {@link #errorLabel}.
	 *
	 * @return Returns true if no errors are detected otherwise false
	 */
	private boolean validateInput() {
		AbstractConfigurator<?> currentConfigurator = (AbstractConfigurator<?>) configuratorCombo.getSelectedItem();
		String name = configurationName.getText();
		boolean configuratorExists = currentConfigurator != null;
		if (errorLabel != null) {
			addButton.setEnabled(false);
			if (configuratorExists) {
				boolean isUnique = controller.isNameUniqueForType(currentConfigurator.getTypeId(), name);
				if ("".equals(name.trim())) {
					errorLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
							"gui.dialog.error.configurable_creation_dialog.invalid_name.message"));
					return false;
				}
				if (!isUnique) {
					errorLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
							"gui.dialog.error.configurable_creation_dialog.invalid_duplicate_name.message"));
					return false;
				}
			} else {
				errorLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.error.configurable_creation_dialog.invalid_configuration_type.message"));
				return false;
			}
			errorLabel.setText("");
		}
		if (!addButton.isEnabled()) {
			addButton.setEnabled(true);
		}
		return true;
	}

	/**
	 * update the content of the configurator combo box depending on the available configurators
	 */
	private void updateConfiguratorComboBox() {

		List<AbstractConfigurator<?>> configurators = new LinkedList<>();
		for (String typeId : ConfigurationManager.getInstance().getAllTypeIds()) {
			// for local connections or if the server did not return a list of type ids, use all
			// connection types
			// otherwise use the typeIds given by the server
			if (source == null || source.getTypeIds() != null && source.getTypeIds().contains(typeId)) {
				AbstractConfigurator<?> c = ConfigurationManager.getInstance().getAbstractConfigurator(typeId);
				configurators.add(c);
			}
		}
		configuratorCombo.setModel(new DefaultComboBoxModel<>(configurators.toArray(new AbstractConfigurator<?>[0])));

		AbstractConfigurator<?> currentConfigurator = (AbstractConfigurator<?>) configuratorCombo.getSelectedItem();
		if (currentConfigurator != null) {
			descriptionArea.setText(currentConfigurator.getDescription());
		} else {
			descriptionArea.setText("");
		}

		// check what needs to be enabled / disabled
		if (configurators.isEmpty()) {
			configuratorCombo.setEnabled(false);
			addButton.setEnabled(false);
		} else {
			if (!configuratorCombo.isEnabled()) {
				configuratorCombo.setEnabled(true);
				addButton.setEnabled(true);
			}
		}
	}

	/**
	 * Creates and returns the button component.
	 *
	 * @return
	 */
	private Component createButtons() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints buttonGBC = new GridBagConstraints();

		buttonGBC.gridx = 0;
		buttonGBC.weightx = 1.0;
		buttonGBC.fill = GridBagConstraints.HORIZONTAL;
		buttonGBC.insets = new Insets(5, 5, 5, 5);
		buttonPanel.add(Box.createHorizontalGlue(), buttonGBC);

		addButton = new JButton(addAction);
		requestedToAddConfigurable = new HashMap<>();

		buttonGBC.gridx += 1;
		buttonGBC.weightx = 0.0;
		buttonPanel.add(addButton, buttonGBC);

		ResourceAction cancelAction = new ResourceAction(false, "configurable_creation_dialog.cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				requestedToAddConfigurable.clear();
				ConfigurableDialog.isAddingDialogOpened = false;
				ConfigurableCreationDialog.this.dispose();
			}

		};

		JButton removeButton = new JButton(cancelAction);

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

		buttonGBC.gridx += 1;
		buttonPanel.add(removeButton, buttonGBC);

		return buttonPanel;
	}

	@Override
	public void setVisible(boolean b) {
		if (b) {
			validateInput();
		}
		super.setVisible(b);
	}
}

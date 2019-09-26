/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.connection.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.gui.actions.SaveConnectionAction;
import com.rapidminer.connection.gui.dto.ConnectionInformationHolder;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.icons.IconFactory;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.repository.Folder;
import com.rapidminer.repository.MalformedRepositoryLocationException;
import com.rapidminer.repository.Repository;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.repository.RepositoryManager;
import com.rapidminer.repository.RepositoryTools;
import com.rapidminer.repository.local.LocalRepository;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * Dialog when a new connection should be created.
 *
 * @author Marco Boeck
 * @since 9.3.0
 */
public class ConnectionCreationDialog extends JDialog {

	/**
	 * Constants for temporary hack that allows to start with drivers tab when creating new database connection
	 * Are the same as constants in com.rapidminer.extension.jdbc.connection.JDBCConnectionHandler.
	 */
	private static final String JDBC_CONNECTORS_JDBC = "jdbc_connectors:jdbc";

	private Callable<ConnectionInformation> ciCreator;
	private Runnable finalAction;

	private enum Status {
		NO_STATUS,

		INFO,

		WORKING,

		WARNING
	}

	private static final String I18N_KEY = "connection.create_new_connection";
	private static final ImageIcon WARNING_ICON = SwingTools.createIcon("16/" + I18N.getGUILabel("connection.warning.icon"));
	private static final ImageIcon INFORMATION_ICON = SwingTools.createIcon("16/" + I18N.getGUILabel("connection.information.icon"));
	private static final ImageIcon WORKING_ICON = SwingTools.createIcon("16/" + I18N.getGUILabel("connection.working.icon"));

	private JTextField nameField;
	private JComboBox<String> typeBox;
	private JComboBox<Repository> repositoryBox;
	private JLabel nameErrorLabel;
	private JLabel typeErrorLabel;
	private JLabel repositoryErrorLabel;
	private JLabel statusIcon;
	private JTextArea statusLabel;
	private JButton nextButton;
	private AtomicBoolean cancelled;

	/**
	 * Creates a new dialog instance.
	 *
	 * @param parent
	 * 		the parent, can be {@code null}
	 * @param repository
	 * 		the repository which should be preselected; can be {@code null}
	 */
	public ConnectionCreationDialog(Window parent, Repository repository) {
		super(parent, I18N.getGUIMessage("gui.dialog." + I18N_KEY + ".title"), Dialog.ModalityType.APPLICATION_MODAL);

		this.cancelled = new AtomicBoolean(false);

		if (repository == null) {
			repository = guessRepository();
		}

		JPanel mainPanel = new JPanel(new BorderLayout());

		JPanel configPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();


		// info label
		JPanel infoPanel = new JPanel(new GridBagLayout());
		gbc.insets = new Insets(20, 20, 5, 20);
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		infoPanel.add(new JLabel(I18N.getGUILabel("connection.create_new.info.label")), gbc);
		mainPanel.add(infoPanel, BorderLayout.NORTH);

		gbc.weightx = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		// connection type chooser
		gbc.gridy += 1;
		gbc.insets = new Insets(20, 20, 5, 20);
		configPanel.add(new JLabel(I18N.getGUILabel("connection.create_new.type.label")), gbc);

		Vector<String> types = new Vector<>(ConnectionHandlerRegistry.getInstance().getAllTypes());
		types.sort(Comparator.comparing(ConnectionI18N::getTypeName));
		typeBox = new JComboBox<>(types);
		typeBox.setSelectedIndex(Math.max(0, types.indexOf(JDBC_CONNECTORS_JDBC)));
		typeBox.setRenderer(new ConnectionListCellRenderer());
		gbc.gridx += 1;
		configPanel.add(typeBox, gbc);

		typeErrorLabel = new JLabel(IconFactory.getEmptyIcon16x16());
		typeErrorLabel.setHorizontalAlignment(SwingConstants.LEFT);
		typeErrorLabel.setIconTextGap(10);
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 20, 5, 20);
		configPanel.add(typeErrorLabel, gbc);

		// repository chooser
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 20, 5, 20);
		configPanel.add(new JLabel(I18N.getGUILabel("connection.create_new.repository.label")), gbc);

		Vector<Repository> repos = RepositoryManager.getInstance(null).getRepositories().stream().
				filter(Repository::supportsConnections).
				filter(repo -> !repo.isReadOnly()).
				sorted(Comparator.comparing(Repository::getName)).
				collect(Collectors.toCollection(Vector::new));
		repositoryBox = new JComboBox<>(repos);
		if (repository != null) {
			repositoryBox.setSelectedItem(repository);
		}
		repositoryBox.setRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Repository repo = ((Repository) value);
				String name = repo.getName();
				String iconName = repo.getIconName();
				label.setText(name);
				label.setIcon(SwingTools.createIcon("16/" + iconName));
				return label;
			}
		});
		gbc.gridx += 1;
		configPanel.add(repositoryBox, gbc);

		repositoryErrorLabel = new JLabel(IconFactory.getEmptyIcon16x16());
		repositoryErrorLabel.setHorizontalAlignment(SwingConstants.LEFT);
		repositoryErrorLabel.setIconTextGap(10);
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 20, 5, 20);
		configPanel.add(repositoryErrorLabel, gbc);

		// name field
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 20, 5, 20);
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		configPanel.add(new JLabel(I18N.getGUILabel("connection.create_new.name.label")), gbc);

		nameField = new JTextField(25);
		nameField.setToolTipText(I18N.getGUILabel("connection.create_new.name.tip"));
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				nextButton.setEnabled(!validateFieldsAndReturnError());
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				nextButton.setEnabled(!validateFieldsAndReturnError());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				nextButton.setEnabled(!validateFieldsAndReturnError());
			}
		});
		SwingTools.setPrompt(I18N.getGUILabel("connection.create_new.name.prompt"), nameField);
		gbc.gridx += 1;
		configPanel.add(nameField, gbc);

		nameErrorLabel = new JLabel(IconFactory.getEmptyIcon16x16());
		nameErrorLabel.setHorizontalAlignment(SwingConstants.LEFT);
		nameErrorLabel.setIconTextGap(10);
		gbc.gridy += 1;
		gbc.insets = new Insets(0, 20, 5, 20);
		configPanel.add(nameErrorLabel, gbc);

		// spacer
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		configPanel.add(new JLabel(), gbc);

		// button panel at bottom
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbcb = new GridBagConstraints();
		gbcb.gridx = 0;
		gbcb.gridy = 1;
		gbcb.weightx = 1;
		gbcb.fill = GridBagConstraints.HORIZONTAL;
		gbcb.insets = new Insets(10, 20, 10, 10);

		// Status
		GridBagConstraints gbcFullWidth = new GridBagConstraints();
		gbcFullWidth.fill = GridBagConstraints.BOTH;
		gbcFullWidth.weightx = 1;
		gbcFullWidth.gridwidth = GridBagConstraints.REMAINDER;
		gbcFullWidth.insets = (Insets) gbcb.insets.clone();
		gbcFullWidth.insets.bottom = 0;
		JPanel warningPanel = new JPanel(new GridBagLayout());

		statusIcon = new JLabel(IconFactory.getEmptyIcon16x16());
		statusLabel = new JTextArea(2, 20);
		statusLabel.setMinimumSize(new Dimension(20, 40));
		statusLabel.setBackground(null);
		statusLabel.setLineWrap(true);
		statusLabel.setWrapStyleWord(true);
		statusLabel.setBorder(BorderFactory.createEmptyBorder());
		statusLabel.setEditable(false);
		GridBagConstraints gbcStatus = new GridBagConstraints();
		gbcStatus.anchor = GridBagConstraints.NORTH;
		warningPanel.add(statusIcon, gbcStatus);
		JScrollPane scrollPane = new ExtendedJScrollPane(statusLabel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(1, 48));
		gbcStatus.fill = GridBagConstraints.HORIZONTAL;
		gbcStatus.insets.left = 10;
		gbcStatus.weightx = 1.0;
		warningPanel.add(scrollPane, gbcStatus);
		buttonPanel.add(warningPanel, gbcFullWidth);
		buttonPanel.add(new JLabel(), gbcb);

		final ResourceAction nextAction = new ResourceAction("connection.create_new.create") {
			@Override
			public void loggedActionPerformed(ActionEvent e) {
				if (validateFieldsAndReturnError()) {
					return;
				}

				nextButton.setEnabled(false);
				updateStatus(Status.WORKING, I18N.getGUILabel("connection.create_new.status.working"));

				String name = nameField.getText();
				Repository repo = (Repository) repositoryBox.getSelectedItem();
				String locationName = RepositoryLocation.REPOSITORY_PREFIX + repo.getName() + RepositoryLocation.SEPARATOR
						+ Folder.CONNECTION_FOLDER_NAME + RepositoryLocation.SEPARATOR + name;

				new ProgressThread(SaveConnectionAction.PROGRESS_THREAD_ID_PREFIX, false, locationName) {
					@Override
					public void run() {
						// validation made sure these fields are all set correctly, so nothing is null or empty
						String type = String.valueOf(typeBox.getSelectedItem());
						Folder connectionFolder;
						RepositoryLocation location;

						try {
							connectionFolder = RepositoryTools.getConnectionFolder(repo);
						} catch (RepositoryException e1) {
							// should not happen, but you never know
							LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.gui.ConnectionCreationDialog.repo_conn_folder_retrieval", e);
							SwingTools.invokeLater(() -> {
								nextButton.setEnabled(true);
								updateStatus(Status.WARNING, I18N.getGUILabel("connection.create_new.error.repo_conn_folder_retrieval"));
							});
							return;
						}

						// check if user cancelled dialog in the meantime
						if (ConnectionCreationDialog.this.cancelled.get()) {
							return;
						}

						if (connectionFolder == null) {
							// should not happen, but you never know
							SwingTools.invokeLater(() -> {
								nextButton.setEnabled(true);
								updateStatus(Status.WARNING, I18N.getGUILabel("connection.create_new.error.repo_no_conn_folder"));
							});
							return;
						}
						try {
							location = new RepositoryLocation(connectionFolder.getLocation(), name);
						} catch (MalformedRepositoryLocationException e1) {
							// should not happen as it is validated before, but you never know
							SwingTools.invokeLater(() -> {
								nextButton.setEnabled(true);
								nameErrorLabel.setText(e1.getMessage());
								nameErrorLabel.setIcon(WARNING_ICON);
								updateStatus(Status.NO_STATUS, null);
							});
							return;
						}

						// check if user cancelled dialog in the meantime
						if (ConnectionCreationDialog.this.cancelled.get()) {
							return;
						}

						try {
							// now we need to check for duplicates. This is a problem on Windows for the Local Repository
							// as it will NOT find different capitalization, while on file-system level, it is a duplicate
							boolean duplicate;
							if (repo instanceof LocalRepository && SystemInfoUtilities.getOperatingSystem() == SystemInfoUtilities.OperatingSystem.WINDOWS) {
								duplicate = connectionFolder.getDataEntries().stream().anyMatch(entry -> name.equalsIgnoreCase(entry.getName()));
							} else {
								// on Unix systems, we can just call locate it it will be fine
								duplicate = location.locateEntry() != null;
							}
							if (duplicate) {
								SwingTools.invokeLater(() -> {
									nextButton.setEnabled(true);
									nameErrorLabel.setText(I18N.getGUILabel("connection.create_new.error.name_duplicate"));
									nameErrorLabel.setIcon(INFORMATION_ICON);
									updateStatus(Status.NO_STATUS, null);
								});
								return;
							}

							// check if user cancelled dialog in the meantime
							if (ConnectionCreationDialog.this.cancelled.get()) {
								return;
							}
							// convert old connection type if necessary
							ConnectionInformation connection = null;
							if (ciCreator != null) {
								try {
									connection = ciCreator.call();
								} catch (Exception e) {
									// conversion failed
									SwingTools.invokeLater(() -> {
										nextButton.setEnabled(true);
										nameErrorLabel.setText(I18N.getGUILabel("connection.create_new.error.conversion", e.getMessage()));
										nameErrorLabel.setIcon(WARNING_ICON);
										updateStatus(Status.NO_STATUS, null);
									});
									return;
								}
							}

							// check if user cancelled dialog in the meantime
							if (ConnectionCreationDialog.this.cancelled.get()) {
								return;
							}

							ConnectionInformationHolder connHolder = connection == null ?
									ConnectionInformationHolder.createNewConnection(name, type, location) :
									ConnectionInformationHolder.from(connection, name, location);
							RepositoryManager.getInstance(null).store(new ConnectionInformationContainerIOObject(connHolder.getConnectionInformation()), location, null);
							SwingTools.invokeLater(() -> {
								ConnectionCreationDialog.this.dispose();
								if (finalAction != null) {
									finalAction.run();
								}
								ConnectionEditDialog connectionEditDialog = new ConnectionEditDialog(parent, connHolder, true);
								// open up the first relevant tab if possible
								connectionEditDialog.showTab(1);

								connectionEditDialog.setVisible(true);
								// scroll to connection location
								// twice because otherwise the repository browser selects the parent...
								RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().expandAndSelectIfExists(location);
								RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().expandAndSelectIfExists(location);
						
							});
						} catch (Exception e) {
							LogService.getRoot().log(Level.SEVERE, "com.rapidminer.connection.gui.ConnectionCreationDialog.creation_failed", e);
							SwingTools.invokeLater(() -> {
								nextButton.setEnabled(true);
								updateStatus(Status.WARNING, I18N.getGUILabel("connection.create_new.status.failed", e.getMessage()));
							});
						}
					}
				}.start();
			}
		};
		gbcb.weightx = 0;
		gbcb.insets = new Insets(10, 0, 10, 10);
		gbcb.gridx += 1;
		nextButton = new JButton(nextAction);
		buttonPanel.add(nextButton, gbcb);

		final ResourceAction cancelAction = new ResourceAction("connection.create_new.cancel") {
			@Override
			public void loggedActionPerformed(ActionEvent e) {
				cancelled.set(true);
				dispose();
			}
		};
		gbcb.gridx += 1;
		buttonPanel.add(new JButton(cancelAction), gbcb);

		mainPanel.add(configPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		// close dialog with ESC
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
		getRootPane().getActionMap().put("CLOSE", cancelAction);
		// next press with ENTER
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "NEXT");
		getRootPane().getActionMap().put("NEXT", nextAction);

		setContentPane(mainPanel);
		setSize(new Dimension(600, 400));
		setLocationRelativeTo(getOwner());

		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_DIALOG, I18N_KEY, "open");
	}

	/**
	 * Prefills the type and name and locks the type.
	 * This is used when converting an existing connection.
	 *
	 * @param type
	 * 		the type of the connection (to be locked in)
	 * @param name
	 * 		the probable name of the connection after conversion
	 * @see com.rapidminer.connection.legacy.ConversionService ConversionService
	 */
	public void preFill(String type, String name) {
		typeBox.setSelectedItem(type);
		typeBox.setEnabled(false);
		nameField.setText(name);
	}

	/**
	 * Sets the conversion callable. This should be used when converting an existing connection to convert
	 * the old connection to a {@link ConnectionInformation}.
	 *
	 * @param ciCreator
	 * 		the creator/converter; if {@code null}, a new connection will be created
	 * @see com.rapidminer.connection.legacy.ConversionService ConversionService
	 */
	public void setConverter(Callable<ConnectionInformation> ciCreator) {
		this.ciCreator = ciCreator;
	}

	/**
	 * Sets action to be done if the creation was successful.
	 * This should be used when converting an existing connection, e.g. to close another parent dialog.
	 *
	 * @param finalAction
	 * 		an action
	 * @see com.rapidminer.connection.legacy.ConversionService ConversionService
	 */
	public void setFinalAction(Runnable finalAction) {
		this.finalAction = finalAction;
	}

	/**
	 * Updates the status message field. Make sure to call on the EDT.
	 *
	 * @param status
	 * 		the status, changes the displayed icon. If {@code Status#NO_STATUS}, no icon will be displayed.
	 * @param message
	 * 		the message to display, can be {@code null}
	 */
	private void updateStatus(Status status, String message) {
		switch (status) {
			case NO_STATUS:
				statusIcon.setIcon(IconFactory.getEmptyIcon16x16());
				break;
			case INFO:
				statusIcon.setIcon(INFORMATION_ICON);
				break;
			case WORKING:
				statusIcon.setIcon(WORKING_ICON);
				break;
			case WARNING:
				statusIcon.setIcon(WARNING_ICON);
				break;
		}
		statusLabel.setText(message);
		statusLabel.setToolTipText(message);
		statusIcon.revalidate();
	}

	/**
	 * Validates the input fields and highlights potential errors.
	 *
	 * @return {@code true} if at least one error is contained in the config; {@code false} otherwise
	 */
	private boolean validateFieldsAndReturnError() {
		boolean error = false;
		String name = nameField.getText();
		if (name == null || name.trim().isEmpty()) {
			nameErrorLabel.setText(I18N.getGUILabel("connection.create_new.error.name_empty"));
			nameErrorLabel.setIcon(INFORMATION_ICON);
			error = true;
		} else if (!RepositoryLocation.isNameValid(name)) {
			nameErrorLabel.setText(I18N.getMessage(I18N.getGUIBundle(),
					"gui.dialog.repository_location.location_invalid_char.label", RepositoryLocation.getIllegalCharacterInName(name)));
			nameErrorLabel.setIcon(WARNING_ICON);
			error = true;
		} else {
			nameErrorLabel.setText("");
			nameErrorLabel.setIcon(IconFactory.getEmptyIcon16x16());
		}
		String type = (String) typeBox.getSelectedItem();
		if (type == null) {
			typeErrorLabel.setText(I18N.getGUILabel("connection.create_new.error.type_empty"));
			typeErrorLabel.setIcon(INFORMATION_ICON);
			error = true;
		} else {
			typeErrorLabel.setText("");
			typeErrorLabel.setIcon(IconFactory.getEmptyIcon16x16());
		}
		Repository repo = (Repository) repositoryBox.getSelectedItem();
		if (repo == null) {
			repositoryErrorLabel.setText(I18N.getGUILabel("connection.create_new.error.repository_empty"));
			repositoryErrorLabel.setIcon(INFORMATION_ICON);
			error = true;
		} else {
			repositoryErrorLabel.setText("");
			repositoryErrorLabel.setIcon(IconFactory.getEmptyIcon16x16());
		}

		return error;
	}

	/**
	 * Returns the {@link Repository} of the currently open {@link com.rapidminer.Process Process} if present,
	 * otherwise from the currently selected {@link com.rapidminer.repository.Entry Entry}
	 * of the {@link com.rapidminer.repository.gui.RepositoryTree RepositoryTree}
	 *
	 * @return the {@link Repository}, or {@code null}
	 */
	private static Repository guessRepository() {
		Callable<Repository> storedProcess = () -> RapidMinerGUI.getMainFrame().getProcess().getRepositoryLocation().getRepository();
		Callable<Repository> selectedEntry = () -> RapidMinerGUI.getMainFrame().getRepositoryBrowser().getRepositoryTree().getSelectedEntry().getLocation().getRepository();

		for (Callable<Repository> repositorySupplier : Arrays.asList(storedProcess, selectedEntry)) {
			try {
				Repository repository = repositorySupplier.call();
				if (repository.supportsConnections()) {
					return repository;
				}
			} catch (Exception e) {
				// next try
			}
		}
		return null;
	}

}

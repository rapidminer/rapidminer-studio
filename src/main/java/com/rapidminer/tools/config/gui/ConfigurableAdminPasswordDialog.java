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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.repository.internal.remote.RemoteRepositoryFactory;
import com.rapidminer.repository.internal.remote.RemoteRepositoryFactoryRegistry;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.config.ConfigurationManager;

import static com.rapidminer.repository.internal.remote.RemoteRepository.AuthenticationType.BASIC;

/**
 * Dialog asking for admin password of a given {@link RemoteRepository}.
 *
 * @author Sabrina Kirstein
 *
 */
public class ConfigurableAdminPasswordDialog extends ButtonDialog {

	private static final long serialVersionUID = 1L;

	/** repository url of the server the admin should connect to */
	private String repositoryURL;

	/** text field with the user name (which is already set to admin user) */
	private JTextField userField = new JTextField(ConfigurationManager.RM_SERVER_CONFIGURATION_USER_ADMIN, 20);

	/** label describing userField */
	private JLabel userLabel = new ResourceLabel("configurable_dialog.password_dialog_admin.user");

	/** field containing the password */
	private JPasswordField passwordField = new JPasswordField(20);

	/** label describing passwordField */
	private JLabel passwordLabel = new ResourceLabel("configurable_dialog.password_dialog_admin.password");

	/** color of {@link #checkLabel} */
	private static final Color FAILURE_STATUS_COLOR = Color.RED;

	/** label indicating that the connection could not be established */
	private JLabel checkLabel = new JLabel();

	private final String sourceName;

	public ConfigurableAdminPasswordDialog(Window owner, RemoteRepository source) {
		super(owner, "configurable_dialog.password_dialog_admin", ModalityType.MODELESS, new Object[] {
		        source != null ? source.getName() : "", source != null ? source.getBaseUrl().toString() : "" });
		JButton okButton = makeOkButton("configurable_dialog.password_dialog_admin.ok");
		JButton cancelButton = makeCancelButton("configurable_dialog.password_dialog_admin.cancel");
		setModal(true);

		// if this is not a remote connection, we don't need to login anywhere
		if (source == null) {
			dispose();
			this.sourceName = null;
		} else {
			this.repositoryURL = source.getBaseUrl().toString();
			this.sourceName = source.getName();

			JPanel mainPanel = makeMainPanel();

			layoutDefault(mainPanel, MESSAGE_BIT_EXTENDED, okButton, cancelButton);
			passwordField.requestFocusInWindow();
		}
	}

	/**
	 * @return the password of the admin (given by user)
	 */
	public char[] getPassword() {
		return passwordField.getPassword();
	}

	/**
	 * @return the user name of the admin (not given by user)
	 */
	public String getUserName() {
		return userField.getText();
	}

	/**
	 * checks whether the given url, user name and password can create a connection to the server
	 * and displays an error, if one occurs, otherwise the window is closed
	 */
	private void checkConnection() {
		wasConfirmed = false;
		if (passwordField.getPassword().length == 0) {
			checkLabel.setText(I18N.getGUILabel("error.configurable_dialog.password_dialog_admin.password"));
		}

		ProgressThread pt = new ProgressThread("check_configuration") {

			@Override
			public void run() {
				RemoteRepositoryFactory remoteRepositoryFactory = RemoteRepositoryFactoryRegistry.INSTANCE.get();
				final String error = remoteRepositoryFactory != null
		                ? remoteRepositoryFactory.checkConfiguration(sourceName, repositoryURL, getUserName(), getPassword(), BASIC)
		                : I18N.getGUILabel("error.configurable_dialog.remote_repo_factory_not_available");

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (error != null) {
							checkLabel.setText(error);
						} else {
							wasConfirmed = true;
							dispose();
						}
					}
				});
			}
		};
		pt.start();
	}

	@Override
	protected void ok() {
		checkConnection();
	}

	private JPanel makeMainPanel() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.weightx = .5;
		c.gridy = 0;
		c.insets = new Insets(4, 4, 4, 4);

		// User name
		c.gridx = 0;
		c.gridy += 1;
		mainPanel.add(userLabel, c);
		c.gridx += 1;
		userField.setMinimumSize(userField.getPreferredSize());
		mainPanel.add(userField, c);
		c.gridx += 1;
		mainPanel.add(Box.createHorizontalGlue(), c);

		// Password
		c.gridx = 0;
		c.gridy += 1;
		mainPanel.add(passwordLabel, c);
		c.gridx += 1;
		passwordField.setMinimumSize(passwordField.getPreferredSize());
		mainPanel.add(passwordField, c);
		c.gridx += 1;
		mainPanel.add(Box.createHorizontalGlue(), c);

		// check label
		c.gridx = 0;
		c.gridy += 1;
		c.gridwidth = 4;
		c.weighty = 1;
		checkLabel.setForeground(FAILURE_STATUS_COLOR);
		mainPanel.add(checkLabel, c);

		return mainPanel;
	}
}

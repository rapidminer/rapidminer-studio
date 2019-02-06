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

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;


/**
 * A drop down button which shows the connected servers with non-admin users and without any user
 * connection. The popup menu allows to login as admin on one server.
 *
 * @author Sabrina Kirstein
 */
public class LoginAsAdminDropDownButton extends DropDownPopupButton {

	private static final long serialVersionUID = -7638556190299142700L;

	public LoginAsAdminDropDownButton(Window owner, Map<String, ConfigurableController> remoteControllers) {
		super("gui.label.configurable_dialog.login_as_admin.small", new LoginAsAdminPopupProvider(owner, remoteControllers));

		setIcon(SwingTools.createIcon("24/" + I18N.getGUILabel("configurable_dialog.login_as_admin.small.icon")));
		setArrowSize(18);
		setEnabled(true);

	}

	private static class LoginAsAdminPopupProvider implements PopupMenuProvider {

		private final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String serverName = e.getActionCommand();
				loginAsAdmin(serverName);
			}
		};

		/**
		 * remote controllers related to the connected servers in the drop down button
		 */
		private Map<String, ConfigurableController> remoteControllers;

		/** the owner of this dropdown button */
		private Window owner;

		public LoginAsAdminPopupProvider(Window owner, Map<String, ConfigurableController> remoteControllers) {
			this.remoteControllers = remoteControllers;
			this.owner = owner;
		}

		@Override
		public JPopupMenu getPopupMenu() {
			JPopupMenu menu = new JPopupMenu();

			// add server items
			for (String serverName : remoteControllers.keySet()) {

				ConfigurableController controller = remoteControllers.get(serverName);

				// if the connection to the server is established
				if (controller.getModel().getSource().isConnected()) {
					if (!controller.getModel().hasAdminRights()) {

						JMenuItem newItem = new JMenuItem(I18N.getGUILabel("configurable_dialog.login_as_admin.small.item",
								serverName));
						newItem.setActionCommand(serverName);
						newItem.addActionListener(actionListener);
						menu.add(newItem);
					}
				}
			}
			return menu;
		}

		private void loginAsAdmin(String serverName) {

			RemoteRepository source = remoteControllers.get(serverName).getModel().getSource();

			if (serverName != null) {
				ConfigurableAdminPasswordDialog passwordDialog = new ConfigurableAdminPasswordDialog(owner, source);
				passwordDialog.setVisible(true);
				if (passwordDialog.wasConfirmed()) {
					String username = passwordDialog.getUserName();
					char[] password = passwordDialog.getPassword();
					// set the new connection as admin
					remoteControllers.get(serverName).getModel().getSource().setUsername(username);
					remoteControllers.get(serverName).getModel().getSource().setPassword(password);
					remoteControllers.get(serverName).getModel().checkForAdminRights();
					// and refresh the configurables
					refreshConfigurables(serverName);
				}
			}
		}

		/** reloads the configurables of the selected server */
		private void refreshConfigurables(String serverName) {
			ConfigurableController controller = remoteControllers.get(serverName);

			// if the connection to the server is established
			if (controller.getModel().getSource().isConnected()) {
				// refresh the configurables
				controller.getView().refreshConfigurables(serverName);
			}
		}

	};

}

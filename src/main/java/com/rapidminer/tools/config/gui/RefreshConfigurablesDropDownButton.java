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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;


/**
 * A drop down button which shows the connected servers. The popup menu allows to refresh all
 * connections of the user or the connections of one server.
 *
 * @author Sabrina Kirstein
 */
public class RefreshConfigurablesDropDownButton extends DropDownPopupButton {

	private static final long serialVersionUID = -7638556190299142700L;

	public RefreshConfigurablesDropDownButton(Map<String, ConfigurableController> remoteControllers) {
		super("gui.label.configurable_dialog.refresh_config", new RefreshConfigurablesPopupProvider(remoteControllers));

		setIcon(SwingTools.createIcon("24/" + I18N.getGUILabel("configurable_dialog.refresh_config.icon")));
		setArrowSize(18);
		setEnabled(true);

	}

	private static class RefreshConfigurablesPopupProvider implements PopupMenuProvider {

		private final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String serverName = e.getActionCommand();
				refreshConfigurables(serverName);
			}
		};

		private Action REFRESH_ALL_ACTION = new ResourceAction("configurable_dialog.refresh_all_configs") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent arg0) {
				refreshAllConfigurables();
			}
		};

		/**
		 * remote controllers related to the connected servers in the drop down button
		 */
		private Map<String, ConfigurableController> remoteControllers;

		/** menu item to refresh all connections */
		private JMenuItem allConnectionsItem;

		public RefreshConfigurablesPopupProvider(Map<String, ConfigurableController> remoteControllers) {
			this.remoteControllers = remoteControllers;
			allConnectionsItem = new JMenuItem(REFRESH_ALL_ACTION);
		}

		@Override
		public JPopupMenu getPopupMenu() {
			JPopupMenu menu = new JPopupMenu();

			List<JMenuItem> serverItems = new LinkedList<>();
			// add server items
			for (String serverName : remoteControllers.keySet()) {

				ConfigurableController controller = remoteControllers.get(serverName);

				// if the connection to the server is established
				if (controller.getModel().getSource().isConnected()) {

					JMenuItem newItem = new JMenuItem(I18N.getGUILabel("configurable_dialog.refresh_config.server.label",
							serverName));
					newItem.setActionCommand(serverName);
					newItem.addActionListener(actionListener);
					serverItems.add(newItem);
				}
			}

			// add items to menu
			if (serverItems.size() >= 2) {
				// add the refresh all connections part only if there are at
				// least
				// two connected servers
				menu.add(allConnectionsItem);
				menu.addSeparator();
			}

			for (JMenuItem item : serverItems) {
				menu.add(item);
			}
			return menu;
		}

		/** reloads the configurables of all connected servers */
		private void refreshAllConfigurables() {
			refreshConfigurables(remoteControllers.keySet().toArray(new String[0]));
		}

		/** reloads the configurables of the selected servers */
		private void refreshConfigurables(String... serverNames) {

			// show dialog if necessary
			if (!userAcceptsDataLoss(serverNames)) {
				return;
			}

			for (String serverName : serverNames) {
				ConfigurableController controller = remoteControllers.get(serverName);

				// if the connection to the server is established
				if (controller.getModel().getSource().isConnected()) {
					// refresh the configurables
					controller.getView().refreshConfigurables(serverName);
				}
			}
		}


		/**
		 * Displays a confirm dialog if unsaved changes would be lost
		 *
		 * @param serverNames
		 * @return false if the refresh should be cancelled
		 */
		private boolean userAcceptsDataLoss(String... serverNames) {
			for (String serverName : serverNames) {
				ConfigurableController controller = remoteControllers.get(serverName);
				//Don't check if not connected
				if (!controller.getModel().getSource().isConnected()) {
					continue;
				}
				//Update the model from the current view
				controller.getView().updateModel();
				//Check if modes has changed
				if (controller.getModel().isModified()) {
					//Ask if refresh should continue
					return SwingTools.showConfirmDialog(remoteControllers.values().iterator().next().getView(), "configurable_confirm_refresh",
							ConfirmDialog.YES_NO_OPTION) == ConfirmDialog.YES_OPTION;
				}
			}
			//No changes, refresh
			return true;
		}

	}

}

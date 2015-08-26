/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.config.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.repository.internal.remote.RemoteRepository;
import com.rapidminer.tools.I18N;


/**
 * A drop down button which shows the connected servers with non-admin users and without any user
 * connection. The popup menu allows to login as admin on one server.
 *
 * @author Sabrina Kirstein
 */
public class LoginAsAdminDropDownButton extends DropDownButton {

	private static final long serialVersionUID = -7638556190299142700L;

	/** remote controllers related to the connected servers in the drop down button */
	private Map<String, ConfigurableController> remoteControllers;

	/** the owner of this dropdown button */
	private Window owner;

	private final ActionListener actionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			String serverName = e.getActionCommand();
			loginAsAdmin(serverName);
		}
	};

	public LoginAsAdminDropDownButton(Window owner, Map<String, ConfigurableController> remoteControllers) {
		// use the same action for button and arrow
		super(null, null, true);
		setUsePopupActionOnMainButton();

		// needs to be done like this to have the icon on the left and the space on the right to
		// draw the arrow
		setText("       ");
		setIcon(SwingTools.createIcon(I18N.getGUILabel("configurable_dialog.login_as_admin.small.icon")));
		setToolTipText(I18N.getGUILabel("configurable_dialog.login_as_admin.small.tip"));
		setEnabled(true);

		this.remoteControllers = remoteControllers;
		this.owner = owner;
	}

	@Override
	protected JPopupMenu getPopupMenu() {

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

	@Override
	public void paintComponent(Graphics g) {

		if (getModel().isArmed()) {
			((Graphics2D) g).translate(1.1, 1.1);
		}

		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Graphics2D g2 = (Graphics2D) g.create();

		// first draw the button
		super.paintComponent(g);

		// draw the arrow above the button
		GeneralPath arrow = new GeneralPath();
		int w, h;
		h = 3;
		w = 6;
		arrow.moveTo(getWidth() * 0.75 - 1.5 * w, getHeight() / 2 - h);
		arrow.lineTo(getWidth() * 0.75 + 0.5 * w, getHeight() / 2 - h);
		arrow.lineTo(getWidth() * 0.75 - 0.5 * w, getHeight() / 2 + h);
		arrow.closePath();
		g2.setColor(Color.BLACK);
		g2.fill(arrow);

		g2.dispose();
	}
}

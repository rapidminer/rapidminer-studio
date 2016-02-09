/**
 * Copyright (C) 2001-2016 by RapidMiner and the contributors
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
package com.rapidminer.gui.properties;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.AbstractButton;
import javax.swing.JButton;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;


/**
 * The settings dialog for user settings.
 *
 * <p>
 * Settings are stored in the file <i>rapidminer-studio-settings.cfg</i> in the user directory
 * <i>.RapidMiner</i> and can overwrite system wide settings. The settings are grouped in
 * {@link SettingsTabs} each of which contains a {@link SettingsPropertyPanel}. Each setting is
 * represented by a {@link SettingsItem}, which maintains the tab, grouping, key, i18n of its title
 * and description.
 * </p>
 *
 * <p>
 * To add a new preference, you have to use the method {@link ParameterService#registerParameter()}.
 * Your new parameter can be added to the i18n by adding the related key and a value to the resource
 * file <i>Settings.properties</i>. Configure the structure of your properties by editing the
 * resource file <i>settings.xml</i>. This affects the order and sub-groups. Extensions can use the
 * resource files <i>SettingsMYEXT.properties</i> and <i>settingsMYEXT.xml</i>. This is documented
 * in <i>How to extend RapidMiner</i>, available at <a
 * href="http://rapidminer.com/documentation/">http://rapidminer.com/documentation/</a>
 * </p>
 *
 * @author Ingo Mierswa, Adrian Wilke
 */
public class SettingsDialog extends ButtonDialog {

	private static final long serialVersionUID = 6665295638614289994L;

	private SettingsTabs tabs;

	/**
	 * Sets up the related {@link SettingsTabs} and buttons.
	 */
	public SettingsDialog() {
		this(null);
	}

	/**
	 * Sets up the related {@link SettingsTabs} and buttons.
	 *
	 * Selects the specified selected tab.
	 *
	 * @param initialSelectedTab
	 *            A key of a preferences group to identify the initial selected tab.
	 */
	public SettingsDialog(String initialSelectedTab) {
		super(ApplicationFrame.getApplicationFrame(), "settings", ModalityType.APPLICATION_MODAL, new Object[] {});

		// Create tabs
		tabs = new SettingsTabs(this);

		// Select tab, if known
		if (initialSelectedTab != null) {
			tabs.selectTab(initialSelectedTab);
		}

		// Create buttons
		Collection<AbstractButton> buttons = new LinkedList<>();
		buttons.add(new JButton(new ResourceAction("settings_ok") {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					tabs.save();
					setConfirmed(true);
					dispose();
				} catch (IOException ioe) {
					SwingTools.showSimpleErrorMessage("cannot_save_properties", ioe);
				}
			}
		}));
		buttons.add(makeCancelButton());

		layoutDefault(tabs, NORMAL_EXTENDED, buttons);
	}

	@Override
	public String getInfoText() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.settings.message");
	}

	@Override
	protected void close() {
		resetItemStatus();
		super.close();
	}

	@Override
	protected void cancel() {
		resetItemStatus();
		super.cancel();
	}

	@Override
	protected void ok() {
		resetItemStatus();
		super.ok();
	}

	/**
	 * Remove the used flag to prevent broken settings dialog after second opening.
	 */
	private void resetItemStatus() {
		for (String key : SettingsItems.INSTANCE.getKeys()) {
			SettingsItems.INSTANCE.get(key).setUsedInDialog(false);
		}
	}

}

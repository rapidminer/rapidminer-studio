/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.prompt.PromptSupport;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
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
 * in <i>How to extend RapidMiner</i>, available at
 * <a href="http://rapidminer.com/documentation/">http://rapidminer.com/documentation/</a>
 * </p>
 *
 * @author Ingo Mierswa, Adrian Wilke
 */
public class SettingsDialog extends ButtonDialog {

	private static final long serialVersionUID = 6665295638614289994L;

	/**
	 * the delay before filtering is started after the user finished typing in milliseconds:
	 * {@value}
	 */
	private static final int FILTER_TIMER_DELAY = 500;

	/** the identifier of the search focus action */
	private static final String ACTION_NAME_SEARCH = "focusSearchField";

	/**
	 * icon used in the {@link TextFieldWithAction} when the filter remove action is hovered
	 */
	private final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	/**
	 * the main container which contains the {@link #tabs} or the {@link #noMatchingSettingsLabel}.
	 */
	private JPanel container;

	/**
	 * the displayed tabs which includes the settings
	 */
	private SettingsTabs tabs;

	/**
	 * cache for the displayed properties
	 */
	private Properties propertyCache = new Properties();

	/**
	 * this label will be shown if no matching settings could be found
	 */
	private JLabel noMatchingSettingsLabel = new JLabel(I18N.getGUILabel("settings.no_matching_settings"),
			SwingConstants.CENTER);

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

		// main component container
		container = new JPanel(new BorderLayout());
		container.add(createSearchPanel(), BorderLayout.NORTH);
		container.add(createTabs(initialSelectedTab, null));

		// Create buttons
		Collection<AbstractButton> buttons = new LinkedList<>();
		buttons.add(new JButton(new ResourceAction("settings_ok") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				try {
					updateFilter(null);
					tabs.save();
					setConfirmed(true);
					dispose();
				} catch (IOException ioe) {
					SwingTools.showSimpleErrorMessage(SettingsDialog.this, "cannot_save_properties", ioe);
				}
			}
		}));
		buttons.add(makeCancelButton());

		layoutDefault(container, NORMAL_EXTENDED, buttons);
		addWindowListener(new BetaFeaturesListener());
		addWindowListener(new AdditionalPermissionsListener());
	}

	/**
	 * Creates the settings tabs in regard to the filter.
	 */
	private JComponent createTabs(String initialSelectedTab, String filter) {
		// Create tabs
		tabs = new SettingsTabs(this, filter, propertyCache);

		// Select tab, if known
		if (initialSelectedTab != null) {
			tabs.selectTab(initialSelectedTab);
		}

		if (tabs.getTabCount() == 0) {
			return noMatchingSettingsLabel;
		} else {
			return tabs;
		}
	}

	/**
	 * Creates the search panel.
	 */
	private JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.insets = new Insets(0, 5, 5, 0);
		searchPanel.add(Box.createHorizontalGlue(), gbc);

		gbc.gridx += 1;
		gbc.weightx = 0;
		JLabel filterLabel = new JLabel(I18N.getGUILabel("settings.filter"));
		searchPanel.add(filterLabel, gbc);

		final JTextField filterNameField = new JTextField(10);
		filterNameField.setMinimumSize(new Dimension(300, 15));
		filterNameField.setPreferredSize(new Dimension(300, 15));

		final ResourceAction filterAction = new ResourceAction(true, "settings.filter") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				updateFilter(filterNameField.getText());
			}

		};
		final DocumentListener filterListener = new DocumentListener() {

			private Timer updateTimer;

			{
				updateTimer = new Timer(FILTER_TIMER_DELAY, filterAction);
				updateTimer.setRepeats(false);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}
		};
		filterNameField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.label.settings.filter_field.tip"));
		filterNameField.addActionListener(filterAction);
		filterNameField.getDocument().addDocumentListener(filterListener);
		filterNameField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				ACTION_NAME_SEARCH);
		filterNameField.getActionMap().put(ACTION_NAME_SEARCH, new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				filterNameField.requestFocusInWindow();
			}
		});
		PromptSupport.setPrompt(I18N.getMessage(I18N.getGUIBundle(), "gui.label.settings.filter_field.prompt"),
				filterNameField);
		PromptSupport.setFontStyle(Font.ITALIC, filterNameField);

		ResourceAction deleteFilterAction = new ResourceAction(true, "settings.filter_delete") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(final ActionEvent e) {
				// immediately show the filter update by calling the update filter method
				// prevent duplicate updates by removing and re-adding the corresponding listener
				filterNameField.getDocument().removeDocumentListener(filterListener);
				filterNameField.setText("");
				updateFilter(null);
				filterNameField.getDocument().addDocumentListener(filterListener);
			}
		};
		TextFieldWithAction searchField = new TextFieldWithAction(filterNameField, deleteFilterAction,
				CLEAR_FILTER_HOVERED_ICON);
		searchField.setMinimumSize(new Dimension(140, 20));
		searchField.setPreferredSize(new Dimension(140, 20));

		gbc.gridx += 1;
		searchPanel.add(searchField, gbc);
		return searchPanel;
	}

	private void updateFilter(String filter) {
		container.remove(tabs);
		container.remove(noMatchingSettingsLabel);
		container.add(createTabs(null, filter), BorderLayout.CENTER);
		container.revalidate();
		container.repaint();
	}

	@Override
	public String getInfoText() {
		return I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.settings.message");
	}

	@Override
	public void setVisible(boolean b) {
		if (tabs != null) {
			tabs.requestFocusInWindow();
		}
		super.setVisible(b);
	}
}

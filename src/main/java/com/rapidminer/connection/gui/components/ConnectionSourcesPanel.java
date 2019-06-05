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
package com.rapidminer.connection.gui.components;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.connection.gui.ValueProviderGUIProvider;
import com.rapidminer.connection.gui.ValueProviderGUIRegistry;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionModelConverter;
import com.rapidminer.connection.gui.model.ValueProviderModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandler;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.gui.look.icons.IconFactory;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;

import javafx.collections.ObservableList;


/**
 * Panel to show or edit the parameters of {@link ValueProvider ValueProviders} for a {@link ConnectionModel}. Register
 * custom UI providers in {@link ValueProviderGUIRegistry}. For an example see DEFAULT_VP_GUI_PROVIDER in
 * {@link ValueProviderGUIRegistry}
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class ConnectionSourcesPanel extends JPanel {
	/**
	 * Where to find the icon to be displayed for properly configured value providers
	 */
	public static final String NOT_WELL_CONFIGURED_VALUE_PROVIDER_ICON = "gui.dialog.connection.valueprovider.needs_configuration.icon";
	/**
	 * The icon to show for properly configured ValueProviders
	 */
	private static final ImageIcon ICON_WARNING = SwingTools.createIcon("16/" + I18N.getGUILabel("connection.unknown_vp.icon"));
	/**
	 * The model containing copies of all the necessary information
	 */
	private final ConnectionModel connectionModel;
	private final JDialog parent;

	/**
	 * Create a new instance for the given connectionModel.
	 *
	 * @param connectionModel
	 * 		The model containing all the {@link ValueProvider ValueProviders} to configure or show
	 */
	public ConnectionSourcesPanel(JDialog parent, ConnectionModel connectionModel) {
		this.connectionModel = connectionModel;
		this.parent = parent;
		setLayout(new BorderLayout());
		JLabel label;
		if (!connectionModel.isEditable()) {
			if (connectionModel.getValueProviders().isEmpty()) {
				label = new JLabel(I18N.getGUIMessage("gui.dialog.connection.valueprovider.header_information_viewmode_empty"));
			} else {
				label = new JLabel(I18N.getGUIMessage("gui.dialog.connection.valueprovider.header_information_viewmode"));
			}
		} else {
			label = new JLabel(I18N.getGUIMessage("gui.dialog.connection.valueprovider.header_information"));
		}
		label.setBorder(new EmptyBorder(16, 32, 10, 16));
		add(label, BorderLayout.NORTH);
		add(createConfigurationPanel(), BorderLayout.CENTER);
	}

	/**
	 * Create a panel that contains all the {@link ValueProvider ValueProviders} and their configuration, depending on
	 * the connectionModel they are editable
	 *
	 * @return a scrollable panel
	 */
	private JComponent createConfigurationPanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1;

		JPanel allVPPanel = new JPanel(new GridBagLayout());

		ConnectionInformation information = ConnectionModelConverter.getConnection(connectionModel);
		final ObservableList<ValueProviderModel> valueProviders = connectionModel.valueProvidersProperty();
		for (ValueProvider valueProvider : valueProviders) {
			if (allVPPanel.getComponentCount() > 0) {
				final Insets insets = gbc.insets;
				gbc.insets = new Insets(0, 16, 0, 0);
				allVPPanel.add(new JSeparator(), gbc);
				gbc.insets = insets;
			}
			allVPPanel.add(createVPPanel(valueProvider, information), gbc);
		}
		gbc.weighty = 1;
		allVPPanel.add(new JPanel(), gbc);
		JPanel toTheLeft = new JPanel();
		toTheLeft.setLayout(new BorderLayout());
		toTheLeft.add(allVPPanel, BorderLayout.CENTER);
		final ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(toTheLeft);
		scrollPane.setBorder(null);
		return scrollPane;
	}

	/**
	 * Create a panel to see the configuration for the given {@link ValueProvider}
	 *
	 * @param provider
	 * 		the {@link ValueProvider} to be viewed or configured, depending on the global connectionModel#editable flag
	 * @param information
	 * 		the information to use for checking the vps
	 * @return a panel containing the configuration values
	 */
	private JPanel createVPPanel(ValueProvider provider, ConnectionInformation information) {
		if (provider == null) {
			return new JPanel();
		}
		JPanel outerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = 1;
		gbc.gridy = 0;
		gbc.insets = new Insets(8, 32, 8, 16);
		gbc.weightx = 1;

		String valueProviderType = provider.getType();
		boolean typeKnown = ValueProviderHandlerRegistry.getInstance().isTypeKnown(valueProviderType);
		final ValueProviderHandler handler = typeKnown ? ValueProviderHandlerRegistry.getInstance().getHandler(valueProviderType) : null;
		int prefixSeparatorIndex = valueProviderType.indexOf(':');
		String namespace = null;
		if (prefixSeparatorIndex > 0) {
			namespace = valueProviderType.substring(0, prefixSeparatorIndex);
		}

		Insets oldinsets = gbc.insets;
		boolean showWarning = typeKnown && handler.validate(provider, information).getType() == TestResult.ResultType.FAILURE;
		gbc.insets = new Insets(oldinsets.top, oldinsets.left - 20, 0, oldinsets.right);

		if (typeKnown) {
			outerPanel.add(new JLabel("<html><b style=\"font-size: 12px\">" + ConnectionI18N.getValueProviderTypeName(valueProviderType) + "</b></html>",
					showWarning ? ICON_WARNING : IconFactory.getEmptyIcon16x16(), SwingConstants.LEFT), gbc);
		} else {
			JLabel label = new JLabel("<html><b>" + I18N.getGUILabel("connection.unknown_vp.label", valueProviderType) + "</b></html>",
					ICON_WARNING, SwingConstants.LEFT);
			label.setIconTextGap(10);
			outerPanel.add(label, gbc);
		}

		gbc.gridy++;
		gbc.insets = oldinsets;
		gbc.insets.left += 10;
		if (!typeKnown) {
			JComponent valueComponent;
			JPanel unknownPanel = new JPanel(new GridBagLayout());

			GridBagConstraints innerGbc = new GridBagConstraints();
			innerGbc.gridx = 0;
			innerGbc.gridy = 0;
			if (namespace != null) {
				unknownPanel.add(new LinkLocalButton(SwingTools.createMarketplaceDownloadActionForNamespace("connection.install_extension_unknown_vp", namespace)), innerGbc);
				valueComponent = unknownPanel;
			} else {
				// old Studio with new value provider? No prefix in type, so we cannot help with anything
				innerGbc.insets = new Insets(0, 8, 0, 0);
				unknownPanel.add(new JLabel(I18N.getGUILabel("connection.unknown_vp_no_help.label", valueProviderType)), innerGbc);
				valueComponent = unknownPanel;
			}
			outerPanel.add(valueComponent, gbc);
		} else if (!handler.isConfigurable() && showWarning) {
			JLabel warningLabel = new JLabel(ConnectionI18N.getConnectionGUIMessage(handler.validate(provider,
					information).getMessageKey()));
			JPanel wrapped = AbstractConnectionGUI.addInformationIcon(warningLabel, provider.getType(),
					"valueprovider", "no_configuration", parent);
			outerPanel.add(wrapped, gbc);
		} else {
			final ValueProviderGUIProvider guiProvider = ValueProviderGUIRegistry.INSTANCE.getGUIProvider(valueProviderType);
			try {
				outerPanel.add(guiProvider.createConfigurationComponent(parent, provider, ConnectionModelConverter.getConnection(connectionModel), connectionModel.isEditable()), gbc);
			} catch (Exception e) {
				LogService.getRoot().log(Level.SEVERE, "Creating the component to configure the value provider handler " + valueProviderType + " failed", e);
			}
		}
		return outerPanel;
	}
}

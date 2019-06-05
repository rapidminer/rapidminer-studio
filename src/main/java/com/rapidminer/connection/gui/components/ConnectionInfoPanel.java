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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.gui.listener.TextChangedDocumentListener;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.FixedWidthLabel;
import com.rapidminer.gui.tools.components.LinkLocalButton;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.FontTools;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.plugin.Plugin;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;


/**
 * Content of the Info tab in the Edit Connection Dialog
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class ConnectionInfoPanel extends JPanel {

	private static final Dimension TEXTAREA_SIZE = new Dimension(1, 90);
	private static final Font OPEN_SANS_12 = FontTools.getFont("Open Sans", Font.PLAIN, 12);
	private static final Font OPEN_SANS_20 = FontTools.getFont("Open Sans", Font.PLAIN, 20);
	private static final Font OPEN_SANS_SEMIBOLD_14 = FontTools.getFont("Open Sans Semibold", Font.BOLD, 14);
	private static final Font OPEN_SANS_SEMIBOLD_24 = FontTools.getFont("Open Sans Semibold", Font.BOLD, 24);
	public static final Color UNKNOWN_TYPE_COLOR = new Color(111, 111, 112);

	private final boolean editable;
	private final boolean isTypeKnown;

	public ConnectionInfoPanel(ConnectionModel connection) {
		this(connection, true);
	}

	/**
	 * Creates a connection information display panel.
	 *
	 * @param connection
	 * 		the model of the connection to show
	 * @param showDescriptions
	 * 		if {@code true} descriptions for the headers are displayed
	 */
	public ConnectionInfoPanel(ConnectionModel connection, boolean showDescriptions) {
		super(new GridBagLayout());
		this.editable = connection.isEditable();
		String connectionType = connection.getType();
		isTypeKnown = ConnectionHandlerRegistry.getInstance().isTypeKnown(connectionType);

		// header with icon, name, and type
		GridBagConstraints gbc = new GridBagConstraints();
		JPanel headerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints headerGbc = new GridBagConstraints();
		headerGbc.gridx = 0;
		headerGbc.gridy = 0;
		headerGbc.anchor = GridBagConstraints.WEST;
		headerGbc.gridheight = 2;
		headerGbc.insets = new Insets(0, 0, 0, 10);
		headerPanel.add(new JLabel(ConnectionI18N.getConnectionIcon(connectionType, IconSize.HUGE)), headerGbc);

		headerGbc.gridx = 1;
		headerGbc.gridheight = 1;
		headerGbc.insets = new Insets(0, 0, 0, 0);
		JLabel nameLabel = new JLabel(connection.getName());
		nameLabel.setToolTipText(connection.getName());
		nameLabel.setFont(OPEN_SANS_SEMIBOLD_24);
		headerPanel.add(nameLabel, headerGbc);

		headerGbc.gridx = 1;
		headerGbc.gridy += 1;
		headerGbc.gridheight = 1;
		JComponent typeComponent = createTypeComponent(connectionType);
		headerPanel.add(typeComponent, headerGbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.insets = new Insets(25, 25, 15, 25);
		JPanel headerOuterPanel = new JPanel(new BorderLayout());
		headerOuterPanel.add(headerPanel, BorderLayout.WEST);
		add(headerOuterPanel, gbc);

		gbc.gridy += 1;
		gbc.weightx = 0.8;
		gbc.insets = new Insets(0, 20, 0, 200);
		JSeparator separator = new JSeparator();
		add(separator, gbc);

		// body with location, description, and tags
		JPanel bodyPanel = new JPanel(new GridLayout(3, 2, 30, 20));
		bodyPanel.add(createDescriptionPanel(ConnectionI18N.getConnectionGUILabel("location"),
				ConnectionI18N.getConnectionGUILabel("location_description"), showDescriptions));

		String repositoryName = connection.getLocation() != null ?
				RepositoryLocation.REPOSITORY_PREFIX + connection.getLocation().getRepositoryName() : "";
		JTextArea locArea = new JTextArea(repositoryName);
		locArea.setEditable(false);
		locArea.setHighlighter(null);
		locArea.setLineWrap(true);
		locArea.setComponentPopupMenu(null);
		locArea.setInheritsPopupMenu(false);
		locArea.setBackground(getBackground());
		locArea.setBorder(BorderFactory.createEmptyBorder());
		locArea.setFont(OPEN_SANS_12);
		if (!isTypeKnown) {
			locArea.setForeground(UNKNOWN_TYPE_COLOR);
		}

		bodyPanel.add(locArea);

		bodyPanel.add(createDescriptionPanel(ConnectionI18N.getConnectionGUILabel("description"),
				ConnectionI18N.getConnectionGUILabel("description_description"), showDescriptions));

		bodyPanel.add(createTextArea(connection.descriptionProperty()));

		bodyPanel.add(createDescriptionPanel(ConnectionI18N.getConnectionGUILabel("tags"),
				ConnectionI18N.getConnectionGUILabel("tags_description"), showDescriptions));

		bodyPanel.add(createTagPanel(connection.getTags(), connection::setTags));

		gbc.gridy += 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(15, 25, 10, 25);
		add(bodyPanel, gbc);
	}

	/**
	 * Create a description panel.
	 */
	private JPanel createDescriptionPanel(String header, String description, boolean showDescription) {
		JPanel descPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JLabel headerLabel = new JLabel(header);
		headerLabel.setFont(OPEN_SANS_SEMIBOLD_14);
		descPanel.add(headerLabel, gbc);

		if (showDescription) {
			gbc.gridy += 1;
			JLabel descLabel = new FixedWidthLabel(300, description);
			descLabel.setFont(OPEN_SANS_12);
			descPanel.add(descLabel, gbc);
			if (!isTypeKnown) {
				descLabel.setForeground(UNKNOWN_TYPE_COLOR);
			}
		}

		gbc.gridy += 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		descPanel.add(new JLabel(), gbc);

		if (!isTypeKnown) {
			headerLabel.setForeground(UNKNOWN_TYPE_COLOR);
		}

		return descPanel;
	}

	/**
	 * Creates the type component which is either the type i18n and the origin name (if known), or an indicator that the
	 * origin is unknown and a link to check the marketplace.
	 *
	 * @return the component
	 */
	private JComponent createTypeComponent(String connectionType) {
		JComponent typeComponent;
		int prefixSeparatorIndex = connectionType.indexOf(':');
		String namespace = null;
		String providerName;
		if (prefixSeparatorIndex > 0) {
			namespace = connectionType.substring(0, prefixSeparatorIndex);
			providerName = Optional.ofNullable(Plugin.getPluginByExtensionId("rmx_" + namespace)).map(Plugin::getName).orElse(null);
		} else {
			providerName = OperatorService.RAPID_MINER_CORE_PREFIX;
		}

		if (isTypeKnown) {
			String type = "<html>" + ConnectionI18N.getTypeName(connectionType);
			if (providerName != null) {
				type += "<span style=\"color: #999999; font-size: 13px\"> (" + providerName + ")</span>";
			}
			type += "</html>";
			typeComponent = new JLabel(type);
			typeComponent.setFont(OPEN_SANS_20);
		} else {
			if (namespace != null) {
				JPanel unknownPanel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();

				gbc.gridx = 0;
				gbc.gridy = 0;
				gbc.anchor = GridBagConstraints.WEST;

				JLabel unknownLabel = new JLabel(I18N.getGUILabel("connection.unknown_type.label"));
				unknownLabel.setFont(OPEN_SANS_20);
				unknownLabel.setForeground(UNKNOWN_TYPE_COLOR);
				unknownLabel.setToolTipText(connectionType.substring(prefixSeparatorIndex + 1));
				unknownPanel.add(unknownLabel, gbc);
				gbc.insets.right = 20;
				gbc.gridx += 1;
				unknownPanel.add(new LinkLocalButton(SwingTools.createMarketplaceDownloadActionForNamespace("connection.install_extension_unknown_type", namespace)), gbc);
				typeComponent = unknownPanel;
			} else {
				// old Studio with new connection? No prefix in type, so we cannot help with anything
				typeComponent = new JLabel(I18N.getGUILabel("connection.unknown_type_no_help.label", connectionType));
				typeComponent.setFont(OPEN_SANS_20);
				typeComponent.setForeground(UNKNOWN_TYPE_COLOR);
			}
		}

		return typeComponent;
	}

	/**
	 * Adds a JTextArea with the given text
	 *
	 * @param text
	 * 		The text
	 */
	private JComponent createTextArea(StringProperty text) {
		// Without the TextArea the GUI is collapsing
		JTextArea multi = new JTextArea(text.get());
		multi.setFont(OPEN_SANS_12);
		// update text
		text.addListener(l -> {
			if (!multi.getText().equals(text.get())) {
				SwingTools.invokeLater(() -> multi.setText(text.get()));
			}
		});
		multi.setWrapStyleWord(true);
		multi.setLineWrap(true);
		multi.setEditable(editable);
		if (!editable) {
			multi.setHighlighter(null);
			multi.setComponentPopupMenu(null);
			multi.setInheritsPopupMenu(false);
			multi.setBackground(getBackground());
			multi.setBorder(BorderFactory.createEmptyBorder());
		} else {
			multi.getDocument().addDocumentListener(new TextChangedDocumentListener(text));
			multi.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		}
		if (!isTypeKnown) {
			multi.setForeground(UNKNOWN_TYPE_COLOR);
		}
		return createWithScrollPane(multi);
	}

	/**
	 * Adds the TagPanel
	 *
	 * @param tags
	 * 		The tags
	 * @param setTags
	 * 		A method to set the Tags
	 */
	private JComponent createTagPanel(ObservableList<String> tags, Consumer<List<String>> setTags) {
		return createWithScrollPane(new ConnectionTagEditPanel(tags, setTags, editable, isTypeKnown));
	}


	/**
	 * Creates the component inside a JScrollPane.
	 *
	 * @param component
	 * 		the component to scroll
	 */
	private JScrollPane createWithScrollPane(JComponent component) {
		JScrollPane scrollPane = new ExtendedJScrollPane(component);
		scrollPane.setBorder(editable ? BorderFactory.createLineBorder(Colors.TEXTFIELD_BORDER) : BorderFactory.createEmptyBorder());
		scrollPane.setBackground(component.getBackground());
		scrollPane.setMinimumSize(TEXTAREA_SIZE);
		scrollPane.setPreferredSize(TEXTAREA_SIZE);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return scrollPane;
	}

}

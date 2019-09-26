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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLDocument;

import com.rapidminer.connection.gui.ValueProviderGUIProvider;
import com.rapidminer.connection.gui.ValueProviderGUIRegistry;
import com.rapidminer.connection.gui.model.ConnectionModelConverter;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.gui.model.ValueProviderModel;
import com.rapidminer.connection.gui.model.ValueProviderParameterModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.ExtendedHTMLJEditorPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.RMUrlHandler;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;


/**
 * Placeholder label that should be shown instead of an injected parameter.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public class InjectedParameterPlaceholderLabel extends JPanel {

	private static final String INJECTED_PARAMETER = ConnectionI18N.getConnectionGUILabel("injected_parameter");
	private static final ImageIcon INJECTED_ICON = SwingTools.createIcon(ConnectionI18N.getConnectionGUIMessage("injected_parameter.icon"));

	private final JEditorPane editorPane = new ExtendedHTMLJEditorPane("text/html", INJECTED_PARAMETER);

	private final transient ChangeListener<String> valueProviderChanged;
	private final transient ListChangeListener<ValueProviderParameterModel> valueProviderParameterChanged;

	public InjectedParameterPlaceholderLabel(ConnectionParameterModel param) {
		super(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		JLabel icon = new JLabel("", INJECTED_ICON, JLabel.LEFT);
		add(icon, gbc);
		Font font = icon.getFont();
		String bodyRule = "body { font-family: " + font.getFamily() + "; " +
				"font-size: " + font.getSize() + "pt; }";
		((HTMLDocument) editorPane.getDocument()).getStyleSheet().addRule(bodyRule);
		editorPane.setOpaque(false);
		editorPane.setBorder(null);
		editorPane.setEditable(false);
		editorPane.addHyperlinkListener(event -> {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED && event.getURL() != null) {
				RMUrlHandler.openInBrowser(event.getURL());
			}
		});
		gbc.insets.left = icon.getIconTextGap();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(editorPane, gbc);

		valueProviderChanged = (i, o, n) -> updateText(param);
		valueProviderParameterChanged = c -> {
			while (c.next()) {
				for (ValueProviderParameterModel valueProviderParameterModel : c.getAddedSubList()) {
					valueProviderParameterModel.valueProperty().removeListener(valueProviderChanged);
					valueProviderParameterModel.valueProperty().addListener(valueProviderChanged);
				}
			}
			updateText(param);
		};
		param.injectorNameProperty().addListener((i, o, n) -> registerListener(param));
		registerListener(param);
	}

	/**
	 * Registers listener on the Value Provider
	 *
	 * @param param
	 * 		the connection parameter model
	 */
	private void registerListener(ConnectionParameterModel param) {
		ValueProviderModel valueProvider = param.getValueProvider();
		if (valueProvider == null) {
			editorPane.setText(INJECTED_PARAMETER);
			return;
		}
		valueProvider.parametersProperty().removeListener(valueProviderParameterChanged);
		valueProvider.parametersProperty().addListener(valueProviderParameterChanged);

		for (ValueProviderParameterModel parameter : valueProvider.parametersProperty()) {
			parameter.valueProperty().removeListener(valueProviderChanged);
			parameter.valueProperty().addListener(valueProviderChanged);
		}

		updateText(param);
	}

	/**
	 * Updates the text of the editorPane if the value provider is updated
	 *
	 * @param param
	 * 		the parameter model
	 */
	private void updateText(ConnectionParameterModel param) {
		ValueProviderModel valueProvider = param.getValueProvider();

		if (valueProvider == null) {
			return;
		}
		ValueProviderGUIProvider guiProvider =  ValueProviderGUIRegistry.INSTANCE.getGUIProvider(valueProvider.getType());
		String hint = guiProvider.getCustomLabel(ValueProviderGUIProvider.CustomLabel.INJECTED_PARAMETER, valueProvider, ConnectionModelConverter.getConnection(param), param.getGroupName(), param.getName());

		if (hint != null) {
			editorPane.setText(hint);
			return;
		}

		String fallback = ConnectionI18N.getConnectionGUIMessageOrNull("valueprovider.hint.injected_parameter_template.label", valueProvider.getName());
		if (fallback != null) {
			editorPane.setText(fallback.trim());
		} else {
			editorPane.setText(INJECTED_PARAMETER);
		}
	}

}

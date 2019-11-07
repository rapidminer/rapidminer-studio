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
package com.rapidminer.tools.mail.connection.gui;

import static com.rapidminer.tools.mail.connection.MailConnectionHandler.GROUP_MAIL;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.GROUP_SENDMAIL;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.GROUP_SMTP;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.PARAMETER_MAILTYPE_AUTHENTICATION;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.PARAMETER_MAILTYPE_SECURITY;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.PARAMETER_MAIL_METHOD;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.SEND;
import static com.rapidminer.tools.mail.connection.MailConnectionHandler.values;

import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;

import com.rapidminer.RapidMiner;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.DefaultConnectionGUI;
import com.rapidminer.connection.gui.components.InjectableComponentWrapper;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.MailUtilities;
import com.rapidminer.tools.mail.connection.MailConnectionHandler;

/**
 * The GUI for connections handled by {@link MailConnectionHandler}.
 *
 * @author Jan Czogalla
 * @since 9.4.1
 */
public class MailConnectionGUI extends DefaultConnectionGUI {

	private static final Map<String, String[]> POSSIBLE_VALUES_MAP;
	static {
		HashMap<String, String[]> possibleValuesMap = new HashMap<>();
		possibleValuesMap.put(GROUP_MAIL + '.' + PARAMETER_MAIL_METHOD, RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_VALUES);
		for (MailConnectionHandler handler : values()) {
			possibleValuesMap.put(handler.getMailTypeGroup() + '.' + PARAMETER_MAILTYPE_SECURITY, RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_SECURITY_VALUES);
			possibleValuesMap.put(handler.getMailTypeGroup() + '.' + PARAMETER_MAILTYPE_AUTHENTICATION, RapidMiner.PROPERTY_RAPIDMINER_TOOLS_SMTP_AUTHENTICATION_VALUES);
		}
		POSSIBLE_VALUES_MAP = Collections.unmodifiableMap(possibleValuesMap);
	}

	private final String conType;
	private MailConnectionHandler handler;

	public MailConnectionGUI(Window parent, ConnectionInformation connection, RepositoryLocation location, boolean editable) {
		super(parent, connection, location, editable);
		conType = connection.getConfiguration().getType();
		handler = Arrays.stream(values()).filter(mailHandler -> mailHandler.getType().equals(conType))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("invalid mail connection type " + conType));
		if (handler == SEND && editable) {
			ConnectionModel connectionModel = getConnectionModel();
			connectionModel.getParameter(GROUP_MAIL, PARAMETER_MAIL_METHOD).valueProperty()
					.addListener((observable, oldValue, newValue) -> updateSMTPUsage(newValue, connectionModel));

		}
	}

	@Override
	public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel) {
		if (groupModel == null || !handler.getMailTypeGroup().equals(groupModel.getName())) {
			return null;
		}
		if (handler != SEND) {
			return super.getComponentForGroup(groupModel, connectionModel);
		}
		ConnectionParameterGroupModel mailGroupModel = connectionModel.getParameterGroup(GROUP_MAIL);
		ConnectionParameterGroupModel sendmailGroupModel = connectionModel.getParameterGroup(GROUP_SENDMAIL);

		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(combinedGroupComponent(connectionModel, mailGroupModel, sendmailGroupModel, groupModel));
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBorder(null);
		return scrollPane;
	}

	@Override
	protected JComponent getParameterLabelComponent(String type, ConnectionParameterModel parameter) {
		return visibilityWrapper(super.getParameterLabelComponent(type, parameter), parameter);
	}

	@Override
	protected JComponent getParameterInputComponent(String type, ConnectionParameterModel parameter) {
		JComponent inputComponent;
		String[] possibleValues = POSSIBLE_VALUES_MAP.get(parameter.getGroupName() + '.' + parameter.getName());
		if (parameter.isEditable() && possibleValues != null && possibleValues.length > 0) {
			JComboBox<String> comboBox = new JComboBox<>(possibleValues);
			comboBox.setSelectedItem(possibleValues[0]);
			if (parameter.getValue() != null) {
				comboBox.setSelectedItem(parameter.getValue());
			}

			comboBox.addItemListener(event -> {
				if (event.getStateChange() == ItemEvent.SELECTED) {
					parameter.setValue(event.getItem().toString());
				}
			});
			inputComponent = new InjectableComponentWrapper(comboBox, parameter);
		} else {
			inputComponent = super.getParameterInputComponent(type, parameter);
		}
		return inputComponent;
	}

	/** Wrap the component with both an information icon as well as a visbility wrapper */
	@Override
	protected JComponent wrapInformationIcon(ConnectionModel connectionModel, ConnectionParameterModel parameter, JComponent component) {
		return visibilityWrapper(super.wrapInformationIcon(connectionModel, parameter, component), parameter);
	}

	@Override
	public List<ConnectionParameterModel> getInjectableParameters(ConnectionParameterGroupModel group) {
		List<ConnectionParameterModel> mainParameters = super.getInjectableParameters(group);
		List<ConnectionParameterModel> injectableParameters = new ArrayList<>();
		if (handler == SEND) {
			ConnectionParameterGroupModel mailGroupModel = connectionModel.getParameterGroup(GROUP_MAIL);
			ConnectionParameterGroupModel sendmailGroupModel = connectionModel.getParameterGroup(GROUP_SENDMAIL);
			injectableParameters.addAll(super.getInjectableParameters(mailGroupModel));
			injectableParameters.addAll(super.getInjectableParameters(sendmailGroupModel));
		}
		injectableParameters.addAll(mainParameters);
		injectableParameters.removeIf(p -> !p.isEnabled());
		return Collections.unmodifiableList(injectableParameters);
	}

	/** Update the enabled state of the parameters when switching between sendmail and smtp */
	private void updateSMTPUsage(String newValue, ConnectionModel connectionModel) {
		boolean useSMTP = MailUtilities.getMailMethodIndex(newValue) == RapidMiner.PROPERTY_RAPIDMINER_TOOLS_MAIL_METHOD_SMTP;
		ConnectionParameterGroupModel sendmailGroup = connectionModel.getParameterGroup(GROUP_SENDMAIL);
		ConnectionParameterGroupModel smtpGroup = connectionModel.getParameterGroup(GROUP_SMTP);
		sendmailGroup.getParameters().forEach(p -> p.setEnabled(!useSMTP));
		smtpGroup.getParameters().forEach(p -> p.setEnabled(useSMTP));
	}
}

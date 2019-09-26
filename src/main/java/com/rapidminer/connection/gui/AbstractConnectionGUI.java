/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.connection.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.components.ConnectionInfoPanel;
import com.rapidminer.connection.gui.components.ConnectionSourcesPanel;
import com.rapidminer.connection.gui.components.InjectionPanel;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionModelConverter;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.gui.model.ValueProviderModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Abstract UI for editing connections. It takes care of providing elements that are always the same and therefore
 * covered by the RapidMiner Studio core, like the injection configuration and the tab structure for groups. However,
 * some behavior can be changed by overriding the protected methods.
 *
 * @author Jonas Wilms-Pfau
 * @since 9.3.0
 */
public abstract class AbstractConnectionGUI implements ConnectionGUI {

	/**
	 * Default horizontal distance between components
	 */
	public static final int HORIZONTAL_COMPONENT_SPACING = 16;

	/**
	 * Default vertical distance between components
	 */
	public static final int VERTICAL_COMPONENT_SPACING = 12;

	/**
	 * Default spacing used by the tab panels
	 */
	public static final Border DEFAULT_PANEL_BORDER = BorderFactory.createEmptyBorder(16, 32, 0, 16);

	/**
	 * The description string used in the tooltip
	 */
	private static final String TOOLTIP_DESCRIPTION = ConnectionI18N.getConnectionGUILabel("tooltip.description");
	/**
	 * The full key string used in the tooltip
	 */
	private static final String TOOLTIP_FULL_KEY = ConnectionI18N.getConnectionGUILabel("tooltip.full_key");

	/** Closing tags that would stop the Swing HTML parser */
	private static final Pattern CLOSING_TAGS = Pattern.compile("</(body|html)>", Pattern.CASE_INSENSITIVE);

	/**
	 * the connection object
	 */
	protected ConnectionInformation connection;

	/**
	 * the connection model
	 */
	protected ConnectionModel connectionModel;

	/**
	 * the tabbed pane that contains the info tab and all connection specific tabs
	 */
	private final JTabbedPane tabbedPane = new ExtendedJTabbedPane();

	/** the parent window */
	private final Window parent;

	/**
	 * the parent dialog
	 */
	private final JDialog parentDialog;

	/**
	 * Creates a new abstract connection gui
	 *  @param parent
	 * 		the parent window
	 * @param connection
	 * 		the edited connection
	 * @param location
	 * 		the location of the connection
	 * @param editable
	 * 		if this UI is in edit mode ({@code true}) or view mode ({@code false})
	 */
	public AbstractConnectionGUI(Window parent, ConnectionInformation connection,
								 RepositoryLocation location, boolean editable) {
		this.connection = connection;
		this.connectionModel = ConnectionModelConverter.fromConnection(connection, location, editable);
		this.parent = parent;
		this.parentDialog = parent instanceof JDialog ? (JDialog) parent : null;
	}

	/**
	 * Returns the connection edit dialog
	 *
	 * @return the outer dialog
	 */
	protected Window getParent() {
		return parent;
	}

	/**
	 * Returns the connection edit dialog
	 *
	 * @return the outer dialog
	 */
	protected JDialog getParentDialog() {
		return parentDialog;
	}

	@Override
	public ConnectionInformation getConnection() {
		return ConnectionModelConverter.applyConnectionModel(connection, connectionModel);
	}

	@Override
	public synchronized JComponent getConnectionEditComponent() {
		// this method is only triggered once
		getTabbedPane().removeAll();
		addInfoTab();
		addGroupTabs();
		addSourcesTab();
		return getTabbedPane();
	}

	/**
	 * Returns the injectable parameters the the given group
	 *
	 * @param group
	 * 		the group that is displayed
	 * @return the injectable parameter for the group
	 */
	@Override
	public List<ConnectionParameterModel> getInjectableParameters(ConnectionParameterGroupModel group) {
		return group.getParameters();
	}

	/**
	 * Returns the {@link JComponent} for a group that is used inside a tabbed pane.
	 * <p>The tab title is queried via the composite i18n key {@code gui.label.connection.group.{type}.{group}.label}
	 * </p>
	 *
	 * @param groupModel
	 * 		the model for the group that is displayed in this tab
	 * @param connectionModel
	 * 		the model for the entire connection
	 * @return the component for this group tab, or {@code null} iff that group does not need a UI for whatever reason
	 */
	protected abstract JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connectionModel);

	/**
	 * Returns the panel for the first tab "Info". Override / extend if the default info tab does not fully cover your
	 * needs.
	 * <p>
	 * Note: The general layout of the info tab should not be changed.
	 * </p>
	 *
	 * @return the panel for the first tab
	 */
	protected JPanel getInfoPanel() {
		return new ConnectionInfoPanel(getConnectionModel());
	}

	/**
	 * The model backing the connection UI.
	 *
	 * @return the connection model, never {@code null}
	 */
	protected ConnectionModel getConnectionModel() {
		return connectionModel;
	}

	/**
	 * Returns the tabbed pane
	 *
	 * @return the {@link JTabbedPane}
	 */
	private JTabbedPane getTabbedPane() {
		return tabbedPane;
	}

	/**
	 * The injection panel contains the injection button and the help text
	 *
	 * @return the injection panel
	 */
	private JPanel getInjectionPanel(Supplier<List<ConnectionParameterModel>> injectableParameters) {
		return new InjectionPanel(getParentDialog(), getConnectionModel().getType(), injectableParameters, getConnectionModel().getValueProviders(), this::setInjected);
	}

	/**
	 * Adds the info tab to the tabbed pane
	 */
	private void addInfoTab() {
		getTabbedPane().addTab(ConnectionI18N.getConnectionGUILabel("info_panel"), getInfoPanel());
	}

	/**
	 * Adds all connection specific tabs
	 */
	private void addGroupTabs() {
		for (ConnectionParameterGroupModel group : getDisplayedGroups()) {
			addInjectableTab(group, getComponentForGroup(group, connectionModel));
		}
	}

	/**
	 * @return the displayed groups, or an empty list if the connection type is unknown
	 */
	protected List<ConnectionParameterGroupModel> getDisplayedGroups() {
		if (!ConnectionHandlerRegistry.getInstance().isTypeKnown(getConnectionModel().getType())) {
			return Collections.emptyList();
		}
		return getConnectionModel().getParameterGroups();
	}

	/**
	 * Adds a tab that contains the given component as well as the injection button on the bottom
	 *
	 * @param group
	 * 		the group that is used for i18n and {@link #getInjectableParameters(ConnectionParameterGroupModel)}
	 * @param component
	 * 		the content of the tab without the inject functionality
	 */
	protected void addInjectableTab(ConnectionParameterGroupModel group, JComponent component) {
		JPanel panelAndInject = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		if (component == null) {
			return;
		}

		c.anchor = GridBagConstraints.WEST;
		c.weighty = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		panelAndInject.add(component, c);

		if (getConnectionModel().isEditable()) {
			c.weighty = 0;
			panelAndInject.add(new JSeparator(JSeparator.HORIZONTAL), c);

			c.anchor = GridBagConstraints.SOUTHWEST;
			panelAndInject.add(getInjectionPanel(() -> getInjectableParameters(group)), c);
		}

		String typeKey = getConnectionModel().getType();
		String groupKey = group.getName();
		String title = ConnectionI18N.getGroupName(typeKey, groupKey, ConnectionI18N.LABEL_SUFFIX, groupKey);
		String tip = ConnectionI18N.getGroupName(typeKey, groupKey, ConnectionI18N.TIP_SUFFIX, null);
		Icon icon = ConnectionI18N.getGroupIcon(typeKey, groupKey);

		getTabbedPane().addTab(title, icon, panelAndInject, tip);
	}

	@Override
	public void validationResult(Map<String, String> parameterErrorMap) {
		// set potential errors
		for (ConnectionParameterGroupModel parameterGroup : getConnectionModel().getParameterGroups()) {
			for (ConnectionParameterModel parameter : parameterGroup.getParameters()) {
				String fullKey = parameterGroup.getName() + "." + parameter.getName();
				parameter.validationErrorProperty().setValue(parameterErrorMap.getOrDefault(fullKey, null));
			}
		}
	}

	@Override
	public boolean preSaveCheck() {
		List<String> configuredUnusedVPs =
				connectionModel.valueProvidersProperty().stream()
						// filter vps that have set parameters
						.filter(vp -> vp.getParameters().stream()
								.anyMatch(p -> p.getValue() != null && !p.getValue().isEmpty()))
						// filter vps that are not used
						.filter(vp -> connectionModel.getParameterGroups().stream()
								.noneMatch(group -> group.getParameters().stream()
										.anyMatch(param -> vp.getName().equals(param.getInjectorName()))))
						.map(ValueProviderModel::getName)
						.collect(Collectors.toList());
		if (configuredUnusedVPs.isEmpty()) {
			return true;
		}
		String vpsString = configuredUnusedVPs.get(0);
		if (configuredUnusedVPs.size() > 1) {
			vpsString += ", " + configuredUnusedVPs.get(1);
			if (configuredUnusedVPs.size() > 2) {
				vpsString += ", " + configuredUnusedVPs.get(2);
				if (configuredUnusedVPs.size() > 3) {
					vpsString += ", ...";
				}
			}
		}
		String finalArgument = vpsString;
		return SwingTools.invokeAndWaitWithResult(() -> {
			ConfirmDialog dialog = new ConfirmDialog(parent, "connection_unused_source",
					ConfirmDialog.OK_CANCEL_OPTION, false, finalArgument) {
				@Override
				protected JButton makeOkButton() {
					return makeOkButton("connection.save_anyway");
				}

				@Override
				protected JButton makeCancelButton() {
					return makeCancelButton("connection.back_to_editing");
				}
			};
			dialog.setVisible(true);
			return dialog.wasConfirmed();
		});
	}

	/**
	 * Adds the Sources tab to the tabbed pane
	 */
	protected void addSourcesTab() {
		final JPanel sourcesPanel = getSourcesPanel();
		if (sourcesPanel != null) {
			getTabbedPane().addTab(ConnectionI18N.getConnectionGUILabel("sources_panel"), sourcesPanel);
		}
	}

	/**
	 * Returns the panel for the first tab "Info"
	 *
	 * @return the panel for the first tab
	 */
	protected JPanel getSourcesPanel() {
		return new ConnectionSourcesPanel(getParentDialog(), getConnectionModel());
	}

	/**
	 * Updates the injected parameters
	 *
	 * @param parameterModels
	 * 		the parameter models
	 */
	private void setInjected(List<ConnectionParameterModel> parameterModels) {
		for (ConnectionParameterModel parameter : parameterModels) {
			ConnectionParameterModel parameterModel = getConnectionModel().getParameter(parameter.getGroupName(), parameter.getName());
			if (parameterModel != null) {
				parameterModel.setInjectorName(parameter.getInjectorName());
			}
		}
	}

	/**
	 * Wraps the given input component in a panel and adds an information icon with a tooltip. The tooltip i18n is
	 * derived from the type and the parameter.
	 *
	 * @param parameterInputComponent
	 * 		the component to wrap
	 * @param type
	 * 		the type of the connection the parameter belongs to
	 * @param parameter
	 * 		the connection parameter
	 * @param parent
	 * 		the parent dialog
	 * @return a new panel containing the old and an additional information icon with tooltip
	 * @see ConnectionI18N#getParameterTooltip(String, String, String, String)
	 */
	public static JPanel addInformationIcon(JComponent parameterInputComponent, String type,
											ConnectionParameterModel parameter, JDialog parent) {
		return addInformationIcon(parameterInputComponent, type, parameter.getGroupName(), parameter.getName(), parent);
	}

	/**
	 * Wraps the given input component in a panel and adds an information icon with a tooltip. The tooltip i18n is
	 * derived from the type, group and parameter name as {@code gui.label.connection.parameter.{type}.{group}.{
	 * parameterName}.tip} and contains the full key of the parameter.
	 *
	 * @param parameterInputComponent
	 * 		the component to wrap
	 * @param type
	 * 		the type of the connection the parameter belongs to
	 * @param group
	 * 		the group the parameter belongs to
	 * @param parameterName
	 * 		the name of the parameter
	 * @param parent
	 * 		the parent dialog
	 * @return a new panel containing the old and an additional information icon with tooltip
	 * @see ConnectionI18N#getParameterTooltip(String, String, String, String)
	 */
	public static JPanel addInformationIcon(JComponent parameterInputComponent, String type, String group,
											String parameterName, JDialog parent) {
		// Similar to com.rapidminer.gui.properties.PropertyPanel#getToolTipText
		String name = ConnectionI18N.getParameterName(type, group, parameterName, parameterName);
		StringBuilder fullTooltip = new StringBuilder();
		fullTooltip.append("<h3 style='padding-bottom:4px'>").append(name).append("</h3>");
		String description = ConnectionI18N.getParameterTooltip(type, group, parameterName, null);
		if (description != null) {
			fullTooltip.append("<p style='padding-bottom:4px'>");
			fullTooltip.append("<b>").append(TOOLTIP_DESCRIPTION).append(": </b>");
			fullTooltip.append(CLOSING_TAGS.matcher(description).replaceAll(""));
			fullTooltip.append("</p>");
		}
		fullTooltip.append("<p style='padding-bottom:4px'>");
		fullTooltip.append("<b>").append(TOOLTIP_FULL_KEY).append(": </b>");
		fullTooltip.append(group).append(".").append(parameterName);
		fullTooltip.append("</p>");
		return addInformationIcon(parameterInputComponent, fullTooltip.toString(), parent);
	}

	/**
	 * Wraps the given input component in a panel and adds an information icon with a tooltip with the specified text.
	 *
	 * @param parameterInputComponent
	 * 		the the component to wrap
	 * @param toolTipText
	 * 		the tooltip text to add
	 * @param parent
	 * 		the parent dialog
	 * @return a new panel containing the old and an additional information icon with tooltip
	 */
	public static JPanel addInformationIcon(JComponent parameterInputComponent, String toolTipText, JDialog parent) {
		JPanel informationWrapper = new JPanel(new BorderLayout());
		informationWrapper.add(parameterInputComponent, BorderLayout.CENTER);
		SwingTools.addTooltipHelpIconToLabel(toolTipText, informationWrapper, parent);
		return informationWrapper;
	}

	/**
	 * Updates and links visibility of the component to its parameter's enabled state.
	 *
	 * @param component
	 * 		the component to be wrapped; must not be {@code null}
	 * @param parameter
	 * 		the parameter, whose enabled state should be listened to; must not be {@code null}
	 * @return the component with visibility awareness
	 * @since 9.4.1
	 */
	public static JComponent visibilityWrapper(JComponent component, ConnectionParameterModel parameter) {
		parameter.enabledProperty().addListener((observable, oldValue, newValue) -> SwingTools.invokeLater(() -> component.setVisible(newValue)));
		component.setVisible(parameter.isEnabled());
		return component;
	}

}

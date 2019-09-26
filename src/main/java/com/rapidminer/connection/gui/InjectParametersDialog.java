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
package com.rapidminer.connection.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.apache.commons.lang.StringUtils;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.components.ConnectionSourcesPanel;
import com.rapidminer.connection.gui.model.ConnectionModelConverter;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.connection.gui.model.InjectParametersModel;
import com.rapidminer.connection.util.ConnectionI18N;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.valueprovider.ValueProvider;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandler;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.gui.look.icons.EmptyIcon;
import com.rapidminer.gui.tools.ExtendedJComboBox;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterListener;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ValidationUtil;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * The InjectParametersDialog shows the dialog to edit injection settings for connection parameters.
 *
 * @author Andreas Timm
 * @since 9.3
 */
public class InjectParametersDialog extends JDialog {

	// Width for all comboboxes
	private static final int COMBOBOX_WIDTH = 297;
	// Icons for properly or not properly configured value providers
	private static final ImageIcon ICON_WARNING = SwingTools.createIcon("16/" + I18N.getGUIMessage(ConnectionSourcesPanel.NOT_WELL_CONFIGURED_VALUE_PROVIDER_ICON));
	// a large main icon used in the top left corner
	private static final ImageIcon LARGE_CONNECTION_ICON = SwingTools.createIcon("48/" + I18N.getGUIMessage("gui.dialog.inject_connection_parameter.icon"));
	// Icon and hover-icon to show when using the search and give the ability to clear the search
	private static final ImageIcon CLEAR_FILTER_HIGHLIGHT_ICON = SwingTools.createIcon("16/" + I18N.getGUIMessage("gui.action.inject_connection_clear_filter.highlight_icon"));
	private static final ImageIcon NO_RESULTS_ICON = SwingTools.createIcon("16/" + I18N.getGUIMessage("gui.dialog.inject_connection_parameter.no_results.icon"));

	// A generic warning panel to display if a malconfigured value provider was chosen
	private static final JLabel WARNING_PANEL = new JLabel(I18N.getGUIMessage("gui.dialog.inject_connection.value_provider_configuration_warning"), ICON_WARNING, SwingConstants.LEFT);
	// an empty panel to show if there is no warning to be shown
	private static final JPanel EMPTY_PANEL = new JPanel();
	// default value provider to use if unknown
	private static final int DEFAULT_VALUE_PROVIDER_INDEX = 0;
	// empty icon so comboboxes won't jump around
	private static final EmptyIcon EMPTY_ICON = new EmptyIcon(0, 16);

	/**
	 * The data to use
	 */
	private final InjectParametersModel data;
	/**
	 * The connection type
	 */
	private final String type;
	/**
	 * Scrollable parameter list, update can be necessary
	 */
	private JPanel parameterPanel = new JPanel(new GridBagLayout());
	/**
	 * Content of the search field for filtering the parameters
	 */
	private String searchValue = "";
	/**
	 * Init on creation the set of properly configured ValueProviders to use this information to show icons and the
	 * warning panel.
	 */
	private Set<String> wellConfiguredVps = new HashSet<>();
	/**
	 * To be changed upon closing the dialog via OK button, else this default is sufficient
	 */
	private boolean wasConfirmed = false;


	/**
	 * Renderer to show icons for the {@link ValueProvider} configuration state
	 */
	private static final class ValueProviderRenderer implements javax.swing.ListCellRenderer<ValueProvider> {

		private final DefaultListCellRenderer renderer = new DefaultListCellRenderer();
		private final ConnectionParameterModel parameter;
		private Set<String> wellConfiguredVps;

		private ValueProviderRenderer(ConnectionParameterModel parameter, Set<String> wellConfiguredVps) {
			this.parameter = parameter;
			this.wellConfiguredVps = wellConfiguredVps;
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends ValueProvider> list, ValueProvider vp, int index, boolean isSelected, boolean cellHasFocus) {
			renderer.getListCellRendererComponent(list, vp, index, isSelected, cellHasFocus);
			ValueProviderGUIProvider guiProvider = ValueProviderGUIRegistry.INSTANCE.getGUIProvider(vp.getType());
			String hint = guiProvider.getCustomLabel(ValueProviderGUIProvider.CustomLabel.INJECTOR_SELECTION, vp, ConnectionModelConverter.getConnection(parameter), parameter.getGroupName(), parameter.getName());
			String text = hint == null ? vp.getName() : hint;
			renderer.setText(text);
			if (wellConfiguredVps.contains(vp.getName())) {
				renderer.setIcon(EMPTY_ICON);
				renderer.setToolTipText(null);
			} else {
				renderer.setIcon(ICON_WARNING);
				renderer.setToolTipText(I18N.getGUIMessage("gui.dialog.connection.valueprovider.needs_configuration.tip"));
			}
			return renderer;
		}
	}

	/**
	 * Renderer for the empty combobox, automatically producing the correct height
	 */
	private static final DefaultListCellRenderer EMPTY_DEFAULT_LIST_CELL_RENDERER = new DefaultListCellRenderer() {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			this.setText((String) value);
			this.setIcon(EMPTY_ICON);
			return this;
		}
	};


	/**
	 * Create a new instance that immediately shows the dialog for the given data as a
	 *
	 * @param owner
	 * 		a {@link Window} that is the parent for this modal dialog
	 * @param type
	 *      the {@link com.rapidminer.connection.gui.model.ConnectionModel#getType() connection type}
	 * @param key
	 * 		the i18n key used for the properties gui.dialog.-key-.title and gui.dialog.-key-.icon
	 * @param data
	 * 		the data, containing parameters to be altered and available {@link ValueProvider ValueProviders}
	 */
	public InjectParametersDialog(Window owner, String type, String key, InjectParametersModel data) {
		super(ValidationUtil.requireNonNull(owner, "owner"), I18N.getMessage(I18N.getGUIBundle(), "gui.dialog." + key + ".title"), Dialog.ModalityType.APPLICATION_MODAL);

		ValidationUtil.requireNonNull(data, "data");
		ValidationUtil.requireNonNull(type, "type");
		this.data = data;
		this.type = type;

		checkVPConfigurations();

		FilterTextField tf = new FilterTextField(25);
		FilterListener filterListener = filter -> {
			searchValue = tf.getText();
			SwingTools.invokeLater(this::fillParameterPanel);
		};
		TextFieldWithAction textField = new TextFieldWithAction(tf, new ResourceAction("inject_connection_clear_filter") {
			@Override
			public void loggedActionPerformed(ActionEvent e) {
				tf.setText("");
				filterListener.valueChanged("");
			}
		}, CLEAR_FILTER_HIGHLIGHT_ICON);

		tf.setDefaultFilterText(I18N.getGUIMessage("gui.field.inject_parameters.prompt"));
		tf.addFilterListener(filterListener);

		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(parameterPanel);
		scrollPane.setBorder(null);

		JPanel buttonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbcb = new GridBagConstraints();
		gbcb.gridx = 0;
		gbcb.gridy = 0;
		gbcb.anchor = GridBagConstraints.WEST;
		gbcb.fill = GridBagConstraints.HORIZONTAL;
		gbcb.insets = new Insets(8, 16, 10, 10);
		gbcb.weightx = 1;
		buttonPanel.add(WARNING_PANEL, gbcb);
		buttonPanel.add(EMPTY_PANEL, gbcb);

		final ResourceAction saveAction = new ResourceAction("connection.save_injection") {
			@Override
			public void loggedActionPerformed(ActionEvent e) {
				wasConfirmed = true;
				data.setChangedParameters();
				dispose();
			}
		};
		gbcb.weightx = 0;
		gbcb.insets.left = 0;
		gbcb.gridx += 1;
		buttonPanel.add(new JButton(saveAction), gbcb);
		final ResourceAction cancelAction = new ResourceAction("connection.cancel_injection_edit") {
			@Override
			public void loggedActionPerformed(ActionEvent e) {
				data.resetChangedParameters();
				dispose();
			}
		};

		gbcb.gridx += 1;
		buttonPanel.add(new JButton(cancelAction), gbcb);

		final JPanel topPanel = new JPanel(new BorderLayout());
		final JLabel comp = new JLabel(I18N.getGUIMessage("gui.dialog.inject_connection_parameter.message"), LARGE_CONNECTION_ICON, SwingConstants.LEFT);
		topPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
		comp.setIconTextGap(8);
		comp.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

		topPanel.add(comp, BorderLayout.CENTER);
		topPanel.add(textField, BorderLayout.SOUTH);

		final JPanel pane = new JPanel(new BorderLayout());
		pane.add(topPanel, BorderLayout.NORTH);
		pane.add(scrollPane, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.SOUTH);
		setContentPane(pane);

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// check changes on close
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancelAction.actionPerformed(null);
			}

		});
		// close dialog with ESC
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CLOSE");
		getRootPane().getActionMap().put("CLOSE", cancelAction);


		fillParameterPanel();
		checkAndShowWarning();

		ActionStatisticsCollector.getInstance().log(ActionStatisticsCollector.TYPE_INJECT_VALUE_PROVIDER_DIALOG, key, "open");
		pack();
		setLocationRelativeTo(getOwner());
	}

	/**
	 * Check the available value providers from the data and keep information if they are properly configured
	 */
	private void checkVPConfigurations() {
		ConnectionInformation connection = null;
		if (!data.getParameters().isEmpty()) {
			connection = ConnectionModelConverter.getConnection(data.getParameters().get(0));
		}
		for (ValueProvider vp : data.getValueProviders()) {
			if (ValueProviderHandlerRegistry.getInstance().isTypeKnown(vp.getType())) {
				final ValueProviderHandler handler = ValueProviderHandlerRegistry.getInstance().getHandler(vp.getType());
				if (handler.validate(vp, connection).getType() == TestResult.ResultType.SUCCESS) {
					wellConfiguredVps.add(vp.getName());
				}
			}
		}
	}

	/**
	 * After instantiation of this class the thread is blocked until the dialog was closed, call this method to receive
	 * the user input so if the user clicked on the OK button, which would return true.
	 *
	 * @return if the user closed the dialog using the OK button {@code true}, else {@code false}
	 */
	public boolean wasConfirmed() {
		return wasConfirmed;
	}

	/**
	 * Set the content of the scrollpane aka parameterPanel.
	 */
	private void fillParameterPanel() {
		parameterPanel.removeAll();

		final ValueProvider[] valueProviders = data.getValueProviders().toArray(new ValueProvider[0]);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 0, 0);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		List<ConnectionParameterModel> filteredParameters = getFilteredParameters();
		for (ConnectionParameterModel parameter : filteredParameters) {
			ExtendedJComboBox<String> selectBox = new ExtendedJComboBox<>(new String[]{I18N.getGUIMessage("gui.dialog.inject_connection.select")});
			selectBox.setEnabled(false);
			selectBox.setVisible(!parameter.isInjected());
			selectBox.setPreferredWidth(COMBOBOX_WIDTH);
			selectBox.setRenderer(EMPTY_DEFAULT_LIST_CELL_RENDERER);

			ExtendedJComboBox<ValueProvider> comboBox;
			comboBox = new ExtendedJComboBox<>(valueProviders);
			if (parameter.getInjectorName() != null) {
				Optional<ValueProvider> optionalValueProvider = Arrays.stream(valueProviders).filter(vp -> vp.getName().equals(parameter.getInjectorName())).findFirst();
				optionalValueProvider.ifPresent(comboBox::setSelectedItem);
			}
			comboBox.setEnabled(parameter.isEditable());
			comboBox.setVisible(parameter.isInjected());
			comboBox.setPreferredWidth(COMBOBOX_WIDTH);
			comboBox.addActionListener(a -> {
				final ValueProvider selectedItem = (ValueProvider) comboBox.getSelectedItem();
				if (selectedItem != null) {
					parameter.setInjectorName(selectedItem.getName());
				}
				checkAndShowWarning();
			});
			comboBox.setRenderer(new ValueProviderRenderer(parameter, wellConfiguredVps));

			gbc.anchor = GridBagConstraints.WEST;
			gbc.gridx = 0;
			gbc.weightx = 0;
			final JCheckBox checkBox = new JCheckBox(ConnectionI18N.getParameterName(type, parameter.getGroupName(), parameter.getName(), parameter.getName()));
			checkBox.setSelected(parameter.isInjected());
			checkBox.setEnabled(parameter.isEditable());
			checkBox.setToolTipText(I18N.getGUILabel("connection.valueprovider.key_for_injection.label", parameter.getName()));
			checkBox.addItemListener(e -> {
				final boolean injected = e.getStateChange() == ItemEvent.SELECTED;
				String name = setParameterInjected(parameter, injected);
				selectBox.setVisible(!injected);
				if (injected && name != null) {
					comboBox.setSelectedIndex(DEFAULT_VALUE_PROVIDER_INDEX);
				}
				comboBox.setVisible(injected);
				checkAndShowWarning();
			});
			parameterPanel.add(checkBox, gbc);

			gbc.anchor = GridBagConstraints.CENTER;
			gbc.weightx = 1;
			gbc.gridx++;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			parameterPanel.add(new JLabel(), gbc);
			gbc.gridx++;
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.NONE;
			int oldri = gbc.insets.right;
			gbc.insets.right = 16;
			parameterPanel.add(comboBox, gbc);
			parameterPanel.add(selectBox, gbc);
			gbc.insets.right = oldri;
			gbc.gridy++;
		}

		if (filteredParameters.isEmpty()) {
			if (data.getParameters().size() > filteredParameters.size()) {
				parameterPanel.add(new JLabel(I18N.getGUIMessage("gui.dialog.inject_connection_parameter.no_results.label"), NO_RESULTS_ICON, SwingConstants.LEFT), gbc);
			} else {
				parameterPanel.add(new JLabel(I18N.getGUIMessage("gui.dialog.inject_connection_parameter.no_parameters.label"), NO_RESULTS_ICON, SwingConstants.LEFT), gbc);
			}
		}

		gbc.weighty = 1;
		parameterPanel.add(new JLabel(), gbc);
		revalidate();
		repaint();
	}

	/**
	 * Check all the parameters and see if any contains a not properly configured value provider, show the info then
	 */
	private void checkAndShowWarning() {
		boolean showWarning = false;
		for (ConnectionParameterModel param : data.getParameters()) {
			if (param.getInjectorName() != null && !wellConfiguredVps.contains(param.getInjectorName())) {
				showWarning = true;
				break;
			}
		}
		WARNING_PANEL.setVisible(showWarning);
		EMPTY_PANEL.setVisible(!showWarning);
	}

	/**
	 * Set the value if the given parameter is injected, will directly set the first available {@link ValueProvider}
	 */
	private String setParameterInjected(ConnectionParameterModel parameter, boolean injected) {
		if (!injected) {
			parameter.setInjectorName(null);
		} else if (!data.getValueProviders().isEmpty()) {
			String name = data.getValueProviders().get(DEFAULT_VALUE_PROVIDER_INDEX).getName();
			parameter.setInjectorName(name);
			return name;
		}
		return null;
	}

	/**
	 * Filter the list of parameters and return only that that do contain the searchvalue.
	 */
	private List<ConnectionParameterModel> getFilteredParameters() {
		List<ConnectionParameterModel> list = new ArrayList<>();
		for (ConnectionParameterModel p : data.getParameters()) {
			String paramName = ConnectionI18N.getParameterName(p.getType(), p.getGroupName(), p.getName(), p.getName());
			if (StringUtils.containsIgnoreCase(paramName, searchValue)) {
				list.add(p);
			}
		}
		return list;
	}
}

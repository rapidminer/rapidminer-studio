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
package com.rapidminer.studio.io.gui.internal.steps.configuration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.example.Attributes;
import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.DropDownPopupButton;
import com.rapidminer.gui.tools.components.DropDownPopupButton.DropDownPopupButtonBuilder;
import com.rapidminer.studio.io.gui.internal.DataImportWizardUtils;
import com.rapidminer.studio.io.gui.internal.DataWizardEventType;
import com.rapidminer.tools.I18N;


/**
 * A component that is used by the {@link ConfigureDataStep} table to display the column name,
 * column type and column role. Furthermore the user can change the column settings via a dropdown
 * menu.
 *
 * @author Nils Woehler, Marcel Michel
 * @since 7.0.0
 *
 */
final class ConfigureDataTableHeader extends JPanel implements TableCellRenderer {

	private static final long serialVersionUID = 1L;

	private static final int DEFAULT_FONT_SIZE = 13;
	private static final String POPUP_SHOWN_COLOR = "#eb7a03";

	private static final Color COLOR_COLUMN_DISABLED = new Color(154, 154, 154);

	private static final String BASE_I18N_KEY = "io.dataimport.step.data_column_configuration";
	private static final String ERROR_DUPLICATE_ROLE_NAME = I18N.getGUILabel(BASE_I18N_KEY + ".duplicate_role_name");
	private static final String ERROR_DUPLICATE_COLUMN_NAME = I18N.getGUILabel(BASE_I18N_KEY + ".duplicate_column_name");
	private static final String ERROR_EMPTY_COLUMN_NAME = I18N.getGUILabel(BASE_I18N_KEY + ".empty_column_name");

	private static final String GUI_ACTION_PREFIX = "gui.action.";
	private static final String CHANGE_TYPE_LABEL = I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".type.label");
	private static final String CHANGE_TYPE_TIP = I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".type.tip");

	private final Action changeRoleAction = new ResourceAction(BASE_I18N_KEY + ".change_role") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			ColumnMetaData columnMetaData = metaData.getColumnMetaData(columnIndex);
			String columnName = columnMetaData.getName();
			String type = DataImportWizardUtils.getNameForColumnType(columnMetaData.getType());
			final String currentRoleName = columnMetaData.getRole();

			List<String> roleList = new ArrayList<>();
			if (currentRoleName != null) {
				roleList.add(currentRoleName);
			}
			for (String attribute : new String[] { Attributes.LABEL_NAME, Attributes.ID_NAME, Attributes.WEIGHT_NAME }) {
				if (attribute.equals(currentRoleName)) {
					continue;
				}
				roleList.add(attribute);
			}

			String newRoleName = SwingTools.showInputDialog(ApplicationFrame.getApplicationFrame(),
					BASE_I18N_KEY + ".change_role", true, roleList, currentRoleName,
					input -> {
						if (input == null) {
							return null;
						}
						String valueString = input.trim();
						if (!valueString.equals(currentRoleName) && validator.isRoleUsed(valueString)) {
							return ERROR_DUPLICATE_ROLE_NAME;
						}
						return null;
					});
			if (newRoleName == null) {
				// user cancelled dialog
				return;
			}
			newRoleName = newRoleName.trim();
			if (newRoleName.isEmpty()) {
				newRoleName = null;
			}
			if (Objects.equals(newRoleName, currentRoleName)) {
				// user has not changed the role
				return;
			}
			columnMetaData.setRole(newRoleName);
			updateMetadataUI(columnName, type, newRoleName);
		}
	};

	private final Action renameAction = new ResourceAction(BASE_I18N_KEY + ".rename") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			ColumnMetaData columnMetaData = metaData.getColumnMetaData(columnIndex);
			final String currentColumnName = columnMetaData.getName();
			String type = DataImportWizardUtils.getNameForColumnType(columnMetaData.getType());
			String roleName = columnMetaData.getRole();

			String newColumnName = SwingTools.showInputDialog(ApplicationFrame.getApplicationFrame(),
					BASE_I18N_KEY + ".rename", currentColumnName, inputString -> {
						if (inputString == null || inputString.trim().isEmpty()) {
							return ERROR_EMPTY_COLUMN_NAME;
						}
						if (!inputString.trim().equals(currentColumnName) && validator.isNameUsed(inputString.trim())) {
							return ERROR_DUPLICATE_COLUMN_NAME;
						}
						return null;
					});
			if (newColumnName == null || newColumnName.trim().equals(currentColumnName)) {
				// user cancelled dialog or did not change the name
				return;
			}
			newColumnName = newColumnName.trim();

			columnMetaData.setName(newColumnName);
			updateMetadataUI(newColumnName, type, roleName);
		}
	};

	private final Action disableEnableAction = new ResourceAction(BASE_I18N_KEY + ".disable") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			boolean wasRemoved = metaData.getColumnMetaData(columnIndex).isRemoved();
			metaData.getColumnMetaData(columnIndex).setRemoved(!wasRemoved);
			Color foreGroundColor = wasRemoved ? Color.BLACK : COLOR_COLUMN_DISABLED;
			nameLabel.setForeground(foreGroundColor);
			typeLabel.setForeground(foreGroundColor);
			roleLabel.setForeground(foreGroundColor);
			validator.validate(columnIndex);
			updateDisableEnableAction();
			ConfigureDataTableHeader.this.table.revalidate();
			ConfigureDataTableHeader.this.table.repaint();
		}
	};

	private final JTable table;
	private final DropDownPopupButton configureColumnButton;

	private final int columnIndex;
	private final DataSetMetaData metaData;
	private final ConfigureDataValidator validator;

	private final JLabel nameLabel;
	private final JLabel roleLabel;
	private final JLabel typeLabel;
	private final ConfigureDataView configureDataView;
	private final JMenu typeMenu;

	public ConfigureDataTableHeader(final JTable table, final int columnIndex, final DataSetMetaData metaData,
			final ConfigureDataValidator validator, final ConfigureDataView configureDataView) {
		this.table = table;
		this.columnIndex = columnIndex;
		this.validator = validator;
		this.metaData = metaData;
		this.configureDataView = configureDataView;
		validator.addObserver((observable, arg) -> {
			if (arg != null && arg.contains(columnIndex)) {
				adjustErrorColors();
			}
		}, false);
		setLayout(new GridBagLayout());
		setBackground(Colors.TABLE_HEADER_BACKGROUND_GRADIENT_START);
		setBorder(BorderFactory.createLineBorder(Colors.TABLE_HEADER_BORDER));
		setMinimumSize(new Dimension(120, 50));

		String columnName = metaData.getColumnMetaData(columnIndex).getName();
		String type = DataImportWizardUtils.getNameForColumnType(metaData.getColumnMetaData(columnIndex).getType());
		String roleName = metaData.getColumnMetaData(columnIndex).getRole();

		setToolTipText(createTooltip(columnName, type, roleName));

		GridBagConstraints gbc = new GridBagConstraints();

		// add name label
		nameLabel = new JLabel(columnName);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, DEFAULT_FONT_SIZE));

		gbc.gridwidth = GridBagConstraints.RELATIVE;
		gbc.insets = new Insets(0, 5, 0, 0);
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		add(nameLabel, gbc);

		// add drop down button
		updateDisableEnableAction();

		typeMenu = new JMenu(CHANGE_TYPE_LABEL);
		updateTypeMenu(DataImportWizardUtils.getNameForColumnType(metaData.getColumnMetaData(columnIndex).getType()));
		typeMenu.setToolTipText(CHANGE_TYPE_TIP);

		configureColumnButton = new DropDownPopupButtonBuilder()
				.with(new ResourceActionAdapter(true, BASE_I18N_KEY + ".header_action"))
				.add(typeMenu).add(changeRoleAction).add(renameAction).add(disableEnableAction).build();
		configureColumnButton.setIcon(null);
		configureColumnButton.setBorder(null);
		configureColumnButton.setOpaque(false);
		configureColumnButton.setContentAreaFilled(false);
		configureColumnButton.setBorderPainted(false);
		configureColumnButton.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				configureColumnButton.setColor(POPUP_SHOWN_COLOR);
				table.getTableHeader().repaint();
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				configureColumnButton.setColor(null);
				table.getTableHeader().repaint();
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {}
		});

		configureColumnButton.setArrowSize(DEFAULT_FONT_SIZE);
		configureColumnButton.setTextSize(DEFAULT_FONT_SIZE);
		configureColumnButton.setText(Ionicon.GEAR_B.getHtml());

		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;

		add(configureColumnButton, gbc);

		// add type label
		typeLabel = new JLabel(type);
		typeLabel.setFont(typeLabel.getFont().deriveFont(Font.ITALIC));

		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 5, 0, 0);

		add(typeLabel, gbc);

		// add role label
		roleLabel = new JLabel(roleName != null ? roleName : " ");
		roleLabel.setFont(roleLabel.getFont().deriveFont(Font.ITALIC));

		add(roleLabel, gbc);

		adjustErrorColors();

		setupMouseListener();
	}
	/**
	 * (Re-)creates the type menu with the selected type
	 * @param selected the selected column Type
	 */
	private void updateTypeMenu(String selected) {
		typeMenu.removeAll();
		ButtonGroup typeGroup = new ButtonGroup();
		for (ColumnType columnType : ColumnType.values()) {
			String columnTypeName = DataImportWizardUtils.getNameForColumnType(columnType);
			JCheckBoxMenuItem checkboxItem = new JCheckBoxMenuItem(columnTypeName);
			if (columnTypeName.equals(selected)) {
				checkboxItem.setSelected(true);
			}
			checkboxItem.addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					changeType(columnType);
				}
			});
			typeGroup.add(checkboxItem);
			typeMenu.add(checkboxItem);
		}
	}

	/**
	 * Adds to the table header a {@link MouseListener} which manages the
	 * {@link #configureColumnButton} action.
	 */
	private void setupMouseListener() {
		table.getTableHeader().addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JTableHeader header = ConfigureDataTableHeader.this.table.getTableHeader();

				// this call is very expensive for many columns, because the
				// default model iterates over every column and computes the corresponding width
				int currentIndex = header.getColumnModel().getColumnIndexAtX(e.getPoint().x);

				if (currentIndex != columnIndex || currentIndex == -1) {
					return;
				}

				Rectangle headerRec = header.getHeaderRect(currentIndex);
				setBounds(headerRec);
				header.add(ConfigureDataTableHeader.this);
				validate();

				Rectangle buttonRec = configureColumnButton.getBounds(null);
				buttonRec.x += headerRec.x;
				buttonRec.y += headerRec.y;

				Rectangle nameRec = nameLabel.getBounds(null);
				nameRec.x += headerRec.x;
				nameRec.y += headerRec.y;

				if (buttonRec.contains(e.getPoint())) {
					configureColumnButton.doClick();
				} else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() >= 2 && nameRec.contains(e.getPoint())) {
					renameAction.actionPerformed(null);
				}

				header.remove(ConfigureDataTableHeader.this);
				header.repaint();
			}
		});
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
			int col) {
		return this;
	}

	/**
	 * Color the name or role red if it is a duplicate.
	 */
	private void adjustErrorColors() {
		if (metaData.getColumnMetaData(columnIndex).isRemoved()) {
			nameLabel.setForeground(COLOR_COLUMN_DISABLED);
			roleLabel.setForeground(COLOR_COLUMN_DISABLED);
			typeLabel.setForeground(COLOR_COLUMN_DISABLED);
		} else {
			Color nameColor = validator.isDuplicateNameColumn(columnIndex) ? Color.RED : Color.BLACK;
			nameLabel.setForeground(nameColor);
			Color roleColor = validator.isDuplicateRoleColumn(columnIndex) ? Color.RED : Color.BLACK;
			roleLabel.setForeground(roleColor);
		}
	}

	/**
	 * Updates the {@link #disableEnableAction} in regard to the {@link #metaData}.
	 */
	private void updateDisableEnableAction() {
		if (metaData.getColumnMetaData(columnIndex).isRemoved()) {
			disableEnableAction.putValue(Action.NAME,
					I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".enable.label"));
			disableEnableAction.putValue(Action.SHORT_DESCRIPTION,
					I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".enable.tip"));

		} else {
			disableEnableAction.putValue(Action.NAME,
					I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".disable.label"));
			disableEnableAction.putValue(Action.SHORT_DESCRIPTION,
					I18N.getGUIMessage(GUI_ACTION_PREFIX + BASE_I18N_KEY + ".disable.tip"));
		}
	}

	/**
	 * Creates the tooltip text for the table header.
	 *
	 * @param columnName
	 *            the human readable name of the column
	 * @param type
	 *            the human readable attribute type
	 * @param roleName
	 *            the human readable role name
	 * @return the created tooltip text
	 */
	private static String createTooltip(String columnName, String type, String roleName) {
		// build header tooltip
		StringBuilder tipBuilder = new StringBuilder();
		tipBuilder.append("<html><table><tbody><tr><td><strong>");
		tipBuilder.append(I18N.getGUILabel(BASE_I18N_KEY + ".tooltip_name"));
		tipBuilder.append("</strong><td><td>");
		tipBuilder.append(columnName);
		tipBuilder.append("<td></tr><tr><td><strong>");
		tipBuilder.append(I18N.getGUILabel(BASE_I18N_KEY + ".tooltip_type"));
		tipBuilder.append("</strong><td><td>");
		tipBuilder.append(type);
		tipBuilder.append("<td></tr>");
		if (roleName != null) {
			tipBuilder.append("<tr><td><strong>");
			tipBuilder.append(I18N.getGUILabel(BASE_I18N_KEY + ".tooltip_role"));
			tipBuilder.append("</strong><td><td>");
			tipBuilder.append(roleName);
			tipBuilder.append("<td></tr>");
		}
		tipBuilder.append("</tbody></table></html>");
		return tipBuilder.toString();
	}

	/**
	 * Updates the column type to the newType. Rereads the column and updates the error table.
	 *
	 * @param newType
	 *            the new column type
	 */
	private void changeType(final ColumnType newType) {
		DataImportWizardUtils.logStats(DataWizardEventType.COLUMN_TYPE_CHANGED,
				metaData.getColumnMetaData(columnIndex).getType() + "->" + newType);
		metaData.getColumnMetaData(columnIndex).setType(newType);
		final ConfigureDataTableModel tableModel = (ConfigureDataTableModel) ConfigureDataTableHeader.this.table.getModel();
		ProgressThread columnThread = new ProgressThread(BASE_I18N_KEY + ".update_column") {

			@Override
			public void run() {
				try {
					tableModel.rereadColumn(columnIndex, getProgressListener());
				} catch (final DataSetException e) {
					SwingTools.invokeLater(() ->
							configureDataView.showErrorNotification(
									BASE_I18N_KEY + ".error_loading_data", e.getMessage()));
					return;
				}
				SwingTools.invokeLater(() -> {
					validator.setParsingErrors(tableModel.getParsingErrors());
					ColumnMetaData columnMetaData = metaData.getColumnMetaData(columnIndex);
					updateMetadataUI(columnMetaData.getName(), DataImportWizardUtils.getNameForColumnType(newType), columnMetaData.getRole());
				});

			}
		};
		columnThread.start();
	}


	/**
	 * Updates this header after the underlying {@link ColumnMetaData} changed.
	 *
	 * @since 9.1
	 */
	void updateMetadataUI() {
		ColumnMetaData columnMetaData = metaData.getColumnMetaData(columnIndex);
		updateMetadataUI(columnMetaData.getName(),
				DataImportWizardUtils.getNameForColumnType(columnMetaData.getType()),
				columnMetaData.getRole());
	}

	/**
	 * Updates this header with the given column name, type name and role name.
	 *
	 * @since 9.1
	 */
	private void updateMetadataUI(String columnName, String type, String roleName) {
		validator.validate(columnIndex);
		SwingTools.invokeAndWait(() -> {
			setToolTipText(createTooltip(columnName, type, roleName));
			nameLabel.setText(columnName);
			roleLabel.setText(roleName != null ? roleName : " ");
			if (!Objects.equals(typeLabel.getText(), type)) {
				updateTypeMenu(type);
			}
			typeLabel.setText(type);
			table.getTableHeader().revalidate();
			table.getTableHeader().repaint();
			table.revalidate();
			table.repaint();
		});
	}

}

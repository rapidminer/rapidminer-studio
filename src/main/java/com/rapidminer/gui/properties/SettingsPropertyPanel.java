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
package com.rapidminer.gui.properties;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.rapidminer.gui.look.icons.IconFactory;
import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.gui.properties.celleditors.value.PropertyValueCellEditor;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.I18N;


/**
 * The SettingsPropertyPanel is used to display settings of the same group. The used settings items
 * are read from the {@link com.rapidminer.tools.ParameterService ParameterService}. This class also holds a method for applying changes
 * in the value back to the {@link com.rapidminer.tools.ParameterService ParameterService}.
 *
 * @author Sebastian Land, Simon Fischer, Adrian Wilke, Jan Czogalla
 */
public class SettingsPropertyPanel extends PropertyPanel {

	private static final long serialVersionUID = 313811558626370370L;
	private static final int FIRST_SUBGROUP_TOP_MARGIN = 6;
	private static final int SUBGROUP_TOP_MARGIN = 20;

	final transient Collection<ParameterType> shownParameterTypes;

	private final transient ParameterHandler parameterHandler;
	private final transient SettingsItemProvider settingsItemProvider;

	private final String groupTitle;

	/** Only draw sub-groups, if information about them exist */
	private boolean useSubGroups = false;

	/** The first sub-group has special margins */
	private boolean isFirstSubGroup = true;

	/** Sub-group 'Miscellaneous' should one be used one time */
	private boolean subGroupMiscUsed = false;

	/** Sub-group with the title of the tab should one be used one time */
	private boolean subGroupTabNameUsed = false;

	public SettingsPropertyPanel(SettingsDialog settingsDialog, String groupTitle, List<SettingsItem> itemSubGroups, List<SettingsItem> itemParameters, String filter) {
		parameterHandler = settingsDialog.getParameterHandler();
		settingsItemProvider = settingsDialog.getSettingsItemProvider();

		this.groupTitle = groupTitle;

		// These data structures are used to provide overwritten PropertyPanel methods
		shownParameterTypes = new LinkedList<>();


		Set<String> definedKeys = parameterHandler.getParameters().getDefinedKeys();
		// Add parameters of sub-groups first
		for (SettingsItem subGroup : itemSubGroups) {
			for (SettingsItem item : subGroup.getChildren(Type.PARAMETER)) {
				if (subGroup.isInFilter(item, filter)) {
					ParameterType parameterType = getParameterType(item);
					if (parameterType != null) {
						shownParameterTypes.add(parameterType);
					}
				}
				if (!definedKeys.contains(item.getKey())) {
					parameterHandler.setParameter(item.getKey(), getParameterValue(item));
				}
			}
		}

		// Add parameters without sub-group
		for (SettingsItem item : itemParameters) {
			ParameterType parameterType = getParameterType(item);
			if (parameterType != null) {
				shownParameterTypes.add(parameterType);
			}
			if (!definedKeys.contains(item.getKey())) {
				parameterHandler.setParameter(item.getKey(), getParameterValue(item));
			}
		}

		if (!itemSubGroups.isEmpty()) {
			useSubGroups = true;
		}

		setupComponents();
	}

	/** @since 9.1 */
	private ParameterType getParameterType(SettingsItem item) {
		return parameterHandler.getParameters().getParameterType(item.getKey());
	}

	/** @since 9.1 */
	private String getParameterValue(SettingsItem item) {
		return settingsItemProvider.getValue(item.getKey());
	}

	@Override
	protected Collection<ParameterType> getProperties() {
		return shownParameterTypes.stream().filter(pt -> !pt.isHidden()).collect(Collectors.toList());
	}

	@Override
	protected String getValue(ParameterType type) {
		String value = parameterHandler.getParameters().getParameterOrNull(type.getKey());
		if (value == null) {
			return null;
		} else {
			return type.transformNewValue(value);
		}
	}

	@Override
	protected void setValue(Operator operator, ParameterType type, String value) {
		if (!settingsItemProvider.containsKey(type.getKey())) {
			// Object is automatically added to an internal list
			settingsItemProvider.createAndAddItem(type.getKey(), Type.PARAMETER);
		}
		parameterHandler.setParameter(type.getKey(), value);
	}

	/**
	 * Applies the properties without saving them.
	 * @deprecated since 9.1; this does nothing anymore
	 * @see SettingsTabs#applyProperties()
	 */
	@Deprecated
	public void applyProperties() {
		// noop; handled in SettingsTabs now
	}

	@Override
	protected Operator getOperator() {
		return null;
	}

	/**
	 * Creates a panel with a heading.
	 *
	 * @param labelText
	 *            The text for the heading
	 *
	 * @return The panel
	 */
	private JPanel createSubGroupPanel(String labelText) {
		JLabel subGroupLabel = new JLabel();
		Font font = subGroupLabel.getFont();
		font = font.deriveFont(Font.BOLD, 14f);
		subGroupLabel.setFont(font);
		subGroupLabel.setForeground(SettingsTabs.COLOR_SUBGROUP);
		subGroupLabel.setText(labelText);
		int top = SUBGROUP_TOP_MARGIN;
		if (isFirstSubGroup) {
			isFirstSubGroup = false;
			top = FIRST_SUBGROUP_TOP_MARGIN;
		}
		subGroupLabel.setBorder(BorderFactory.createEmptyBorder(top, 0, 7, 0));

		JPanel subGroupPanel = new JPanel();
		subGroupPanel.setLayout(new BoxLayout(subGroupPanel, BoxLayout.LINE_AXIS));
		subGroupPanel.add(subGroupLabel);
		subGroupPanel.add(Box.createHorizontalGlue());
		return subGroupPanel;
	}

	/**
	 * Checks, if parent item should be added as heading.
	 *
	 * @param parent
	 *            The parent settings item
	 * @return if the parent item should be added
	 */
	private boolean useParentAsHeading(SettingsItem parent) {
		// Only sub-groups are added as headings
		// No sub-group should be added twice
		return parent.getType() == Type.SUB_GROUP && !parent.isUsedInDialog();
	}

	/**
	 * Checks, if a heading 'miscellaneous' should be added.
	 *
	 * @param parent
	 *            The parent settings item
	 * @return if the heading 'misc' should be added
	 */
	private boolean useMiscHeading(SettingsItem parent) {
		// Do not add Miscellaneous, if there is a sub-group with a better title
		// The sub group 'Misc' should not be added twice
		// Only use 'Misc', if there are other known sub-groups
		return parent.getType() != Type.SUB_GROUP && !subGroupMiscUsed && useSubGroups;
	}

	/**
	 * Checks, if a heading with the tab name should be added.
	 *
	 * @param parent
	 *            The parent settings item
	 * @return if the tab name should be added as heading
	 */
	private boolean useTabHeading(SettingsItem parent) {
		// Do not add tab name, if the is a sub-group with a better title
		// The heading should only be added one time
		// Only use tab name, if no other sub-groups are used
		return parent.getType() != Type.SUB_GROUP && !subGroupTabNameUsed && !useSubGroups;
	}

	@Override
	protected JPanel createParameterPanel(ParameterType type, PropertyValueCellEditor editor,
			final Component editorComponent) {

		// Remove default tool tip, which is set in superclass
		if (editorComponent instanceof JComponent) {
			((JComponent) editorComponent).setToolTipText(null);
		}

		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.PAGE_AXIS));

		SettingsItem settingsItem = settingsItemProvider.get(type.getKey());

		// Add sub-group title before adding properties

		SettingsItem parent = settingsItem.getParent();
		if (parent != null) {
			if (useParentAsHeading(parent)) {
				parent.setUsedInDialog(true);
				containerPanel.add(createSubGroupPanel(parent.getTitle()));

			} else if (useMiscHeading(parent)) {
				subGroupMiscUsed = true;
				containerPanel.add(createSubGroupPanel(I18N.getGUIMessage("gui.dialog.settings.misc")));

			} else if (useTabHeading(parent)) {
				subGroupTabNameUsed = true;
				containerPanel.add(createSubGroupPanel(groupTitle));
			}
		}

		// Add parameter

		JPanel contentsPanel = new JPanel();
		contentsPanel.setOpaque(isOpaque());
		contentsPanel.setPreferredSize(
				new Dimension((int) contentsPanel.getPreferredSize().getWidth(), VALUE_CELL_EDITOR_HEIGHT));
		JPanel parameterPanel = null;
		if (editor.rendersLabel()) {
			// Editor renders label for: Checkboxes

			parameterPanel = new JPanel(new BorderLayout());

			// Contents panel contains component

			contentsPanel.setLayout(new BorderLayout());
			contentsPanel.add(editorComponent,
					editorComponent instanceof JCheckBox ? BorderLayout.WEST : BorderLayout.CENTER);
			parameterPanel.add(contentsPanel);

			// Replace checkbox text
			// (For default implementation see DefaultCellEditor.java)
			Component[] components = contentsPanel.getComponents();
			if (components.length != 0) {
				Component component = components[0];
				if (component instanceof JCheckBox) {
					JCheckBox jcheckbox = (JCheckBox) component;
					jcheckbox.setText(settingsItem.getTitle());
					jcheckbox.setFont(jcheckbox.getFont().deriveFont(~Font.ITALIC));
				}
			}
		} else {
			// Editor does not render label for: Input and selection boxes

			parameterPanel = new JPanel();
			parameterPanel.setLayout(new BoxLayout(parameterPanel, BoxLayout.LINE_AXIS));

			// Contents panel contains label and component

			contentsPanel.setLayout(new GridLayout(1, 2));
			JLabel label = new JLabel(settingsItem.getTitle());
			label.setOpaque(isOpaque());
			label.setFont(getFont());
			label.setBackground(getBackground());
			int style = Font.PLAIN;
			if (!type.isOptional()) {
				style |= Font.BOLD;
			}
			label.setFont(label.getFont().deriveFont(style));
			label.setLabelFor(editorComponent);
			if (!isEnabled()) {
				SwingTools.setEnabledRecursive(label, false);
			}
			contentsPanel.add(label);
			contentsPanel.add(editorComponent);
			parameterPanel.add(contentsPanel);
		}

		JPanel surroundingPanel = new JPanel(new BorderLayout());
		surroundingPanel.add(parameterPanel, BorderLayout.CENTER);

		JPanel helpWrapperPanel = surroundingPanel;
		if (settingsItemProvider.hasEnforcedSettings()) {
			JPanel infoPanel = new JPanel(new BorderLayout());
			helpWrapperPanel = new JPanel(new BorderLayout());
			infoPanel.add(helpWrapperPanel, BorderLayout.CENTER);
			surroundingPanel.add(infoPanel, BorderLayout.EAST);
			final JLabel lockedByAdminLabel;
			if (settingsItemProvider.isSettingEnforced(type.getKey())) {
				SwingTools.setEnabledRecursive(contentsPanel, false);
				lockedByAdminLabel = new ResourceLabel("preferences.setting_enforced");
			} else {
				lockedByAdminLabel = new JLabel(IconFactory.getEmptyIcon16x16());
			}
			infoPanel.add(lockedByAdminLabel, BorderLayout.EAST);
		}

		addHelpLabel(type.getKey(), settingsItem.getTitle(), settingsItem.getDescription(), type.getRange(),
				type.isOptional(), helpWrapperPanel);

		containerPanel.add(surroundingPanel);
		return containerPanel;
	}

	@Override
	public void setupComponents() {
		// reset items used state
		shownParameterTypes.stream().flatMap(pt -> {
			SettingsItem item = settingsItemProvider.get(pt.getKey());
			return Stream.of(item, item.getParent());
		}).filter(Objects::nonNull)//.filter(item -> item.getType() != Type.GROUP)
				.forEach(si -> si.setUsedInDialog(false));
		// reset other states
		useSubGroups = subGroupMiscUsed = subGroupTabNameUsed = false;
		isFirstSubGroup = true;
		super.setupComponents();
	}
}

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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

import com.rapidminer.RapidMiner;
import com.rapidminer.external.alphanum.AlphanumComparator;
import com.rapidminer.gui.properties.SettingsItem.Type;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTabbedPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tools.components.ToolTipWindow.TooltipLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.ParameterService;


/**
 * The tabs for the different groups of RapidMiner settings. Each tab contains a
 * {@link SettingsPropertyPanel} for the settings in this group.
 *
 * @author Sebastian Land, Ingo Mierswa, Adrian Wilke
 */
public class SettingsTabs extends ExtendedJTabbedPane implements Observer<String> {

	private static final long serialVersionUID = -229446448782516589L;

	private final List<SettingsPropertyPanel> parameterPanels = new LinkedList<>();

	private final Map<String, Integer> groupKeysToTabIndexMap = new HashMap<>();

	private static Map<JComponent, String> tooltipDescriptions = new HashMap<>();

	/** The containing dialog */
	private final SettingsDialog settingsDialog;

	/** Color matching the icons */
	public static final Color COLOR_SUBGROUP = new Color(32, 100, 148);

	/** Color matching the tabs border */
	public static final Color COLOR_GROUP_DESCRIPTION_BORDER = new Color(220, 220, 224);

	/** Color for the backgournd of the tab description */
	public static final Color COLOR_GROUP_DESCRIPTION_BACKGROUND = new Color(230, 230, 234);

	/** Color of the group/tab descriptions */
	public static final Color COLOR_GROUP_DESCRIPTION = SwingTools.RAPIDMINER_GRAY;

	/** Compares titles of SettingItem objects */
	private static final Comparator<String> SETTINGS_TITLE_COMPARATOR = new AlphanumComparator();
	private static final Comparator<SettingsItem> SETTINGS_ITEM_COMPARATOR = (itemA, itemB) -> {
		if (itemA == null) {
			return itemB == null ? 0 : 1;
		}
		return itemB == null ? -1 : SETTINGS_TITLE_COMPARATOR.compare(itemA.getTitle(), itemB.getTitle());
	};

	/**
	 * Creates necessary {@link SettingsItem}s and related tabs for the settings.
	 *
	 * @param settingsDialog
	 *            The containing dialog. Is used to create {@link ToolTipWindow}s for tabs.
	 */
	public SettingsTabs(SettingsDialog settingsDialog) {
		this(settingsDialog, null);
	}

	/**
	 * Creates necessary {@link SettingsItem}s and related tabs for the settings.
	 *
	 * @param settingsDialog
	 *            The containing dialog. Is used to create {@link ToolTipWindow}s for tabs.
	 * @param filter
	 *            Used to filter the setting parameters
	 */
	public SettingsTabs(SettingsDialog settingsDialog, String filter) {
		this.settingsDialog = settingsDialog;

		setTabPlacement(JTabbedPane.LEFT);
		// Get defined-parameters
		// These are all parameters, which will appear in dialog
		Collection<String> definedParameterKeys = new HashSet<>(settingsDialog.getParameterHandler().getParameters().getKeys());

		// Get parsed settings items
		SettingsItemProvider items = settingsDialog.getSettingsItemProvider();

		// Remove structured settings items, which are not in defined-parameter
		// (The structure is known, but they would not be used)
		items.removeParameterInverse(definedParameterKeys);

		// Get defined-parameters, which are not known in XML structure
		// (They will be attached, even if the structure is not known)
		Collection<String> structuredKeys = items.getKeys();
		Iterator<String> iterator = definedParameterKeys.iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			if (structuredKeys.contains(key)) {
				iterator.remove();
			}
		}

		// Generate structure for remaining defined-parameters
		boolean isDebugMode = Boolean
				.parseBoolean(ParameterService.getParameterValue(RapidMiner.PROPERTY_RAPIDMINER_GENERAL_DEBUGMODE));
		iterator = definedParameterKeys.iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();

			items.createAndAddItem(key, Type.PARAMETER);

			if (isDebugMode) {
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.gui.properties.SettingsTabs.no_parameter_in_xml",
						key);
			}
		}

		// Remove empty subgroups and groups
		items.clean();

		// Create tabs
		List<SettingsItem> groups = items.getItems(Type.GROUP);
		if (!items.isGroupingLoaded()) {
			// Sort groups lexicographically, if XML could not be parsed
			groups.sort(SETTINGS_ITEM_COMPARATOR);
		}
		for (SettingsItem group : groups) {
			List<SettingsItem> subGroups = group.getChildren(Type.SUB_GROUP, filter);
			List<SettingsItem> parameters = group.getChildren(Type.PARAMETER, filter);
			boolean isSubGroupsEmpty = subGroups.stream().allMatch(subGroup -> subGroup.getChildren(Type.PARAMETER, filter).isEmpty());
			if (!parameters.isEmpty() || !isSubGroupsEmpty) {
				createTab(group.getKey(), group.getTitle(), group.getDescription(), subGroups, parameters, filter);
			}
		}

		// Remove the used flag to prevent broken settings dialog after second opening.
		for (String key : items.getKeys()) {
			items.get(key).setUsedInDialog(false);
		}
	}

	/**
	 * Selects the tab, which is related to the specified groupKey.
	 *
	 * @param groupKey
	 *            A key of a preferences group.
	 */
	public void selectTab(String groupKey) {
		if (groupKeysToTabIndexMap.containsKey(groupKey)) {
			setSelectedIndex(groupKeysToTabIndexMap.get(groupKey));
		}
	}

	/**
	 * Creates a tab
	 */
	private void createTab(String groupKey, String groupTitle, String groupDescription, List<SettingsItem> subGroups,
			List<SettingsItem> parameters, String filter) {

		JPanel containerPanel = new JPanel();
		containerPanel.setLayout(new BoxLayout(containerPanel, BoxLayout.PAGE_AXIS));
		containerPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

		// Add group description
		JPanel descriptionPanel = new JPanel();
		descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.LINE_AXIS));
		descriptionPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_GROUP_DESCRIPTION_BORDER));
		if (groupDescription != null && !groupDescription.isEmpty()) {
			JLabel descriptionLabel = new JLabel();
			descriptionLabel.setForeground(COLOR_GROUP_DESCRIPTION);
			descriptionLabel.setText(groupDescription);
			descriptionLabel.setBorder(BorderFactory.createEmptyBorder(6, 3, 6, 0));
			descriptionPanel.add(descriptionLabel);
			descriptionPanel.add(Box.createHorizontalGlue());
			descriptionPanel.setBackground(COLOR_GROUP_DESCRIPTION_BACKGROUND);
		} else {
			Dimension dim = new Dimension(0, 1);
			descriptionPanel.add(new Box.Filler(dim, dim, dim));
		}
		containerPanel.add(descriptionPanel);

		final SettingsPropertyPanel table = new SettingsPropertyPanel(settingsDialog, groupTitle, subGroups, parameters, filter);

		new ToolTipWindow(settingsDialog, new TipProvider() {

			@Override
			public String getTip(Object id) {
				return id == null ? null : SettingsTabs.tooltipDescriptions.get(id);
			}

			@Override
			public Object getIdUnder(Point point) {
				Point tableScreenLocation = table.getLocationOnScreen();
				int mouseX = point.x + tableScreenLocation.x;
				int mouseY = point.y + tableScreenLocation.y;
				return SettingsTabs.tooltipDescriptions.keySet().stream().filter(JComponent::isShowing)
						.filter(component -> {
							int compX = component.getLocationOnScreen().x;
							int compY = component.getLocationOnScreen().y;
							return  (mouseX > compX && mouseY > compY && mouseX < compX + component.getWidth()
									&& mouseY < compY + component.getHeight());
						}).findFirst().orElse(null);
			}

			@Override
			public Component getCustomComponent(Object id) {
				return null;
			}
		}, table, TooltipLocation.RIGHT).setOnlyWhenFocussed(false).setReactOnMousePressed(true);

		parameterPanels.add(table);
		containerPanel.add(table);

		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(containerPanel);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setPreferredSize(new Dimension(600, 300));
		scrollPane.setBorder(null);

		addTab(groupTitle, scrollPane);
		groupKeysToTabIndexMap.put(groupKey, getTabCount() - 1);
	}

	@Override
	public void update(Observable<String> observable, String key) {
		// find affected property panels in regards to conditions, then update their components
		parameterPanels.stream().filter(spp -> spp.shownParameterTypes.stream()
				.flatMap(pt -> pt.getConditions().stream()).anyMatch(pc -> key.equals(pc.getConditionParameter())))
				.forEach(SettingsPropertyPanel::setupComponents);
	}

	public void applyProperties() {
		settingsDialog.getSettingsItemProvider().applyValues(settingsDialog.getParameterHandler());
	}

	/**
	 * This method will save the parameters defined in this tab
	 */
	public void save() throws IOException {
		applyProperties();
		settingsDialog.getSettingsItemProvider().saveSettings();
	}

	/**
	 * Adds a tool tip description for a component
	 */
	public static void addToolTipDescription(JComponent component, String description) {
		SettingsTabs.tooltipDescriptions.put(component, description);
	}
}

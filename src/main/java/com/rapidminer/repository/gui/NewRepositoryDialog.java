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
package com.rapidminer.repository.gui;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.MultiPageDialog;
import com.rapidminer.repository.CustomRepositoryFactory;
import com.rapidminer.repository.CustomRepositoryRegistry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.container.Pair;


/**
 * A dialog to create new remote or local repositories.
 *
 * @author Simon Fischer, Nils Woehler
 *
 */
public class NewRepositoryDialog extends MultiPageDialog {

	private static final long serialVersionUID = 1L;

	private final JRadioButton localButton;
	private final LocalRepositoryPanel localRepositoryPanel = new LocalRepositoryPanel(getFinishButton(), true);

	private final Map<String, Pair<RepositoryConfigurationPanel, JRadioButton>> repoConfigPanels = new HashMap<>();

	private NewRepositoryDialog() {
		super(RapidMinerGUI.getMainFrame(), "repositorydialog", true, new Object[] {});

		Box firstPage = new Box(BoxLayout.Y_AXIS);
		ButtonGroup checkBoxGroup = new ButtonGroup();
		localButton = new JRadioButton(new ResourceActionAdapter("new_local_repositiory"));
		localButton.setSelected(true);

		checkBoxGroup.add(localButton);
		firstPage.add(localButton);

		Map<String, Component> cards = new HashMap<String, Component>();
		cards.put("first", firstPage);
		cards.put("local", localRepositoryPanel);

		// register a radio button for each custom repository type
		for (CustomRepositoryFactory factory : CustomRepositoryRegistry.INSTANCE.getFactories()) {
			String key = factory.getI18NKey();
			RepositoryConfigurationPanel repositoryConfigurationPanel = factory.getRepositoryConfigurationPanel();
			JRadioButton radioButton = new JRadioButton(new ResourceActionAdapter(key));
			radioButton.setEnabled(factory.enableRepositoryConfiguration());
			repoConfigPanels.put(key, new Pair<>(repositoryConfigurationPanel, radioButton));

			checkBoxGroup.add(radioButton);
			firstPage.add(radioButton);

			cards.put(factory.getI18NKey(), repositoryConfigurationPanel.getComponent());
		}

		firstPage.add(Box.createVerticalGlue());
		layoutDefault(cards);
	}

	public static void createNew() {
		NewRepositoryDialog d = new NewRepositoryDialog();
		d.setVisible(true);
	}

	@Override
	protected void finish() {
		try {
			if (localButton.isSelected()) {
				localRepositoryPanel.makeRepository();
			} else {
				// check all custom repository radio buttons
				for (Pair<RepositoryConfigurationPanel, JRadioButton> value : repoConfigPanels.values()) {

					// once we have found the selected radio button
					if (value.getSecond().isSelected()) {

						// lookup the corresponding factory and create the repository
						value.getFirst().makeRepository();
					}
				}
			}
			super.finish();
		} catch (RepositoryException e) {
			SwingTools.showSimpleErrorMessage(this, "cannot_create_repository", e);
		}
	}

	@Override
	protected void previous() {
		// reset the finish button when going back, e.g. when selecting another type of repo
		if (!localButton.isSelected() && finish.isEnabled()) {
			updateFinishButton(false);
		}
		super.previous();
	}

	@Override
	protected void next() {
		super.next();
		// set the finish button when reaching the last page
		if (!localButton.isSelected() && finish.isEnabled()) {
			updateFinishButton(true);
		}
	}

	/**
	 * Sets or resets the {@link #finish} button for the selected repository panel
	 *
	 * @param set to set or reset the finish button
	 * @since 9.0.2
	 */
	private void updateFinishButton(boolean set) {
		repoConfigPanels.values().stream().filter(p -> p.getSecond().isSelected())
				.forEach(p -> p.getFirst().setOkButton(set ? finish : null));
	}

	@Override
	protected String getNameForStep(int step) {
		switch (step) {
			case 0:
				if (repoConfigPanels.entrySet().isEmpty()) {
					return "local";
				} else {
					return "first";
				}
			case 1:
				if (localButton.isSelected()) {
					return "local";
				} else {
					// go through the custom radio buttons and return the key of the selected button
					for (Entry<String, Pair<RepositoryConfigurationPanel, JRadioButton>> entry : repoConfigPanels.entrySet()) {
						if (entry.getValue().getSecond().isSelected()) {
							return entry.getKey();
						}
					}
				}
			default:
				throw new IllegalArgumentException("Illegal index: " + step);
		}
	}

	@Override
	protected boolean isComplete() {
		return isLastStep(getCurrentStep());
	}

	@Override
	protected boolean isLastStep(int step) {
		return step >= 1;
	}
}

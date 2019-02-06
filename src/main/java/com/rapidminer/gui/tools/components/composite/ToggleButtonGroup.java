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
package com.rapidminer.gui.tools.components.composite;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;


/**
 * Group of {@link JToggleButton}s with a behavior similar to a radio button group.
 *
 * @author Michael Knopf, Marcel Michel
 * @since 7.0.0
 */
public class ToggleButtonGroup extends JPanel {

	private static final long serialVersionUID = 1L;

	// pseudo tab behavior for buttons
	protected final ActionListener buttonChooser = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			for (JToggleButton button : primaryButtons) {
				if (button != e.getSource() && button.isSelected()) {
					button.setSelected(false);
				} else if (button == e.getSource() && !button.isSelected()) {
					button.setSelected(true);
				}

				if (button.isSelected()) {
					button.setFont(button.getFont().deriveFont(Font.BOLD));
				} else {
					button.setFont(button.getFont().deriveFont(Font.PLAIN));
				}
			}

			if (secondaryButton != null) {
				if (secondaryButton != e.getSource() && secondaryButton.isSelected()) {
					secondaryButton.clearMenuSelection();
				}
			}
		}
	};

	/** Array of primary buttons. */
	protected CompositeToggleButton[] primaryButtons;

	/** Button which shows the secondary actions, if available */
	protected CompositeMenuToggleButton secondaryButton;

	/** The preferred size of the nested {@link CompositeToggleButton}s. */
	protected Dimension preferredSize;

	/**
	 * Creates a new button group from the given Actions (requires at least two actions).
	 *
	 * @param actions
	 *            the action
	 */
	public ToggleButtonGroup(Action... actions) {
		this(null, actions);
	}

	/**
	 * Creates a new button group from the given Actions (requires at least two actions).
	 *
	 * @param preferredSize
	 *            the preferredSize of the nested {@link CompositeToggleButton}s or {@code null}
	 * @param actions
	 *            the action
	 */
	public ToggleButtonGroup(Dimension preferredSize, Action... actions) {
		if (actions.length < 2) {
			throw new IllegalArgumentException("At least two primary actions must be specified.");
		}

		this.setOpaque(false);
		this.preferredSize = preferredSize;

		primaryButtons = new CompositeToggleButton[actions.length];
		for (int i = 0; i < actions.length; i++) {
			int position;
			if (i == 0) {
				position = SwingConstants.LEFT;
			} else if (i < actions.length - 1) {
				position = SwingConstants.CENTER;
			} else {
				position = SwingConstants.RIGHT;
			}
			primaryButtons[i] = new CompositeToggleButton(actions[i], position);
		}

		// align buttons left to right with no padding
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 0, 0, 0);
		gbc.fill = GridBagConstraints.VERTICAL;
		gbc.weighty = 1;

		for (JToggleButton button : primaryButtons) {
			button.addActionListener(buttonChooser);
			if (preferredSize != null) {
				button.setMinimumSize(preferredSize);
				button.setPreferredSize(preferredSize);
			}
			add(button, gbc);
		}
	}

	/**
	 * Displays the given actions with a {@link CompositeMenuToggleButton}.
	 *
	 * @param actions
	 *            the secondary actions
	 */
	public void addSeconderyActions(Action... actions) {
		if (secondaryButton == null) {
			secondaryButton = createCompositeMenuToggleButton(actions);
			secondaryButton.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(ItemEvent e) {
					if (((CompositeMenuToggleButton) e.getSource()).isPopupMenuItemSelected()) {
						for (AbstractButton primaryButton : primaryButtons) {
							primaryButton.setSelected(false);
							primaryButton.setFont(primaryButton.getFont().deriveFont(Font.PLAIN));
						}
					}
				}
			});
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(0, 0, 0, 0);
			gbc.fill = GridBagConstraints.VERTICAL;
			gbc.weighty = 1;

			CompositeToggleButton oldPrimaryButton = primaryButtons[primaryButtons.length - 1];
			oldPrimaryButton.removeActionListener(buttonChooser);
			remove(oldPrimaryButton);

			CompositeToggleButton newPrimaryButton = new CompositeToggleButton(oldPrimaryButton.getAction(),
					SwingConstants.CENTER);
			newPrimaryButton.addActionListener(buttonChooser);
			primaryButtons[primaryButtons.length - 1] = newPrimaryButton;

			if (preferredSize != null) {
				newPrimaryButton.setMinimumSize(preferredSize);
				newPrimaryButton.setPreferredSize(preferredSize);
				secondaryButton.setPreferredSize(new Dimension(secondaryButton.getPreferredSize().width + 10,
						secondaryButton.getPreferredSize().height));
			}

			add(newPrimaryButton, gbc);
			add(secondaryButton, gbc);
		} else {
			secondaryButton.addActions(actions);
		}
	}

	/**
	 * Creates a {@link CompositeMenuToggleButton} with the given actions and the position
	 * {@link SwingConstants#RIGHT}.
	 *
	 * @param actions
	 *            the actions which should be included
	 * @return the created button
	 */
	protected CompositeMenuToggleButton createCompositeMenuToggleButton(Action... actions) {
		return new CompositeMenuToggleButton(SwingConstants.RIGHT, actions);
	}

	/**
	 * Selects the corresponding UI element.
	 *
	 * @param action
	 *            the action which should be selected
	 */
	public void setSelected(Action action) {
		for (CompositeToggleButton primaryButton : primaryButtons) {
			if (action == primaryButton.getAction()) {
				primaryButton.setSelected(true);
				primaryButton.setFont(primaryButton.getFont().deriveFont(Font.BOLD));
			} else {
				primaryButton.setSelected(false);
				primaryButton.setFont(primaryButton.getFont().deriveFont(Font.PLAIN));
			}
		}
		if (secondaryButton != null) {
			secondaryButton.setSelected(action);
		}
	}
}

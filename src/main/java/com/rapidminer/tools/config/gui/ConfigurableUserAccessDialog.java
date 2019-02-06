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
package com.rapidminer.tools.config.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ProgressThread;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.PasswordInputCanceledException;
import com.rapidminer.tools.config.Configurable;
import com.rapidminer.tools.config.ConfigurationManager;


/**
 * This dialog can be used to modify the user access rights of a {@link Configurable}s, which has a
 * remote source. The dialog shows two lists. One with users that have no access to a remote
 * {@link Configurable} and one list with users that have access.
 *
 * A user is able to select one or multiple user groups and put it to the other list.
 *
 * @author Sabrina Kirstein
 *
 */
public class ConfigurableUserAccessDialog extends ButtonDialog {

	private static final long serialVersionUID = 4537846891423068444L;

	private static final Icon LEFT_ICON = SwingTools.createIcon("24/nav_left.png");
	private static final Icon RIGHT_ICON = SwingTools.createIcon("24/nav_right.png");

	private Configurable configurable;

	private DefaultListModel<String> restrictedListModel;
	private DefaultListModel<String> accessListModel;
	private JList<String> restrictedList;
	private JList<String> accessList;

	private Set<String> originalAccessList;

	private boolean confirmed = false;

	public ConfigurableUserAccessDialog(Window owner, Configurable configurable) {
		super(owner, "configurable_user_access_dialog", ModalityType.MODELESS);
		this.configurable = configurable;

		initGui();
	}

	private void initGui() {

		setModal(true);

		final JPanel selectionPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		restrictedListModel = new DefaultListModel<>();
		restrictedList = new JList<>(restrictedListModel);
		restrictedList.setLayoutOrientation(JList.VERTICAL);
		restrictedList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					allowAccessToSelectedUserGroups();
					restrictedList.setSelectedIndex(-1);
					selectionPanel.requestFocus();
					restrictedList.updateUI();
				}
			}
		});

		accessListModel = new DefaultListModel<>();
		accessList = new JList<>(accessListModel);
		accessList.setLayoutOrientation(JList.VERTICAL);
		accessList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					restrictAccessToSelectedUserGroups();
					accessList.setSelectedIndex(-1);
					selectionPanel.requestFocus();
					accessList.updateUI();
				}
			}
		});

		originalAccessList = new HashSet<>();

		ProgressThread pt = new ProgressThread("load_user_groups") {

			@Override
			public void run() {
				Collection<String> allGroups;

				try {
					allGroups = configurable.getSource().getContentManager().getAllGroupNames();
				} catch (PasswordInputCanceledException | RepositoryException e) {
					allGroups = ConfigurationManager.getInstance().getPermittedGroupsForConfigurable(configurable);
				}

				final Set<String> permittedGroups = ConfigurationManager.getInstance()
						.getPermittedGroupsForConfigurable(configurable);

				final Collection<String> allGroupsFinal = allGroups;

				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {

						for (String element : allGroupsFinal) {
							if (permittedGroups.contains(element)) {
								accessListModel.addElement(element);
								originalAccessList.add(element.toString());
							} else {
								restrictedListModel.addElement(element);
							}
						}
					}
				});

			}
		};
		pt.start();

		JPanel accessSelectionButtonsPanel = new JPanel(new BorderLayout());

		JButton accessButton = null;
		if (RIGHT_ICON != null) {
			accessButton = new JButton(RIGHT_ICON);
		} else {
			accessButton = new JButton(">");
		}
		accessButton.setToolTipText(I18N.getGUILabel("configurable_user_access_dialog.allow.label"));
		accessButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				allowAccessToSelectedUserGroups();
			}
		});

		JButton restrictButton = null;
		if (LEFT_ICON != null) {
			restrictButton = new JButton(LEFT_ICON);
		} else {
			restrictButton = new JButton("<");
		}
		restrictButton.setToolTipText(I18N.getGUILabel("configurable_user_access_dialog.restrict.label"));
		restrictButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				restrictAccessToSelectedUserGroups();
			}
		});
		accessSelectionButtonsPanel.add(accessButton, BorderLayout.NORTH);
		accessSelectionButtonsPanel.add(restrictButton, BorderLayout.SOUTH);

		JScrollPane restrictedAccessScrollPane = new ExtendedJScrollPane(restrictedList) {

			private static final long serialVersionUID = 8474453689364798720L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d), super.getPreferredSize().height);
			}
		};
		JScrollPane allowedAccessScrollPane = new ExtendedJScrollPane(accessList) {

			private static final long serialVersionUID = -7089596032717082128L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension((int) (getWidth() * 0.25d), super.getPreferredSize().height);
			}
		};

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		restrictedAccessScrollPane.setBorder(createTitledBorder("Restricted"));
		selectionPanel.add(restrictedAccessScrollPane, c);

		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		selectionPanel.add(accessSelectionButtonsPanel, c);

		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		allowedAccessScrollPane.setBorder(createTitledBorder("Access"));
		selectionPanel.add(allowedAccessScrollPane, c);

		layoutDefault(selectionPanel, createButtons(), NORMAL);
	}

	/**
	 * Creates and returns the button component.
	 *
	 * @return
	 */
	private JPanel createButtons() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints buttonGBC = new GridBagConstraints();

		buttonGBC.gridx = 0;
		buttonGBC.weightx = 1.0;
		buttonGBC.fill = GridBagConstraints.HORIZONTAL;
		buttonGBC.insets = new Insets(5, 5, 5, 5);
		buttonPanel.add(Box.createHorizontalGlue(), buttonGBC);

		JButton saveButton = new JButton(new ResourceActionAdapter(false, "configurable_user_access_dialog.save") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				confirmed = true;
				dispose();
			}

		});

		buttonGBC.gridx += 1;
		buttonGBC.weightx = 0.0;
		buttonPanel.add(saveButton, buttonGBC);

		ResourceAction cancelAction = new ResourceAction(false, "cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				dispose();
			}

		};

		JButton removeButton = new JButton(cancelAction);

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);

		buttonGBC.gridx += 1;
		buttonPanel.add(removeButton, buttonGBC);

		return buttonPanel;
	}

	private void allowAccessToSelectedUserGroups() {

		List<String> userKeys = restrictedList.getSelectedValuesList();
		int[] newSelection = new int[userKeys.size()];

		for (int i = 0; i < userKeys.size(); i++) {
			String userKey = userKeys.get(i);
			restrictedListModel.removeElement(userKey);
			int index = addElement(accessListModel, userKey);
			if (index != -1) {
				newSelection[i] = index;
			}
		}
		restrictedList.setSelectedIndex(-1);
		accessList.setSelectedIndices(newSelection);
		accessList.updateUI();
	}

	private void restrictAccessToSelectedUserGroups() {

		List<String> userKeys = accessList.getSelectedValuesList();
		int[] newSelection = new int[userKeys.size()];

		for (int i = 0; i < userKeys.size(); i++) {
			String userKey = userKeys.get(i);
			accessListModel.removeElement(userKey);
			int index = addElement(restrictedListModel, userKey);
			if (index != -1) {
				newSelection[i] = index;
			}
		}
		accessList.setSelectedIndex(-1);
		restrictedList.setSelectedIndices(newSelection);
		restrictedList.updateUI();
	}

	/**
	 * Adds the element sorted to the model
	 *
	 * @param model
	 * @param element
	 * @return index of the new element
	 */
	private int addElement(DefaultListModel<String> model, String element) {

		if (model.isEmpty()) {
			model.addElement(element);
			return 0;
		} else {
			for (int j = 0; j < model.getSize(); j++) {

				int compareValue = String.CASE_INSENSITIVE_ORDER.compare(model.getElementAt(j).toString(), element);
				if (compareValue > 0) {
					model.add(j, element);
					return j;
				}
				if (j == model.getSize() - 1) {
					model.add(j + 1, element);
					return j + 1;
				}
			}
		}
		// sth went wrong
		return -1;
	}

	/**
	 *
	 * @return the selected user groups that have access to the configurable
	 */
	public Set<String> getPermittedUserGroups() {

		Set<String> permittedUserGroups = new HashSet<>();
		if (confirmed) {
			for (int i = 0; i < accessListModel.getSize(); i++) {
				permittedUserGroups.add(accessListModel.getElementAt(i).toString());
			}
			return permittedUserGroups;
		} else {
			return originalAccessList;
		}
	}
}

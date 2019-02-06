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
package com.rapidminer.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.actions.Actions;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.tools.CamelCaseFilter;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.OperatorList;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.UpdateQueue;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * A dialog for adding new operators to the currently selected operator chain of the operator tree.
 * The new operator can be searched by name, by groups, by input types, and by output types (or
 * combinations). A short description of the operator is also shown. Therefore this dialog might be
 * useful for less experienced RapidMiner users. A shorter way for adding operators is to use the
 * context menu in the tree view or the new operator tab.
 *
 * @author Ingo Mierswa, Helge Homburg, Tobias Malbrecht
 */
public final class NewOperatorDialog extends ButtonDialog {

	private static final long serialVersionUID = 390653805759799295L;

	private transient OperatorDescription description = null;

	private final JPanel mainPanel = new JPanel();

	private OperatorInfoPanel operatorInfo = null;

	private final JList<OperatorDescription> operatorList = new OperatorList();

	private String searchText = "";

	private boolean isFullText;

	private Class<? extends IOObject> inputClass = null;

	private Class<? extends IOObject> outputClass = null;

	private String group = null;

	private transient OperatorCapability firstCapability = null;

	private transient OperatorCapability secondCapability = null;

	private final Actions actions;

	private final JCheckBox fullTextCheckBox;

	private final UpdateQueue updateQueue = new UpdateQueue("operator-filter");

	public NewOperatorDialog(Actions actions) {
		this(actions, null, null, null, null, false);
	}

	public NewOperatorDialog(Actions actions, final Class<? extends IOObject> inputClass,
			final Class<? extends IOObject> outputClass, final OperatorCapability firstCapability,
			final OperatorCapability secondCapability, boolean modal) {
		super(ApplicationFrame.getApplicationFrame(), "new_operator", modal ? ModalityType.APPLICATION_MODAL
				: ModalityType.MODELESS, new Object[] {});
		this.actions = actions;
		this.inputClass = inputClass;
		this.outputClass = outputClass;
		this.firstCapability = firstCapability;
		this.secondCapability = secondCapability;

		// main
		mainPanel.setLayout(new BorderLayout(GAP, 0));

		// search panel
		int rows = 2;
		if (inputClass == null) {
			rows++;
		}
		if (outputClass == null) {
			rows++;
		}
		if (firstCapability == null) {
			rows++;
		}
		if (secondCapability == null) {
			rows++;
		}
		JPanel searchPanel = new JPanel(new GridLayout(rows + 1, 2));
		searchPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.new_op_dialog.search_constraints")));

		mainPanel.add(searchPanel, BorderLayout.NORTH);

		// input objects (for in- and output classes)
		final String[] ioObjects = convertSet2Strings(OperatorService.getIOObjectsNames());
		String[] inputObjects = new String[ioObjects.length + 1];
		inputObjects[0] = "Any";
		System.arraycopy(ioObjects, 0, inputObjects, 1, ioObjects.length);

		// search text
		final JPanel searchFieldPanel = new JPanel(new BorderLayout());
		final JTextField searchField = new JTextField(searchText);
		searchField.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				searchText = searchField.getText().trim();
				updateOperatorList();
			}
		});
		searchFieldPanel.add(searchField, BorderLayout.CENTER);
		JButton clearSearchFieldButton = new JButton(new ResourceAction(true, "new_op_dialog.clear_search_field") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				searchText = "";
				searchField.setText(searchText);
				searchField.requestFocusInWindow();
				updateOperatorList();
			}
		});
		searchFieldPanel.add(clearSearchFieldButton, BorderLayout.EAST);
		ResourceLabel textLabel = new ResourceLabel("new_op_dialog.search_text");
		textLabel.setLabelFor(searchField);
		searchPanel.add(textLabel);
		searchPanel.add(searchFieldPanel);

		searchPanel.add(new JPanel());
		fullTextCheckBox = new JCheckBox(new ResourceAction("new_op_dialog.full_text_search") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				isFullText = fullTextCheckBox.isSelected();
			}
		});
		isFullText = fullTextCheckBox.isSelected();
		searchPanel.add(fullTextCheckBox);

		// groups
		java.util.List<String> allGroups = new LinkedList<String>();
		allGroups.add("Any");
		GroupTree groupTree = OperatorService.getGroups();
		addGroups(groupTree, null, allGroups);
		Collections.sort(allGroups);
		final String[] groupArray = new String[allGroups.size()];
		allGroups.toArray(groupArray);
		final JComboBox<String> groupComboBox = new JComboBox<>(groupArray);
		groupComboBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = groupComboBox.getSelectedIndex();
				if (selectedIndex <= 0) {
					group = null;
				} else {
					group = groupArray[selectedIndex];
				}
				updateOperatorList();
			}
		});
		ResourceLabel groupLabel = new ResourceLabel("new_op_dialog.operator_group");
		searchPanel.add(groupLabel);
		groupLabel.setLabelFor(groupComboBox);
		searchPanel.add(groupComboBox);

		// input
		if (inputClass == null) {
			final JComboBox<String> inputType = new JComboBox<>(inputObjects);
			inputType.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedIndex = inputType.getSelectedIndex();
					if (selectedIndex <= 0) {
						NewOperatorDialog.this.inputClass = null;
					} else {
						NewOperatorDialog.this.inputClass = OperatorService.getIOObjectClass(ioObjects[selectedIndex - 1]);
					}
					updateOperatorList();
				}
			});
			ResourceLabel inputLabel = new ResourceLabel("new_op_dialog.required_input");
			searchPanel.add(inputLabel);
			inputLabel.setLabelFor(inputType);
			searchPanel.add(inputType);
		}

		// output
		if (outputClass == null) {
			final JComboBox<String> outputType = new JComboBox<>(inputObjects);
			outputType.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedIndex = outputType.getSelectedIndex();
					if (selectedIndex <= 0) {
						NewOperatorDialog.this.outputClass = null;
					} else {
						NewOperatorDialog.this.outputClass = OperatorService.getIOObjectClass(ioObjects[selectedIndex - 1]);
					}
					updateOperatorList();
				}
			});
			ResourceLabel outputLabel = new ResourceLabel("new_op_dialog.delivered_output");
			searchPanel.add(outputLabel);
			outputLabel.setLabelFor(outputType);
			searchPanel.add(outputType);
		}

		// capabilities
		OperatorCapability[] caps = OperatorCapability.values();
		String[] capabilities = new String[caps.length + 1];
		capabilities[0] = "Any";
		int k = 1;
		for (OperatorCapability capability : caps) {
			capabilities[k++] = capability.getDescription();
		}

		if (firstCapability == null) {
			final JComboBox<String> firstCapabilityType = new JComboBox<>(capabilities);
			firstCapabilityType.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedIndex = firstCapabilityType.getSelectedIndex();
					if (selectedIndex <= 0) {
						NewOperatorDialog.this.firstCapability = null;
					} else {
						NewOperatorDialog.this.firstCapability = OperatorCapability.values()[selectedIndex - 1];
					}
					updateOperatorList();
				}
			});
			ResourceLabel firstCapabilityLabel = new ResourceLabel("new_op_dialog.first_capability");
			searchPanel.add(firstCapabilityLabel);
			firstCapabilityLabel.setLabelFor(firstCapabilityType);
			searchPanel.add(firstCapabilityType);
		}

		if (secondCapability == null) {
			final JComboBox<String> secondCapabilityType = new JComboBox<>(capabilities);
			secondCapabilityType.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					int selectedIndex = secondCapabilityType.getSelectedIndex();
					if (selectedIndex <= 0) {
						NewOperatorDialog.this.secondCapability = null;
					} else {
						NewOperatorDialog.this.secondCapability = OperatorCapability.values()[selectedIndex - 1];
					}
					updateOperatorList();
				}
			});
			ResourceLabel secondCapabilityLabel = new ResourceLabel("new_op_dialog.second_capability");
			searchPanel.add(secondCapabilityLabel);
			secondCapabilityLabel.setLabelFor(secondCapabilityType);
			searchPanel.add(secondCapabilityType);
		}

		// list panel
		operatorList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					OperatorDescription selection = operatorList.getSelectedValue();
					setSelectedOperator(selection);
				}
			}
		});
		operatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScrollPane = new ExtendedJScrollPane(operatorList);
		listScrollPane.setBorder(null);
		listScrollPane.setPreferredSize(new Dimension(250, 50));
		GridBagLayout layout = new GridBagLayout();
		JPanel listPanel = new JPanel(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1;
		c.weighty = 1;
		listPanel.setBorder(BorderFactory.createLineBorder(Colors.TEXTFIELD_BORDER));
		layout.setConstraints(listScrollPane, c);
		listPanel.add(listScrollPane);
		mainPanel.add(listPanel, BorderLayout.WEST);

		updateOperatorListNow();
		updateQueue.start();

		if (!modal) {
			JButton addButton = new JButton(new ResourceAction("add_operator_now") {

				private static final long serialVersionUID = -6725386765826715152L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					add();
				}
			});
			layoutDefault(mainPanel, LARGE, addButton, makeCloseButton());
		} else {
			JButton okButton = makeOkButton();
			layoutDefault(mainPanel, LARGE, okButton, makeCancelButton());
		}
		getRootPane().setDefaultButton(null);
	}

	private void updateOperatorList() {
		updateQueue.execute(new Runnable() {

			@Override
			public void run() {
				updateOperatorListNow();
			}
		});
	}

	private void updateOperatorListNow() {
		CamelCaseFilter filter = null;
		if (searchText != null) {
			filter = new CamelCaseFilter(searchText.trim());
		}
		final Vector<OperatorDescription> operators = new Vector<OperatorDescription>();
		for (String key : OperatorService.getOperatorKeys()) {
			OperatorDescription description = OperatorService.getOperatorDescription(key);
			if (description.isDeprecated()) {
				continue;
			}
			if (searchText != null && searchText.length() > 0) {
				if (isFullText) {
					if (!description.getLongDescriptionHTML().toLowerCase().contains(searchText.toLowerCase())) {
						continue;
					}
				} else {
					if (!filter.matches(description.getName())) {
						continue;
					}
				}
			}

			if (group != null && group.length() > 0 && description.getGroup().indexOf(group) < 0) {
				continue;
			}
			try {
				Operator operator = description.createOperatorInstance();
				if (inputClass != null && !operator.acceptsInput(inputClass)) {
					continue;
				}
				if (outputClass != null && !operator.producesOutput(outputClass)) {
					continue;
				}
				if (firstCapability != null) {
					if (!(operator instanceof CapabilityProvider)
							|| !((CapabilityProvider) operator).supportsCapability(firstCapability)) {
						continue;
					}
				}
				if (secondCapability != null) {
					if (!(operator instanceof CapabilityProvider)
							|| !((CapabilityProvider) operator).supportsCapability(secondCapability)) {
						continue;
					}
				}
			} catch (Exception e) {
			}
			operators.add(description);
		}
		Collections.sort(operators);

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				operatorList.removeAll();
				operatorList.setListData(operators);
				if (operators.size() > 0) {
					operatorList.setSelectedIndex(0);
				}
			}
		});
	}

	private void addGroups(GroupTree tree, String parentName, Collection<String> names) {
		Iterator<? extends GroupTree> i = tree.getSubGroups().iterator();
		while (i.hasNext()) {
			GroupTree subGroup = i.next();
			String name = parentName == null ? subGroup.getName() : parentName + "." + subGroup.getName();
			names.add(name);
			addGroups(subGroup, name, names);
		}
	}

	private void setSelectedOperator(OperatorDescription descriptionName) {
		if (operatorInfo != null) {
			mainPanel.remove(operatorInfo);
		}
		if (descriptionName != null) {
			this.description = descriptionName;
			operatorInfo = new OperatorInfoPanel(this.description);
		} else {
			operatorInfo = new OperatorInfoPanel(null);
		}
		mainPanel.add(operatorInfo, BorderLayout.CENTER);
		operatorInfo.revalidate();
	}

	private String[] convertSet2Strings(Collection<String> ioObjects) {
		String[] objectArray = new String[ioObjects.size()];
		Iterator<String> i = ioObjects.iterator();
		int index = 0;
		while (i.hasNext()) {
			objectArray[index++] = i.next();
		}
		return objectArray;
	}

	private Operator getOperator() throws OperatorCreationException {
		if (description != null) {
			Operator operator = OperatorService.createOperator(description);
			return operator;
		} else {
			return null;
		}
	}

	private void add() {
		try {
			Operator operator = getOperator();
			if (operator != null) {
				actions.insert(Collections.singletonList(operator));
				ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_NEW_OPERATOR_DIALOG, "inserted", operator.getOperatorDescription().getKey());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			SwingTools.showSimpleErrorMessage(this, "cannot_create_operator", ex);
		}
	}

	/**
	 * This method opens a dialog for selecting one operator fulfilling the given conditions.
	 * Conditions not needed must be null.
	 *
	 * @throws OperatorCreationException
	 */
	public static Operator selectMatchingOperator(Actions actions, Class<? extends IOObject> inputClass,
			Class<? extends IOObject> outputClass, OperatorCapability firstCapability, OperatorCapability secondCapability)
					throws OperatorCreationException {
		NewOperatorDialog dialog = new NewOperatorDialog(actions, inputClass, outputClass, firstCapability,
				secondCapability, true);
		dialog.setVisible(true);
		if (dialog.wasConfirmed()) {
			return dialog.getOperator();
		}
		return null;
	}
}

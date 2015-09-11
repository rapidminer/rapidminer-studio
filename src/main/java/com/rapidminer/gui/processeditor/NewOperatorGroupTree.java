/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ToggleAction;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJToolBar;
import com.rapidminer.gui.tools.FilterListener;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.SelectionNavigationListener;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
import com.rapidminer.gui.tools.components.DropDownButton;
import com.rapidminer.gui.tools.components.ToolTipWindow;
import com.rapidminer.gui.tools.components.ToolTipWindow.TipProvider;
import com.rapidminer.gui.tools.components.ToolTipWindow.TooltipLocation;
import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseEvent.LicenseEventType;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.tools.GroupTree;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.parameter.ParameterChangeListener;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;


/**
 * This tree displays all groups and can be used to change the selected operators.
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Land
 */
public class NewOperatorGroupTree extends JPanel implements FilterListener, SelectionNavigationListener {

	/**
	 * A checkbox menu item which always resembles the current state of a boolean parameter from
	 * ParameterService.
	 *
	 * @author Marius Helf
	 *
	 */
	private static class CheckBoxMenuItemParameterServiceListener extends JCheckBoxMenuItem implements
			ParameterChangeListener {

		private static final long serialVersionUID = 1L;

		private final String parameterKey;

		/**
		 * Instantiates a new CheckBoxMenuItemParameterServiceListener.
		 *
		 * @param parameterKey
		 *            The key of the parameter whose state this checkbox should resemble.
		 */
		public CheckBoxMenuItemParameterServiceListener(final String parameterKey, final ResourceAction resourceAction) {
			super(resourceAction);
			this.parameterKey = parameterKey;

			setSelected("true".equals(ParameterService.getParameterValue(parameterKey)));

			ParameterService.registerParameterChangeListener(this);
		}

		@Override
		public void informParameterChanged(final String key, final String value) {
			if (key != null && key.equals(parameterKey)) {
				setSelected("true".equals(value));
			}
		}

		@Override
		public void informParameterSaved() {
			// Do nothing
		}

	}

	private static final long serialVersionUID = 133086849304885475L;

	private final FilterTextField filterField = new FilterTextField(12);

	private transient final NewOperatorGroupTreeModel model = new NewOperatorGroupTreeModel();

	private final JTree operatorGroupTree = new JTree(model);

	private final NewOperatorEditor editor;

	private transient final ResourceAction CLEAR_FILTER_ACTION = new ResourceAction(true, "clear_filter") {

		private static final long serialVersionUID = 3236281211064051583L;

		@Override
		public void actionPerformed(final ActionEvent e) {
			filterField.clearFilter();
			filterField.requestFocusInWindow();
		}
	};

	private final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	private transient final ToggleAction FILTER_DEPRECATED_ACTION = new ToggleAction(true, "filter_deprecated") {

		private static final long serialVersionUID = -35181409559416043L;

		{
			setSelected(true);
			actionToggled(null);
		}

		@Override
		public void actionToggled(final ActionEvent e) {
			Enumeration<TreePath> expandedPaths = operatorGroupTree.getExpandedDescendants(new TreePath(operatorGroupTree
					.getModel().getRoot()));
			TreePath selectedPath = operatorGroupTree.getSelectionPath();
			model.setFilterDeprecated(isSelected());
			while (expandedPaths.hasMoreElements()) {
				operatorGroupTree.expandPath(expandedPaths.nextElement());
			}
			operatorGroupTree.setSelectionPath(selectedPath);
		}
	};

	private transient final ToggleAction SORT_BY_USAGE_ACTION = new ToggleAction(true, "sort_by_usage") {

		private static final long serialVersionUID = 1L;
		{
			setSelected(true);
			actionToggled(null);
		}

		@Override
		public void actionToggled(final ActionEvent e) {
			model.setSortByUsage(isSelected());
		}
	};

	public transient final Action INFO_OPERATOR_ACTION = new InfoOperatorAction() {

		private static final long serialVersionUID = 7157100643209732656L;

		@Override
		protected Operator getOperator() {
			return getSelectedOperator();
		}
	};

	private final JCheckBoxMenuItem autoWireInputsItem = new CheckBoxMenuItemParameterServiceListener(
			RapidMinerGUI.PROPERTY_AUTOWIRE_INPUT, new ResourceAction("auto_wire_inputs_on_add") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_AUTOWIRE_INPUT,
							Boolean.toString(autoWireInputsItem.isSelected()));
					ParameterService.saveParameters();
				}
			});

	private final JCheckBoxMenuItem autoWireOutputsItem = new CheckBoxMenuItemParameterServiceListener(
			RapidMinerGUI.PROPERTY_AUTOWIRE_OUTPUT, new ResourceAction("auto_wire_outputs_on_add") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(final ActionEvent e) {
					ParameterService.setParameterValue(RapidMinerGUI.PROPERTY_AUTOWIRE_OUTPUT,
							Boolean.toString(autoWireOutputsItem.isSelected()));
					ParameterService.saveParameters();
				}
			});

	private NewOperatorGroupTreeRenderer renderer;

	public NewOperatorGroupTree(final NewOperatorEditor editor) {
		this.editor = editor;
		setLayout(new BorderLayout());

		// operatorGroupTree.setRootVisible(true);
		operatorGroupTree.setShowsRootHandles(true);
		renderer = new NewOperatorGroupTreeRenderer();
		operatorGroupTree.setCellRenderer(renderer);
		operatorGroupTree.expandRow(0); // because we let the renderer determine the height
		operatorGroupTree.setRowHeight(0);

		JScrollPane scrollPane = new ExtendedJScrollPane(operatorGroupTree);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		filterField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.filter_deprecated.tip"));
		filterField.addFilterListener(this);
		filterField.addSelectionNavigationListener(this);

		JToolBar toolBar = new ExtendedJToolBar();
		toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		toolBar.setFloatable(false);
		DropDownButton autoWireMenuButton = DropDownButton.makeDropDownButton(new ResourceActionAdapter(true,
				"auto_wire_on_add"));
		autoWireMenuButton.setUsePopupActionOnMainButton();
		autoWireMenuButton.add(autoWireInputsItem);
		autoWireMenuButton.add(autoWireOutputsItem);
		autoWireMenuButton.addToToolBar(toolBar);
		toolBar.addSeparator();
		toolBar.add(new TextFieldWithAction(filterField, CLEAR_FILTER_ACTION, CLEAR_FILTER_HOVERED_ICON));
		JToggleButton filterDeprecatedButton = FILTER_DEPRECATED_ACTION.createToggleButton();
		filterDeprecatedButton.setText("");
		toolBar.add(filterDeprecatedButton);

		JToggleButton sortButton = SORT_BY_USAGE_ACTION.createToggleButton();
		sortButton.setText("");
		toolBar.add(sortButton);

		add(toolBar, BorderLayout.NORTH);

		operatorGroupTree.setRootVisible(false);
		operatorGroupTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		operatorGroupTree.setDragEnabled(true);
		operatorGroupTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(final TreeSelectionEvent e) {
				Operator op = getSelectedOperator();
				if (op != null) {
					// TODO: Re-enable when documentation is ready
					// RapidMinerGUI.getMainFrame().getOperatorDocumentationBrowser().setSelection(op);
					RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(op);
				}
			}
		});
		operatorGroupTree.addTreeExpansionListener(new TreeExpansionListener() {

			@Override
			public void treeExpanded(final TreeExpansionEvent event) {
				updateMaxUsageCount();
			}

			@Override
			public void treeCollapsed(final TreeExpansionEvent event) {
				updateMaxUsageCount();
			}
		});

		operatorGroupTree.setTransferHandler(new OperatorTransferHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<Operator> getDraggedOperators() {
				Operator selectedOperator = NewOperatorGroupTree.this.getSelectedOperator();
				if (selectedOperator == null) {
					return Collections.emptyList();
				} else {
					return Collections.singletonList(selectedOperator);
				}
			}
		});
		operatorGroupTree.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					insertSelected();
				} else {
					TreePath selPath = operatorGroupTree.getPathForLocation(e.getX(), e.getY());
					if (selPath != null) {
						operatorGroupTree.setSelectionPath(selPath);
					}
					evaluatePopup(e);
				}
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				TreePath selPath = operatorGroupTree.getPathForLocation(e.getX(), e.getY());
				if (selPath != null) {
					operatorGroupTree.setSelectionPath(selPath);
				}
				evaluatePopup(e);
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				TreePath selPath = operatorGroupTree.getPathForLocation(e.getX(), e.getY());
				if (selPath != null) {
					operatorGroupTree.setSelectionPath(selPath);
				}
				evaluatePopup(e);
			}
		});
		operatorGroupTree.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(final KeyEvent e) {}

			@Override
			public void keyReleased(final KeyEvent e) {
				// insert selected operator upon press of enter or space
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
					case KeyEvent.VK_SPACE:
						if (getSelectedOperator() != null) {
							insertSelected();
						} else {
							// folder or nothing selected

							TreePath path = operatorGroupTree.getSelectionPath();
							if (path != null) {
								// folder selected

								if (operatorGroupTree.isExpanded(path)) {
									operatorGroupTree.collapsePath(path);
								} else {
									operatorGroupTree.expandPath(path);
								}
							}
						}
						e.consume();
						return;
				}
			}

			@Override
			public void keyTyped(final KeyEvent e) {}

		});

		// we need to know when the license changes because operators may become
		// supported/unsupported
		ProductConstraintManager.INSTANCE.registerLicenseManagerListener(new LicenseManagerListener() {

			@Override
			public <S, C> void handleLicenseEvent(final LicenseEvent<S, C> event) {
				if (event.getType() == LicenseEventType.ACTIVE_LICENSE_CHANGED) {
					operatorGroupTree.repaint();
				}
			}
		});

		new ToolTipWindow(new TipProvider() {

			@Override
			public String getTip(final Object o) {
				OperatorDescription opDesc;
				if (o instanceof OperatorDescription) {
					opDesc = (OperatorDescription) o;
				} else if (o instanceof GroupTree) {
					GroupTree groupTree = (GroupTree) o;
					return "<h3>" + groupTree.getName() + "</h3><p>" + groupTree.getDescription() + "</p>";
				} else {
					return null;
				}
				StringBuilder b = new StringBuilder();
				b.append("<h3>").append(opDesc.getName()).append("</h3><p>");
				// b.append(opDesc.getLongDescriptionHTML()).append("</p>");
				b.append(opDesc.getShortDescription()).append("</p>");
				return b.toString();
			}

			@Override
			public Object getIdUnder(final Point point) {
				TreePath path = operatorGroupTree.getPathForLocation((int) point.getX(), (int) point.getY());
				if (path != null) {
					return path.getLastPathComponent();
				} else {
					return null;
				}
			}

			@Override
			public Component getCustomComponent(final Object id) {
				return null;
			}
		}, operatorGroupTree, TooltipLocation.RIGHT);
	}

	@Override
	public void valueChanged(final String value) {
		TreePath[] selectionPaths = operatorGroupTree.getSelectionPaths();

		List<TreePath> expandedPaths = model.applyFilter(value);

		for (TreePath expandedPath : expandedPaths) {
			int row = this.operatorGroupTree.getRowForPath(expandedPath);
			this.operatorGroupTree.expandRow(row);
		}

		GroupTree root = (GroupTree) this.operatorGroupTree.getModel().getRoot();
		TreePath path = new TreePath(root);
		showNodes(root, path, expandedPaths);
		if (selectionPaths != null) {
			for (TreePath selectionPath : selectionPaths) {
				Object lastPathComponent = selectionPath.getLastPathComponent();
				if (model.contains(lastPathComponent)) {
					operatorGroupTree.addSelectionPath(selectionPath);
				}
			}
		}
	}

	private void showNodes(final GroupTree tree, TreePath path, List<TreePath> expandedPaths) {
		if (tree.getSubGroups().size() == 0) {
			int row = this.operatorGroupTree.getRowForPath(path);
			this.operatorGroupTree.expandRow(row);
			this.editor.setOperatorList(tree);
		} else if (tree.getSubGroups().size() > 0) {
			int row = this.operatorGroupTree.getRowForPath(path);
			this.operatorGroupTree.expandRow(row);
			for (GroupTree child : tree.getSubGroups()) {
				TreePath childPath = path.pathByAddingChild(child);
				if (expandedPaths.contains(childPath)) {
					showNodes(child, childPath, expandedPaths);
				}
			}
		} else {
			int row = this.operatorGroupTree.getRowForPath(path);
			this.operatorGroupTree.expandRow(row);
			this.editor.setOperatorList(null);
		}
	}

	public boolean shouldAutoConnectNewOperatorsInputs() {
		return "true".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_AUTOWIRE_INPUT));
	}

	public boolean shouldAutoConnectNewOperatorsOutputs() {
		return "true".equals(ParameterService.getParameterValue(RapidMinerGUI.PROPERTY_AUTOWIRE_OUTPUT));
	}

	public JTree getTree() {
		return this.operatorGroupTree;
	}

	/** Creates a new popup menu for the selected operator. */
	private JPopupMenu createOperatorPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(this.INFO_OPERATOR_ACTION);
		menu.addSeparator();
		menu.add(new ResourceAction(true, "add_operator_now") {

			private static final long serialVersionUID = 4363124048356045034L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				insertSelected();
			}
		});
		return menu;
	}

	/**
	 * Checks if the given mouse event is a popup trigger and creates a new popup menu if necessary.
	 */
	private void evaluatePopup(final MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (getSelectedOperator() != null) {
				createOperatorPopupMenu().show(operatorGroupTree, e.getX(), e.getY());
			}
		}
	}

	private Operator getSelectedOperator() {
		if (operatorGroupTree.getSelectionPath() == null) {
			return null;
		}
		Object selectedOperator = operatorGroupTree.getSelectionPath().getLastPathComponent();
		if (selectedOperator != null && selectedOperator instanceof OperatorDescription) {
			try {
				return ((OperatorDescription) selectedOperator).createOperatorInstance();
			} catch (OperatorCreationException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return null;
		}
	}

	private void insertSelected() {
		Operator operator = getSelectedOperator();
		if (operator == null) {
			return;
		}
		MainFrame mainFrame = RapidMinerGUI.getMainFrame();
		mainFrame.getActions().insert(Collections.singletonList(operator));
	}

	@Override
	public void down() {
		int[] selectionRows = operatorGroupTree.getSelectionRows();
		if (selectionRows != null) {
			if (selectionRows.length > 0 && selectionRows[0] < operatorGroupTree.getRowCount() - 1) {
				operatorGroupTree.setSelectionRow(selectionRows[0] + 1);
			}
		} else {
			if (operatorGroupTree.getRowCount() > 0) {
				operatorGroupTree.setSelectionRow(0);
			}
		}
	}

	@Override
	public void left() {}

	@Override
	public void right() {}

	@Override
	public void up() {
		int[] selectionRows = operatorGroupTree.getSelectionRows();
		if (selectionRows != null) {
			if (selectionRows.length > 0 && selectionRows[0] > 0) {
				operatorGroupTree.setSelectionRow(selectionRows[0] - 1);
			}
		} else {
			if (operatorGroupTree.getRowCount() > 0) {
				operatorGroupTree.setSelectionRow(operatorGroupTree.getRowCount());
			}
		}
	}

	@Override
	public void selected() {
		insertSelected();
	}

	private void updateMaxUsageCount() {
		if (SORT_BY_USAGE_ACTION.isSelected()) {
			renderer.setMaxVisibleUsageCount(getMaxVisibleUsage());
		} else {
			renderer.setMaxVisibleUsageCount(0);
		}
	}

	private int getMaxVisibleUsage() {
		int max = 0;
		for (int i = 0; i < operatorGroupTree.getRowCount(); i++) {
			TreePath path = operatorGroupTree.getPathForRow(i);
			Object leaf = path.getLastPathComponent();
			if (leaf instanceof OperatorDescription) {
				OperatorDescription operatorDescription = (OperatorDescription) leaf;
				if (operatorDescription.getDeprecationInfo() == null) {
					int usageCount1 = (int) ActionStatisticsCollector.getInstance().getCount(
							ActionStatisticsCollector.TYPE_OPERATOR, operatorDescription.getKey(),
							ActionStatisticsCollector.OPERATOR_EVENT_EXECUTION);
					max = Math.max(max, usageCount1);
				}
			}
		}
		return max;
	}

	public AbstractPatchedTransferHandler getOperatorTreeTransferhandler() {
		return (AbstractPatchedTransferHandler) operatorGroupTree.getTransferHandler();
	}
}

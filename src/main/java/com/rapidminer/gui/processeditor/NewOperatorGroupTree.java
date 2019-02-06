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
package com.rapidminer.gui.processeditor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.rapidminer.core.license.ProductConstraintManager;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.dnd.AbstractPatchedTransferHandler;
import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.dnd.TransferableOperator;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.FilterListener;
import com.rapidminer.gui.tools.FilterTextField;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SelectionNavigationListener;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.TextFieldWithAction;
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
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector;
import com.rapidminer.tools.usagestats.ActionStatisticsCollector.UsageObject;


/**
 * This tree displays all groups and can be used to change the selected operators.
 *
 * @author Ingo Mierswa, Tobias Malbrecht, Sebastian Land
 */
public class NewOperatorGroupTree extends JPanel implements FilterListener, SelectionNavigationListener {

	/**
	 * Simple POJO implementation of {@link UsageObject} to log stats
	 * using {@link #logSearchTerm(String, String, Operator)}.
	 *
	 * @since 8.1.2
	 * @author Jan Czogalla
	 */
	private static final class OperatorTreeUsageObject implements UsageObject {

		private final String event;
		private final String searchText;
		private final Operator operator;

		private OperatorTreeUsageObject(String event, String searchText, Operator operator) {
			this.event = event;
			this.searchText = searchText;
			this.operator = operator;
		}

		@Override
		public void logUsage() {
			logSearchTerm(event, searchText, operator);
		}
	}

	private static final long serialVersionUID = 133086849304885475L;

	private final FilterTextField filterField = new FilterTextField(12);

	private transient final NewOperatorGroupTreeModel model = new NewOperatorGroupTreeModel();

	private final JTree operatorGroupTree = new JTree(model);

	private transient final ResourceAction CLEAR_FILTER_ACTION = new ResourceAction(true, "clear_filter") {

		private static final long serialVersionUID = 3236281211064051583L;

		@Override
		public void loggedActionPerformed(final ActionEvent e) {
			filterField.clearFilter();
			filterField.requestFocusInWindow();
		}
	};

	private final ImageIcon CLEAR_FILTER_HOVERED_ICON = SwingTools.createIcon("16/x-mark_orange.png");

	public transient final Action INFO_OPERATOR_ACTION = new InfoOperatorAction() {

		private static final long serialVersionUID = 7157100643209732656L;

		@Override
		protected Operator getOperator() {
			return getSelectedOperator();
		}
	};

	private final NewOperatorGroupTreeRenderer renderer = new NewOperatorGroupTreeRenderer();

	/**
	 *
	 * @param editor
	 *            NewOperatorEditor is no longer used
	 */
	public NewOperatorGroupTree(final NewOperatorEditor editor) {
		setLayout(new BorderLayout());

		operatorGroupTree.setShowsRootHandles(true);
		operatorGroupTree.setCellRenderer(renderer);
		operatorGroupTree.expandRow(0); // because we let the renderer determine the height
		operatorGroupTree.setRowHeight(0);

		JScrollPane scrollPane = new ExtendedJScrollPane(operatorGroupTree);
		scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Colors.TEXTFIELD_BORDER));
		add(scrollPane, BorderLayout.CENTER);

		filterField.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.filter_operators.tip"));
		filterField.addFilterListener(this);
		filterField.addSelectionNavigationListener(this);
		filterField.setDefaultFilterText(I18N.getMessage(I18N.getGUIBundle(), "gui.field.filter_operators.prompt"));
		filterField.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// log focus lost for operator search statistics
				String text = filterField.getText();
				if (!text.isEmpty()) {
					logSearchTerm("focus_lost", text);
				}
			}

			@Override
			public void focusGained(FocusEvent e) {
				// not needed
			}
		});
		filterField.getDocument().addDocumentListener(new DocumentListener() {

			private Timer updateTimer;

			{
				updateTimer = new Timer(1000, new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						// log timeout for operator search statistics
						String text = filterField.getText();
						if (!text.isEmpty()) {
							logSearchTerm("timeout", text);
						}
					}
				});
				updateTimer.setRepeats(false);
			}

			@Override
			public void removeUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				updateTimer.restart();
			}
		});

		JPanel headerBar = new JPanel(new BorderLayout());
		TextFieldWithAction tf = new TextFieldWithAction(filterField, CLEAR_FILTER_ACTION, CLEAR_FILTER_HOVERED_ICON) {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(super.getPreferredSize().width, PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
			}
		};
		headerBar.add(tf, BorderLayout.CENTER);
		headerBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(headerBar, BorderLayout.NORTH);

		operatorGroupTree.setRootVisible(false);
		operatorGroupTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		operatorGroupTree.setDragEnabled(true);
		operatorGroupTree.addTreeSelectionListener(new TreeSelectionListener() {

			@Override
			public void valueChanged(final TreeSelectionEvent e) {
				Operator op = getSelectedOperator();
				if (op != null) {
					RapidMinerGUI.getMainFrame().getOperatorDocViewer().setDisplayedOperator(op);
				}
			}
		});

		operatorGroupTree.setTransferHandler(new OperatorTransferHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			public Transferable createTransferable(JComponent c) {
				Operator selectedOperator = NewOperatorGroupTree.this.getSelectedOperator();
				if (selectedOperator == null) {
					return null;
				}
				TransferableOperator transferable = new TransferableOperator(new Operator[]{selectedOperator});
				transferable.setUsageObject(new OperatorTreeUsageObject("inserted", filterField.getText(), selectedOperator));
				return transferable;
			}

			@Override
			protected List<Operator> getDraggedOperators() {
				// noop, overriding createTransferable
				return null;
			}

		});
		operatorGroupTree.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(final MouseEvent e) {
				// don't do on resize drag
				if (SwingUtilities.isLeftMouseButton(e)) {
					return;
				}

				ProcessRendererModel modelRenderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer()
						.getModel();

				modelRenderer.setOperatorSourceHovered(true);
				modelRenderer.fireMiscChanged();
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				ProcessRendererModel modelRenderer = RapidMinerGUI.getMainFrame().getProcessPanel().getProcessRenderer()
						.getModel();

				modelRenderer.setOperatorSourceHovered(false);
				modelRenderer.fireMiscChanged();
			}

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
				if (OperatorService.isOperatorBlacklisted(opDesc.getKey())) {
					b.append(I18N.getGUILabel("operator.blacklisted.tip"));
				} else {
					b.append(opDesc.getShortDescription());
				}
				b.append("</p>");

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
			public void loggedActionPerformed(final ActionEvent e) {
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
		logSearchTerm("inserted", filterField.getText(), operator);
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

	public AbstractPatchedTransferHandler getOperatorTreeTransferhandler() {
		return (AbstractPatchedTransferHandler) operatorGroupTree.getTransferHandler();
	}

	/**
	 * Logs the searchText under the given event. Shortens the searchText if is longer than 50 characters.
	 * Does not specify an operator key.
	 */
	private static void logSearchTerm(String event, String searchText) {
		logSearchTerm(event, searchText, (String) null);
	}

	/**
	 * Logs the searchText under the given event. Shortens the searchText if is longer than 50 characters.
	 * Extracts the operator key from the given operator.
	 *
	 * @since 8.1.2
	 */
	private static void logSearchTerm(String event, String searchText, Operator operator) {
		logSearchTerm(event, searchText, operator == null ? null : operator.getOperatorDescription().getKey());
	}

	/**
	 * Logs the searchText under the given event. Shortens the searchText if is longer than 50 characters.
	 * Uses the specified operator key iff not {@code null}, otherwise uses the empty string.
	 *
	 * @since 8.1.2
	 */
	private static void logSearchTerm(String event, String searchText, String operatorID) {
		if (searchText.length() > 50) {
			searchText = searchText.substring(0, 50) + "[...]";
		}
		if (operatorID == null) {
			operatorID = "";
		}
		StringBuilder arg = new StringBuilder(searchText);
		arg.append(ActionStatisticsCollector.ARG_GLOBAL_SEARCH_SPACER);
		arg.append(operatorID);
		ActionStatisticsCollector.INSTANCE.log(ActionStatisticsCollector.TYPE_OPERATOR_SEARCH, event, arg.toString());
	}

	public FilterTextField getFilterField() {
		return filterField;
	}
}

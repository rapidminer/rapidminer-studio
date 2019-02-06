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
package com.rapidminer.gui.flow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ExtendedJTable;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.ResourceMenu;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.ViewToolBar;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.MetaDataError;
import com.rapidminer.operator.ports.quickfix.QuickFix;
import com.rapidminer.tools.I18N;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;


/**
 *
 * @author Simon Fischer
 */
public class ErrorTable extends JPanel implements Dockable, ProcessEditor {

	private final TableCellRenderer iconRenderer = new DefaultTableCellRenderer() {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (value instanceof ProcessSetupError) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, ((ProcessSetupError) value).getMessage(),
						isSelected, hasFocus, row, column);
				switch (((ProcessSetupError) value).getSeverity()) {
					case WARNING:
						label.setIcon(IMAGE_WARNING);
						break;
					case ERROR:
						label.setIcon(IMAGE_ERROR);
						break;
					default:
						label.setIcon(null); // cannot happen
				}

				return label;
			} else if (value instanceof Port) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, ((Port) value).getSpec(), isSelected,
						hasFocus, row, column);
				label.setIcon(((Port) value).getPorts().getOwner().getOperator().getOperatorDescription().getSmallIcon());
				return label;
			} else if (value instanceof Operator) {
				JLabel label = (JLabel) super.getTableCellRendererComponent(table, ((Operator) value).getName(), isSelected,
						hasFocus, row, column);
				label.setIcon(((Operator) value).getOperatorDescription().getSmallIcon());
				return label;
			} else {
				if (column == 1) {
					JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
							column);
					if (value == null) {
						label.setIcon(IMAGE_NO_QUICKFIX);
						label.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.no_quickfix_available.label"));
					}
					if (value instanceof List) {
						label.setIcon(IMAGE_QUICKFIX);
						label.setText(I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.choose_quickfix.label",
								((List<?>) value).size()));
					}
					if (value instanceof QuickFix) {
						QuickFix quickFix = (QuickFix) value;
						label.setIcon((Icon) quickFix.getAction().getValue(Action.SMALL_ICON));
						label.setText(quickFix.toString());
					}
					return label;
				} else {
					JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
							column);
					label.setIcon(null);
					return label;
				}
			}
		}
	};

	private final ExtendedJTable table = new ExtendedJTable(true, false, true) {

		private static final long serialVersionUID = 1L;

		@Override
		public TableCellRenderer getCellRenderer(int row, int column) {
			return iconRenderer;
		}

		@Override
		public void populatePopupMenu(JPopupMenu menu) {
			List<? extends QuickFix> fixes = errors.get(getSelectedRow()).getQuickFixes();
			if (!fixes.isEmpty()) {
				JMenu fixMenu = new ResourceMenu("quick_fixes");
				for (QuickFix fix : fixes) {
					fixMenu.add(fix.getAction());
				}
				menu.add(fixMenu);
				menu.addSeparator();
			}
			super.populatePopupMenu(menu);
		}

		@Override
		protected JTableHeader createDefaultTableHeader() {
			return new JTableHeader(columnModel) {

				private static final long serialVersionUID = -2000774622129683602L;

				@Override
				public String getToolTipText(MouseEvent e) {
					java.awt.Point p = e.getPoint();
					int index = columnModel.getColumnIndexAtX(p.x);
					int realIndex = columnModel.getColumn(index).getModelIndex();
					return COLUMN_TOOLTIPS[realIndex];
				};
			};
		};

		@Override
		public String getToolTipText(MouseEvent e) {
			Point p = e.getPoint();
			int realColumnIndex = convertColumnIndexToModel(columnAtPoint(p));
			int rowIndex = rowAtPoint(p);
			if (rowIndex >= 0 && rowIndex < getRowCount() && realColumnIndex == 1) {
				Object value = getModel().getValueAt(rowIndex, realColumnIndex);
				if (value == null) {
					return I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.no_quickfix_available.tip");
				}
				if (value instanceof List) {
					return I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.choose_quickfix.tip",
							((List<?>) value).size());
				}
				if (value instanceof QuickFix) {
					return ((QuickFix) value).toString();
				}
			}
			return super.getToolTipText(e);
		}
	};

	private final AbstractTableModel model = new AbstractTableModel() {

		private static final long serialVersionUID = 1L;

		@Override
		public String getColumnName(int col) {
			return COLUMN_NAMES[col];
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public int getRowCount() {
			return errors.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			ProcessSetupError error = errors.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return error;
				case 1:
					List<? extends QuickFix> fixes = error.getQuickFixes();
					if (fixes.size() > 1) {
						return fixes;
					}
					if (fixes.size() == 1) {
						return fixes.get(0);
					}
					return null;
				case 2:
					if (error instanceof MetaDataError) {
						return ((MetaDataError) error).getPort();
					} else {
						return error.getOwner().getOperator();
					}
				default:
					return null;
			}
		}
	};

	private static final long serialVersionUID = 1L;

	private static final ImageIcon IMAGE_WARNING = SwingTools.createIcon("16/sign_warning.png");

	private static final ImageIcon IMAGE_ERROR = SwingTools.createIcon("16/error.png");

	private static final ImageIcon IMAGE_NO_QUICKFIX = SwingTools.createIcon("16/"
			+ I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.no_quickfix_available.icon"));

	private static final ImageIcon IMAGE_QUICKFIX = SwingTools.createIcon("16/"
			+ I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.choose_quickfix.icon"));

	private static final String[] COLUMN_NAMES = {
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.message.label"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.fixes.label"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.location.label") };

	private static final String[] COLUMN_TOOLTIPS = {
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.message.tip"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.fixes.tip"),
			I18N.getMessage(I18N.getGUIBundle(), "gui.errortable.header.location.tip") };

	public static final String ERROR_TABLE_DOCK_KEY = "error_table";

	private final DockKey DOCK_KEY = new ResourceDockKey(ERROR_TABLE_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	private final MainFrame mainFrame;

	private Operator currentOperator;

	private Process currentProcess;

	private final JLabel headerLabel = new JLabel();

	private final JToggleButton onlyCurrent = new JToggleButton(new ResourceAction(true, "error_table_only_current") {

		private static final long serialVersionUID = -1454330266199555397L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			updateErrors();
		}
	});

	private List<ProcessSetupError> errors = new LinkedList<ProcessSetupError>();

	public ErrorTable(final MainFrame mainFrame) {
		super(new BorderLayout());

		this.mainFrame = mainFrame;
		onlyCurrent.setSelected(false);
		table.setShowVerticalLines(false);
		table.setModel(model);
		table.installToolTip();

		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.getColumnModel().getColumn(1).setPreferredWidth(200);
		table.getColumnModel().getColumn(2).setPreferredWidth(150);

		headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		headerLabel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		table.setBorder(null);
		JScrollPane scrollPane = new ExtendedJScrollPane(table);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		add(scrollPane, BorderLayout.CENTER);

		ViewToolBar toolBar = new ViewToolBar();
		toolBar.add(onlyCurrent);
		onlyCurrent.setText(null);
		toolBar.add(headerLabel);
		add(toolBar, BorderLayout.NORTH);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					switch (table.getSelectedColumn()) {
					// quick fixes
						case 1:
							List<? extends QuickFix> quickFixes = errors.get(table.getSelectedRow()).getQuickFixes();
							if (quickFixes.size() == 1) {
								quickFixes.get(0).apply();
							}
							if (quickFixes.size() > 1) {
								new QuickFixDialog(quickFixes).setVisible(true);
							}
							break;
						default:
							ProcessSetupError error = errors.get(table.getSelectedRow());
							Operator op = error.getOwner().getOperator();
							ErrorTable.this.mainFrame.selectAndShowOperator(op, true);
							// other
					}
				}
			}
		});
	}

	@Override
	public void processChanged(Process process) {
		currentProcess = process;
		updateErrors();
	}

	@Override
	public void processUpdated(Process process) {
		currentProcess = process;
		updateErrors();
	}

	@Override
	public void setSelection(List<Operator> selection) {
		this.currentOperator = selection.isEmpty() ? null : selection.get(0);
		updateErrors();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	private void updateErrors() {
		if (currentOperator != null && onlyCurrent.isSelected()) {
			fill(currentOperator);
		} else {
			if (currentProcess != null) {
				fill(currentProcess.getRootOperator());
			}
		}
	}

	private void fill(Operator root) {
		int numTotal = root.getProcess().getRootOperator().getErrorList().size();
		errors = root.getErrorList();
		String errorString;
		switch (errors.size()) {
			case 0:
				errorString = "No problems found";
				break;
			case 1:
				errorString = "One potential problem";
				break;
			default:
				errorString = errors.size() + " potential problems";
				break;
		}
		if (errors.size() != numTotal) {
			errorString = errorString + " (" + (numTotal - errors.size()) + " Filtered)";
		}
		headerLabel.setText(errorString);
		model.fireTableDataChanged();
	}

}

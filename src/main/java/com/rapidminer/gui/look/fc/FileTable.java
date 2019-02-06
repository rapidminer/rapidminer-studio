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
package com.rapidminer.gui.look.fc;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.tools.SwingTools;


/**
 * A table for the file details.
 *
 * @author Ingo Mierswa
 */
public class FileTable extends JTable implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = -8700859510439797254L;

	private static class LabelRenderer extends FileTableLabel implements TableCellRenderer {

		private static final long serialVersionUID = 8972168539366862236L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (value == null) {
				return null;
			}

			FileTableLabel label = (FileTableLabel) value;
			label.setOpaque(false);
			label.setSelected(isSelected);
			if (isSelected) {
				label.setForeground(UIManager.getColor("textHighlightText"));
			} else {
				label.setForeground(UIManager.getColor("textText"));
			}
			return label;
		}
	}

	private class HeaderActionListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			headerMenuChanged(e.getActionCommand(), (JCheckBoxMenuItem) e.getSource());
			captureColumnsWidth();
		}
	}

	private class TableHeaderMouseListener extends MouseAdapter implements MouseMotionListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (!e.isPopupTrigger()) {
				JTableHeader h = (JTableHeader) e.getSource();
				TableColumnModel columnModel = h.getColumnModel();
				int viewColumn = columnModel.getColumnIndexAtX(e.getX());
				int column = columnModel.getColumn(viewColumn).getModelIndex();
				if (column != -1) {
					if (columnModel.getColumn(viewColumn).getHeaderValue().equals("File Name")) {
						FileTable.this.fileList.orderBy(FileList.ORDER_BY_FILE_NAME, false);
						FileTable.this.fileList.updateTableData();
					} else if (columnModel.getColumn(viewColumn).getHeaderValue().equals("Type")) {
						FileTable.this.fileList.orderBy(FileList.ORDER_BY_FILE_TYPE, false);
						FileTable.this.fileList.updateTableData();
					} else if (columnModel.getColumn(viewColumn).getHeaderValue().equals("Last Modified")) {
						FileTable.this.fileList.orderBy(FileList.ORDER_BY_FILE_MODIFIED, false);
						FileTable.this.fileList.updateTableData();
					} else if (columnModel.getColumn(viewColumn).getHeaderValue().equals("Size")) {
						FileTable.this.fileList.orderBy(FileList.ORDER_BY_FILE_SIZE, false);
						FileTable.this.fileList.updateTableData();
					}
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			evaluateClick(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			evaluateClick(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			updateMainColumnIndex();
			captureColumnsWidth();
		}

		@Override
		public void mouseMoved(MouseEvent e) {}
	}

	private class TableKeyListener extends KeyAdapter {

		private FileTable table;

		private int tempNum = 0;

		@Override
		public void keyPressed(KeyEvent e) {
			if (this.table == null) {
				this.table = (FileTable) e.getSource();
			}

			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				FileTable.this.fileList.filechooserUI.getCancelSelectionAction().actionPerformed(null);
			} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
				updateLastSelected();
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {

				this.tempNum = FileTable.this.fileList.visibleItemsList.indexOf(FileTable.this.fileList.lastSelected);
				if (this.tempNum < FileTable.this.fileList.visibleItemsList.size() && this.tempNum > 0) {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList
							.elementAt(this.tempNum - 1);
					updateLastSelected();
				}

			} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				this.tempNum = FileTable.this.fileList.visibleItemsList.indexOf(FileTable.this.fileList.lastSelected);
				if (this.tempNum < FileTable.this.fileList.visibleItemsList.size() - 1) {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList
							.elementAt(this.tempNum + 1);
					updateLastSelected();
				}
			} else if (e.getKeyCode() == KeyEvent.VK_HOME) {
				FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList.elementAt(0);
				updateLastSelected();
			} else if (e.getKeyCode() == KeyEvent.VK_END) {
				FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList
						.elementAt(FileTable.this.fileList.visibleItemsList.size() - 1);
				updateLastSelected();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				doActionPerformed();
			} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
				this.tempNum = FileTable.this.fileList.visibleItemsList.indexOf(FileTable.this.fileList.lastSelected);

				int diff = (int) (((JViewport) this.table.getParent()).getViewRect().getHeight() / this.table.getCellRect(
						this.tempNum, 0, true).getHeight()) - 1;

				if (this.tempNum - diff < 0) {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList.elementAt(0);
					updateLastSelected();
				} else {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList.elementAt(this.tempNum
							- diff);
					updateLastSelected();
				}
			} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
				this.tempNum = FileTable.this.fileList.visibleItemsList.indexOf(FileTable.this.fileList.lastSelected);

				int diff = (int) (((JViewport) this.table.getParent()).getViewRect().getHeight() / this.table.getCellRect(
						this.tempNum, 0, true).getHeight()) - 1;

				if (this.tempNum + diff >= FileTable.this.fileList.visibleItemsList.size()) {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList
							.elementAt(FileTable.this.fileList.visibleItemsList.size() - 1);
					updateLastSelected();
				} else {
					FileTable.this.fileList.lastSelected = FileTable.this.fileList.visibleItemsList.elementAt(this.tempNum
							+ diff);
					updateLastSelected();
				}
			} else {
				if (KeyEvent.getKeyText(e.getKeyCode()).toLowerCase().equals("a")
						&& e.getModifiersEx() == InputEvent.CTRL_DOWN_MASK) {
					this.table.selectAll();
				}
				if (e.getModifiersEx() == 0) {
					this.table.forwardToNearestFor(String.valueOf(e.getKeyChar()).toLowerCase());
				}
			}
		}
	}

	private boolean mouseDownFlag = false;

	private boolean pointChanged = false;

	private int mpx, mpy, mpcx, mpcy;

	private int tx, ty;

	private Rectangle selectionRect, scrollRect;

	private JPopupMenu panePopup, headerPopup;

	private JMenuItem menuItem = new JMenuItem();

	private int row;

	private int column;

	private FileList fileList;

	private DefaultTableModel model;

	private Vector<String> columnNames = new Vector<>();

	private Vector<Integer> originalColumsWidth = new Vector<Integer>();

	private TableHeaderMouseListener tableHeaderListener = new TableHeaderMouseListener();

	private TableKeyListener tablekeylistener = new TableKeyListener();

	private Item tempItem;

	private int mainColumnIndex;

	private HeaderActionListener hal = new HeaderActionListener();

	private boolean mouseDragging = false;

	protected int[] columnsWidth;

	public FileTable(FileList chooser) {
		this.fileList = chooser;
		init();
	}

	public int getHeaderHeight() {
		return (int) this.tableHeader.getHeaderRect(0).getHeight();
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		this.row = this.rowAtPoint(event.getPoint());
		this.column = this.columnAtPoint(event.getPoint());

		if (this.column == this.mainColumnIndex && this.row > -1) {
			this.fileList.visibleItemsList.elementAt(this.row).getAdditionalFileData();
			return String.valueOf(this.fileList.visibleItemsList.elementAt(this.row).getToolTipText());
		} else {
			return null;
		}
	}

	@Override
	public boolean isCellEditable(int r, int c) {
		return false;
	}

	private void init() {
		this.model = (DefaultTableModel) this.getModel();

		this.columnNames.removeAllElements();

		this.columnNames.add("File Name");
		this.columnNames.add("Size");
		this.columnNames.add("Type");
		this.columnNames.add("Last Modified");

		this.originalColumsWidth.add(Integer.valueOf(0));
		this.originalColumsWidth.add(Integer.valueOf(0));
		this.originalColumsWidth.add(Integer.valueOf(0));
		this.originalColumsWidth.add(Integer.valueOf(0));

		this.setBackground(Color.white);
		this.setShowGrid(false);

		this.setRowMargin(0);
		this.setSelectionBackground(Color.white);
		this.setFont(Item.menuFont);
		this.setRowHeight(Math.max(getFont().getSize(), 18) + 2);
		this.getTableHeader().setPreferredSize(new Dimension(20, 20));
		this.getTableHeader().setSize(new Dimension(20, 20));
		initPopupMenu();

		this.getTableHeader().addMouseListener(this.tableHeaderListener);
		this.getTableHeader().addMouseMotionListener(this.tableHeaderListener);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addKeyListener(this.tablekeylistener);

		this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.setCellSelectionEnabled(true);

		this.setAutoCreateRowSorter(true);

		clearSelection();
	}

	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		// do nothing
	}

	private void initPopupMenu() {
		this.panePopup = this.fileList.getPanePopup();

		this.headerPopup = new JPopupMenu();

		for (String col : columnNames) {
			this.menuItem = new JCheckBoxMenuItem(col);
			this.menuItem.setSelected(true);
			this.menuItem.addActionListener(this.hal);
			this.headerPopup.add(this.menuItem);
		}
	}

	private void synchFilechooser() {
		this.fileList.selectedFilesVector.removeAllElements();

		for (int i = 0; i < this.getRowCount(); i++) {
			this.tempItem = this.fileList.visibleItemsList.elementAt(i);
			if (this.getSelectionModel().isSelectedIndex(i)) {
				this.tempItem.updateSelectionMode(true);
				this.fileList.selectedFilesVector.add(this.tempItem);
			} else {
				this.tempItem.updateSelectionMode(false);
				if (this.fileList.selectedFilesVector.contains(this.tempItem)) {
					this.fileList.selectedFilesVector.remove(this.tempItem);
				}
			}
		}

		this.fileList.synchFilechoserSelection();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (this.fileList.fc.isMultiSelectionEnabled()) {
			if (this.pointChanged) {
				if (this.mpcx < 0) {
					this.tx = this.mpcx + this.mpx;
					this.mpcx *= -1;
				} else {
					this.tx = this.mpx;
				}

				if (this.mpcy < 0) {
					this.ty = this.mpcy + this.mpy;
					this.mpcy *= -1;
				} else {
					this.ty = this.mpy;
				}

				this.selectionRect = new Rectangle(this.tx, this.ty, this.mpcx, this.mpcy);
				testFor(this.selectionRect);
				this.pointChanged = false;
			}

			if (this.mouseDownFlag) {
				g.setColor(new Color(RapidLookTools.getColors().getFileChooserColors()[0].getRed(), RapidLookTools
						.getColors().getFileChooserColors()[0].getGreen(),
						RapidLookTools.getColors().getFileChooserColors()[0].getBlue(), 40));
				g.fillRect(this.tx, this.ty, this.mpcx, this.mpcy);

				g.setColor(RapidLookTools.getColors().getFileChooserColors()[0]);
				g.drawRect(this.tx, this.ty, this.mpcx, this.mpcy);
			}
		}
	}

	private void testFor(Rectangle rect) {
		clearSelection();

		int x = (int) this.tableHeader.getHeaderRect(this.mainColumnIndex).getX();
		int y = 0;
		for (int i = 0; i < this.getRowCount(); i++) {

			y = (i - 1) * this.getRowHeight() + this.getHeaderHeight();

			Dimension d = ((FileTableLabel) this.getValueAt(i, this.mainColumnIndex)).getPreferredSize();
			Rectangle r = new Rectangle(x, y,
					Math.min((int) d.getWidth(), this.getColumnModel().getColumn(this.mainColumnIndex).getWidth()),
					this.getRowHeight(i));
			if (r.intersects(rect)) {
				if (!this.getSelectionModel().isSelectedIndex(i)) {
					updateSelectionInterval(i, true);
				}
			} else {
				if (!this.getSelectionModel().isSelectedIndex(i)) {
					getSelectionModel().removeIndexInterval(i, i);
				}
			}
		}
		synchFilechooser();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		this.mpcx = e.getX() - this.mpx;
		this.mpcy = e.getY() - this.mpy;
		this.pointChanged = true;
		this.mouseDragging = true;

		this.scrollRect = new Rectangle(e.getX(), e.getY(), 1, 1);
		this.scrollRectToVisible(this.scrollRect);

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	private void resetPane() {
		this.mouseDownFlag = false;
		this.mpcx = 0;
		this.mpcy = 0;
		this.mpx = 0;
		this.mpy = 0;
		this.tx = 0;
		this.ty = 0;
		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		synchFilechooser();
		if (e.getClickCount() == 2 && !(e.getSource() instanceof JTableHeader)) {
			this.row = ((JTable) e.getComponent()).rowAtPoint(e.getPoint());
			this.column = ((JTable) e.getComponent()).columnAtPoint(e.getPoint());

			if (getColumnName(this.column).equals(getColumnName(this.mainColumnIndex)) && this.row > -1) {
				this.tempItem = this.fileList.visibleItemsList.elementAt(this.row);
				if (this.tempItem.isDirectory()) {
					this.fileList.filechooserUI.setCurrentDirectoryOfFileChooser(this.tempItem.getFile());
				} else {
					this.fileList.fc.setSelectedFile(this.tempItem.getFile());
					this.fileList.filechooserUI.getApproveSelectionAction().actionPerformed(null);
				}
			}
		}
	}

	private void doActionPerformed() {
		if (this.fileList.selectedFilesVector.contains(this.fileList.lastSelected)) {
			this.tempItem = this.fileList.lastSelected;
			if (this.tempItem.isDirectory()) {
				this.fileList.filechooserUI.setCurrentDirectoryOfFileChooser(this.tempItem.getFile());
			} else {
				this.fileList.fc.setSelectedFile(this.tempItem.getFile());
				this.fileList.filechooserUI.getApproveSelectionAction().actionPerformed(null);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		this.mpx = e.getX();
		this.mpy = e.getY();
		this.mouseDownFlag = true;
		evaluateClick(e);

		if (e.getSource() instanceof JTable && !this.mouseDragging) {
			this.row = ((JTable) e.getComponent()).rowAtPoint(e.getPoint());
			this.column = ((JTable) e.getComponent()).columnAtPoint(e.getPoint());

			if (this.row < 0) {
				return;
			}

			int x = (int) this.tableHeader.getHeaderRect(this.mainColumnIndex).getX();
			int y = 0;
			y = (this.row - 1) * this.getRowHeight() + this.getHeaderHeight();
			Dimension d = ((FileTableLabel) this.getValueAt(this.row, this.mainColumnIndex)).getPreferredSize();
			Rectangle r = new Rectangle(x, y,
					Math.min((int) d.getWidth(), this.getColumnModel().getColumn(this.mainColumnIndex).getWidth()),
					this.getRowHeight(this.row));

			if (r.contains(e.getPoint())) {
				if (SwingTools.isControlOrMetaDown(e)) {
					if (this.isCellSelected(this.row, this.mainColumnIndex)) {
						if (!e.isPopupTrigger()) {
							this.getSelectionModel().removeSelectionInterval(this.row, this.row);
						}
					} else {
						updateSelectionInterval(this.row, true);
					}
				} else {
					if (this.isCellSelected(this.row, this.mainColumnIndex)) {
						// do nothing
					} else {
						updateSelectionInterval(this.row, false);
					}
				}
			} else {
				clearSelection();
			}
		}
		synchFilechooser();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.mouseDownFlag = false;
		this.mouseDragging = false;
		resetPane();
		evaluateClick(e);
		repaint();
	}

	@Override
	public void clearSelection() {
		super.clearSelection();
		this.getColumnModel().getSelectionModel().setSelectionInterval(this.mainColumnIndex, this.mainColumnIndex);
	}

	private void evaluateClick(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (e.getComponent() instanceof JTableHeader) {
				this.headerPopup.show(e.getComponent(), e.getX(), e.getY());
			} else {

				this.row = ((JTable) e.getComponent()).rowAtPoint(e.getPoint());
				this.column = ((JTable) e.getComponent()).columnAtPoint(e.getPoint());

				if (this.row < 0) {
					this.panePopup.show(e.getComponent(), e.getX(), e.getY());
					return;
				}

				int x = (int) this.tableHeader.getHeaderRect(this.mainColumnIndex).getX();
				int y = (this.row - 1) * this.getRowHeight() + this.getHeaderHeight();

				Dimension d = ((FileTableLabel) this.getValueAt(this.row, this.mainColumnIndex)).getPreferredSize();
				Rectangle r = new Rectangle(x, y, Math.min((int) d.getWidth(),
						this.getColumnModel().getColumn(this.mainColumnIndex).getWidth()), this.getRowHeight(this.row));

				if (r.contains(e.getPoint())) {
					this.tempItem = this.fileList.visibleItemsList.elementAt(this.row);
					this.tempItem.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
					this.fileList.lastSelected = this.tempItem;
				} else {
					this.panePopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	}

	public void updateData(Object[][] vals) {
		Enumeration<TableColumn> en = this.getColumnModel().getColumns();

		Vector<String> vec = new Vector<String>();
		while (en.hasMoreElements()) {
			vec.add(en.nextElement().getHeaderValue().toString());
		}

		if (vec.size() <= 0) {
			this.model.setDataVector(vals, this.columnNames.toArray());
		} else {
			this.model.setDataVector(vals, vec.toArray());
		}

		if (shouldRecalculateColumnSize()) {
			this.columnsWidth = new int[] { (int) (this.getWidth() * 0.50), (int) (this.getWidth() * 0.10),
					(int) (this.getWidth() * 0.21), (int) (this.getWidth() * 0.19) };
		}

		en = this.getColumnModel().getColumns();

		int i = 0;
		while (en.hasMoreElements()) {
			TableColumn tempColumn = en.nextElement();

			int cw = this.columnsWidth[i];
			if (cw == 0) {
				tempColumn.setMinWidth(cw);
			}
			tempColumn.setPreferredWidth(cw);
			tempColumn.setWidth(cw);
			i++;
		}
		this.mainColumnIndex = this.getColumnModel().getColumnIndex("File Name");
		this.getColumnModel().getColumn(this.mainColumnIndex).setCellRenderer(new LabelRenderer());
		this.setSize(getPreferredSize());
		clearSelection();
	}

	private void captureColumnsWidth() {
		this.columnsWidth = new int[] { this.getColumnModel().getColumn(0).getWidth(),
				this.getColumnModel().getColumn(1).getWidth(), this.getColumnModel().getColumn(2).getWidth(),
				this.getColumnModel().getColumn(3).getWidth() };
	}

	private boolean shouldRecalculateColumnSize() {
		if (this.columnsWidth == null || this.columnsWidth.length < 4) {
			return true;
		} else {
			if (this.columnsWidth[0] == 0 && this.columnsWidth[1] == 0 && this.columnsWidth[2] == 0
					&& this.columnsWidth[3] == 0) {
				return true;
			}
		}
		return false;
	}

	public int getInitialHeight() {
		int height = 0;
		int rowCount = this.getRowCount();
		if (rowCount > 0 && this.getColumnCount() > 0) {
			Rectangle r = this.getCellRect(rowCount - 1, 0, true);
			height = r.y + r.height;
		}
		return height + this.getTableHeader().getHeight();
	}

	private void updateLastSelected() {
		int tempNum = this.fileList.visibleItemsList.indexOf(this.fileList.lastSelected);
		updateSelectionInterval(tempNum, false);
		synchFilechooser();
	}

	@Override
	public void selectAll() {
		if (this.fileList.fc.isMultiSelectionEnabled()) {
			clearSelection();
			getSelectionModel().addSelectionInterval(0, this.getRowCount() - 1);
			synchFilechooser();
		}
	}

	private void updateMainColumnIndex() {
		this.mainColumnIndex = this.getColumnModel().getColumnIndex(this.columnNames.elementAt(0));
	}

	protected void updateSelectionInterval(int row, boolean add) {
		if (add && this.fileList.fc.isMultiSelectionEnabled()) {
			this.getSelectionModel().addSelectionInterval(row, row);
		} else {
			clearSelection();
			this.getSelectionModel().addSelectionInterval(row, row);
		}
		this.fileList.lastSelected = this.fileList.visibleItemsList.elementAt(row);
		if (!this.mouseDownFlag) {
			this.scrollRectToVisible(this.getCellRect(row, this.mainColumnIndex, false));
		}
	}

	private void headerMenuChanged(String col, JCheckBoxMenuItem source) {
		int index = this.getColumnModel().getColumnIndex(col); // real index
		if (!source.isSelected()) {
			int tv = 0;
			for (int i = 0; i < this.getColumnCount(); i++) {
				tv += this.getColumnModel().getColumn(i).getWidth();
			}
			if (tv == this.getColumn(col).getWidth()) {
				source.setSelected(true);
				return;
			}
			this.originalColumsWidth.set(this.columnNames.indexOf(col), new Integer(this.getColumnModel().getColumn(index)
					.getWidth()));
			this.getColumnModel().getColumn(index).setMaxWidth(0);
			this.getColumnModel().getColumn(index).setMinWidth(0);
			this.getColumnModel().getColumn(index).setWidth(0);
			this.getColumnModel().getColumn(index).setPreferredWidth(0);
			this.getTableHeader().resizeAndRepaint();
		} else {
			this.getColumnModel().getColumn(index).setMaxWidth(2147483647);
			this.getColumnModel().getColumn(index)
					.setPreferredWidth(this.originalColumsWidth.get(this.columnNames.indexOf(col)).intValue());
			this.getColumnModel().getColumn(index)
					.setWidth(this.originalColumsWidth.get(this.columnNames.indexOf(col)).intValue());
			this.originalColumsWidth.set(this.columnNames.indexOf(col), Integer.valueOf(0));
			this.getTableHeader().resizeAndRepaint();
		}
	}

	private void forwardToNearestFor(String pre) {
		int index = this.fileList.visibleItemsList.indexOf(this.fileList.lastSelected) + 1;
		for (int i = 0; i < this.fileList.visibleItemsList.size(); i++) {
			if (index == this.fileList.visibleItemsList.size()) {
				index = 0;
			}
			if (this.fileList.visibleItemsList.elementAt(index).getFileName().toLowerCase().startsWith(pre)) {
				updateSelectionInterval(index, false);
				this.scrollRectToVisible(this.getCellRect(index, this.mainColumnIndex, false));
				updateLastSelected();
				return;
			}
			index++;
		}
	}
}

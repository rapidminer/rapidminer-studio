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

import com.rapidminer.gui.look.RapidLookTools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingConstants;


/**
 * A panel containing all items (files etc.)
 * 
 * @author Ingo Mierswa
 */
public class ItemPanel extends JPanel implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = -4300786583560333090L;

	private DragSelectionThread selectingThread;

	private int mpcy = 0, mpcx = 0, mpy = 0, mpx = 0;

	private int tx = 0, ty = 0;

	private boolean mouseDownFlag = false;

	private Rectangle selectionRect;

	protected FileList filePane;

	private boolean pointChanged = false;

	private Rectangle scrollRect = new Rectangle(0, 0, 0, 0);

	private JPopupMenu popup;

	private ItemPanelKeyboardListener keylistener = new ItemPanelKeyboardListener();

	private int totalColumn;

	private int maxWidth;

	public ItemPanel(FileList parent) {
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
		this.addKeyListener(this.keylistener);

		this.setLayout(null);
		this.filePane = parent;
		this.setBackground(Color.white);

		this.popup = this.filePane.getPanePopup();

		this.selectingThread = new DragSelectionThread(this, this.filePane);
	}

	public FileList getFilePane() {
		return this.filePane;
	}

	@Override
	public void paint(Graphics g) {
		determinePreferredSize();
		super.paint(g);

		if (this.filePane.fc.isMultiSelectionEnabled()) {
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
				this.pointChanged = false;
			}

			this.selectionRect = new Rectangle(this.tx, this.ty, this.mpcx, this.mpcy);

			if (this.mouseDownFlag) {

				g.setColor(new Color(RapidLookTools.getColors().getFileChooserColors()[0].getRed(), RapidLookTools
						.getColors().getFileChooserColors()[0].getGreen(),
						RapidLookTools.getColors().getFileChooserColors()[0].getBlue(), 40));
				g.fillRect(this.tx, this.ty, this.mpcx, this.mpcy);

				g.setColor(RapidLookTools.getColors().getFileChooserColors()[0]);
				g.drawRect(this.tx, this.ty, this.mpcx, this.mpcy);

				updateSelectionsForDrag();

			}
		}
	}

	private void determinePreferredSize() {
		int w = 0, h = 0;
		for (Item tempItem : filePane.visibleItemsList) {
			if (tempItem.getX() + tempItem.getWidth() > w) {
				w = tempItem.getX() + tempItem.getWidth();
			}
			if (tempItem.getY() + tempItem.getHeight() > h) {
				h = tempItem.getY() + tempItem.getHeight();
			}
		}

		this.setPreferredSize(new Dimension(w + 20, h + 20));
		this.revalidate();

		this.setPreferredSize(new Dimension(w + 20, h + 20));
		this.setMinimumSize(new Dimension(w + 20, h + 20));
	}

	private void updateSelectionsForDrag() {
		this.selectingThread.startThread(this.selectionRect);
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		this.filePane.clearSelectedItemsList();
		requestFocusInWindow();
		this.mpx = e.getX();
		this.mpy = e.getY();
		this.mouseDownFlag = true;

		evaluateClick(e);
	}

	private void evaluateClick(MouseEvent e) {
		if (e.isPopupTrigger()) {
			this.popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		resetPane();
		evaluateClick(e);
	}

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

	/**
	 * @deprecated Since 7.4; did nothing
	 */
	@Deprecated
	public void moveSelectedItems(int difX, int difY) {}

	@Override
	public void mouseDragged(MouseEvent e) {
		this.mpcx = e.getX() - this.mpx;
		this.mpcy = e.getY() - this.mpy;
		this.pointChanged = true;
		this.scrollRect = new Rectangle(e.getX(), e.getY(), 1, 1);
		this.scrollRectToVisible(this.scrollRect);
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public void selectedComponentMousePressed(MouseEvent e) {
		for (Item tempItem : filePane.selectedFilesVector) {
			tempItem.componentMousePressed(e);
		}
	}

	public void selectedComponentMouseReleased(MouseEvent e) {
		for (Item tempItem : filePane.selectedFilesVector) {
			tempItem.componentMouseReleased(e);
		}
		this.determinePreferredSize();
	}

	public void selectedComponentMouseDragged(Point point) {
		for (Item tempItem : filePane.selectedFilesVector) {
			tempItem.componentMouseDragged(point);
		}
		this.scrollRectToVisible(this.filePane.lastSelected.getBounds());
	}

	protected void arrangeTheFiles() {
		arrangeTheFiles((int) ((JComponent) this.getParent().getParent().getParent()).getSize().getWidth());
	}

	protected void arrangeTheFiles(int w) {
		int counter = 0, r, c;

		if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL)) {
			this.totalColumn = (w - 30) / 130;
			if (this.totalColumn < 1) {
				this.totalColumn = 1;
			}
			for (Item tc : filePane.visibleItemsList) {
				tc.setVisible(true);
				r = counter / this.totalColumn;
				c = counter % this.totalColumn;
				tc.setLocation(c * 130 + 30, r * 150 + 30);
				counter++;
			}
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_ICON)) {
			this.totalColumn = (w - 30) / 90;
			if (this.totalColumn < 1) {
				this.totalColumn = 1;
			}
			for (Item tc : filePane.visibleItemsList) {
				r = counter / this.totalColumn;
				c = counter % this.totalColumn;
				tc.setLocation(c * 90 + 20, r * 100 + 20);
				counter++;
			}
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_LIST)) {
			if (this.maxWidth == 0) {
				return;
			}
			this.totalColumn = (w - 30) / (this.maxWidth + 30);
			if (this.totalColumn < 1) {
				this.totalColumn = 1;
			}
			for (Item tc : filePane.visibleItemsList) {
				tc.setVisible(true);
				r = counter / this.totalColumn;
				c = counter % this.totalColumn;
				tc.setLocation(c * (this.maxWidth + 30) + (c + 1) * 10, r * 18 + 10);
				counter++;
			}
		}

		repaint();
	}

	public void findBestConfig(Item thumb) {
		int r = 0, c = 0;
		int counter = this.filePane.visibleItemsList.size() - 1;

		if (this.filePane.filechooserUI.getView().equals(FileChooserUI.FILECHOOSER_VIEW_LIST)) {
			updateForListView(thumb);
			if (this.totalColumn == 0) {
				this.totalColumn = 1;
			}

			r = counter / this.totalColumn;
			c = counter % this.totalColumn;
			thumb.setLocation(c * (this.maxWidth + 30) + (c + 1) * 10, r * 18 + 10);
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_ICON)) {
			updateForIconView(thumb);
			r = counter / this.totalColumn;
			c = counter % this.totalColumn;
			thumb.setLocation(c * 90 + 20, r * 100 + 20);
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL)) {
			updateForThumbnailView(thumb);
			r = counter / this.totalColumn;
			c = counter % this.totalColumn;
			thumb.setLocation(c * 130 + 30, r * 150 + 30);
		}
	}

	public void updateForViewAndArrange() {
		if (this.filePane.filechooserUI.getView().equals(FileChooserUI.FILECHOOSER_VIEW_THUMBNAIL)) {
			for (Item item : filePane.visibleItemsList) {
				updateForThumbnailView(item);
			}
			this.filePane.updateThumbnail();
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_ICON)) {
			for (Item item : filePane.visibleItemsList) {
				updateForIconView(item);
			}
		} else if (this.filePane.filechooserUI.viewType.equals(FileChooserUI.FILECHOOSER_VIEW_LIST)) {
			for (Item item : filePane.visibleItemsList) {
				updateForListView(item);
			}
			this.maxWidth = 0;
			for (Item tc : filePane.visibleItemsList) {
				if (tc.imageLabel.getPreferredSize().getWidth() + tc.nameLabel.getPreferredLineWidth() > this.maxWidth) {
					this.maxWidth = (int) (tc.imageLabel.getPreferredSize().getWidth()
							+ tc.nameLabel.getPreferredLineWidth());
				}
			}
		}

		for (Item tempItem : filePane.selectedFilesVector) {
			if (!this.filePane.filechooserUI.getView().equals(FileChooserUI.FILECHOOSER_VIEW_LIST)) {
				tempItem.nameLabel.setMultiLine(true);
			}
			tempItem.updateSelectionMode(true);
		}

		arrangeTheFiles();
	}

	public void updateForListView(Item tc) {
		tc.nameLabel.setMultiLine(false);

		tc.setBestSize(tc.nameLabel.getPreferredLineWidth() + 25, 18);
		tc.setSize(tc.getBestSize());

		tc.setLayout(new BorderLayout());

		tc.remove(tc.imageLabel);
		tc.remove(tc.nameLabel);

		tc.setBorder(null);
		tc.imageLabel.setBorder(Item.emptyImageBorder);
		tc.nameLabel.setBorder(Item.emptyImageBorder);

		tc.nameLabel.setHorizontalAlignment(SwingConstants.LEFT);

		tc.add(tc.imageLabel, BorderLayout.WEST);
		tc.add(tc.nameLabel, BorderLayout.CENTER);

		tc.updateItemIcon();
	}

	private void updateForIconView(Item tc) {
		tc.setBestSize(80, 70);
		tc.setPreferredSize(tc.getBestSize());
		tc.setSize(tc.getBestSize());
		tc.nameLabel.setSize(80, tc.nameLabel.getPreferredLineHeight());

		tc.setLayout(new BorderLayout());

		tc.remove(tc.imageLabel);
		tc.remove(tc.nameLabel);

		tc.imageLabel.setBorder(Item.emptyImageBorder);
		tc.setBorder(Item.defaultThumbBorder);

		tc.nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

		tc.imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		tc.imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		tc.add(tc.imageLabel, BorderLayout.CENTER);
		tc.add(tc.nameLabel, BorderLayout.SOUTH);

		tc.setSelectionMode(false);
		tc.nameLabel.setMultiLine(false);
		tc.revalidate();

		tc.updateItemIcon();
	}

	private void updateForThumbnailView(Item tc) {
		tc.setBestSize(105, 105 + tc.nameLabel.getPreferredLineHeight());
		tc.setPreferredSize(tc.getBestSize());
		tc.setSize(tc.getBestSize());
		tc.nameLabel.setSize(105, tc.nameLabel.getPreferredLineHeight());

		tc.setLayout(new BorderLayout());

		tc.remove(tc.imageLabel);
		tc.remove(tc.nameLabel);

		tc.nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

		tc.imageLabel.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
		tc.imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		tc.imageLabel.setBorder(Item.grayImageBorder);
		tc.setBorder(Item.defaultThumbBorder);

		tc.add(tc.imageLabel, BorderLayout.CENTER);
		tc.add(tc.nameLabel, BorderLayout.SOUTH);

		tc.setSelectionMode(false);
		tc.nameLabel.setMultiLine(false);
		tc.revalidate();

		tc.updateItemIcon();
	}

	public void updateViewType() {
		updateForViewAndArrange();
	}

	public void useKeyMoves(String str, boolean isCtrl) {
		if (this.filePane.lastSelected == null || this.filePane.visibleItemsList.size() == 0) {
			return;
		}
		Item tempLast = this.filePane.lastSelected;

		Point center = new Point();
		Point mainCenter = this.filePane.lastSelected.getLocation();
		Item res = this.filePane.lastSelected;

		if (str.equals("HOME")) {
			this.filePane.updateFilechooserSelectedItems(this.filePane.visibleItemsList.elementAt(0), isCtrl);
		}
		if (str.equals("END")) {
			this.filePane.updateFilechooserSelectedItems(
					this.filePane.visibleItemsList.elementAt(this.filePane.visibleItemsList.size() - 1), isCtrl);
		}
		if (str.equals("SPACE")) {
			this.filePane.updateFilechooserSelectedItems(this.filePane.lastSelected, false);
		}
		if (str.equals("PAGE_UP")) {
			int n = this.filePane.visibleItemsList.indexOf(this.filePane.lastSelected);
			if (n >= 0) {
				tempLast = this.filePane.lastSelected;
			} else {
				return;
			}

			boolean flag = true;
			mainCenter = res.getLocation();
			tempLast = res;
			while (flag) {
				res = tempLast;
				mainCenter = tempLast.getLocation();
				for (Item tc : filePane.visibleItemsList) {
					center = tc.getLocation();
					if (mainCenter.getY() > center.getY()) {
						if (res.getLocation().distance(tempLast.getLocation()) == 0 || tc.getLocation()
								.distance(tempLast.getLocation()) < res.getLocation().distance(tempLast.getLocation())) {
							res = tc;
						}
					}
				}

				if (((JViewport) this.getParent()).getViewRect().getHeight() < res.getLocation()
						.distance(this.filePane.lastSelected.getLocation())
						|| tempLast.getFileName().equals(res.getFileName())) {
					flag = false;
					this.filePane.updateFilechooserSelectedItems(tempLast, isCtrl);
				} else {
					tempLast = res;
				}
			}

		} else if (str.equals("PAGE_DOWN")) {
			int n = this.filePane.visibleItemsList.indexOf(this.filePane.lastSelected);
			if (n >= 0) {
				tempLast = this.filePane.lastSelected;
			} else {
				return;
			}

			boolean flag = true;
			mainCenter = res.getLocation();
			// res=null;
			tempLast = res;
			while (flag) {
				res = tempLast;
				mainCenter = tempLast.getLocation();
				for (Item tc : filePane.visibleItemsList) {
					center = tc.getLocation();
					if (mainCenter.getY() < center.getY()) {
						if (res.getLocation().distance(tempLast.getLocation()) == 0 || tc.getLocation()
								.distance(tempLast.getLocation()) < res.getLocation().distance(tempLast.getLocation())) {
							res = tc;
						}
					}
				}

				if (((JViewport) this.getParent()).getViewRect().getHeight() < res.getLocation()
						.distance(this.filePane.lastSelected.getLocation())
						|| tempLast.getFileName().equals(res.getFileName())) {
					flag = false;
					this.filePane.updateFilechooserSelectedItems(tempLast, isCtrl);
					this.scrollRectToVisible(this.filePane.lastSelected.getBounds());
				} else {
					tempLast = res;
				}
			}

		} else if (str.equals("DOWN")) {
			for (Item tc : filePane.visibleItemsList) {
				center = tc.getLocation();
				if (mainCenter.getY() < center.getY()) {
					if (res.getLocation().distance(this.filePane.lastSelected.getLocation()) == 0
							|| tc.getLocation().distance(this.filePane.lastSelected.getLocation()) < res.getLocation()
									.distance(this.filePane.lastSelected.getLocation())) {
						res = tc;

					}
				}
			}
			this.filePane.updateFilechooserSelectedItems(res, isCtrl);
		} else if (str.equals("UP")) {
			for (Item tc : filePane.visibleItemsList) {
				center = tc.getLocation();
				if (mainCenter.getY() > center.getY()) {
					if (res.getLocation().distance(this.filePane.lastSelected.getLocation()) == 0
							|| tc.getLocation().distance(this.filePane.lastSelected.getLocation()) < res.getLocation()
									.distance(this.filePane.lastSelected.getLocation())) {
						res = tc;
					}
				}
			}
			this.filePane.updateFilechooserSelectedItems(res, isCtrl);
		} else if (str.equals("LEFT")) {
			for (Item tc : filePane.visibleItemsList) {
				center = tc.getLocation();
				if (mainCenter.getX() > center.getX()) {
					if (res.getLocation().distance(this.filePane.lastSelected.getLocation()) == 0
							|| tc.getLocation().distance(this.filePane.lastSelected.getLocation()) < res.getLocation()
									.distance(this.filePane.lastSelected.getLocation())) {
						res = tc;
					}
				}
			}

			this.filePane.updateFilechooserSelectedItems(res, isCtrl);

		} else if (str.equals("RIGHT")) {
			for (Item tc : filePane.visibleItemsList) {
				center = tc.getLocation();
				if (mainCenter.getX() < center.getX()) {
					if (res.getLocation().distance(this.filePane.lastSelected.getLocation()) == 0
							|| tc.getLocation().distance(this.filePane.lastSelected.getLocation()) < res.getLocation()
									.distance(this.filePane.lastSelected.getLocation())) {
						res = tc;
					}
				}
			}
			this.filePane.updateFilechooserSelectedItems(res, isCtrl);
		}
	}

	public String getCurrentView() {
		return this.filePane.filechooserUI.viewType;
	}

	public Vector<Item> getItemsList() {
		return this.filePane.visibleItemsList;
	}

	public void forwardToNearestFor(String pre) {
		int index = this.filePane.visibleItemsList.indexOf(this.filePane.lastSelected) + 1;
		for (int i = 0; i < this.filePane.visibleItemsList.size(); i++) {
			if (index == this.filePane.visibleItemsList.size()) {
				index = 0;
			}
			if (this.filePane.visibleItemsList.elementAt(index).getFileName().toLowerCase().startsWith(pre)) {
				this.filePane.updateFilechooserSelectedItems(this.filePane.visibleItemsList.elementAt(index), false);
				break;
			}
			index++;
		}
	}
}

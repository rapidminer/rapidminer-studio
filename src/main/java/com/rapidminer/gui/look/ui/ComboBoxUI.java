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
package com.rapidminer.gui.look.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;

import com.rapidminer.gui.look.RapidLookComboBoxEditor;
import com.rapidminer.gui.look.RapidLookListCellRenderer;
import com.rapidminer.gui.look.borders.Borders;
import com.rapidminer.gui.look.painters.CashedPainter;
import com.rapidminer.gui.tools.SwingTools;


/**
 * The UI for combo boxes.
 *
 * @author Ingo Mierswa
 */
public class ComboBoxUI extends BasicComboBoxUI {

	private class RapidLookComboPopup extends BasicComboPopup {

		private static final long serialVersionUID = 1389744017891652801L;

		public RapidLookComboPopup(JComboBox comboBox) {
			super(comboBox);
		}

		@Override
		protected void configureScroller() {
			this.scroller.setFocusable(false);
			this.scroller.getVerticalScrollBar().setFocusable(false);
			this.scroller.setBorder(null);
			this.scroller.setOpaque(false);
		}

		@Override
		protected void configurePopup() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorderPainted(true);
			setBorder(Borders.getPopupBorder());
			setOpaque(false);
			add(this.scroller);
			setDoubleBuffered(true);
			setFocusable(false);
		}

		@Override
		protected JList createList() {
			return new JList(this.comboBox.getModel()) {

				private static final long serialVersionUID = -2467344849011408539L;

				@Override
				public void processMouseEvent(MouseEvent e) {
					if (SwingTools.isControlOrMetaDown(e)) {
						e = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers()
								^ Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(), e.getX(), e.getY(),
								e.getClickCount(), e.isPopupTrigger());
					}
					super.processMouseEvent(e);
				}
			};
		}

		@Override
		protected void configureList() {
			super.configureList();
			this.list.setBackground(Color.white);
		}

		@Override
		public void delegateFocus(MouseEvent e) {
			super.delegateFocus(e);
		}

		@Override
		public void hide() {
			MenuSelectionManager manager = MenuSelectionManager.defaultManager();
			MenuElement[] selection = manager.getSelectedPath();
			for (MenuElement element : selection) {
				if (element == this) {
					manager.clearSelectedPath();
					break;
				}
			}
			ComboBoxUI.this.isDown = false;
			this.comboBox.repaint();
		}

		@Override
		public void show() {
			setListSelection(this.comboBox.getSelectedIndex());
			Point location = getPopupLocation();
			show(this.comboBox, location.x + 3, location.y - 2);
		}

		private void setListSelection(int selectedIndex) {
			if (selectedIndex == -1) {
				this.list.clearSelection();
			} else {
				this.list.setSelectedIndex(selectedIndex);
				this.list.ensureIndexIsVisible(selectedIndex);
			}
		}

		private Point getPopupLocation() {
			Dimension popupSize = new Dimension((int) this.comboBox.getSize().getWidth() - 6, (int) this.comboBox.getSize()
					.getHeight());
			Insets insets = getInsets();
			popupSize.setSize(popupSize.width - (insets.right + insets.left),
					getPopupHeightForRowCount(this.comboBox.getMaximumRowCount()));
			Rectangle popupBounds = computePopupBounds(0, this.comboBox.getBounds().height, popupSize.width,
					popupSize.height);
			Dimension scrollSize = popupBounds.getSize();
			Point popupLocation = popupBounds.getLocation();

			this.scroller.setMaximumSize(scrollSize);
			this.scroller.setPreferredSize(scrollSize);
			this.scroller.setMinimumSize(scrollSize);

			this.list.revalidate();
			return popupLocation;
		}
	}

	private class ComboBoxPropertyListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("enabled")) {
				synchronizeEditorStatus(((Boolean) evt.getNewValue()).booleanValue());
			}
		}
	}

	private class ComboBoxMouseListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			ComboBoxUI.this.isDown = true;
			super.mousePressed(e);
			getComboBox().repaint();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			getComboBox().repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			super.mouseEntered(e);
			getComboBox().repaint();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			ComboBoxUI.this.isDown = false;
			super.mouseExited(e);
			getComboBox().repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			ComboBoxUI.this.isDown = false;
			super.mouseReleased(e);
			getComboBox().repaint();
		}
	}

	// ================================================================

	private ComboBoxMouseListener comboBoxListener = new ComboBoxMouseListener();

	private ComboBoxPropertyListener changeListener = new ComboBoxPropertyListener();

	private boolean isDown = false;

	public static ComponentUI createUI(JComponent c) {
		return new ComboBoxUI();
	}

	@Override
	protected void installComponents() {
		this.arrowButton = createArrowButton();
		if (this.arrowButton != null) {
			this.comboBox.add(this.arrowButton);
		}
		if (this.comboBox.isEditable()) {
			addEditor();
		}
		this.comboBox.add(this.currentValuePane);
	}

	@Override
	public void installUI(JComponent c) {
		super.installUI(c);
		this.listBox.setCellRenderer(new RapidLookListCellRenderer());
	}

	@Override
	protected void installListeners() {
		super.installListeners();
		this.comboBox.addMouseListener(this.comboBoxListener);
		this.comboBox.addPropertyChangeListener(this.changeListener);
	}

	@Override
	protected void uninstallListeners() {
		this.comboBox.removeMouseListener(this.comboBoxListener);
		this.comboBox.removePropertyChangeListener(this.changeListener);
		super.uninstallListeners();
	}

	@Override
	public void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		this.hasFocus = this.comboBox.hasFocus();
		CashedPainter.drawComboBox(c, g, this.isDown);
		CashedPainter.drawComboBoxBorder(c, g, this.isDown, false);
		if (!this.comboBox.isEditable()) {
			Rectangle r = rectangleForCurrentValue();
			paintCurrentValue(g, r, this.hasFocus);
		}
	}

	@Override
	public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
		ListCellRenderer renderer = this.comboBox.getRenderer();
		Component c;

		if (hasFocus && !isPopupVisible(this.comboBox)) {
			c = renderer.getListCellRendererComponent(this.listBox, this.comboBox.getSelectedItem(), -1, true, false);
		} else {
			c = renderer.getListCellRendererComponent(this.listBox, this.comboBox.getSelectedItem(), -1, false, false);
			c.setBackground(UIManager.getColor("ComboBox.background"));
		}
		c.setFont(this.comboBox.getFont());

		if (this.comboBox.isEnabled()) {
			c.setForeground(this.comboBox.getForeground());
			c.setBackground(this.comboBox.getBackground());
		} else {
			c.setForeground(UIManager.getColor("ComboBox.disabledForeground"));
			c.setBackground(UIManager.getColor("ComboBox.disabledBackground"));
		}

		boolean shouldValidate = false;
		if (c instanceof JPanel) {
			shouldValidate = true;
		}

		c.setBackground(new Color(255, 255, 255, 0));

		this.currentValuePane.paintComponent(g, c, this.comboBox, bounds.x, bounds.y, bounds.width, bounds.height,
				shouldValidate);
	}

	@Override
	protected JButton createArrowButton() {
		return null;
	}

	@Override
	protected ComboBoxEditor createEditor() {
		return new RapidLookComboBoxEditor.UIResource();
	}

	@Override
	protected ComboPopup createPopup() {
		return new RapidLookComboPopup(this.comboBox);
	}

	private JComboBox getComboBox() {
		return this.comboBox;
	}

	protected void synchronizeEditorStatus(boolean enabled) {
		if (this.comboBox.getEditor() instanceof RapidLookComboBoxEditor) {
			((RapidLookComboBoxEditor) this.comboBox.getEditor()).setEnable(enabled);
		}
	}

	@Override
	protected Rectangle rectangleForCurrentValue() {
		int width = this.comboBox.getWidth();
		int height = this.comboBox.getHeight();

		return new Rectangle(5, 3, width - 25, height - 6);
	}
}

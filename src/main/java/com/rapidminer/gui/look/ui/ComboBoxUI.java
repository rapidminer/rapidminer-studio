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
package com.rapidminer.gui.look.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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

import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.RapidLookComboBoxEditor;
import com.rapidminer.gui.look.RapidLookListCellRenderer;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.look.borders.Borders;
import com.rapidminer.gui.tools.MenuShortcutJList;


/**
 * The UI for combo boxes.
 *
 * @author Ingo Mierswa
 */
public class ComboBoxUI extends BasicComboBoxUI {

	private class RapidLookComboPopup extends BasicComboPopup {

		private static final long serialVersionUID = 1389744017891652801L;
		/** as wide as our min resolution */
		private static final int MAX_POPUP_WIDTH = 1280;

		public RapidLookComboPopup(JComboBox<?> comboBox) {
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

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected JList<?> createList() {
			return new MenuShortcutJList(this.comboBox.getModel(), false);
		}

		@Override
		protected void configureList() {
			super.configureList();
			this.list.setBackground(Colors.WHITE);
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
			show(this.comboBox, location.x, location.y - 3);
		}

		@Override
		protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
			return super.computePopupBounds(px, py, Math.min(MAX_POPUP_WIDTH, Math.max(comboBox.getPreferredSize().width, pw)), ph);
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
			Dimension popupSize = new Dimension((int) this.comboBox.getSize().getWidth(), (int) this.comboBox.getSize()
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

	private class ComboBoxEditorFocusListener extends FocusAdapter {

		@Override
		public void focusGained(FocusEvent e) {
			getComboBox().repaint();
		}
	}

	// ================================================================

	private ComboBoxMouseListener comboBoxListener = new ComboBoxMouseListener();

	private ComboBoxPropertyListener changeListener = new ComboBoxPropertyListener();

	private ComboBoxEditorFocusListener focusListener = new ComboBoxEditorFocusListener();

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

	@SuppressWarnings("unchecked")
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
		this.comboBox.getEditor().getEditorComponent().addFocusListener(this.focusListener);
	}

	@Override
	protected void uninstallListeners() {
		this.comboBox.removeMouseListener(this.comboBoxListener);
		this.comboBox.removePropertyChangeListener(this.changeListener);
		this.comboBox.getEditor().getEditorComponent().removeFocusListener(this.focusListener);
		super.uninstallListeners();
	}

	@Override
	public void installDefaults() {
		super.installDefaults();
	}

	@Override
	public void paint(Graphics g, JComponent c) {
		paintBox(g, c);
		paintBorder(g, c);

		if (!this.comboBox.isEditable()) {
			Rectangle r = rectangleForCurrentValue();
			paintCurrentValue(g, r, this.comboBox.hasFocus());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
		ListCellRenderer<Object> renderer = this.comboBox.getRenderer();
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

		if (Boolean.parseBoolean(String.valueOf(comboBox.getClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK)))) {
			c.setBackground(Colors.COMBOBOX_BACKGROUND_DARK);
		} else {
			c.setBackground(Colors.COMBOBOX_BACKGROUND);
		}

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

	private JComboBox<?> getComboBox() {
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

	/**
	 * Draws the combobox itself.
	 */
	private void paintBox(Graphics g, JComponent c) {
		int w = c.getWidth();
		int h = c.getHeight() - 1;
		if (w <= 0 || h <= 0) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (c.isEnabled()) {
			if (Boolean.parseBoolean(String.valueOf(c.getClientProperty(RapidLookTools.PROPERTY_INPUT_BACKGROUND_DARK)))) {
				g2.setColor(Colors.COMBOBOX_BACKGROUND_DARK);
			} else {
				g2.setColor(Colors.COMBOBOX_BACKGROUND);
			}
		} else {
			g2.setColor(Colors.COMBOBOX_BACKGROUND_DISABLED);
		}

		g2.fillRoundRect(0, 0, w - 1, h, RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		// arrow
		int ny = c.getSize().height / 2 - 3;
		int nx = c.getWidth() - 15;

		if (isDown && c.isEnabled()) {
			nx++;
			ny++;
		}
		g2.translate(nx, ny);

		if (c.isEnabled()) {
			g2.setColor(Colors.COMBOBOX_ARROW);
		} else {
			g2.setColor(Colors.COMBOBOX_ARROW_DISABLED);
		}

		w = 14;
		Polygon arrow = new Polygon(new int[] { 0, 4, 8 }, new int[] { 0, 6, 0 }, 3);
		g2.fillPolygon(arrow);

		g2.translate(-nx, -ny);
	}

	/**
	 * Draws the border of the combobox.
	 */
	private void paintBorder(Graphics g, JComponent c) {
		int w = c.getWidth();
		int h = c.getHeight();
		if (w <= 0 || h <= 0) {
			return;
		}

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		boolean hasFocus = comboBox.isEditable()
				? c.isFocusOwner() || ((JComboBox<?>) c).getEditor().getEditorComponent().isFocusOwner() : c.isFocusOwner();
				if (c.isEnabled()) {
					if (hasFocus) {
						g2.setColor(Colors.COMBOBOX_BORDER_FOCUS);
					} else {
						g2.setColor(Colors.COMBOBOX_BORDER);
					}
				} else {
					g2.setColor(Colors.COMBOBOX_BORDER_DISABLED);
				}

				g2.drawRoundRect(0, 0, w - 1, h - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS, RapidLookAndFeel.CORNER_DEFAULT_RADIUS);
	}
}

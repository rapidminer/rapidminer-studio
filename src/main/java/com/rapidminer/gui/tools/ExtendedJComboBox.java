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
package com.rapidminer.gui.tools;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.accessibility.Accessible;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;


/**
 * A combo box which can use a predefined preferred size. Can also show the full value as tool tip
 * in cases where the strings were too short.
 *
 * @author Ingo Mierswa
 */
public class ExtendedJComboBox<E> extends JComboBox<E> {

	private static final long serialVersionUID = 8320969518243948543L;

	private static class ExtendedComboBoxRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = -6192190927539294311L;

		@SuppressWarnings("rawtypes")
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
				if (index >= 0) {
					list.setToolTipText((value == null) ? null : value.toString());
				}
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setFont(list.getFont());
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	private boolean isScrollingToTopOnChange = false;

	// this listener is used to scroll back on top, if the data has been changed.
	private ListDataListener scrollToTopListener = new ListDataListener() {

		@Override
		public void intervalRemoved(ListDataEvent e) {}

		@Override
		public void intervalAdded(ListDataEvent e) {
			if (isScrollingToTopOnChange) {
				Accessible accessibleChild = ExtendedJComboBox.this.getUI().getAccessibleChild(ExtendedJComboBox.this, 0);
				if (!(accessibleChild instanceof JPopupMenu)) {
					return;
				}
				Object scrollPaneObject = ((JPopupMenu) accessibleChild).getComponent(0);
				if (scrollPaneObject instanceof JScrollPane) {
					((JScrollPane) scrollPaneObject).getVerticalScrollBar().setValue(0);
				}
			}
		}

		@Override
		public void contentsChanged(ListDataEvent e) {}
	};

	private int preferredWidth = -1;

	private int minimumWidth = -1;

	private boolean layingOut = false;

	private boolean wide = true;

	public ExtendedJComboBox(E[] values) {
		this(-1, -1, true, values);
	}

	public ExtendedJComboBox() {
		this(-1, -1);
	}

	public ExtendedJComboBox(int preferredWidth) {
		this(preferredWidth, -1);
	}

	public ExtendedJComboBox(int preferredWidth, int minimumWidth) {
		this(preferredWidth, minimumWidth, true);
	}

	// parameter cannot be null, must be array; there is no type checking on empty array
	@SuppressWarnings("unchecked")
	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide) {
		this(preferredWidth, minimumWidth, wide, (E[]) new Object[0]);
	}

	public ExtendedJComboBox(ComboBoxModel<E> model) {
		this(-1, -1, true, model);
	}

	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide, ComboBoxModel<E> model) {
		super(model);
		this.preferredWidth = preferredWidth;
		this.minimumWidth = minimumWidth;
		this.wide = wide;

		addScrollToTopListener();
	}

	// ExtendedComboBoxRenderer cannot be typed because of super class
	@SuppressWarnings("unchecked")
	public ExtendedJComboBox(int preferredWidth, int minimumWidth, boolean wide, E[] values) {
		super(values);
		this.preferredWidth = preferredWidth;
		this.minimumWidth = minimumWidth;
		this.wide = wide;

		addScrollToTopListener();
		setRenderer(new ExtendedComboBoxRenderer());
	}

	public void setPreferredWidth(int preferredWidth) {
		this.preferredWidth = preferredWidth;
	}

	@Override
	public void setModel(ComboBoxModel<E> aModel) {
		super.setModel(aModel);
		addScrollToTopListener();
	}

	private void addScrollToTopListener() {
		// this condition is only here because of bad pattern of calling overridden method
		// (setModel()) in super constructor (in which case, scrollToTopListener is uninitialized)
		if (scrollToTopListener != null) {
			getModel().addListDataListener(scrollToTopListener);
		}
	}

	public boolean isScrollingToTopOnChange() {
		return isScrollingToTopOnChange;
	}

	/**
	 * If the scrolling is enabled, the scroll bar of the combo box pop up list will be scrolled to
	 * top when the model's data changed.
	 */
	public void enableScrollingToTopOnChange(boolean enable) {
		isScrollingToTopOnChange = enable;
	}

	public boolean isWide() {
		return wide;
	}

	public void setWide(boolean wide) {
		this.wide = wide;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension dim = super.getPreferredSize();
		if (this.preferredWidth != -1) {
			return new Dimension(preferredWidth, (int) dim.getHeight());
		} else {
			return dim;
		}
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension dim = super.getMinimumSize();
		if (this.minimumWidth != -1) {
			return new Dimension(minimumWidth, (int) dim.getHeight());
		} else {
			return dim;
		}
	}

	@Override
	public void doLayout() {
		try {
			layingOut = true;
			super.doLayout();
		} finally {
			layingOut = false;
		}
	}

	@Override
	public Dimension getSize() {
		Dimension dim = super.getSize();
		if (!layingOut && isWide()) {
			dim.width = Math.max(dim.width, super.getPreferredSize().width);
		}
		dim.width = Math.min(dim.width, Toolkit.getDefaultToolkit().getScreenSize().width - 40);
		return dim;
	}
}

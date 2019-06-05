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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.LinkedList;
import javax.swing.JTextField;
import javax.swing.text.Document;

import com.rapidminer.tools.I18N;


/**
 * A text field for JList, JTable, or JTree filters. Updates all registered {@link FilterListener}
 * objects each time a key is pressed.
 * 
 * @author Tobias Malbrecht, Nils Woehler
 */
public class FilterTextField extends JTextField {

	private static final long serialVersionUID = -7613936832117084427L;

	private String defaultFilterText = I18N.getMessage(I18N.getGUIBundle(), "gui.filter_text_field.label");

	/** the value of the field when the last update was fired, init with default getText() result */
	private String valueLastUpdate = "";

	private final Collection<FilterListener> filterListeners;

	private final Collection<SelectionNavigationListener> selectionNavigationListeners;

	public FilterTextField() {
		this(null, null, 0);
	}

	public FilterTextField(int columns) {
		this(null, null, columns);
	}

	public FilterTextField(String text) {
		this(null, text, 0);
	}

	public FilterTextField(String text, int columns) {
		this(null, text, columns);
	}

	public FilterTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
		filterListeners = new LinkedList<>();
		selectionNavigationListeners = new LinkedList<>();
		if (text != null && text.length() != 0) {
			setDefaultFilterText(text);
		}
		SwingTools.setPrompt(defaultFilterText, this);
		addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e == null) {
					return;
				}
				switch (e.getKeyCode()) {
					case KeyEvent.VK_UP:
						for (SelectionNavigationListener l : selectionNavigationListeners) {
							l.up();
						}
						e.consume();
						return;
					case KeyEvent.VK_DOWN:
						for (SelectionNavigationListener l : selectionNavigationListeners) {
							l.down();
						}
						e.consume();
						return;
					case KeyEvent.VK_ENTER:
						for (SelectionNavigationListener l : selectionNavigationListeners) {
							l.selected();
						}
						e.consume();
						return;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				updateFilter(e);
			}

			@Override
			public void keyTyped(KeyEvent e) {}
		});
		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
				updateFilter(null);
			}
		});
	}

	public void addFilterListener(FilterListener l) {
		filterListeners.add(l);
	}

	public void removeFilterListener(FilterListener l) {
		filterListeners.remove(l);
	}

	public void addSelectionNavigationListener(SelectionNavigationListener l) {
		selectionNavigationListeners.add(l);
	}

	public void removeSelectionNavigationListener(SelectionNavigationListener l) {
		selectionNavigationListeners.remove(l);
	}

	public void clearFilter() {
		setText(null);
		updateFilter(null);
	}

	private void updateFilter(KeyEvent e) {
		String filterText = getText();
		// do nothing if no actual change was done
		if (valueLastUpdate != null && valueLastUpdate.equals(filterText)) {
			return;
		}

		if ((filterText == null) || (filterText.length() == 0)) {
			if ((e == null)
					|| ((e.getKeyCode() != KeyEvent.VK_BACK_SPACE) && (e.getKeyCode() != KeyEvent.VK_DELETE)
							&& (e.getKeyCode() != KeyEvent.VK_SHIFT) && (e.getKeyCode() != KeyEvent.VK_ALT)
							&& (e.getKeyCode() != KeyEvent.VK_ALT_GRAPH) && (e.getKeyCode() != KeyEvent.VK_CONTROL)
							&& (e.getKeyCode() != KeyEvent.VK_META) && (!e.isActionKey()))) {
				setText(null);
			}
		}

		// store text when listeners were notified
		valueLastUpdate = filterText;
		for (FilterListener l : filterListeners) {
			l.valueChanged(filterText);
		}
	}

	public void setDefaultFilterText(String text) {
		this.defaultFilterText = text;
		SwingTools.setPrompt(text, this);
	}
	
	public void setFilterText(String filterText) {
		setText(filterText);
		updateFilter(null);
	}

}

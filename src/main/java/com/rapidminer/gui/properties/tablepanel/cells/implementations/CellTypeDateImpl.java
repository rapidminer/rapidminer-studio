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
package com.rapidminer.gui.properties.tablepanel.cells.implementations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import com.michaelbaranov.microba.calendar.DatePicker;
import com.rapidminer.example.set.CustomFilter.CustomFilters;
import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDate;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDateTime;
import com.rapidminer.gui.properties.tablepanel.model.TablePanelModel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.I18N;


/**
 * GUI component for the {@link TablePanel} for {@link CellTypeDate} and {@link CellTypeDateTime}.
 *
 * @author Marco Boeck
 *
 */
public class CellTypeDateImpl extends JPanel implements CellTypeDate, CellTypeDateTime {

	private static final long serialVersionUID = 2280442691332299994L;

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/** the format for date_time */
	private static final ThreadLocal<DateFormat> FORMAT_DATE_TIME = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat(CustomFilters.DATE_TIME_FORMAT_STRING, Locale.ENGLISH);
		}
	};

	private static final ThreadLocal<DateFormat> FORMAT_DATE_TIME_OLD = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat(CustomFilters.DATE_TIME_FORMAT_STRING_OLD, Locale.ENGLISH);
		}
	};

	// ThreadLocal because DateFormat is NOT threadsafe and creating a new DateFormat is
	// EXTREMELY expensive
	/** the format for date */
	private static final ThreadLocal<DateFormat> FORMAT_DATE = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat(CustomFilters.DATE_FORMAT_STRING, Locale.ENGLISH);
		}
	};

	private static final ThreadLocal<DateFormat> FORMAT_DATE_OLD = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat(CustomFilters.DATE_FORMAT_STRING_OLD, Locale.ENGLISH);
		}
	};

	/**
	 * Creates a panel for date/date_time cells. Adds a date picker next to the field. Does not
	 * validate the model, so make sure this call works!
	 *
	 * @param model
	 * @param rowIndex
	 * @param columnIndex
	 * @param cellClass
	 */
	public CellTypeDateImpl(final TablePanelModel model, final int rowIndex, final int columnIndex,
			final Class<? extends CellType> cellClass) {
		super();

		setLayout(new BorderLayout());

		// get date from model if possible
		String text = String.valueOf(model.getValueAt(rowIndex, columnIndex));
		Date date = new Date();
		if (text != null && !"".equals(text.trim())) {
			try {
				// keep compatibility with processes from versions prior to 6.0.004
				int yearIndex = text.lastIndexOf("/") + 1;
				int firstWhitespaceIndex = text.indexOf(" ");
				String yearString = null;
				if (yearIndex > 0 && firstWhitespaceIndex > 0 && yearIndex < firstWhitespaceIndex) {
					yearString = text.substring(yearIndex, text.indexOf(" "));
				}
				// if year consists of 2 chars, use old (bugged) version
				if (CellTypeDate.class.isAssignableFrom(cellClass)) {
					if (yearString != null && yearString.length() == 2) {
						date = FORMAT_DATE_OLD.get().parse(text);
					} else {
						date = FORMAT_DATE.get().parse(text);
					}
				} else {
					if (yearString != null && yearString.length() == 2) {
						date = FORMAT_DATE_TIME_OLD.get().parse(text);
					} else {
						date = FORMAT_DATE_TIME.get().parse(text);
					}
				}
			} catch (ParseException e) {
				date = null;
			}
		}

		// create and add the date picker
		final DatePicker datePicker;
		if (CellTypeDate.class.isAssignableFrom(cellClass)) {
			datePicker = new DatePicker(date, FORMAT_DATE.get());
		} else {
			datePicker = new DatePicker(date, FORMAT_DATE_TIME.get());
		}

		// add ctrl+space shortcut for date picker (to surrounding panel [for tab focus reasons] and
		// to input field)
		Action caAction = new AbstractAction() {

			private static final long serialVersionUID = 5092311623220201432L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				datePicker.showPopup();
			}
		};
		datePicker.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"contentAssistAction");
		datePicker.getActionMap().put("contentAssistAction", caAction);

		final JFormattedTextField field = CellTypeImplHelper.createFormattedTextField(model, rowIndex, columnIndex);

		field.getInputMap().put(
				KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
				"contentAssistAction");
		field.getActionMap().put("contentAssistAction", caAction);

		// set syntax assist if available
		String syntaxHelp = model.getSyntaxHelpAt(rowIndex, columnIndex);
		if (syntaxHelp != null && !"".equals(syntaxHelp.trim())) {
			SwingTools.setPrompt(syntaxHelp, field);
		}

		// misc settings
		datePicker.setToolTipText(I18N.getMessageOrNull(I18N.getGUIBundle(), "gui.action.date_picker.tip"));
		datePicker.showButtonOnly(true);
		datePicker.setShowNoneButton(false);
		datePicker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				// empty date, no need to convert
				if (datePicker.getDate() == null) {
					field.setText("");
					return;
				}
				if (CellTypeDate.class.isAssignableFrom(cellClass)) {
					field.setText(FORMAT_DATE.get().format(datePicker.getDate()));
				} else {
					field.setText(FORMAT_DATE_TIME.get().format(datePicker.getDate()));
				}
			}

		});
		add(datePicker, BorderLayout.EAST);

		// set text to model value
		field.setText(text);
		if (CellTypeDate.class.isAssignableFrom(cellClass)) {
			field.setToolTipText(field.getToolTipText() + " (" + CustomFilters.DATE_FORMAT_STRING + ")");
		} else {
			field.setToolTipText(field.getToolTipText() + " (" + CustomFilters.DATE_TIME_FORMAT_STRING + ")");
		}
		add(field, BorderLayout.CENTER);

		// set size so panels don't grow larger when they get the chance
		setPreferredSize(new Dimension(300, 20));
		setMinimumSize(new Dimension(100, 15));
		setMaximumSize(new Dimension(1600, 30));
	}
}

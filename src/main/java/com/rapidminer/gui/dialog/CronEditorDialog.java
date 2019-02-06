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
package com.rapidminer.gui.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeCronExpression;
import com.rapidminer.tools.I18N;


/**
 * Dialog to create a cron expression via GUI. Call {@link #getCronExpression()} to get the cron
 * expression after the dialog has been confirmed.
 *
 * @author Marco Boeck, Miguel Buescher
 *
 */
public class CronEditorDialog extends ButtonDialog {

	// seconds elements
	private JRadioButton radioButtonSecOnce;
	private JRadioButton radioButtonSecEvery;
	private JCheckBox checkBoxSecRepeat;
	private JSpinner spinnerSecStart;
	private JSpinner spinnerSecRepeat;

	// minutes elements
	private JRadioButton radioButtonMinOnce;
	private JRadioButton radioButtonMinEvery;
	private JCheckBox checkBoxMinRepeat;
	private JSpinner spinnerMinStart;
	private JSpinner spinnerMinRepeat;

	// hours elements
	private JRadioButton radioButtonHourOnce;
	private JRadioButton radioButtonHourEvery;
	private JCheckBox checkBoxHourRepeat;
	private JSpinner spinnerHourStart;
	private JSpinner spinnerHourRepeat;

	// days elements
	private JRadioButton radioButtonDayOnce;
	private JRadioButton radioButtonDayEvery;
	private JCheckBox checkBoxDayRepeat;
	private JRadioButton radioButtonDayUseDayOfWeek;
	private JSpinner spinnerDayStart;
	private JSpinner spinnerDayRepeat;
	private JCheckBox checkBoxMonday;
	private JCheckBox checkBoxTuesday;
	private JCheckBox checkBoxWednesday;
	private JCheckBox checkBoxThursday;
	private JCheckBox checkBoxFriday;
	private JCheckBox checkBoxSaturday;
	private JCheckBox checkBoxSunday;

	// months elements
	private JRadioButton radioButtonMonthOnce;
	private JRadioButton radioButtonMonthEvery;
	private JCheckBox checkBoxMonthRepeat;
	private JRadioButton radioButtonMonthUseMonthOfYear;
	private JSpinner spinnerMonthStart;
	private JSpinner spinnerMonthRepeat;
	private JCheckBox checkBoxJanuary;
	private JCheckBox checkBoxFebruary;
	private JCheckBox checkBoxMarch;
	private JCheckBox checkBoxApril;
	private JCheckBox checkBoxMay;
	private JCheckBox checkBoxJune;
	private JCheckBox checkBoxJuly;
	private JCheckBox checkBoxAugust;
	private JCheckBox checkBoxSeptember;
	private JCheckBox checkBoxOctober;
	private JCheckBox checkBoxNovember;
	private JCheckBox checkBoxDecember;

	// years elements
	private JCheckBox checkBoxYearEnabled;
	private JRadioButton radioButtonYearOnce;
	private JRadioButton radioButtonYearEvery;
	private JCheckBox checkBoxYearRepeat;
	private JSpinner spinnerYearStart;
	private JSpinner spinnerYearRepeat;

	// warning label
	private JLabel warningLabel;

	private static final long serialVersionUID = 837836954191730785L;

	public CronEditorDialog(Operator operator, ParameterTypeCronExpression type) {
		this(ApplicationFrame.getApplicationFrame());
	}

	/**
	 * Creates a new cron editor dialog.
	 *
	 * @param owner
	 *            the parent window for this dialog
	 */
	public CronEditorDialog(Window owner) {
		super(owner, "croneditordialog", ModalityType.APPLICATION_MODAL, new Object[] {});

		setupGUI();

		// misc settings
		setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
	}

	/**
	 * Creates the GUI.
	 */
	private void setupGUI() {
		warningLabel = new JLabel(I18N.getGUILabel("cron_editor.high_frequency"),
				SwingTools.createIcon(I18N.getGUILabel("cron_editor.high_frequency.icon")), JLabel.RIGHT);

		// setup GUI
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		// seconds section
		JPanel panelSec = new JPanel();
		panelSec.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_sec.label")));
		panelSec.setLayout(new GridBagLayout());

		spinnerSecStart = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
		spinnerSecStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_once_spinner.tip"));
		spinnerSecRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
		spinnerSecRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_repeat_spinner.tip"));

		radioButtonSecOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_once.label"));
		radioButtonSecEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_every.label"));
		checkBoxSecRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_repeat.label"));
		ButtonGroup secButtonGroup = new ButtonGroup();
		secButtonGroup.add(radioButtonSecOnce);
		secButtonGroup.add(radioButtonSecEvery);

		radioButtonSecOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerSecStart.setEnabled(true);
				spinnerSecRepeat.setEnabled(checkBoxSecRepeat.isSelected());
				checkBoxSecRepeat.setEnabled(true);

				warningLabel.setVisible(false);
			}
		});
		radioButtonSecOnce.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_once.tip"));
		radioButtonSecOnce.doClick();
		radioButtonSecEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerSecStart.setEnabled(false);
				spinnerSecRepeat.setEnabled(false);
				checkBoxSecRepeat.setEnabled(false);
				warningLabel.setVisible(true);
			}
		});
		radioButtonSecEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_every.tip"));

		checkBoxSecRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerSecRepeat.setEnabled(checkBoxSecRepeat.isSelected());
			}
		});
		checkBoxSecRepeat.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_sec_repeat.tip"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.anchor = GridBagConstraints.WEST;
		panelSec.add(radioButtonSecEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panelSec.add(radioButtonSecOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelSec.add(spinnerSecStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelSec.add(checkBoxSecRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelSec.add(spinnerSecRepeat, gbc);

		// minutes section
		JPanel panelMin = new JPanel();
		panelMin.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_min.label")));
		panelMin.setLayout(new GridBagLayout());

		spinnerMinStart = new JSpinner(new SpinnerNumberModel(0, 0, 59, 1));
		spinnerMinStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_once_spinner.tip"));
		spinnerMinRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
		spinnerMinRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_repeat_spinner.tip"));

		radioButtonMinOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_once.label"));
		radioButtonMinEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_every.label"));
		checkBoxMinRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_repeat.label"));
		ButtonGroup minButtonGroup = new ButtonGroup();
		minButtonGroup.add(radioButtonMinOnce);
		minButtonGroup.add(radioButtonMinEvery);

		radioButtonMinOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMinStart.setEnabled(true);
				spinnerMinRepeat.setEnabled(checkBoxMinRepeat.isSelected());
				checkBoxMinRepeat.setEnabled(true);
			}
		});
		radioButtonMinOnce.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_once.tip"));
		radioButtonMinOnce.doClick();
		radioButtonMinEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMinStart.setEnabled(false);
				spinnerMinRepeat.setEnabled(false);
				checkBoxMinRepeat.setEnabled(false);
			}
		});
		radioButtonMinEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_every.tip"));

		checkBoxMinRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMinRepeat.setEnabled(checkBoxMinRepeat.isSelected());
			}
		});
		checkBoxMinRepeat.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_min_repeat.tip"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelMin.add(radioButtonMinEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panelMin.add(radioButtonMinOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelMin.add(spinnerMinStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelMin.add(checkBoxMinRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelMin.add(spinnerMinRepeat, gbc);

		// hours section
		JPanel panelHour = new JPanel();
		panelHour.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_hour.label")));
		panelHour.setLayout(new GridBagLayout());

		spinnerHourStart = new JSpinner(new SpinnerNumberModel(0, 0, 23, 1));
		spinnerHourStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_once_spinner.tip"));
		spinnerHourRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
		spinnerHourRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_repeat_spinner.tip"));

		radioButtonHourOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_once.label"));
		radioButtonHourEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_every.label"));
		checkBoxHourRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_repeat.label"));
		ButtonGroup hourButtonGroup = new ButtonGroup();
		hourButtonGroup.add(radioButtonHourOnce);
		hourButtonGroup.add(radioButtonHourEvery);

		radioButtonHourOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerHourStart.setEnabled(true);
				spinnerHourRepeat.setEnabled(checkBoxHourRepeat.isSelected());
				checkBoxHourRepeat.setEnabled(true);
			}
		});
		radioButtonHourOnce
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_once.tip"));
		radioButtonHourEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerHourStart.setEnabled(false);
				spinnerHourRepeat.setEnabled(false);
				checkBoxHourRepeat.setEnabled(false);
			}
		});
		radioButtonHourEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_every.tip"));
		radioButtonHourEvery.doClick();
		checkBoxHourRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerHourRepeat.setEnabled(checkBoxHourRepeat.isSelected());
			}
		});
		checkBoxHourRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_hour_repeat.tip"));

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelHour.add(radioButtonHourEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panelHour.add(radioButtonHourOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelHour.add(spinnerHourStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelHour.add(checkBoxHourRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelHour.add(spinnerHourRepeat, gbc);

		// days section
		JPanel panelDay = new JPanel();
		panelDay.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_day.label")));
		panelDay.setLayout(new GridBagLayout());

		spinnerDayStart = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
		spinnerDayStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_once_spinner.tip"));
		spinnerDayRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
		spinnerDayRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_repeat_spinner.tip"));

		radioButtonDayOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_once.label"));
		radioButtonDayEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_every.label"));
		checkBoxDayRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_repeat.label"));
		radioButtonDayUseDayOfWeek = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_day_of_week.label"));
		ButtonGroup dayButtonGroup = new ButtonGroup();
		dayButtonGroup.add(radioButtonDayOnce);
		dayButtonGroup.add(radioButtonDayEvery);
		dayButtonGroup.add(radioButtonDayUseDayOfWeek);

		checkBoxMonday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_monday.label"));
		checkBoxMonday
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_monday.tip"));
		checkBoxTuesday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_tuesday.label"));
		checkBoxTuesday
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_tuesday.tip"));
		checkBoxWednesday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_wednesday.label"));
		checkBoxWednesday.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_wednesday.tip"));
		checkBoxThursday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_thursday.label"));
		checkBoxThursday.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_thursday.tip"));
		checkBoxFriday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_friday.label"));
		checkBoxFriday
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_friday.tip"));
		checkBoxSaturday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_saturday.label"));
		checkBoxSaturday.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_saturday.tip"));
		checkBoxSunday = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_sunday.label"));
		checkBoxSunday
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_of_week_sunday.tip"));

		radioButtonDayOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerDayStart.setEnabled(true);
				spinnerDayRepeat.setEnabled(checkBoxDayRepeat.isSelected());
				checkBoxMonday.setEnabled(false);
				checkBoxTuesday.setEnabled(false);
				checkBoxWednesday.setEnabled(false);
				checkBoxThursday.setEnabled(false);
				checkBoxFriday.setEnabled(false);
				checkBoxSaturday.setEnabled(false);
				checkBoxSunday.setEnabled(false);
				checkBoxDayRepeat.setEnabled(true);
			}
		});
		radioButtonDayOnce.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_once.tip"));
		radioButtonDayEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerDayStart.setEnabled(false);
				spinnerDayRepeat.setEnabled(false);
				checkBoxMonday.setEnabled(false);
				checkBoxTuesday.setEnabled(false);
				checkBoxWednesday.setEnabled(false);
				checkBoxThursday.setEnabled(false);
				checkBoxFriday.setEnabled(false);
				checkBoxSaturday.setEnabled(false);
				checkBoxSunday.setEnabled(false);
				checkBoxDayRepeat.setEnabled(false);
			}
		});
		radioButtonDayEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_every.tip"));
		radioButtonDayEvery.doClick();
		checkBoxDayRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerDayRepeat.setEnabled(checkBoxDayRepeat.isSelected());
			}
		});
		checkBoxDayRepeat.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_repeat.tip"));
		radioButtonDayUseDayOfWeek.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerDayStart.setEnabled(false);
				spinnerDayRepeat.setEnabled(false);
				checkBoxMonday.setEnabled(true);
				checkBoxTuesday.setEnabled(true);
				checkBoxWednesday.setEnabled(true);
				checkBoxThursday.setEnabled(true);
				checkBoxFriday.setEnabled(true);
				checkBoxSaturday.setEnabled(true);
				checkBoxSunday.setEnabled(true);
				checkBoxDayRepeat.setEnabled(false);
			}
		});
		radioButtonDayUseDayOfWeek
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_day_day_of_week.tip"));

		JPanel panelDayOfWeek = new JPanel();
		panelDayOfWeek.setLayout(new GridBagLayout());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelDay.add(radioButtonDayEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panelDay.add(radioButtonDayOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelDay.add(spinnerDayStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelDay.add(checkBoxDayRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelDay.add(spinnerDayRepeat, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 7;
		gbc.anchor = GridBagConstraints.WEST;
		panelDay.add(radioButtonDayUseDayOfWeek, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelDayOfWeek.add(checkBoxMonday, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxTuesday, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxWednesday, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxThursday, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxFriday, gbc);

		gbc.gridx = 5;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxSaturday, gbc);

		gbc.gridx = 6;
		gbc.gridy = 0;
		panelDayOfWeek.add(checkBoxSunday, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 7;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		panelDay.add(panelDayOfWeek, gbc);

		// months section
		JPanel panelMonth = new JPanel();
		panelMonth.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_month.label")));
		panelMonth.setLayout(new GridBagLayout());

		spinnerMonthStart = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
		spinnerMonthStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_once_spinner.tip"));
		spinnerMonthRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
		spinnerMonthRepeat.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_repeat_spinner.tip"));

		radioButtonMonthOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_once.label"));
		radioButtonMonthEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_every.label"));
		checkBoxMonthRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_repeat.label"));
		radioButtonMonthUseMonthOfYear = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_month_of_year.label"));
		ButtonGroup monthButtonGroup = new ButtonGroup();
		monthButtonGroup.add(radioButtonMonthOnce);
		monthButtonGroup.add(radioButtonMonthEvery);
		monthButtonGroup.add(radioButtonMonthUseMonthOfYear);

		checkBoxJanuary = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_january.label"));
		checkBoxJanuary.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_january.tip"));
		checkBoxFebruary = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_february.label"));
		checkBoxFebruary.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_february.tip"));
		checkBoxMarch = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_march.label"));
		checkBoxMarch
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_march.tip"));
		checkBoxApril = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_april.label"));
		checkBoxApril
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_april.tip"));
		checkBoxMay = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_may.label"));
		checkBoxMay
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_may.tip"));
		checkBoxJune = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_june.label"));
		checkBoxJune
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_june.tip"));
		checkBoxJuly = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_july.label"));
		checkBoxJuly
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_july.tip"));
		checkBoxAugust = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_august.label"));
		checkBoxAugust.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_august.tip"));
		checkBoxSeptember = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_september.label"));
		checkBoxSeptember.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_september.tip"));
		checkBoxOctober = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_october.label"));
		checkBoxOctober.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_october.tip"));
		checkBoxNovember = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_november.label"));
		checkBoxNovember.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_november.tip"));
		checkBoxDecember = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_december.label"));
		checkBoxDecember.setToolTipText(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_of_year_december.tip"));

		radioButtonMonthOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMonthStart.setEnabled(true);
				spinnerMonthRepeat.setEnabled(checkBoxMonthRepeat.isSelected());
				checkBoxJanuary.setEnabled(false);
				checkBoxFebruary.setEnabled(false);
				checkBoxMarch.setEnabled(false);
				checkBoxApril.setEnabled(false);
				checkBoxMay.setEnabled(false);
				checkBoxJune.setEnabled(false);
				checkBoxJuly.setEnabled(false);
				checkBoxAugust.setEnabled(false);
				checkBoxSeptember.setEnabled(false);
				checkBoxOctober.setEnabled(false);
				checkBoxNovember.setEnabled(false);
				checkBoxDecember.setEnabled(false);
				checkBoxMonthRepeat.setEnabled(true);
			}
		});
		radioButtonMonthOnce
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_once.tip"));
		radioButtonMonthEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMonthStart.setEnabled(false);
				spinnerMonthRepeat.setEnabled(false);
				checkBoxJanuary.setEnabled(false);
				checkBoxFebruary.setEnabled(false);
				checkBoxMarch.setEnabled(false);
				checkBoxApril.setEnabled(false);
				checkBoxMay.setEnabled(false);
				checkBoxJune.setEnabled(false);
				checkBoxJuly.setEnabled(false);
				checkBoxAugust.setEnabled(false);
				checkBoxSeptember.setEnabled(false);
				checkBoxOctober.setEnabled(false);
				checkBoxNovember.setEnabled(false);
				checkBoxDecember.setEnabled(false);
				checkBoxMonthRepeat.setEnabled(false);
			}
		});
		radioButtonMonthEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_every.tip"));
		radioButtonMonthEvery.doClick();
		checkBoxMonthRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMonthRepeat.setEnabled(checkBoxMonthRepeat.isSelected());
			}
		});
		checkBoxMonthRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_repeat.tip"));
		radioButtonMonthUseMonthOfYear.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerMonthStart.setEnabled(false);
				spinnerMonthRepeat.setEnabled(false);
				checkBoxJanuary.setEnabled(true);
				checkBoxFebruary.setEnabled(true);
				checkBoxMarch.setEnabled(true);
				checkBoxApril.setEnabled(true);
				checkBoxMay.setEnabled(true);
				checkBoxJune.setEnabled(true);
				checkBoxJuly.setEnabled(true);
				checkBoxAugust.setEnabled(true);
				checkBoxSeptember.setEnabled(true);
				checkBoxOctober.setEnabled(true);
				checkBoxNovember.setEnabled(true);
				checkBoxDecember.setEnabled(true);
				checkBoxMonthRepeat.setEnabled(false);
			}
		});
		radioButtonMonthUseMonthOfYear
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_month_month_of_year.tip"));

		JPanel panelMonthOfYear = new JPanel();
		panelMonthOfYear.setLayout(new GridBagLayout());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelMonth.add(radioButtonMonthEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		panelMonth.add(radioButtonMonthOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelMonth.add(spinnerMonthStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelMonth.add(checkBoxMonthRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelMonth.add(spinnerMonthRepeat, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 7;
		gbc.anchor = GridBagConstraints.WEST;
		panelMonth.add(radioButtonMonthUseMonthOfYear, gbc);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelMonthOfYear.add(checkBoxJanuary, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;
		panelMonthOfYear.add(checkBoxFebruary, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		panelMonthOfYear.add(checkBoxMarch, gbc);

		gbc.gridx = 3;
		gbc.gridy = 0;
		panelMonthOfYear.add(checkBoxApril, gbc);

		gbc.gridx = 4;
		gbc.gridy = 0;
		panelMonthOfYear.add(checkBoxMay, gbc);

		gbc.gridx = 5;
		gbc.gridy = 0;
		panelMonthOfYear.add(checkBoxJune, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxJuly, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxAugust, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxSeptember, gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxOctober, gbc);

		gbc.gridx = 4;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxNovember, gbc);

		gbc.gridx = 5;
		gbc.gridy = 1;
		panelMonthOfYear.add(checkBoxDecember, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 7;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.BOTH;
		panelMonth.add(panelMonthOfYear, gbc);

		// years section
		JPanel panelYear = new JPanel();
		panelYear.setBorder(BorderFactory
				.createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.panel_year.label")));
		panelYear.setLayout(new GridBagLayout());

		Calendar cal = Calendar.getInstance();
		spinnerYearStart = new JSpinner(
				new SpinnerNumberModel(cal.get(Calendar.YEAR), cal.get(Calendar.YEAR), cal.get(Calendar.YEAR) + 100, 1));
		spinnerYearStart
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_once_spinner.tip"));
		spinnerYearRepeat = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
		spinnerYearRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_repeat_spinner.tip"));

		radioButtonYearOnce = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_once.label"));
		radioButtonYearEvery = new JRadioButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_every.label"));
		checkBoxYearRepeat = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_repeat.label"));
		ButtonGroup yearButtonGroup = new ButtonGroup();
		yearButtonGroup.add(radioButtonYearOnce);
		yearButtonGroup.add(radioButtonYearEvery);

		radioButtonYearOnce.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerYearStart.setEnabled(true);
				spinnerYearRepeat.setEnabled(checkBoxYearRepeat.isSelected());
				checkBoxYearRepeat.setEnabled(true);
			}
		});
		radioButtonYearOnce
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_once.tip"));
		radioButtonYearEvery.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerYearStart.setEnabled(false);
				spinnerYearRepeat.setEnabled(false);
				checkBoxYearRepeat.setEnabled(false);
			}
		});
		radioButtonYearEvery
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_every.tip"));
		radioButtonYearEvery.doClick();
		checkBoxYearRepeat.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				spinnerYearRepeat.setEnabled(checkBoxYearRepeat.isSelected());
			}
		});
		checkBoxYearRepeat
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_repeat.tip"));

		checkBoxYearEnabled = new JCheckBox(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_enabled.label"));
		checkBoxYearEnabled
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.cron_editor.cron_year_enabled.tip"));
		checkBoxYearEnabled.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (checkBoxYearEnabled.isSelected()) {
					radioButtonYearOnce.setEnabled(true);
					radioButtonYearEvery.setEnabled(true);
					checkBoxYearRepeat.setEnabled(true);
					if (radioButtonYearOnce.isSelected()) {
						radioButtonYearOnce.doClick();
					} else if (radioButtonYearEvery.isSelected()) {
						radioButtonYearEvery.doClick();
					}
				} else {
					radioButtonYearOnce.setEnabled(false);
					radioButtonYearEvery.setEnabled(false);
					checkBoxYearRepeat.setEnabled(false);
					spinnerYearStart.setEnabled(false);
					spinnerYearRepeat.setEnabled(false);
				}
			}
		});
		checkBoxYearEnabled.setSelected(true);
		checkBoxYearEnabled.doClick();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 5;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panelYear.add(checkBoxYearEnabled, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.WEST;
		panelYear.add(radioButtonYearEvery, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panelYear.add(radioButtonYearOnce, gbc);

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 1;
		panelYear.add(spinnerYearStart, gbc);

		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.weightx = 0;
		panelYear.add(checkBoxYearRepeat, gbc);

		gbc.gridx = 4;
		gbc.gridy = 1;
		panelYear.add(spinnerYearRepeat, gbc);

		// button and warning label section
		JPanel panelButtons = new JPanel();
		panelButtons.setLayout(new GridBagLayout());

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;

		warningLabel.setVisible(false);
		panelButtons.add(warningLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 0;

		// don't want to dispose of dialog, can be reused with previously entered values
		// so removing standard disposing listener and creating own listener
		Action okAction = new ResourceAction("ok") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				String errorMsg = getErrorMessage();
				if (errorMsg != null) {
					SwingTools.showVerySimpleErrorMessage(CronEditorDialog.this, "cron_editor.invalid_settings", errorMsg);
					return;
				}
				wasConfirmed = true;
				setVisible(false);
			}

		};
		JButton okButton = new JButton(okAction);
		getRootPane().setDefaultButton(okButton);
		panelButtons.add(okButton, gbc);

		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.weightx = 0;
		// don't want to dispose of dialog, can be reused with previously entered values
		// so removing standard disposing listener and creating own listener
		Action cancelAction = new ResourceAction("cancel") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				wasConfirmed = false;
				setVisible(false);
			}
		};
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "CANCEL");
		getRootPane().getActionMap().put("CANCEL", cancelAction);
		JButton cancelButton = new JButton(cancelAction);
		panelButtons.add(cancelButton, gbc);

		// add panels
		JPanel mainPanel = new JPanel(new GridBagLayout());
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 10, 5, 10);
		mainPanel.add(panelSec, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		mainPanel.add(panelMin, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		mainPanel.add(panelHour, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		mainPanel.add(panelDay, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		mainPanel.add(panelMonth, gbc);

		gbc.gridx = 0;
		gbc.gridy = 5;
		mainPanel.add(panelYear, gbc);

		// center dialog
		layoutDefault(mainPanel, panelButtons, ButtonDialog.TALL);
	}

	/**
	 * Returns the cron expression as a {@link String} if {@link #wasConfirmed()} returns
	 * <code>true</code>, otherwise returns an empty {@link String} .
	 *
	 * @return
	 */
	public String getCronExpression() {
		if (wasConfirmed()) {
			StringBuffer cronBuffer = new StringBuffer();

			// seconds
			if (radioButtonSecEvery.isSelected()) {
				cronBuffer.append('*');
			} else {
				cronBuffer.append(spinnerSecStart.getValue());
				if (checkBoxSecRepeat.isSelected()) {
					cronBuffer.append('/');
					cronBuffer.append(spinnerSecRepeat.getValue());
				}
			}

			cronBuffer.append(' ');

			// minutes
			if (radioButtonMinEvery.isSelected()) {
				cronBuffer.append('*');
			} else {
				cronBuffer.append(spinnerMinStart.getValue());
				if (checkBoxMinRepeat.isSelected()) {
					cronBuffer.append('/');
					cronBuffer.append(spinnerMinRepeat.getValue());
				}
			}

			cronBuffer.append(' ');

			// hours
			if (radioButtonHourEvery.isSelected()) {
				cronBuffer.append('*');
			} else {
				cronBuffer.append(spinnerHourStart.getValue());
				if (checkBoxHourRepeat.isSelected()) {
					cronBuffer.append('/');
					cronBuffer.append(spinnerHourRepeat.getValue());
				}
			}

			cronBuffer.append(' ');

			// days
			if (radioButtonDayEvery.isSelected()) {
				cronBuffer.append('*');
			} else if (radioButtonDayUseDayOfWeek.isSelected()) {
				cronBuffer.append('?');
			} else {
				cronBuffer.append(spinnerDayStart.getValue());
				if (checkBoxDayRepeat.isSelected()) {
					cronBuffer.append('/');
					cronBuffer.append(spinnerDayRepeat.getValue());
				}
			}

			cronBuffer.append(' ');

			// months
			if (radioButtonMonthEvery.isSelected()) {
				cronBuffer.append('*');
			} else if (radioButtonMonthUseMonthOfYear.isSelected()) {
				if (checkBoxJanuary.isSelected()) {
					cronBuffer.append("JAN,");
				}
				if (checkBoxFebruary.isSelected()) {
					cronBuffer.append("FEB,");
				}
				if (checkBoxMarch.isSelected()) {
					cronBuffer.append("MAR,");
				}
				if (checkBoxApril.isSelected()) {
					cronBuffer.append("APR,");
				}
				if (checkBoxMay.isSelected()) {
					cronBuffer.append("MAY,");
				}
				if (checkBoxJune.isSelected()) {
					cronBuffer.append("JUN,");
				}
				if (checkBoxJuly.isSelected()) {
					cronBuffer.append("JUL,");
				}
				if (checkBoxAugust.isSelected()) {
					cronBuffer.append("AUG,");
				}
				if (checkBoxSeptember.isSelected()) {
					cronBuffer.append("SEP,");
				}
				if (checkBoxOctober.isSelected()) {
					cronBuffer.append("OCT,");
				}
				if (checkBoxNovember.isSelected()) {
					cronBuffer.append("NOV,");
				}
				if (checkBoxDecember.isSelected()) {
					cronBuffer.append("DEC,");
				}
				// remove last ','
				if (cronBuffer.charAt(cronBuffer.length() - 1) == ',') {
					cronBuffer.deleteCharAt(cronBuffer.length() - 1);
				}
			} else {
				cronBuffer.append(spinnerMonthStart.getValue());
				if (checkBoxMonthRepeat.isSelected()) {
					cronBuffer.append('/');
					cronBuffer.append(spinnerMonthRepeat.getValue());
				}
			}

			cronBuffer.append(' ');

			// day of week
			if (!radioButtonDayUseDayOfWeek.isSelected()) {
				cronBuffer.append('?');
			} else {
				// cron week starts on sunday
				if (checkBoxSunday.isSelected()) {
					cronBuffer.append("SUN,");
				}
				if (checkBoxMonday.isSelected()) {
					cronBuffer.append("MON,");
				}
				if (checkBoxTuesday.isSelected()) {
					cronBuffer.append("TUE,");
				}
				if (checkBoxWednesday.isSelected()) {
					cronBuffer.append("WED,");
				}
				if (checkBoxThursday.isSelected()) {
					cronBuffer.append("THU,");
				}
				if (checkBoxFriday.isSelected()) {
					cronBuffer.append("FRI,");
				}
				if (checkBoxSaturday.isSelected()) {
					cronBuffer.append("SAT,");
				}
				// remove last ','
				if (cronBuffer.charAt(cronBuffer.length() - 1) == ',') {
					cronBuffer.deleteCharAt(cronBuffer.length() - 1);
				}
			}

			// optional: year
			if (checkBoxYearEnabled.isSelected()) {
				cronBuffer.append(' ');

				if (radioButtonYearEvery.isSelected()) {
					cronBuffer.append('*');
				} else {
					cronBuffer.append(spinnerYearStart.getValue());
					if (checkBoxYearRepeat.isSelected()) {
						cronBuffer.append('/');
						cronBuffer.append(spinnerYearRepeat.getValue());
					}
				}
			}

			return cronBuffer.toString();
		} else {
			return "";
		}
	}

	public void setCheckboxes(boolean b) {
		radioButtonSecEvery.setSelected(b);
	}

	public void setSpinnerSecStartValue(String cronExpression) {
		Number nn = null;
		try {
			nn = NumberFormat.getInstance().parse(cronExpression);
		} catch (ParseException e) {
			// TODO
		}
		spinnerSecStart.setValue(nn);
		spinnerSecStart.setEnabled(true);
	}

	public void expressionparser(String[] Array) {
		if (Array[0] != "*") {
			spinnerSecStart.setEnabled(true);
		}
	}

	/**
	 * Shows the cron editor dialog.
	 */
	public void prompt() {
		setVisible(true);
	}

	/**
	 * Checks the dialog for errors and in case there is one returns the error message as
	 * {@link String}. If there is no error, returns <code>null</code>.
	 *
	 * @return
	 */
	private String getErrorMessage() {
		// if day of week is used, make sure at least one day is selected
		if (radioButtonDayUseDayOfWeek.isSelected()) {
			if (!checkBoxMonday.isSelected() && !checkBoxTuesday.isSelected() && !checkBoxWednesday.isSelected()
					&& !checkBoxThursday.isSelected() && !checkBoxFriday.isSelected() && !checkBoxSaturday.isSelected()
					&& !checkBoxSunday.isSelected()) {
				return I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.cron_editor.cron_error.day_of_week_not_selected.label");
			}
		}

		// if month of year is used, make sure at least one month is selected
		if (radioButtonMonthUseMonthOfYear.isSelected()) {
			if (!checkBoxJanuary.isSelected() && !checkBoxFebruary.isSelected() && !checkBoxMarch.isSelected()
					&& !checkBoxApril.isSelected() && !checkBoxMay.isSelected() && !checkBoxJune.isSelected()
					&& !checkBoxJuly.isSelected() && !checkBoxAugust.isSelected() && !checkBoxSeptember.isSelected()
					&& !checkBoxOctober.isSelected() && !checkBoxNovember.isSelected() && !checkBoxDecember.isSelected()) {
				return I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.cron_editor.cron_error.month_of_year_not_selected.label");
			}
		}

		return null;
	}

	// Parsing cronexpression
	public void setSpinnerCronExpressionValues(String cronExpression) {
		String[] numbers = cronExpression.split(" ");
		if (!cronExpression.equals("")) {
			for (int i = 0; i < numbers.length; i++) {
				String currentCronExpresssion = numbers[i];
				if (i == 0) {
					setSpinnerSecStart(currentCronExpresssion);
				} else if (i == 1) {
					setSpinnerMinStart(currentCronExpresssion);
				} else if (i == 2) {
					setSpinnerHourStart(currentCronExpresssion);
				} else if (i == 3) {
					setSpinnerDayStart(currentCronExpresssion);
				} else if (i == 4) {
					setSpinnerMonthStart(currentCronExpresssion);
				} else if (i == 5) {
					setSpinnerConcreteDay(currentCronExpresssion);
				} else if (i == 6) {
					setSpinnerYearStart(currentCronExpresssion);
				}
			}
		}
	}

	// Parsing to second dialog buttons
	private void setSpinnerSecStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonSecOnce.setEnabled(true);
			radioButtonSecEvery.setSelected(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String second = repeater[0];
			String rsecond = repeater[1];
			spinnerSecStart.setValue(calculateNumberForCronExpression(second));
			spinnerSecRepeat.setValue(calculateNumberForCronExpression(rsecond));

			radioButtonSecOnce.setSelected(true);
			radioButtonSecEvery.setEnabled(true);
			spinnerSecStart.setEnabled(true);
			checkBoxSecRepeat.setSelected(true);
			spinnerSecRepeat.setEnabled(true);
			checkBoxSecRepeat.setEnabled(true);
		} else if (!currentCronExpresssion.toString().contains("*")) {
			spinnerSecStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonSecOnce.setSelected(true);
			radioButtonSecEvery.setEnabled(true);
			spinnerSecStart.setEnabled(true);
			checkBoxSecRepeat.setEnabled(true);
		}

	}

	// Parsing to minute dialog buttons
	private void setSpinnerMinStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonMinOnce.setEnabled(true);
			radioButtonMinEvery.setSelected(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String minute = repeater[0];
			String rminute = repeater[1];

			spinnerMinStart.setValue(calculateNumberForCronExpression(minute));
			spinnerMinRepeat.setValue(calculateNumberForCronExpression(rminute));

			radioButtonMinOnce.setSelected(true);
			radioButtonMinEvery.setEnabled(true);
			spinnerMinStart.setEnabled(true);
			checkBoxMinRepeat.setSelected(true);
			spinnerMinRepeat.setEnabled(true);
			checkBoxMinRepeat.setEnabled(true);
		} else if (!currentCronExpresssion.toString().contains("*")) {
			spinnerMinStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonMinOnce.setSelected(true);
			radioButtonMinEvery.setEnabled(true);
			spinnerMinStart.setEnabled(true);
			checkBoxMinRepeat.setEnabled(true);
		}
	}

	// Parsing to hour dialog buttons
	private void setSpinnerHourStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonHourOnce.setEnabled(true);
			radioButtonHourEvery.setSelected(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String hour = repeater[0];
			String rhour = repeater[1];
			spinnerHourStart.setValue(calculateNumberForCronExpression(hour));
			spinnerHourRepeat.setValue(calculateNumberForCronExpression(rhour));

			radioButtonHourOnce.setSelected(true);
			radioButtonHourEvery.setEnabled(true);
			spinnerHourStart.setEnabled(true);
			checkBoxHourRepeat.setSelected(true);
			spinnerHourRepeat.setEnabled(true);
			checkBoxHourRepeat.setEnabled(true);
		} else if (!currentCronExpresssion.toString().contains("*")) {
			spinnerHourStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonHourOnce.setSelected(true);
			radioButtonHourEvery.setEnabled(true);
			spinnerHourStart.setEnabled(true);
			checkBoxHourRepeat.setEnabled(true);
		}

	}

	// Parsing to day dialog buttons
	private void setSpinnerDayStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonDayEvery.setSelected(true);
			radioButtonDayEvery.setEnabled(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String day = repeater[0];
			String rday = repeater[1];
			spinnerDayStart.setValue(calculateNumberForCronExpression(day));
			spinnerDayRepeat.setValue(calculateNumberForCronExpression(rday));

			radioButtonDayEvery.setEnabled(true);
			spinnerDayStart.setEnabled(true);
			spinnerDayRepeat.setEnabled(true);
			checkBoxDayRepeat.setEnabled(true);
			checkBoxDayRepeat.setSelected(true);
			radioButtonDayOnce.setSelected(true);
		} else if (!currentCronExpresssion.toString().contains("/")) {
			spinnerDayStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonDayEvery.setEnabled(true);
			spinnerDayStart.setEnabled(true);
			radioButtonDayOnce.setSelected(true);
			checkBoxDayRepeat.setEnabled(true);
		} else if (!currentCronExpresssion.toString().contains("?")) {
			radioButtonDayEvery.setSelected(true);
			radioButtonDayEvery.setEnabled(true);
		}

	}

	// Parsing to month dialog buttons
	private void setSpinnerMonthStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonMonthEvery.setSelected(true);
			radioButtonMonthEvery.setEnabled(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String month = repeater[0];
			String rmonth = repeater[1];
			spinnerMonthStart.setValue(calculateNumberForCronExpression(month));
			spinnerMonthRepeat.setValue(calculateNumberForCronExpression(rmonth));

			radioButtonMonthOnce.setSelected(true);
			radioButtonMonthEvery.setEnabled(true);
			spinnerMonthStart.setEnabled(true);
			checkBoxMonthRepeat.setSelected(true);
			spinnerMonthRepeat.setEnabled(true);
			checkBoxMonthRepeat.setEnabled(true);
		} else if (currentCronExpresssion.toString().contains("J") || currentCronExpresssion.toString().contains("F")
				|| currentCronExpresssion.toString().contains("M") || currentCronExpresssion.toString().contains("A")
				|| currentCronExpresssion.toString().contains("S") || currentCronExpresssion.toString().contains("O")
				|| currentCronExpresssion.toString().contains("N") || currentCronExpresssion.toString().contains("D")) {
			calculateMonthForCronExpression(currentCronExpresssion);
			radioButtonMonthEvery.setEnabled(true);
			radioButtonMonthUseMonthOfYear.setSelected(true);
			radioButtonMonthUseMonthOfYear.setEnabled(true);
			checkBoxJanuary.setEnabled(true);
			checkBoxFebruary.setEnabled(true);
			checkBoxMarch.setEnabled(true);
			checkBoxApril.setEnabled(true);
			checkBoxMay.setEnabled(true);
			checkBoxJune.setEnabled(true);
			checkBoxJuly.setEnabled(true);
			checkBoxAugust.setEnabled(true);
			checkBoxSeptember.setEnabled(true);
			checkBoxOctober.setEnabled(true);
			checkBoxNovember.setEnabled(true);
			checkBoxDecember.setEnabled(true);

		} else if (!currentCronExpresssion.toString().contains("/")) {
			spinnerMonthStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonMonthEvery.setEnabled(true);
			// radioButtonMonthEvery.setSelected(true);
			spinnerMonthStart.setEnabled(true);
			radioButtonMonthOnce.setSelected(true);
			checkBoxMonthRepeat.setEnabled(true);
		}

	}

	// Parsing day checkbox dialog buttons
	private void setSpinnerConcreteDay(String currentCronExpresssion) {
		if (!currentCronExpresssion.toString().contains("?")) {
			calculateDayForCronExpression(currentCronExpresssion);
			radioButtonDayUseDayOfWeek.setEnabled(true);
			radioButtonDayUseDayOfWeek.setSelected(true);
			checkBoxMonday.setEnabled(true);
			checkBoxTuesday.setEnabled(true);
			checkBoxWednesday.setEnabled(true);
			checkBoxThursday.setEnabled(true);
			checkBoxFriday.setEnabled(true);
			checkBoxSaturday.setEnabled(true);
			checkBoxSunday.setEnabled(true);
			radioButtonDayEvery.setEnabled(true);
			radioButtonDayEvery.setSelected(false);
			spinnerDayStart.setEnabled(false);
			checkBoxDayRepeat.setEnabled(false);
		}
	}

	// Parsing to year dialog buttons
	private void setSpinnerYearStart(String currentCronExpresssion) {
		if (currentCronExpresssion.toString().contains("*")) {
			radioButtonYearOnce.setEnabled(true);
			radioButtonYearEvery.setEnabled(true);
			radioButtonYearEvery.setSelected(true);
			checkBoxYearEnabled.setEnabled(true);
			checkBoxYearEnabled.setSelected(true);
		} else if (currentCronExpresssion.toString().contains("/")) {
			String[] repeater = currentCronExpresssion.split("/");
			String year = repeater[0];
			String ryear = repeater[1];
			spinnerYearStart.setValue(calculateNumberForCronExpression(year));
			spinnerYearRepeat.setValue(calculateNumberForCronExpression(ryear));

			radioButtonYearOnce.setSelected(true);
			radioButtonYearOnce.setEnabled(true);
			radioButtonYearEvery.setEnabled(true);
			spinnerYearStart.setEnabled(true);
			checkBoxYearRepeat.setSelected(true);
			spinnerYearRepeat.setEnabled(true);
			checkBoxYearRepeat.setEnabled(true);
			checkBoxYearEnabled.setEnabled(true);
			checkBoxYearEnabled.setSelected(true);
		} else if (!currentCronExpresssion.toString().contains("*")) {
			spinnerYearStart.setValue(calculateNumberForCronExpression(currentCronExpresssion));
			radioButtonYearOnce.setSelected(true);
			radioButtonYearOnce.setEnabled(true);
			radioButtonYearEvery.setEnabled(true);
			checkBoxYearEnabled.setEnabled(true);
			checkBoxYearEnabled.setSelected(true);
			spinnerYearStart.setEnabled(true);
			checkBoxYearRepeat.setEnabled(true);
		}

	}

	// Parsing String to number for dialog buttonfields
	private Number calculateNumberForCronExpression(String currentCronExpresssion) {
		Number nn = null;
		try {
			nn = NumberFormat.getInstance().parse(currentCronExpresssion);
		} catch (ParseException e) {
			// TODO Should not occure except someone changes the cronexpression in RM to a wrong
			// expression
			return 1;
		}
		return nn;
	}

	// Parsing months for dialog checkboxes
	private void calculateMonthForCronExpression(String currentCronExpresssion) {
		String[] monthexpression = currentCronExpresssion.split(",");
		for (int i = 0; i < monthexpression.length; i++) {
			String month = monthexpression[i];
			if (month.equals("JAN")) {
				checkBoxJanuary.setSelected(true);
			} else if (month.equals("FEB")) {
				checkBoxFebruary.setSelected(true);
			} else if (month.equals("MAR")) {
				checkBoxMarch.setSelected(true);
			} else if (month.equals("APR")) {
				checkBoxApril.setSelected(true);
			} else if (month.equals("MAY")) {
				checkBoxMay.setSelected(true);
			} else if (month.equals("JUN")) {
				checkBoxJune.setSelected(true);
			} else if (month.equals("JUL")) {
				checkBoxJuly.setSelected(true);
			} else if (month.equals("AUG")) {
				checkBoxAugust.setSelected(true);
			} else if (month.equals("SEP")) {
				checkBoxSeptember.setSelected(true);
			} else if (month.equals("OCT")) {
				checkBoxOctober.setSelected(true);
			} else if (month.equals("NOV")) {
				checkBoxNovember.setSelected(true);
			} else if (month.equals("DEC")) {
				checkBoxDecember.setSelected(true);
			}
		}
	}

	// Parsing days for dialog checkboxes
	private void calculateDayForCronExpression(String currentCronExpresssion) {
		String[] dayexpression = currentCronExpresssion.split(",");
		for (int i = 0; i < dayexpression.length; i++) {
			String day = dayexpression[i];
			if (day.equals("MON")) {
				checkBoxMonday.setSelected(true);
			} else if (day.equals("TUE")) {
				checkBoxTuesday.setSelected(true);
			} else if (day.equals("WED")) {
				checkBoxWednesday.setSelected(true);
			} else if (day.equals("THU")) {
				checkBoxThursday.setSelected(true);
			} else if (day.equals("FRI")) {
				checkBoxFriday.setSelected(true);
			} else if (day.equals("SAT")) {
				checkBoxSaturday.setSelected(true);
			} else if (day.equals("SUN")) {
				checkBoxSunday.setSelected(true);
			}
		}
	}
}

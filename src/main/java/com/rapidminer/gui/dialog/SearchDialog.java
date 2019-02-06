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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.ResourceActionAdapter;
import com.rapidminer.gui.tools.ResourceLabel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.gui.tools.dialogs.ConfirmDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Tools;


/**
 * The search dialog for searching strings in a {@link SearchableTextComponent}.
 *
 * @author Ingo Mierswa, Simon Fischer, Tobias Malbrecht
 */
public class SearchDialog extends ButtonDialog {

	private static final long serialVersionUID = -1019890951712706875L;

	private static class Result {

		private final int start, end;

		private Result(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return "start: " + start + ", end: " + end;
		}
	}

	private final JTextField patternField = new JTextField(20);

	private final JTextField replaceField = new JTextField(20);

	private final JCheckBox caseSensitive = new JCheckBox(new ResourceActionAdapter("case_sensitive"));

	private final JCheckBox regExp = new JCheckBox(new ResourceActionAdapter("search_regular_expression"));

	private final JRadioButton forwardRadioButton = new JRadioButton(new ResourceActionAdapter("search_forward"));

	private final JRadioButton backwardRadioButton = new JRadioButton(new ResourceActionAdapter("search_backward"));

	private transient final SearchableTextComponent textComponent;

	public SearchDialog(Component owner, SearchableTextComponent textComponent) {
		this(owner, textComponent, false);
	}

	public SearchDialog(Component owner, SearchableTextComponent textComponent, boolean allowReplace) {
		super(owner != null ? SwingUtilities.getWindowAncestor(owner) : null, allowReplace ? "search_replace" : "search",
				ModalityType.MODELESS, new Object[] {});

		this.textComponent = textComponent;
		this.textComponent.requestFocus();
		this.textComponent.setCaretPosition(0);

		if (allowReplace) {
			setTitle(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".search_replace.title"));
		} else {
			setTitle(I18N.getMessage(I18N.getGUIBundle(), getKey() + ".search.title"));
		}

		JPanel searchPanel = new JPanel(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(4, 0, 4, 0);
		c.gridwidth = 1;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		JLabel label = new ResourceLabel("search_what");
		searchPanel.add(label, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 1;
		c.gridy = 0;
		searchPanel.add(patternField, c);
		label.setLabelFor(patternField);

		Collection<AbstractButton> buttons = new LinkedList<>();
		final JButton search = new JButton(new ResourceAction("start_search") {

			private static final long serialVersionUID = -7450018802479146549L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				search();
			}
		});
		buttons.add(search);

		if (allowReplace) {
			c.insets = new Insets(4, 0, 4, 0);
			c.gridwidth = 1;
			c.gridx = 0;
			c.gridy = 1;
			label = new ResourceLabel("replace_with");
			searchPanel.add(label, c);

			c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridx = 1;
			c.gridy = 1;
			searchPanel.add(replaceField, c);

			final JButton replaceButton = new JButton(new ResourceAction("start_replace") {

				private static final long serialVersionUID = -5028551435610677265L;

				@Override
				public void loggedActionPerformed(ActionEvent e) {
					replace();
					search();
				}
			});
			buttons.add(replaceButton);
		}

		GridLayout settingsPanelLayout = new GridLayout(1, 2);
		settingsPanelLayout.setVgap(0);
		settingsPanelLayout.setHgap(0);
		JPanel settingsPanel = new JPanel(settingsPanelLayout);
		GridLayout directionPanelLayout = new GridLayout(2, 1);
		directionPanelLayout.setVgap(0);
		directionPanelLayout.setHgap(0);
		JPanel directionPanel = new JPanel(directionPanelLayout);
		directionPanel.setBorder(BorderFactory.createTitledBorder("Direction"));
		ButtonGroup directionGroup = new ButtonGroup();
		directionGroup.add(forwardRadioButton);
		directionGroup.add(backwardRadioButton);
		forwardRadioButton.setSelected(true);
		directionPanel.add(forwardRadioButton);
		directionPanel.add(backwardRadioButton);
		settingsPanel.add(directionPanel);

		JPanel optionsPanel = new JPanel(new GridLayout(2, 1));
		optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
		optionsPanel.add(caseSensitive);
		optionsPanel.add(regExp);
		settingsPanel.add(optionsPanel);
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridx = 0;
		c.gridy = 2;
		searchPanel.add(settingsPanel, c);

		buttons.add(makeCloseButton());
		layoutDefault(searchPanel, buttons);
		getRootPane().setDefaultButton(search);
	}

	private void search() {
		String pattern = patternField.getText().trim();
		if (pattern.length() == 0) {
			return;
		}
		int startPos = textComponent.getCaretPosition();
		String text = textComponent.getText();
		if (startPos > text.length()) {
			startPos = 0;
		}

		if (forwardRadioButton.isSelected()) {
			Result result = search(startPos, pattern, text, textComponent.canHandleCarriageReturn());
			if (result == null) {
				noMoreHits();
				return;
			} else {
				textComponent.select(result.start, result.end);
			}
		} else {
			Result lastResult = null;
			int pos = 0;
			while (true) {
				Result result = search(pos, pattern, text, textComponent.canHandleCarriageReturn());
				if (result != null) {
					if (result.end < startPos) {
						pos = result.start + 1;
						lastResult = result;
					} else {
						break;
					}
				} else {
					break;
				}
			}

			if (lastResult == null) {
				noMoreHits();
			} else {
				textComponent.select(lastResult.start, lastResult.end);
			}
		}
	}

	private void replace() {
		textComponent.replaceSelection(replaceField.getText());
	}

	private Result search(int start, String pattern, String text, boolean canHandleCarriageReturn) {
		if (!canHandleCarriageReturn) {
			text = Tools.transformAllLineSeparators(text);
		}
		if (regExp.isSelected()) {
			Matcher matcher = Pattern.compile(pattern, caseSensitive.isSelected() ? 0 : Pattern.CASE_INSENSITIVE)
					.matcher(text.subSequence(start, text.length()));
			if (matcher.find()) {
				return new Result(start + matcher.start(), start + matcher.end());
			} else {
				return null;
			}
		} else {
			if (!caseSensitive.isSelected()) {
				text = text.toLowerCase();
				pattern = pattern.toLowerCase();
			}
			int result = text.indexOf(pattern, start);
			if (result == -1) {
				return null;
			} else {
				return new Result(result, result + pattern.length());
			}
		}
	}

	private void noMoreHits() {
		String restartAt = backwardRadioButton.isSelected() ? "end" : "beginning";
		switch (SwingTools.showConfirmDialog(SearchDialog.this, "editor.search_replace.no_more_hits",
				ConfirmDialog.YES_NO_OPTION, restartAt)) {
			case ConfirmDialog.YES_OPTION:
				textComponent.setCaretPosition(
						backwardRadioButton.isSelected() ? textComponent.getText().replaceAll("\r", "").length() : 0);
				search();
				break;
			case ConfirmDialog.NO_OPTION:
			default:
				return;
		}
	}
}

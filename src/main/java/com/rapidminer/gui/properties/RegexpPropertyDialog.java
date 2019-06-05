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
package com.rapidminer.gui.properties;

import static com.rapidminer.gui.search.GlobalSearchGUIUtilities.HTML_TAG_CLOSE;
import static com.rapidminer.gui.search.GlobalSearchGUIUtilities.HTML_TAG_OPEN;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.commons.lang.ArrayUtils;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.gui.tools.components.PlainArrowDropDownButton;
import com.rapidminer.gui.tools.dialogs.ButtonDialog;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;

import groovy.swing.impl.DefaultAction;


/**
 * A dialog to create and edit regular expressions. Can be created with a given predefined regular
 * expression (normally a previously set value). A collection of item strings can be given to the
 * dialog which are then available as shortcuts. Additionally, a list shows which of these items
 * match the regular expression. If the item collection is null, both lists will not be visible.
 *
 * The dialog shows an inline preview displaying where the given pattern matches. It also shows a
 * list of matches, together with their matching groups.
 *
 * @author Tobias Malbrecht, Dominik Halfkann, Simon Fischer
 */
public class RegexpPropertyDialog extends ButtonDialog {

	private static final long serialVersionUID = 5396725165122306231L;

	private RegexpSearchStyledDocument inlineSearchDocument = null;
	private RegexpReplaceStyledDocument inlineReplaceDocument = null;

	private JTabbedPane testExp = null;

	private DefaultListModel<RegExpResult> resultsListModel = new DefaultListModel<>();

	private static String[][] regexpConstructs = {
			{ ".", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.any_character") },
			{ "[]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.bracket_expression") },
			{ "[^]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.not_bracket_expression") },
			{ "()", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.capturing_group") },
			{ "?", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.zero_one_quantifier") },
			{ "*", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.zero_more_quantifier") },
			{ "+", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.one_more_quantifier") },
			{ "{n}", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.exact_quantifier") },
			{ "{min,}", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.min_quantifier") },
			{ "{min,max}",
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.min_max_quantifier") },
			{ "|", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.constructs.disjunction") }, };

	// adjust the caret by this amount upon insertion
	private static int[] regexpConstructInsertionCaretAdjustment = { 0, -1, -1, -1, 0, 0, 0, -1, -2, -1, -5, 0, };

	// select these construct characters upon insertion
	private static int[][] regexpConstructInsertionSelectionIndices = { { 1, 1 }, { 1, 1 }, { 2, 2 }, { 1, 1 }, { 1, 1 },
			{ 1, 1 }, { 1, 1 }, { 1, 2 }, { 1, 4 }, { 1, 8 }, { 1, 1 }, };

	// enclose selected by construct
	private static boolean[] regexpConstructInsertionEncloseSelected = { false, true, true, true, false, false, false, false,
			false, false, false, };

	private static String[][] regexpShortcuts = {
			{ ".*", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.arbitrary") },
			{ "[a-zA-Z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.letter") },
			{ "[a-z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.lowercase_letter") },
			{ "[A-Z]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.uppercase_letter") },
			{ "[0-9]", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.digit") },
			{ "\\w", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.word") },
			{ "\\W", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.non_word") },
			{ "\\s", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.whitespace") },
			{ "\\S", I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.non_whitespace") },
			{ "[-!\"#$%&'()*+,./:;<=>?@\\[\\\\\\]_`{|}~]",
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.shortcuts.punctuation") }, };

	private static final String ERROR_MESSAGE = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.parameter.regexp.error.label");

	private static final Icon ERROR_ICON = SwingTools
			.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.error.icon"));

	private static final String NO_ERROR_MESSAGE = I18N.getMessage(I18N.getGUIBundle(),
			"gui.dialog.parameter.regexp.no_error.label");

	private static final Icon NO_ERROR_ICON = SwingTools
			.createIcon("16/" + I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.no_error.icon"));

	private String infoText;

	private final JTextField regexpTextField;

	private final JTextField replacementTextField;

	private JList<String> itemShortcutsList;

	private DefaultListModel<String> matchedItemsListModel;

	private final Collection<String> items;

	private boolean supportsItems = false;

	private final JLabel errorMessage;

	private JButton okButton;

	private JCheckBox cbCaseInsensitive;
	private JCheckBox cbMultiline;
	private JCheckBox cbDotall;
	private JCheckBox cbUnicodeCase;

	/** Class representing a single regexp search result. **/
	private class RegExpResult {

		private String match;
		private String[] groups;
		private int number;
		private boolean empty = false;

		public RegExpResult(String match, String[] groups, int number) {
			this.match = match;
			this.groups = groups;
			this.number = number;
		}

		public RegExpResult() {
			// empty result
			empty = true;
		}

		@Override
		public String toString() {
			StringBuilder output = new StringBuilder();
			if (!empty) {
				output.append(HTML_TAG_OPEN + "<span style=\"font-size:11px;margin:2px 0 2px 4px;\">").append(
						// "Match "+number+": <b>'"+match+"'</b>" +
						I18N.getMessage(I18N.getGUIBundle(),
								"gui.dialog.parameter.regexp.regular_expression.result_list.match", number,
								"<b>'" + Tools.escapeHTML(match) + "'</b>")
				).append("</span>");
				if (groups.length > 0) {
					output.append("<ol style=\"margin:1px 0 0 24px\">");
					for (String group : groups) {
						// output += "<li>Group matches: <b>'" + groups[i] +"'</b></li>";
						output.append("<li>").append(I18N.getMessage(I18N.getGUIBundle(),
								"gui.dialog.parameter.regexp.regular_expression.result_list.group_match",
								"<b>'" + Tools.escapeHTML(group) + "'</b>")).append("</li>");

					}
					output.append("</ul>");
				}
				output.append(HTML_TAG_CLOSE);
			} else {
				output.append(HTML_TAG_OPEN + "<span style=\"font-size:11px;margin:2px 0 2px 4px;\">").append(I18N.getMessage(
						I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.empty")).append("</span>");
				output.append(HTML_TAG_CLOSE);
			}
			return output.toString();
		}
	}

	/** A StyledDocument providing a live regexp search **/
	private class RegexpSearchStyledDocument extends DefaultStyledDocument {

		private static final long serialVersionUID = 1L;

		private Matcher matcher = Pattern.compile("").matcher("");

		private Style keyStyle;
		private Style rootStyle;

		{
			rootStyle = addStyle("root", null);

			keyStyle = addStyle("key", rootStyle);
			StyleConstants.setBackground(keyStyle, Color.YELLOW);
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			super.insertString(offs, str, a);
			checkDocument();
		}

		@Override
		public void remove(int offs, int len) throws BadLocationException {
			super.remove(offs, len);
			checkDocument();
		}

		private void checkDocument() {

			setCharacterAttributes(0, getLength(), rootStyle, true);
			try {
				matcher.reset(getText(0, getLength()));
				int count = 0;
				resultsListModel.clear();
				while (matcher.find()) {
					if (matcher.end() <= matcher.start()) {
						continue;
					}
					setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keyStyle, true);

					String[] groups = new String[matcher.groupCount()];
					for (int i = 1; i <= matcher.groupCount(); i++) {
						groups[i - 1] = matcher.group(i);
					}
					resultsListModel.addElement(new RegExpResult(
							this.getText(matcher.start(), matcher.end() - matcher.start()), groups, count + 1));
					count++;
				}

				if (count == 0) {
					// add empty element
					resultsListModel.addElement(new RegExpResult());
				}

				testExp.setTitleAt(1, I18N.getMessage(I18N.getGUIBundle(),
						"gui.dialog.parameter.regexp.regular_expression.result_list.title") + " (" + count + ")");
				String replacedText;
				try {
					replacedText = matcher.replaceAll(replacementTextField.getText());
				} catch (Exception e) {
					replacedText = "";
				}
				inlineReplaceDocument.setText(replacedText);
				updateRegexpOptions();
			} catch (BadLocationException ex) {
				LogService.getRoot().log(Level.WARNING, RegexpPropertyDialog.class.getName() + ".bad_location", ex);
			}
		}

		public void updatePattern(String pattern) {
			this.matcher = Pattern.compile(pattern).matcher("");
			checkDocument();
		}

		public void clearResults() {
			resultsListModel.clear();
			resultsListModel.addElement(new RegExpResult());
			testExp.setTitleAt(1,
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title")
							+ " (0)");
			setCharacterAttributes(0, getLength(), rootStyle, true);
		}

	}

	/** A StyledDocument with an added setText() method for interting the replaced text **/
	private class RegexpReplaceStyledDocument extends DefaultStyledDocument {

		private static final long serialVersionUID = 1L;

		public RegexpReplaceStyledDocument() {
			super();
		}

		public void setText(String text) {
			try {
				remove(0, getLength());
				insertString(0, text, null);
			} catch (BadLocationException e) {
				LogService.getRoot().log(Level.WARNING, RegexpPropertyDialog.class.getName() + ".bad_location", e);
			}
		}
	}

	/** Same as {@link #RegexpPropertyDialog(Collection, String, String, String) RegexpPropertyDialog(items, predefinedRegexp, description, null)} */
	public RegexpPropertyDialog(final Collection<String> items, String predefinedRegexp, String description) {
		this(items, predefinedRegexp, description, null);
	}

	/**
	 * Creates a new {@link RegexpPropertyDialog} with the given items, predefined regexp and description,
	 * as well as an optional key for a replacement parameter. If the replacement key is not {@code null},
	 * the dialog indicates which parameter is associated with the replacement, and the actual replacement expression
	 * can be extracted using {@link #getReplacement()}. If no replacement key is provided, the dialog will indicate
	 * that te replacement is only used as a preview.
	 *
	 * @param items
	 * 		list of predefined regexps that can be selected from a dropdown
	 * @param predefinedRegexp
	 * 		the initial regex
	 * @param description
	 * 		the description of the associated parameter
	 * @param replacementKey
	 * 		the key of a parameter that takes care of the replacement; can be {@code null}
	 * @since 9.3
	 */
	public RegexpPropertyDialog(final Collection<String> items, String predefinedRegexp, String description, String replacementKey) {
		super(ApplicationFrame.getApplicationFrame(), "parameter.regexp", ModalityType.APPLICATION_MODAL, new Object[0]);
		this.items = items;
		this.supportsItems = items != null;
		this.infoText = HTML_TAG_OPEN + I18N.getMessage(I18N.getGUIBundle(), getKey() + ".title") + ": <br/>"
				+ description + HTML_TAG_CLOSE;
		Dimension size = new Dimension(420, 500);
		this.setMinimumSize(size);
		this.setPreferredSize(size);

		JPanel panel = new JPanel(createGridLayout(1, supportsItems ? 2 : 1));

		// create regexp text field
		regexpTextField = new JTextField(predefinedRegexp);
		regexpTextField
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.tip"));
		regexpTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				fireRegularExpressionUpdated();
			}

		});
		regexpTextField.requestFocus();

		// create replacement text field
		replacementTextField = new JTextField();
		replacementTextField
				.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.replacement.tip"));
		replacementTextField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				fireRegularExpressionUpdated();
			}

		});

		// create inline search documents
		inlineSearchDocument = new RegexpSearchStyledDocument();
		inlineReplaceDocument = new RegexpReplaceStyledDocument();

		// create search results list
		DefaultListCellRenderer resultCellRenderer = new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setBackground(list.getBackground());
				setForeground(list.getForeground());
				setBorder(getNoFocusBorder());
				return this;
			}

			private Border getNoFocusBorder() {
				return BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray);
			}
		};

		JList<RegExpResult> regexpFindingsList = new JList<>(resultsListModel);
		regexpFindingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		regexpFindingsList.setLayoutOrientation(JList.VERTICAL);
		regexpFindingsList.setCellRenderer(resultCellRenderer);

		// regexp panel on left side of dialog
		JPanel regexpPanel = new JPanel(new GridBagLayout());
		regexpPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.border")));
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(4, 4, 4, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		regexpPanel.add(regexpTextField, c);

		// make shortcut button
		final Action nullAction = new DefaultAction();
		PlainArrowDropDownButton autoWireDropDownButton = PlainArrowDropDownButton.makeDropDownButton(nullAction);

		for (String[] popupItem : (String[][]) ArrayUtils.addAll(regexpConstructs, regexpShortcuts)) {
			String shortcut = popupItem[0].length() > 14 ? popupItem[0].substring(0, 14) + "..." : popupItem[0];
			autoWireDropDownButton
					.add(new InsertionAction("<html><table border=0 cellpadding=0 cellspacing=0><tr><td width=100>"
							+ shortcut + "</td><td>" + popupItem[1] + "</td></tr></table></html>", popupItem[0]));
		}
		c.insets = new Insets(4, 0, 4, 0);
		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		regexpPanel.add(autoWireDropDownButton.getDropDownArrowButton(), c);

		// make delete button
		c.insets = new Insets(4, 0, 4, 4);
		c.gridx = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JButton clearRegexpTextFieldButton = new JButton(SwingTools.createIcon("16/delete.png"));
		clearRegexpTextFieldButton.addActionListener(e -> {
			regexpTextField.setText("");
			fireRegularExpressionUpdated();
			regexpTextField.requestFocusInWindow();
		});

		regexpPanel.add(clearRegexpTextFieldButton, c);

		errorMessage = new JLabel(NO_ERROR_MESSAGE, NO_ERROR_ICON, SwingConstants.LEFT);
		errorMessage.setFocusable(false);
		c.insets = new Insets(4, 8, 4, 4);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = GridBagConstraints.REMAINDER;
		regexpPanel.add(errorMessage, c);

		// create replacement panel
		JPanel replacementPanel = new JPanel(new GridBagLayout());
		String replacementBorderKey;
		if (replacementKey == null) {
			replacementBorderKey = "gui.dialog.parameter.regexp.replacement.border";
		} else {
			replacementBorderKey = "gui.dialog.parameter.regexp.replacement_non_preview.border";
			replacementKey = replacementKey.replace('_', ' ');
		}
		replacementPanel.setBorder(createTitledBorder(I18N.getMessage(I18N.getGUIBundle(), replacementBorderKey, replacementKey)));

		JPanel testerPanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		replacementPanel.add(replacementTextField, c);

		// create inline search panel
		JPanel inlineSearchPanel = new JPanel(new GridBagLayout());

		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		inlineSearchPanel.add(
				new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.inline_search.search")), c);

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		inlineSearchPanel.add(new JScrollPane(new JTextPane(inlineSearchDocument)), c);

		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		inlineSearchPanel.add(
				new JLabel(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.inline_search.replaced")), c);

		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		JTextPane replaceTextPane = new JTextPane(inlineReplaceDocument);
		replaceTextPane.setEditable(false);
		JScrollPane scrollpane = new JScrollPane(replaceTextPane);
		scrollpane.setBorder(null);
		inlineSearchPanel.add(new JScrollPane(replaceTextPane), c);

		// create regexp options panel
		ItemListener defaultOptionListener = e -> fireRegexpOptionsChanged();

		cbCaseInsensitive = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.case_insensitive"));
		cbCaseInsensitive.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.case_insensitive.tip"));
		cbCaseInsensitive.addItemListener(defaultOptionListener);

		cbMultiline = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.multiline_mode"));
		cbMultiline.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.multiline_mode.tip"));
		cbMultiline.addItemListener(defaultOptionListener);

		cbDotall = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.dotall_mode"));
		cbDotall.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.dotall_mode.tip"));
		cbDotall.addItemListener(defaultOptionListener);

		cbUnicodeCase = new JCheckBox(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.unicode_case"));
		cbUnicodeCase.setToolTipText(I18N.getMessage(I18N.getGUIBundle(),
				"gui.dialog.parameter.regexp.regular_expression.regexp_options.unicode_case.tip"));
		cbUnicodeCase.addItemListener(defaultOptionListener);

		JPanel regexpOptionsPanelWrapper = new JPanel(new BorderLayout());
		JPanel regexpOptionsPanel = new JPanel(new GridBagLayout());
		regexpOptionsPanelWrapper.add(regexpOptionsPanel, BorderLayout.NORTH);

		c.insets = new Insets(12, 4, 0, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		regexpOptionsPanel.add(cbMultiline, c);
		c.insets = new Insets(8, 4, 0, 4);
		c.gridy = 1;
		regexpOptionsPanel.add(cbCaseInsensitive, c);
		c.gridy = 2;
		regexpOptionsPanel.add(cbUnicodeCase, c);
		c.gridy = 3;
		regexpOptionsPanel.add(cbDotall, c);

		// create tabbed panel
		c.insets = new Insets(8, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		testExp = new JTabbedPane();
		JScrollPane spInline = new ExtendedJScrollPane(inlineSearchPanel);
		spInline.setBorder(null);
		testExp.add(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.inline_search.title"),
				spInline);
		JScrollPane spFindings = new ExtendedJScrollPane(regexpFindingsList);
		spFindings.setBorder(null);
		testExp.add(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.result_list.title"),
				spFindings);
		testExp.add(
				I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.regular_expression.regexp_options.title"),
				regexpOptionsPanelWrapper);
		testerPanel.add(testExp, c);

		JPanel groupPanel = new JPanel(new GridBagLayout());
		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		groupPanel.add(regexpPanel, c);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		groupPanel.add(replacementPanel, c);

		c.insets = new Insets(4, 4, 4, 4);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		groupPanel.add(testerPanel, c);

		panel.add(groupPanel, 1, 0);

		if (supportsItems) {
			// item shortcuts list
			itemShortcutsList = new JList<>(items.toArray(new String[0]));
			itemShortcutsList
					.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.item_shortcuts.tip"));
			itemShortcutsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			itemShortcutsList.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						String text = regexpTextField.getText();
						int cursorPosition = regexpTextField.getCaretPosition();
						int index = itemShortcutsList.getSelectedIndex();
						if (index > -1 && index < itemShortcutsList.getModel().getSize()) {
							String insertionString = itemShortcutsList.getModel().getElementAt(index);
							String newText = text.substring(0, cursorPosition) + insertionString
									+ (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
							regexpTextField.setText(newText);
							regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
							regexpTextField.requestFocus();
							fireRegularExpressionUpdated();
						}
					}
				}
			});
			JScrollPane itemShortcutsPane = new ExtendedJScrollPane(itemShortcutsList);
			itemShortcutsPane.setBorder(createTitledBorder(
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.item_shortcuts.border")));

			// matched items list
			matchedItemsListModel = new DefaultListModel<>();
			JList<String> matchedItemsList = new JList<>(matchedItemsListModel);
			matchedItemsList
					.setToolTipText(I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.matched_items.tip"));
			// add custom cell renderer to disallow selections
			matchedItemsList.setCellRenderer(new DefaultListCellRenderer() {

				private static final long serialVersionUID = -5795848004756768378L;

				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					return super.getListCellRendererComponent(list, value, index, false, false);
				}
			});
			JScrollPane matchedItemsPanel = new ExtendedJScrollPane(matchedItemsList);
			matchedItemsPanel.setBorder(createTitledBorder(
					I18N.getMessage(I18N.getGUIBundle(), "gui.dialog.parameter.regexp.matched_items.border")));

			// item panel on right side of dialog
			JPanel itemPanel = new JPanel(createGridLayout(1, 2));
			itemPanel.add(itemShortcutsPane, 0, 0);
			itemPanel.add(matchedItemsPanel, 0, 1);

			panel.add(itemPanel, 0, 1);
		}

		okButton = makeOkButton("regexp_property_dialog_apply");
		fireRegularExpressionUpdated();

		layoutDefault(panel, supportsItems ? NORMAL : NARROW, okButton, makeCancelButton());
	}

	private void updateRegexpOptions() {
		boolean multiline = cbMultiline.isSelected();
		boolean caseInsensitive = cbCaseInsensitive.isSelected();
		boolean dotall = cbDotall.isSelected();
		boolean unicodeCase = cbUnicodeCase.isSelected();

		String flags = getFlags(regexpTextField.getText());

		if (multiline != flags.contains("m")) {
			cbMultiline.setSelected(flags.contains("m"));
		}
		if (caseInsensitive != flags.contains("i")) {
			cbCaseInsensitive.setSelected(flags.contains("i"));
		}
		if (dotall != flags.contains("s")) {
			cbDotall.setSelected(flags.contains("s"));
		}
		if (unicodeCase != flags.contains("u")) {
			cbUnicodeCase.setSelected(flags.contains("u"));
		}
	}

	private String getFlags(String pattern) {
		if (!pattern.startsWith("(?")) {
			return "";
		}
		if (pattern.startsWith("(?-")) {
			return "";
		}
		int closingBracket = pattern.indexOf(')');
		if (closingBracket == -1) {
			return "";
		}
		String flags = pattern.substring(2, closingBracket);
		return flags.split("-")[0];
	}

	private void fireRegexpOptionsChanged() {
		boolean multiline = cbMultiline.isSelected();
		boolean caseInsensitive = cbCaseInsensitive.isSelected();
		boolean dotall = cbDotall.isSelected();
		boolean unicodeCase = cbUnicodeCase.isSelected();

		String pattern = regexpTextField.getText();
		String flags = getFlags(pattern);
		if (flags.contains("m")) {
			if (!multiline) {
				flags = flags.replace("m", "");
			}
		} else {
			if (multiline) {
				flags += "m";
			}
		}

		if (flags.contains("i")) {
			if (!caseInsensitive) {
				flags = flags.replace("i", "");
			}
		} else {
			if (caseInsensitive) {
				flags += "i";
			}
		}

		if (flags.contains("u")) {
			if (!unicodeCase) {
				flags = flags.replace("u", "");
			}
		} else {
			if (unicodeCase) {
				flags += "u";
			}
		}

		if (flags.contains("s")) {
			if (!dotall) {
				flags = flags.replace("s", "");
			}
		} else {
			if (dotall) {
				flags += "s";
			}
		}

		if (!flags.isEmpty() || pattern.startsWith("(?") && getFlags(pattern).isEmpty()) {
			flags = "(?" + flags + ")";
		}

		if (pattern.startsWith("(?") && !pattern.startsWith("(?-")) {
			int oldFlagsEnd = pattern.indexOf(')');
			if (oldFlagsEnd == -1) {
				oldFlagsEnd = 1;
			}
			oldFlagsEnd++;
			pattern = flags + pattern.substring(oldFlagsEnd);
		} else {
			pattern = flags + pattern;
		}
		int caretPosition = regexpTextField.getCaretPosition();
		regexpTextField.setText(pattern);
		if (caretPosition < pattern.length()) {
			regexpTextField.setCaretPosition(caretPosition);
		} else {
			regexpTextField.setCaretPosition(pattern.length());
		}
		fireRegularExpressionUpdated();
	}

	private void fireRegularExpressionUpdated() {
		boolean regularExpressionValid;
		Pattern pattern = null;
		try {
			pattern = Pattern.compile(regexpTextField.getText());
			regularExpressionValid = true;
		} catch (PatternSyntaxException e) {
			regularExpressionValid = false;
		}
		if (supportsItems) {
			matchedItemsListModel.clear();
			if (regularExpressionValid) {
				for (String previewString : items) {
					if (pattern.matcher(previewString).matches()) {
						matchedItemsListModel.addElement(previewString);
					}
				}
			}
		}
		if (regularExpressionValid) {
			errorMessage.setText(NO_ERROR_MESSAGE);
			errorMessage.setIcon(NO_ERROR_ICON);
			okButton.setEnabled(true);
			inlineSearchDocument.updatePattern(regexpTextField.getText());

		} else {
			errorMessage.setText(ERROR_MESSAGE);
			errorMessage.setIcon(ERROR_ICON);
			okButton.setEnabled(false);
			inlineSearchDocument.clearResults();
		}
	}

	private class InsertionAction extends LoggedAbstractAction {

		private static final long serialVersionUID = -5185173378762191200L;
		private final String insertionString;

		public InsertionAction(String title, String insertionString) {
			putValue(Action.NAME, title);
			this.insertionString = insertionString;
		}

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			String text = regexpTextField.getText();

			// is shortcut a construct?
			boolean isConstruct = false;
			int row = -1;
			for (int i = 0; i < regexpConstructs.length; i++) {
				if (regexpConstructs[i][0].equals(insertionString)) {
					isConstruct = true;
					row = i;
					break;
				}
			}
			if (isConstruct) {
				if (regexpConstructInsertionEncloseSelected[row] && regexpTextField.getSelectedText() != null) {
					int selectionStart = regexpTextField.getSelectionStart();
					int selectionEnd = regexpTextField.getSelectionEnd();
					String newText = text.substring(0, selectionStart)
							+ insertionString.substring(0, regexpConstructInsertionSelectionIndices[row][0])
							+ text.substring(selectionStart, selectionEnd) + insertionString
									.substring(regexpConstructInsertionSelectionIndices[row][0], insertionString.length())
							+ text.substring(selectionEnd, text.length());
					regexpTextField.setText(newText);
					regexpTextField.setCaretPosition(selectionEnd - regexpConstructInsertionCaretAdjustment[row]);
					regexpTextField.setSelectionStart(selectionStart + regexpConstructInsertionSelectionIndices[row][0]);
					regexpTextField.setSelectionEnd(selectionEnd + regexpConstructInsertionSelectionIndices[row][1]);
				} else {
					int cursorPosition = regexpTextField.getCaretPosition();
					String newText = text.substring(0, cursorPosition) + insertionString
							+ (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
					regexpTextField.setText(newText);
					regexpTextField.setCaretPosition(
							cursorPosition + insertionString.length() + regexpConstructInsertionCaretAdjustment[row]);
					regexpTextField.setSelectionStart(cursorPosition + regexpConstructInsertionSelectionIndices[row][0]);
					regexpTextField.setSelectionEnd(cursorPosition + regexpConstructInsertionSelectionIndices[row][1]);
				}
			} else {
				int cursorPosition = regexpTextField.getCaretPosition();
				String newText = text.substring(0, cursorPosition) + insertionString
						+ (cursorPosition < text.length() ? text.substring(cursorPosition) : "");
				regexpTextField.setText(newText);
				regexpTextField.setCaretPosition(cursorPosition + insertionString.length());
			}
			regexpTextField.requestFocus();
			fireRegularExpressionUpdated();
		}
	}

	/**
	 * Sets the text of the search field.
	 *
	 * @param text
	 */
	public void setSearchFieldText(String text) {
		try {
			this.inlineSearchDocument.insertString(0, text, new SimpleAttributeSet());
		} catch (BadLocationException e) {
		}
	}

	public String getRegexp() {
		return regexpTextField.getText();
	}

	/**
	 * Get the content of the {@link #replacementTextField}.
	 *
	 * @since 9.3
	 */
	public String getReplacement() {
		return replacementTextField.getText();
	}

	/**
	 * Set the content of the {@link #replacementTextField}.
	 *
	 * @since 9.3
	 */
	public void setReplacement(String replacement) {
		replacementTextField.setText(replacement);
	}

	@Override
	protected String getInfoText() {
		return infoText;
	}
}

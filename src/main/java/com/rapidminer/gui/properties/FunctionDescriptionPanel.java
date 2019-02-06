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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.rapidminer.gui.tools.AttributeGuiTools;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.AbstractObservable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.Observable;
import com.rapidminer.tools.Observer;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.SystemInfoUtilities;
import com.rapidminer.tools.SystemInfoUtilities.OperatingSystem;
import com.rapidminer.tools.expression.FunctionDescription;


/**
 * Panel which displays a {@link FunctionDescription}.
 *
 * @author Sabrina Kirstein
 * @since 6.5.0
 */
public class FunctionDescriptionPanel extends JPanel {

	private static final long serialVersionUID = 3290719075570794252L;

	/**
	 * As the FunctionDescriptionPanel is a Panel, it cannot be an observable. It owns an
	 * {@link Observable}, which informs the observers about click changes.
	 *
	 * @author Sabrina Kirstein
	 */
	private class PrivateObservable extends AbstractObservable<FunctionDescription> {

		@Override
		public void fireUpdate() {
			fireUpdate(functionEntry);
		}
	}

	private JLabel lblReturnTypeIcon;

	private JLabel lblFunctionName;

	private JButton btShowInfo;

	private JTextArea textareaInfoText;

	/** panel which contains the {@link #btShowInfo} button */
	private JPanel buttonPanel;

	/** panel which contains a label and the info of the {@link FunctionDescription} */
	private JPanel infoPanel;

	private Color defaultBackground;

	private MouseListener hoverMouseListener;

	private MouseListener dispatchMouseListener;

	private FunctionDescription functionEntry;

	private boolean isExpanded = false;

	private boolean initialized = false;

	private PrivateObservable observable = new PrivateObservable();

	/** dummy text area used to calculate the height of the panel */
	private static JTextArea dummyTextArea = new JTextArea();

	/** Parameter types that should be highlighted in the function name with parameters */
	private static final String[] PARAMETER_TYPES = { "Condition", "Attribute_value", "Nominal", "Numeric", "Integer",
			"Constant", "Date" };

	private static final ImageIcon INFO_ICON = SwingTools.createIcon("13/"
			+ I18N.getGUILabel("function_description.info.icon"));

	private static final ImageIcon INFO_ICON_HOVERED = SwingTools.createIcon("13/"
			+ I18N.getGUILabel("function_description.info.hovered.icon"));

	private static final ImageIcon ICON_ATTRIBUTE_VALUE = SwingTools.createIcon("16/question.png");

	private static final Color COLOR_LABEL = Color.DARK_GRAY;

	private static final Color COLOR_HIGHLIGHT = new Color(225, 225, 225);

	private static final Dimension DIMENSION_LABEL = new Dimension(100, 25);

	private static final int FIRST_ROW_HEIGHT = 35;

	private static final int ROW_HEIGHT = 22;

	private static final String HTML_TAB = "&nbsp;";

	/** defines when the width of function names is cropped */
	private static int MAX_WIDTH_OF_TEXT = 600;

	/**
	 * Creates a panel for the given {@link FunctionDescription}. When the panel is expanded, the
	 * extra information is shown.
	 *
	 * @param functionEntry
	 */
	public FunctionDescriptionPanel(FunctionDescription functionEntry) {
		this.functionEntry = functionEntry;

		initGUI();
		if (functionEntry != null) {
			updateFunctionEntry(functionEntry);
			showMoreInformation(isExpanded);
		} else {
			showMoreInformation(false);
		}
		registerMouseListener();
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent arg0) {
				updateHeight();
			}
		});
		initialized = true;
	}

	/**
	 * Register an observer to react on click events
	 */
	public void registerObserver(Observer<FunctionDescription> observer) {
		observable.addObserver(observer, false);
	}

	public static void updateMaximalWidth(int maxWidth) {
		MAX_WIDTH_OF_TEXT = maxWidth - 15;
	}

	/**
	 * initializes the graphical user interface
	 */
	private void initGUI() {

		isExpanded = false;
		setDefaultBackground(getBackground());

		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weighty = 0.3;
		gbc.gridy = 0;
		gbc.insets = new Insets(8, 7, 7, 7);

		gbc.anchor = GridBagConstraints.WEST;
		lblReturnTypeIcon = new JLabel("");
		add(lblReturnTypeIcon, gbc);

		gbc.weightx = 1;
		gbc.insets = new Insets(7, 0, 7, 7);
		gbc.gridx += 1;
		lblFunctionName = new JLabel("");
		lblFunctionName.setAlignmentX(LEFT_ALIGNMENT);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		add(lblFunctionName, gbc);

		// Button panel
		gbc.gridx += 1;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.EAST;
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		buttonPanel.setOpaque(false);

		btShowInfo = new JButton(new ResourceAction(true, "function_description.more_information") {

			private static final long serialVersionUID = 1L;

			@Override
			public void loggedActionPerformed(ActionEvent e) {
				toggleMoreInformation();
			}
		});
		btShowInfo.setIcon(INFO_ICON);
		btShowInfo.setContentAreaFilled(false);
		btShowInfo.setBorderPainted(false);
		btShowInfo.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseExited(MouseEvent e) {
				highlightInfoButton(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				highlightInfoButton(true);
			}

		});
		buttonPanel.add(btShowInfo);
		add(btShowInfo, gbc);

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// Info panel
		infoPanel = new JPanel();
		infoPanel.setOpaque(false);
		infoPanel.setLayout(new GridBagLayout());

		textareaInfoText = new JTextArea() {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getMinimumSize() {
				return FunctionDescriptionPanel.DIMENSION_LABEL;
			}

			@Override
			public Dimension getPreferredSize() {
				return getMinimumSize();
			};
		};
		textareaInfoText.setForeground(COLOR_LABEL);
		textareaInfoText.setAlignmentX(SwingConstants.LEFT);
		textareaInfoText.setBackground(getBackground());
		textareaInfoText.setEditable(false);
		textareaInfoText.setBorder(null);
		textareaInfoText.setLineWrap(true);
		textareaInfoText.setWrapStyleWord(true);
		textareaInfoText.setFocusable(false);

		dummyTextArea.setBorder(null);
		dummyTextArea.setAlignmentX(SwingConstants.LEFT);
		dummyTextArea.setEditable(false);
		dummyTextArea.setLineWrap(true);
		dummyTextArea.setWrapStyleWord(true);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 7, 7, 7);
		infoPanel.add(textareaInfoText, gbc);
		gbc.gridy += 1;
		gbc.weighty = 0.7;
		add(infoPanel, gbc);

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				// add the input name to the expression
				observable.fireUpdate();
			}

			@Override
			public void mouseExited(MouseEvent e) {
				highlightFunctionName(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				highlightFunctionName(true);
			}
		});

		updateHeight();
	}

	/**
	 * registers the mouse listener to show more information and to enable row highlighting
	 */
	private void registerMouseListener() {
		this.addMouseListener(createOrGetHoverMouseListener());
		textareaInfoText.addMouseListener(createOrGetDispatchMouseListener());
		lblReturnTypeIcon.addMouseListener(createOrGetDispatchMouseListener());
		lblFunctionName.addMouseListener(createOrGetDispatchMouseListener());
		infoPanel.addMouseListener(createOrGetDispatchMouseListener());
	}

	private void setDefaultBackground(Color color) {
		defaultBackground = color;
	}

	/**
	 * updates the displayed {@link FunctionDescription}
	 *
	 * @param functionEntry
	 */
	private void updateFunctionEntry(final FunctionDescription functionEntry) {
		if (functionEntry == null) {
			return;
		}

		// the tt tags are a hack to ensure that the label stays on the same location when the input
		// panel is toggled. Do not delete them!
		String croppedText = "<html>"
				+ SwingTools
						.getStrippedJComponentText(this,
								HTML_TAB + HTML_TAB + HTML_TAB + functionEntry.getFunctionNameWithParameters(),
								MAX_WIDTH_OF_TEXT, 0) + "<tt> </tt></html>";
		for (String parameterType : PARAMETER_TYPES) {
			croppedText = croppedText.replaceAll(parameterType, "<tt>" + parameterType + "</tt>");
		}

		lblFunctionName.setText(croppedText);
		String helpText = "<html><b>"
				+ functionEntry.getHelpTextName()
				+ "</b>: "
				+ (functionEntry.getFunctionNameWithParameters() != null ? functionEntry.getFunctionNameWithParameters()
						: functionEntry.getDisplayName()) + "</html>";
		lblFunctionName.setToolTipText(helpText);

		Icon icon = null;
		if (functionEntry.getReturnType() == Ontology.ATTRIBUTE_VALUE) {
			icon = ICON_ATTRIBUTE_VALUE;
		} else {
			icon = AttributeGuiTools.getIconForValueType(functionEntry.getReturnType(), true);
		}
		lblReturnTypeIcon.setIcon(icon);
		lblReturnTypeIcon.setToolTipText(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndexToDisplayName(functionEntry.getReturnType()));

		String infoText = getFunctionInfo();
		textareaInfoText.setText(infoText);
		textareaInfoText.setToolTipText(infoText);

		infoPanel.setVisible(isExpanded);

		setVisible(true);
		updateHeight();
	}

	/**
	 * shows the info text in the {@link FunctionDescriptionPanel} if <code>show</code> is true
	 *
	 * @param show
	 */
	private void showMoreInformation(boolean show) {

		infoPanel.setVisible(show);
		updateHeight();
	}

	/**
	 * Toggles the visibility of the info text in the {@link FunctionDescriptionPanel}
	 */
	private void toggleMoreInformation() {
		isExpanded = !isExpanded;
		showMoreInformation(isExpanded);
	}

	/**
	 * Creates the {@link MouseListener} which toggles the advanced information. If it is already
	 * created this will return the current instance.
	 *
	 * @return
	 */
	private MouseListener createOrGetHoverMouseListener() {
		if (hoverMouseListener == null) {
			hoverMouseListener = new MouseAdapter() {

				@Override
				public void mouseExited(MouseEvent e) {
					if (!SwingTools.isMouseEventExitedToChildComponents(FunctionDescriptionPanel.this, e)) {
						highlight(false);
					}
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					highlight(true);
				}
			};
		}
		return hoverMouseListener;
	}

	/**
	 * Creates the {@link MouseListener} which delivers {@link MouseEvent}s to the
	 * {@link FunctionDescriptionPanel}. Some GUI elements, like {@link JLabel} with Tooltips, may
	 * consume all events and does not inform the parent component.
	 *
	 * @return
	 */
	private MouseListener createOrGetDispatchMouseListener() {
		if (dispatchMouseListener == null) {
			dispatchMouseListener = new MouseListener() {

				@Override
				public void mouseClicked(MouseEvent e) {
					FunctionDescriptionPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
							FunctionDescriptionPanel.this));
				}

				@Override
				public void mousePressed(MouseEvent e) {
					FunctionDescriptionPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
							FunctionDescriptionPanel.this));
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					FunctionDescriptionPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
							FunctionDescriptionPanel.this));
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					FunctionDescriptionPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
							FunctionDescriptionPanel.this));
				}

				@Override
				public void mouseExited(MouseEvent e) {
					FunctionDescriptionPanel.this.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e,
							FunctionDescriptionPanel.this));
				}

			};
		}
		return dispatchMouseListener;
	}

	/**
	 * Highlight the info button and also the containing {@link FunctionDescriptionPanel}.
	 *
	 * @param highlight
	 */
	private void highlightInfoButton(boolean highlight) {

		if (highlight) {
			btShowInfo.setIcon(INFO_ICON_HOVERED);
		} else {
			btShowInfo.setIcon(INFO_ICON);
		}
		highlight(highlight);
	}

	/**
	 * Highlight the {@link #lblFunctionName}.
	 *
	 * @param highlight
	 */
	private void highlightFunctionName(boolean highlight) {

		if (highlight) {
			lblFunctionName.setForeground(SwingTools.RAPIDMINER_ORANGE);
		} else {
			lblFunctionName.setForeground(Color.BLACK);
		}
		highlight(highlight);
	}

	/**
	 * Highlight the {@link FunctionDescriptionPanel}.
	 *
	 * @param highlight
	 */
	private void highlight(boolean highlight) {
		if (highlight) {
			setBackground(COLOR_HIGHLIGHT);
			textareaInfoText.setBackground(COLOR_HIGHLIGHT);
		} else {
			if (defaultBackground != null) {
				setBackground(defaultBackground);
				textareaInfoText.setBackground(defaultBackground);
			}
		}
	}

	/**
	 * Updates the max and the min height of the {@link FunctionDescriptionPanel}
	 */
	private void updateHeight() {
		int totalHeight = FIRST_ROW_HEIGHT;

		if (isVisible() && textareaInfoText.getText() != null && !textareaInfoText.getText().isEmpty()
				&& functionEntry != null && isExpanded && initialized) {
			double numberOfLines = Math.ceil(getContentHeight(textareaInfoText.getText(), getWidth()) / 16.0);
			totalHeight += numberOfLines * ROW_HEIGHT;
			// add magic number 7 for one line descriptions
			if (numberOfLines == 1) {
				totalHeight += 7;
			}
		}

		infoPanel.setMinimumSize(new Dimension(getPreferredSize().width, totalHeight - FIRST_ROW_HEIGHT));
		infoPanel.setPreferredSize(new Dimension(getPreferredSize().width, totalHeight - FIRST_ROW_HEIGHT));
		infoPanel.setMaximumSize(new Dimension(getMaximumSize().width, totalHeight - FIRST_ROW_HEIGHT));
		setMinimumSize(new Dimension(getPreferredSize().width, totalHeight));
		setPreferredSize(new Dimension(getPreferredSize().width, totalHeight));
		setMaximumSize(new Dimension(getMaximumSize().width, totalHeight));
	}

	/**
	 * Gives the additional function information
	 *
	 * @return function information
	 */
	private String getFunctionInfo() {
		return functionEntry.getDescription();
	}

	/**
	 * Calculates the preferred height of an text area with the given fixed width for the specified
	 * string.
	 *
	 * @param info
	 *            the description of the {@link FunctionDescription}
	 * @param width
	 *            the width of the content
	 * @return the preferred height given the comment
	 */
	private static int getContentHeight(final String info, final int width) {
		if (info == null) {
			throw new IllegalArgumentException("info must not be null!");
		}
		dummyTextArea.setText(info);
		dummyTextArea.setSize(width, Short.MAX_VALUE);

		// height is not exact. Multiply by magic number to get a more fitting value...
		if (SystemInfoUtilities.getOperatingSystem() == OperatingSystem.OSX
				|| SystemInfoUtilities.getOperatingSystem() == OperatingSystem.UNIX
				|| SystemInfoUtilities.getOperatingSystem() == OperatingSystem.SOLARIS) {
			return (int) (dummyTextArea.getPreferredSize().getHeight() * 1.05f);
		} else {
			return (int) dummyTextArea.getPreferredSize().getHeight();
		}
	}
}

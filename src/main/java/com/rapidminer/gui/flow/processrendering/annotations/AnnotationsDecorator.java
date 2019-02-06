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
package com.rapidminer.gui.flow.processrendering.annotations;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DocumentFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import com.rapidminer.gui.ApplicationFrame;
import com.rapidminer.gui.flow.processrendering.annotations.event.AnnotationEventHook;
import com.rapidminer.gui.flow.processrendering.annotations.model.AnnotationsModel;
import com.rapidminer.gui.flow.processrendering.annotations.model.OperatorAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.ProcessAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotation;
import com.rapidminer.gui.flow.processrendering.annotations.model.WorkflowAnnotations;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationAlignment;
import com.rapidminer.gui.flow.processrendering.annotations.style.AnnotationColor;
import com.rapidminer.gui.flow.processrendering.draw.OperatorDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawDecorator;
import com.rapidminer.gui.flow.processrendering.draw.ProcessDrawUtils;
import com.rapidminer.gui.flow.processrendering.model.ProcessRendererModel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.gui.flow.processrendering.view.RenderPhase;
import com.rapidminer.gui.tools.EditableLinkController;
import com.rapidminer.gui.tools.Ionicon;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RMUrlHandler;


/**
 * This class handles event hooks and draw decorators registered to the {@link ProcessRendererView}
 * for workflow annotations.
 *
 * @author Marco Boeck
 * @since 6.4.0
 *
 */
public final class AnnotationsDecorator {

	/** name of the paste action (context menu) */
	private static final String PASTE_ACTION_NAME = "paste";

	/** name of the paste from clipboard action (ctrl+v) */
	private static final String PASTE_FROM_CLIPBOARD_ACTION_NAME = DefaultEditorKit.pasteAction;

	/** icon depicting annotations on an operator */
	private static final ImageIcon IMAGE_ANNOTATION = SwingTools.createIcon("16/note_pinned.png");
	private static final ImageIcon IMAGE_ANNOTATION_ZOOMED = SwingTools.createIcon("32/note_pinned.png");

	/** the width of the edit panel above/below the annotation editor */
	private static final int EDIT_PANEL_WIDTH = 190;

	/** the height of the edit panel above/below the annotation editor */
	private static final int EDIT_PANEL_HEIGHT = 30;

	/** the gap betwen the annotation and the edit buttons */
	private static final int BUTTON_PANEL_GAP = 10;

	/** the width of the color panel above/below the annotation editor */
	private static final int EDIT_COLOR_PANEL_WIDTH = 215;

	/** the height of the color panel above/below the annotation editor */
	private static final int EDIT_COLOR_PANEL_HEIGHT = EDIT_PANEL_HEIGHT;

	/** the pane which can be used to edit the text */
	private JEditorPane editPane;

	/** the panel which can be used to edit color and alignment during editing */
	private JPanel editPanel;

	/** the dialog panel which can be used to edit color while editing */
	private JDialog colorOverlay;

	/** the button opening the color overlay */
	private JToggleButton colorButton;

	/** the process renderer */
	private final ProcessRendererView view;

	/** the process renderer model */
	private final ProcessRendererModel rendererModel;

	/** the annotation visualizer instance */
	private final AnnotationsVisualizer visualizer;

	/** the model backing this decorator */
	private final AnnotationsModel model;

	/** the drawer for the annotations */
	private final AnnotationDrawer drawer;

	/** the event handling for annotations */
	private final AnnotationEventHook hook;

	/** draws process (free-flowing) annotations behind operators */
	private ProcessDrawDecorator processAnnotationDrawer = new ProcessDrawDecorator() {

		@Override
		public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, false);
		}

		@Override
		public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, true);
		}

		/**
		 * Draws the background annoations.
		 */
		private void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {
			if (!visualizer.isActive()) {
				return;
			}

			// background annotations
			WorkflowAnnotations annotations = rendererModel.getProcessAnnotations(process);
			if (annotations != null) {
				for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
					// selected is drawn by highlight decorator
					if (anno.equals(model.getSelected())) {
						continue;
					}

					// paint the annotation itself
					Graphics2D g2P = (Graphics2D) g2.create();
					drawer.drawAnnotation(anno, g2P, printing);
					g2P.dispose();
				}
			}
		}
	};

	/** draws operator annotations */
	private ProcessDrawDecorator operatorAnnotationDrawer = new ProcessDrawDecorator() {

		@Override
		public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, false);
		}

		@Override
		public void print(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, true);
		}

		/**
		 * Draws the operator annoations.
		 */
		private void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {
			if (!visualizer.isActive()) {
				return;
			}

			// operator attached annotations
			List<Operator> selectedOperators = rendererModel.getSelectedOperators();
			for (Operator operator : process.getOperators()) {
				if (selectedOperators.contains(operator)) {
					continue;
				}
				drawOpAnno(operator, g2, rendererModel, printing);
			}
			// selected operators annotations need to be drawn over non selected ones
			for (Operator selOp : selectedOperators) {
				if (process.equals(selOp.getExecutionUnit())) {
					drawOpAnno(selOp, g2, rendererModel, printing);
				}
			}
		}

		/**
		 * Draws the annotation for the given operator (if he has one).
		 */
		private void drawOpAnno(final Operator operator, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {
			WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(operator);
			if (annotations == null) {
				return;
			}
			for (WorkflowAnnotation anno : annotations.getAnnotationsDrawOrder()) {
				// selected is drawn by highlight decorator
				if (anno.equals(model.getSelected())) {
					continue;
				}

				// paint the annotation itself
				Graphics2D g2P = (Graphics2D) g2.create();
				drawer.drawAnnotation(anno, g2P, printing);
				g2P.dispose();
			}
		}
	};

	/** draws process (free-flowing) annotations which are selected. Drawn over operators */
	private ProcessDrawDecorator workflowAnnotationDrawerHighlight = new ProcessDrawDecorator() {

		@Override
		public void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(process, g2, rendererModel, false);
		}

		@Override
		public void print(ExecutionUnit process, Graphics2D g2, ProcessRendererModel model) {
			draw(process, g2, rendererModel, true);
		}

		/**
		 * Draws the selected annotation.
		 */
		private void draw(final ExecutionUnit process, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {
			if (!visualizer.isActive()) {
				return;
			}

			// paint the selected annotation
			WorkflowAnnotation selected = model.getSelected();
			if (selected != null) {
				// only draw in correct execution unit
				if (selected.getProcess().equals(process)) {
					// only paint annotation if not editing
					if (editPane == null) {
						// paint the annotation itself
						Graphics2D g2P = (Graphics2D) g2.create();
						drawer.drawAnnotation(selected, g2P, printing);
						g2P.dispose();
					} else {
						// only paint border
						Rectangle2D loc = selected.getLocation();
						g2.setColor(Color.BLACK);
						g2.draw(new Rectangle2D.Double(loc.getX() - 1, loc.getY() - 1,
								editPane.getBounds().getWidth() * (1 / rendererModel.getZoomFactor()) + 1,
								editPane.getBounds().getHeight() * (1 / rendererModel.getZoomFactor()) + 1));
					}
				}
			}
		}
	};

	/** draws annotation icons on operators */
	private OperatorDrawDecorator opAnnotationIconDrawer = new OperatorDrawDecorator() {

		@Override
		public void draw(final Operator operator, final Graphics2D g2, final ProcessRendererModel rendererModel) {
			draw(operator, g2, rendererModel, true);
		}

		@Override
		public void print(Operator operator, Graphics2D g2, ProcessRendererModel model) {
			draw(operator, g2, rendererModel, true);
		}

		/**
		 * Draws the annotation icon on operators.
		 */
		private void draw(final Operator operator, final Graphics2D g2, final ProcessRendererModel rendererModel,
				final boolean printing) {
			// Draw annotation icons only if hidden
			if (visualizer.isActive()) {
				return;
			}

			WorkflowAnnotations annotations = rendererModel.getOperatorAnnotations(operator);
			if (annotations == null || annotations.isEmpty()) {
				return;
			}
			Rectangle2D frame = rendererModel.getOperatorRect(operator);
			int iconSize = 16;
			int xOffset = (iconSize + 2) * 2;
			int iconX = (int) (frame.getX() + frame.getWidth() - xOffset);
			int iconY = (int) (frame.getY() + frame.getHeight() - iconSize - 1);
			ImageIcon icon = ProcessDrawUtils.getIcon(operator, rendererModel.getZoomFactor() <= 1d ? IMAGE_ANNOTATION : IMAGE_ANNOTATION_ZOOMED);
			RenderingHints originalRenderingHints = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.drawImage(icon.getImage(), iconX, iconY, iconSize, iconSize, null);
			g2.setRenderingHints(originalRenderingHints);
		}
	};

	/** listener which triggers color panel moving if required */
	private ComponentListener colorPanelMover = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {
			updateColorPanelPosition();
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			updateColorPanelPosition();
		}

	};

	/**
	 * Creates a new workflow annotation decorator
	 *
	 * @param view
	 *            the process renderer instance
	 * @param visualizer
	 *            the annotation visualizer instance
	 * @param model
	 *            the model backing this instance
	 */
	public AnnotationsDecorator(final ProcessRendererView view, final AnnotationsVisualizer visualizer,
			final AnnotationsModel model) {
		this.view = view;
		this.model = model;
		this.rendererModel = view.getModel();
		this.drawer = new AnnotationDrawer(model, rendererModel);
		this.hook = new AnnotationEventHook(this, model, visualizer, drawer, view, rendererModel);
		this.visualizer = visualizer;
	}

	/**
	 * Start inline editing of the selected annotation. If no annotation is selected, does nothing.
	 */
	public void editSelected() {
		if (model.getSelected() == null) {
			return;
		}
		// editor to actually edit comment string
		removeEditor();
		createEditor();

		// panel to edit alignment and color
		createEditPanel();

		editPane.requestFocusInWindow();
		view.repaint();
	}

	/**
	 * Stop all editing and remove editors.
	 */
	public void reset() {
		drawer.reset();
		removeEditor();
	}

	/**
	 * Registers the event hooks and draw decorators to the process renderer.
	 */
	void registerEventHooks() {
		view.addDrawDecorator(processAnnotationDrawer, RenderPhase.ANNOTATIONS);
		view.addDrawDecorator(operatorAnnotationDrawer, RenderPhase.OPERATOR_ANNOTATIONS);
		view.addDrawDecorator(workflowAnnotationDrawerHighlight, RenderPhase.OVERLAY);
		view.addDrawDecorator(opAnnotationIconDrawer);

		view.getOverviewPanelDrawer().addDecorator(processAnnotationDrawer, RenderPhase.ANNOTATIONS);
		view.getOverviewPanelDrawer().addDecorator(operatorAnnotationDrawer, RenderPhase.OPERATOR_ANNOTATIONS);

		hook.registerDecorators();

		// this listener makes the color edit panel move when required
		view.addComponentListener(colorPanelMover);
		ApplicationFrame.getApplicationFrame().addComponentListener(colorPanelMover);
	}

	/**
	 * Removes the event hooks and draw decorators from the process renderer.
	 */
	void unregisterDecorators() {
		view.removeDrawDecorator(processAnnotationDrawer, RenderPhase.ANNOTATIONS);
		view.removeDrawDecorator(operatorAnnotationDrawer, RenderPhase.OPERATOR_ANNOTATIONS);
		view.removeDrawDecorator(workflowAnnotationDrawerHighlight, RenderPhase.OVERLAY);
		view.removeDrawDecorator(opAnnotationIconDrawer);

		view.getOverviewPanelDrawer().removeDecorator(processAnnotationDrawer, RenderPhase.ANNOTATIONS);
		view.getOverviewPanelDrawer().removeDecorator(operatorAnnotationDrawer, RenderPhase.OPERATOR_ANNOTATIONS);

		hook.unregisterEventHooks();

		view.removeComponentListener(colorPanelMover);
		ApplicationFrame.getApplicationFrame().removeComponentListener(colorPanelMover);
	}

	/**
	 * Creates and adds the JEditorPane for the currently selected annotation to the process
	 * renderer.
	 */
	private void createEditor() {
		final WorkflowAnnotation selected = model.getSelected();
		Rectangle2D loc = selected.getLocation();

		// JEditorPane to edit the comment string
		editPane = new JEditorPane("text/html", "");
		editPane.setBorder(null);
		editPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
		int paneX = (int) (loc.getX() * rendererModel.getZoomFactor());
		int paneY = (int) (loc.getY() * rendererModel.getZoomFactor());
		int index = view.getModel().getProcessIndex(selected.getProcess());
		Point absolute = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(paneX, paneY), index, rendererModel);
		if (absolute == null) {
			return;
		}
		editPane.setBounds((int) absolute.getX(), (int) absolute.getY(),
				(int) (loc.getWidth() * rendererModel.getZoomFactor()),
				(int) (loc.getHeight() * rendererModel.getZoomFactor()));
		float originalSize = AnnotationDrawUtils.ANNOTATION_FONT.getSize();
		// without this, scaling is off even more when zooming out..
		if (rendererModel.getZoomFactor() < 1.0d) {
			originalSize -= 1f;
		}
		float fontSize = (float) (originalSize * rendererModel.getZoomFactor());
		Font annotationFont = AnnotationDrawUtils.ANNOTATION_FONT.deriveFont(fontSize);
		editPane.setFont(annotationFont);
		editPane.setText(AnnotationDrawUtils.createStyledCommentString(selected));
		// use proxy for paste actions to trigger reload of editor after paste
		Action pasteFromClipboard = editPane.getActionMap().get(PASTE_FROM_CLIPBOARD_ACTION_NAME);
		Action paste = editPane.getActionMap().get(PASTE_ACTION_NAME);
		if (pasteFromClipboard != null) {
			editPane.getActionMap().put(PASTE_FROM_CLIPBOARD_ACTION_NAME,
					new PasteAnnotationProxyAction(pasteFromClipboard, this));
		}
		if (paste != null) {
			editPane.getActionMap().put(PASTE_ACTION_NAME, new PasteAnnotationProxyAction(paste, this));
		}
		// use proxy for transfer actions to convert e.g. HTML paste to plaintext paste
		editPane.setTransferHandler(new TransferHandlerAnnotationPlaintext(editPane));
		// IMPORTANT: Linebreaks do not work without the following!
		// this filter inserts a \r every time the user enters a newline
		// this signal is later used to convert newline to <br/>
		((HTMLDocument) editPane.getDocument()).setDocumentFilter(new DocumentFilter() {

			@Override
			public void insertString(DocumentFilter.FilterBypass fb, int offs, String str, AttributeSet a)
					throws BadLocationException {
				// this is never called..
				super.insertString(fb, offs, str.replaceAll("\n", "\n" + AnnotationDrawUtils.ANNOTATION_HTML_NEWLINE_SIGNAL),
						a);
			}

			@Override
			public void replace(FilterBypass fb, int offs, int length, String str, AttributeSet a)
					throws BadLocationException {
				if (selected instanceof OperatorAnnotation) {
					// operator annotations have a character limit, enforce here
					try {
						int existingLength = AnnotationDrawUtils.getPlaintextFromEditor(editPane, false).length() - length;
						if (existingLength + str.length() > OperatorAnnotation.MAX_CHARACTERS) {
							// insert at beginning or end is fine, cut off excess characters
							if (existingLength <= 0 || offs >= existingLength) {
								int acceptableLength = OperatorAnnotation.MAX_CHARACTERS - existingLength;
								int newLength = Math.max(acceptableLength, 0);
								str = str.substring(0, newLength);
							} else {
								// inserting into middle, do NOT paste at all
								return;
							}
						}
					} catch (IOException e) {
						// should not happen, if it does this is our smallest problem -> ignore
					}
				}
				super.replace(fb, offs, length,
						str.replaceAll("\n", "\n" + AnnotationDrawUtils.ANNOTATION_HTML_NEWLINE_SIGNAL), a);
			}
		});
		// set background color
		if (selected.getStyle().getAnnotationColor() == AnnotationColor.TRANSPARENT) {
			editPane.setBackground(Color.WHITE);
		} else {
			editPane.setBackground(selected.getStyle().getAnnotationColor().getColorHighlight());
		}
		editPane.addFocusListener(new FocusAdapter() {

			@Override
			public void focusLost(final FocusEvent e) {
				// right-click menu
				if (e.isTemporary()) {
					return;
				}
				if (editPane != null && e.getOppositeComponent() != null) {
					// style edit menu, no real focus loss
					if (SwingUtilities.isDescendingFrom(e.getOppositeComponent(), editPanel)) {
						return;
					}
					if (SwingUtilities.isDescendingFrom(e.getOppositeComponent(), colorOverlay)) {
						return;
					}
					if (colorOverlay.getParent() == e.getOppositeComponent()) {
						return;
					}

					saveEdit(selected);
					removeEditor();
				}
			}

		});
		editPane.addKeyListener(new KeyAdapter() {

			/** keep track of control down so Ctrl+Enter works but Enter+Ctrl not */
			private boolean controlDown;

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
					controlDown = true;
				}
				// consume so undo/redo etc are not passed to the process
				if (SwingTools.isControlOrMetaDown(e) && e.getKeyCode() == KeyEvent.VK_Z
						|| e.getKeyCode() == KeyEvent.VK_Y) {
					e.consume();
				}
			}

			@Override
			public void keyReleased(final KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_CONTROL:
						controlDown = false;
						break;
					case KeyEvent.VK_ENTER:
						if (!controlDown) {
							updateEditorHeight(selected);
						} else {
							// if control was down before Enter was pressed, save & exit
							saveEdit(selected);
							removeEditor();
							model.setSelected(null);
						}
						break;
					case KeyEvent.VK_ESCAPE:
						// ignore changes on escape
						removeEditor();
						model.setSelected(null);
						break;
					default:
						break;
				}
			}

		});
		// allow hyperlink activation in edit mode
		// do this by removing original LinkController listener that prevents it and then add our own that allows it
		MouseListener originalLinkControllerListener = null;
		for (MouseListener mouseListener : editPane.getMouseListeners()) {
			if (mouseListener instanceof HTMLEditorKit.LinkController) {
				originalLinkControllerListener = mouseListener;
			}
		}
		editPane.removeMouseListener(originalLinkControllerListener);
		editPane.addMouseListener(new EditableLinkController());
		editPane.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				RMUrlHandler.handleUrl(e.getDescription());
			}
		});

		editPane.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateEditorHeight(selected);
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				updateEditorHeight(selected);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				updateEditorHeight(selected);
			}

		});
		view.add(editPane);
		editPane.selectAll();
	}

	/**
	 * Creates and adds the edit panel for the currently selected annotation to the process
	 * renderer.
	 *
	 */
	private void createEditPanel() {
		final WorkflowAnnotation selected = model.getSelected();
		Rectangle2D loc = selected.getLocation();

		// panel containing buttons
		editPanel = new JPanel();
		editPanel.setCursor(Cursor.getDefaultCursor());
		editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.LINE_AXIS));
		updateEditPanelPosition(loc, false);
		editPanel.setOpaque(true);
		editPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		// consume mouse events so focus is not lost
		editPanel.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}
		});

		// add alignment controls
		final List<JToggleButton> alignmentButtonList = new LinkedList<JToggleButton>();
		ButtonGroup nonSelectionGroup = new ButtonGroup() {

			private static final long serialVersionUID = 1L;

			@Override
			public void setSelected(ButtonModel model, boolean selected) {
				if (selected) {
					super.setSelected(model, selected);
				} else {
					clearSelection();
				}
			}
		};
		for (AnnotationAlignment align : AnnotationAlignment.values()) {
			final Action action = align.makeAlignmentChangeAction(model, model.getSelected());
			final JToggleButton alignButton = new JToggleButton();
			nonSelectionGroup.add(alignButton);
			alignButton.setIcon((Icon) action.getValue(Action.SMALL_ICON));
			alignButton.setBorderPainted(false);
			alignButton.setBorder(null);
			alignButton.setFocusable(false);
			if (align == selected.getStyle().getAnnotationAlignment()) {
				alignButton.setSelected(true);
			}
			alignButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					removeColorPanel();
					colorButton.setSelected(false);

					int caretPos = editPane.getCaretPosition();
					// remember if we were at last position because doc length can change after 1st
					// save
					boolean lastPos = caretPos == editPane.getDocument().getLength();
					int selStart = editPane.getSelectionStart();
					int selEnd = editPane.getSelectionEnd();
					// change alignment and save current comment
					action.actionPerformed(e);
					saveEdit(selected);
					// reload edit pane with changes
					editPane.setText(AnnotationDrawUtils.createStyledCommentString(selected));
					// special handling for documents of length 1 to avoid not being able to type
					if (editPane.getDocument().getLength() == 1) {
						caretPos = 1;
					} else if (lastPos) {
						caretPos = editPane.getDocument().getLength();
					} else {
						caretPos = Math.min(editPane.getDocument().getLength(), caretPos);
					}
					editPane.setCaretPosition(caretPos);
					if (selEnd - selStart > 0) {
						editPane.setSelectionStart(selStart);
						editPane.setSelectionEnd(selEnd);
					}
					editPane.requestFocusInWindow();
				}
			});
			editPanel.add(alignButton);
			alignmentButtonList.add(alignButton);
		}

		// add small empty space
		editPanel.add(Box.createHorizontalStrut(2));

		// add color controls
		colorOverlay = new JDialog(ApplicationFrame.getApplicationFrame());
		colorOverlay.setCursor(Cursor.getDefaultCursor());
		colorOverlay.getRootPane().setLayout(new BoxLayout(colorOverlay.getRootPane(), BoxLayout.LINE_AXIS));
		colorOverlay.setUndecorated(true);
		colorOverlay.getRootPane().setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		colorOverlay.setFocusable(false);
		colorOverlay.setAutoRequestFocus(false);
		// consume mouse events so focus is not lost
		colorOverlay.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mousePressed(MouseEvent e) {
				e.consume();
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				e.consume();
			}
		});

		for (final AnnotationColor color : AnnotationColor.values()) {
			final Action action = color.makeColorChangeAction(model, selected);
			JButton colChangeButton = new JButton();
			colChangeButton.setText(null);
			colChangeButton.setBorderPainted(false);
			colChangeButton.setBorder(null);
			colChangeButton.setFocusable(false);
			final Icon icon = SwingTools.createIconFromColor(color.getColor(), Color.BLACK, 16, 16,
					new Rectangle2D.Double(1, 1, 14, 14));
			colChangeButton.setIcon(icon);
			colChangeButton.setToolTipText(color.getKey());
			colChangeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// change color and save current comment
					action.actionPerformed(e);
					saveEdit(selected);
					// set edit pane bg color
					editPane.requestFocusInWindow();
					if (color == AnnotationColor.TRANSPARENT) {
						editPane.setBackground(Color.WHITE);
					} else {
						editPane.setBackground(color.getColorHighlight());
					}

					// adapt color of main button, remove color panel
					colorButton.setIcon(icon);
					if (removeColorPanel()) {
						colorButton.setSelected(false);
						view.repaint();
					}
				}
			});

			colorOverlay.getRootPane().add(colChangeButton);
		}

		colorButton = new JToggleButton(
				"<html><span style=\"color: 4F4F4F;\">" + Ionicon.ARROW_DOWN_B.getHtml() + "</span></html>");
		colorButton.setBorderPainted(false);
		colorButton.setFocusable(false);
		AnnotationColor color = selected.getStyle().getAnnotationColor();
		colorButton.setIcon(
				SwingTools.createIconFromColor(color.getColor(), Color.BLACK, 16, 16, new Rectangle2D.Double(1, 1, 14, 14)));
		colorButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (removeColorPanel()) {
					colorButton.setSelected(false);
					view.repaint();
					return;
				}

				updateColorPanelPosition();
				colorOverlay.setVisible(true);
				editPane.requestFocusInWindow();
				view.repaint();
			}

		});
		editPanel.add(colorButton);

		// add separator
		JLabel separator = new JLabel() {

			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;

				g2.setColor(Color.LIGHT_GRAY);
				g2.drawLine(2, 0, 2, 20);
			}
		};
		separator.setText(" "); // dummy text to show label
		editPanel.add(separator);

		// add delete button
		final JButton deleteButton = new JButton(
				I18N.getMessage(I18N.getGUIBundle(), "gui.action.workflow.annotation.delete.label"));
		deleteButton.setForeground(Color.RED);
		deleteButton.setContentAreaFilled(false);
		deleteButton.setFocusable(false);
		deleteButton.setBorderPainted(false);
		deleteButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				model.deleteAnnotation(selected);
				removeEditor();
			}
		});
		deleteButton.addMouseListener(new MouseAdapter() {

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public void mouseExited(MouseEvent e) {
				Font font = deleteButton.getFont();
				Map attributes = font.getAttributes();
				attributes.put(TextAttribute.UNDERLINE, -1);
				deleteButton.setFont(font.deriveFont(attributes));
			}

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public void mouseEntered(MouseEvent e) {
				Font font = deleteButton.getFont();
				Map attributes = font.getAttributes();
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				deleteButton.setFont(font.deriveFont(attributes));
			}
		});
		editPanel.add(deleteButton);

		// add panel to view
		view.add(editPanel);
	}

	/**
	 * Saves the content of the comment editor as the new comment for the given
	 * {@link WorkflowAnnotation}.
	 *
	 * @param selected
	 *            the annotation for which the content of the editor pane should be saved as new
	 *            comment
	 */
	private void saveEdit(final WorkflowAnnotation selected) {
		if (editPane == null) {
			return;
		}
		HTMLDocument document = (HTMLDocument) editPane.getDocument();
		StringWriter writer = new StringWriter();
		try {
			editPane.getEditorKit().write(writer, document, 0, document.getLength());
		} catch (IndexOutOfBoundsException | IOException | BadLocationException e1) {
			// should not happen
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.flow.processrendering.annotations.AnnotationsDecorator.cannot_save");
		}
		String comment = writer.toString();
		comment = AnnotationDrawUtils.removeStyleFromComment(comment);
		Rectangle2D loc = selected.getLocation();
		Rectangle2D newLoc = new Rectangle2D.Double(loc.getX(), loc.getY(),
				editPane.getBounds().getWidth() * (1 / rendererModel.getZoomFactor()),
				editPane.getBounds().getHeight() * (1 / rendererModel.getZoomFactor()));
		selected.setLocation(newLoc);

		boolean overflowing = false;
		int prefHeight = AnnotationDrawUtils.getContentHeight(
				AnnotationDrawUtils.createStyledCommentString(comment, selected.getStyle()), (int) newLoc.getWidth(), AnnotationDrawUtils.ANNOTATION_FONT);
		if (prefHeight > Math.ceil(newLoc.getHeight())) {
			overflowing = true;
		}
		selected.setOverflowing(overflowing);

		model.setAnnotationComment(selected, comment);
	}

	/**
	 * Reloads the editor pane content to match editor and annotation styling. After this call, the
	 * editor pane displays the annotation in the same way as it is displayed in the process
	 * renderer.
	 */
	void updateEditorContent() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (editPane == null || model.getSelected() == null) {
					return;
				}
				HTMLDocument document = (HTMLDocument) editPane.getDocument();
				StringWriter writer = new StringWriter();
				try {
					editPane.getEditorKit().write(writer, document, 0, document.getLength());
				} catch (IndexOutOfBoundsException | IOException | BadLocationException e1) {
					// should not happen
					LogService.getRoot().log(Level.WARNING,
							"com.rapidminer.gui.flow.processrendering.annotations.AnnotationsDecorator.cannot_save");
				}
				String comment = writer.toString();
				comment = AnnotationDrawUtils.removeStyleFromComment(comment);
				int caretPos = editPane.getCaretPosition();
				boolean lastPos = caretPos == editPane.getDocument().getLength();
				editPane.setText(AnnotationDrawUtils.createStyledCommentString(comment, model.getSelected().getStyle()));
				if (lastPos) {
					caretPos = editPane.getDocument().getLength();
				}
				caretPos = Math.min(caretPos, editPane.getDocument().getLength());
				editPane.setCaretPosition(caretPos);
				editPane.requestFocusInWindow();
			}
		});
	}

	/**
	 * Removes the annotation editor from the process renderer.
	 */
	private void removeEditor() {
		if (editPane != null) {
			view.remove(editPane);
			editPane = null;
		}
		if (editPanel != null) {
			view.remove(editPanel);
			editPanel = null;
		}
		removeColorPanel();

		// this makes sure that pressing F2 afterwards works
		// otherwise nothing is focused until the next click
		view.requestFocusInWindow();
		view.repaint();
	}

	/**
	 * Tries to remove the color panel. If not found or not showing, does nothing.
	 */
	private boolean removeColorPanel() {
		if (colorOverlay != null && colorOverlay.isShowing()) {
			colorOverlay.dispose();
			return true;
		}

		return false;
	}

	/**
	 * Updates the position and size of the edit panel relative to the given location.
	 *
	 * @param loc
	 *            the location the edit panel should be relative to
	 * @param absolute
	 *            if {@code true} the loc is treated as absolute position on the process renderer;
	 *            if {@code false} it is treated as relative to the current process
	 */
	private void updateEditPanelPosition(final Rectangle2D loc, final boolean absolute) {
		if (absolute) {
			int panelX = (int) (loc.getCenterX() - EDIT_PANEL_WIDTH / 2);
			int panelY = (int) (loc.getY() - EDIT_PANEL_HEIGHT - BUTTON_PANEL_GAP);
			// if panel would be outside process renderer, fix it
			if (panelX < WorkflowAnnotation.MIN_X) {
				panelX = WorkflowAnnotation.MIN_X;
			}
			if (panelY < 0) {
				panelY = (int) loc.getMaxY() + BUTTON_PANEL_GAP;
			}
			// last fallback is cramped to the bottom. If that does not fit either, don't care
			if (panelY + EDIT_PANEL_HEIGHT > view.getSize().getHeight() - BUTTON_PANEL_GAP * 2) {
				panelY = (int) loc.getMaxY();
			}
			editPanel.setBounds(panelX, panelY, EDIT_PANEL_WIDTH, EDIT_PANEL_HEIGHT);
		} else {
			int panelX = (int) (loc.getCenterX() * rendererModel.getZoomFactor() - EDIT_PANEL_WIDTH / 2);
			int panelY = (int) (loc.getY() * rendererModel.getZoomFactor() - EDIT_PANEL_HEIGHT - BUTTON_PANEL_GAP);
			// if panel would be outside process renderer, fix it
			if (panelX < WorkflowAnnotation.MIN_X) {
				panelX = WorkflowAnnotation.MIN_X;
			}
			if (panelY < 0) {
				panelY = (int) (loc.getMaxY() * rendererModel.getZoomFactor()) + BUTTON_PANEL_GAP;
			}
			// last fallback is cramped to the bottom. If that does not fit either, don't care
			if (panelY + EDIT_PANEL_HEIGHT > view.getSize().getHeight() - BUTTON_PANEL_GAP * 2) {
				panelY = (int) (loc.getMaxY() * rendererModel.getZoomFactor());
			}
			int index = view.getModel().getProcessIndex(model.getSelected().getProcess());
			Point absoluteP = ProcessDrawUtils.convertToAbsoluteProcessPoint(new Point(panelX, panelY), index,
					rendererModel);
			editPanel.setBounds((int) absoluteP.getX(), (int) absoluteP.getY(), EDIT_PANEL_WIDTH, EDIT_PANEL_HEIGHT);
		}
	}

	/**
	 * Makes sure the current editor height matches its content if the annotation was never resized.
	 * If the annotation has been manually resized before, does nothing.
	 *
	 * @param anno
	 *            the annotation currently in the editor
	 */
	private void updateEditorHeight(final WorkflowAnnotation anno) {
		if (anno.wasResized()) {
			return;
		}

		Rectangle bounds = editPane.getBounds();
		// height is either the pref height or the current height, depending on what is bigger
		int prefHeight;
		if (anno instanceof ProcessAnnotation) {
			prefHeight = (int) Math.max(getContentHeightOfEditor((int) bounds.getWidth()), bounds.getHeight());
		} else {
			prefHeight = Math.max(getContentHeightOfEditor((int) bounds.getWidth()), OperatorAnnotation.MIN_HEIGHT);
		}
		Rectangle newBounds = new Rectangle((int) bounds.getX(), (int) bounds.getY(), (int) bounds.getWidth(), prefHeight);
		if (!bounds.equals(newBounds)) {
			editPane.setBounds(newBounds);
			updateEditPanelPosition(newBounds, true);
			view.getModel().fireAnnotationMiscChanged(anno);
		}
	}

	/**
	 * Updates the location of the color edit panel (if shown).
	 */
	private void updateColorPanelPosition() {
		if (editPanel != null && colorOverlay != null) {
			int colorPanelX = (int) editPanel.getLocationOnScreen().getX() + colorButton.getX();
			int colorPanelY = (int) (editPanel.getLocationOnScreen().getY() + editPanel.getBounds().getHeight());
			colorOverlay.setBounds(colorPanelX, colorPanelY, EDIT_COLOR_PANEL_WIDTH, EDIT_COLOR_PANEL_HEIGHT);
		}
	}

	/**
	 * Calculates the preferred height of the editor pane with the given fixed width.
	 *
	 * @param width
	 *            the width of the pane
	 * @return the preferred height given the current editor pane content or {@code -1} if there was
	 *         a problem. Value will never exceed {@link ProcessAnnotation#MAX_HEIGHT} or {@link OperatorAnnotation#MAX_HEIGHT}
	 */
	private int getContentHeightOfEditor(final int width) {
		HTMLDocument document = (HTMLDocument) editPane.getDocument();
		StringWriter writer = new StringWriter();
		try {
			editPane.getEditorKit().write(writer, document, 0, document.getLength());
		} catch (IndexOutOfBoundsException | IOException | BadLocationException e1) {
			// should not happen
			return -1;
		}
		String comment = writer.toString();
		comment = AnnotationDrawUtils.removeStyleFromComment(comment);

		int maxHeight = model.getSelected() instanceof ProcessAnnotation ? ProcessAnnotation.MAX_HEIGHT
				: OperatorAnnotation.MAX_HEIGHT;
		// factor OUT zoom
		int result = Math.min(
				AnnotationDrawUtils.getContentHeight(
						AnnotationDrawUtils.createStyledCommentString(comment, model.getSelected().getStyle()),
						(int) (width * (1 / rendererModel.getZoomFactor())), AnnotationDrawUtils.ANNOTATION_FONT),
				maxHeight);
		result *= rendererModel.getZoomFactor();
		// extra pixel because not everything is pixel perfect here
		result += 1;
		return result;
	}

}

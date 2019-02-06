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
package com.rapidminer.gui.tools.syntax;

/*
 * InputHandler.java - Manages key bindings and executes actions Copyright (C) 1999 Slava Pestov
 * 
 * You may use and modify this package for any purpose. Redistribution is permitted, in both source
 * and binary form, provided that this notice remains intact in all source distributions of this
 * package.
 */

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;

import com.rapidminer.gui.LoggedAbstractAction;
import com.rapidminer.gui.dialog.SearchDialog;
import com.rapidminer.gui.dialog.SearchableJEditTextArea;
import com.rapidminer.gui.tools.IconSize;
import com.rapidminer.tools.Tools;


/**
 * An input handler converts the user's key strokes into concrete actions. It also takes care of
 * macro recording and action repetition.
 * <p>
 * 
 * This class provides all the necessary support code for an input handler, but doesn't actually do
 * any key binding logic. It is up to the implementations of this class to do so.
 * 
 * @author Slava Pestov, Ingo Mierswa
 */
public abstract class InputHandler extends KeyAdapter {

	/**
	 * If this client property is set to Boolean.TRUE on the text area, the home/end keys will
	 * support 'smart' BRIEF-like behaviour (one press = start/end of line, two presses = start/end
	 * of viewscreen, three presses = start/end of document). By default, this property is not set.
	 */
	public static final String SMART_HOME_END_PROPERTY = "InputHandler.homeEnd";

	public static final ActionListener BACKSPACE = new Backspace();

	public static final ActionListener BACKSPACE_WORD = new BackspaceWord();

	public static final ActionListener DELETE = new Delete();

	public static final ActionListener DELETE_WORD = new DeleteWord();

	public static final ActionListener END = new End(false);

	public static final ActionListener DOCUMENT_END = new DocumentEnd(false);

	public static final ActionListener SELECT_END = new End(true);

	public static final ActionListener SELECT_DOC_END = new DocumentEnd(true);

	public static final ActionListener INSERT_BREAK = new InsertBreak();

	public static final ActionListener INSERT_TAB = new InsertTab();

	public static final ActionListener HOME = new Home(false);

	public static final ActionListener DOCUMENT_HOME = new DocumentHome(false);

	public static final ActionListener SELECT_HOME = new Home(true);

	public static final ActionListener SELECT_DOC_HOME = new DocumentHome(true);

	public static final ActionListener NEXT_CHAR = new NextChar(false);

	public static final ActionListener NEXT_LINE = new NextLine(false);

	public static final ActionListener NEXT_PAGE = new NextPage(false);

	public static final ActionListener NEXT_WORD = new NextWord(false);

	public static final ActionListener SELECT_NEXT_CHAR = new NextChar(true);

	public static final ActionListener SELECT_NEXT_LINE = new NextLine(true);

	public static final ActionListener SELECT_NEXT_PAGE = new NextPage(true);

	public static final ActionListener SELECT_NEXT_WORD = new NextWord(true);

	public static final ActionListener OVERWRITE = new Overwrite();

	public static final ActionListener PREV_CHAR = new PrevChar(false);

	public static final ActionListener PREV_LINE = new PrevLine(false);

	public static final ActionListener PREV_PAGE = new PrevPage(false);

	public static final ActionListener PREV_WORD = new PrevWord(false);

	public static final ActionListener SELECT_PREV_CHAR = new PrevChar(true);

	public static final ActionListener SELECT_PREV_LINE = new PrevLine(true);

	public static final ActionListener SELECT_PREV_PAGE = new PrevPage(true);

	public static final ActionListener SELECT_PREV_WORD = new PrevWord(true);

	public static final ActionListener REPEAT = new Repeat();

	public static final ActionListener TOGGLE_RECT = new ToggleRect();

	// Default action
	public static final ActionListener INSERT_CHAR = new insert_char();

	// Clipboard
	public static final Action CLIP_COPY = new ClipCopy();

	public static final Action CLIP_PASTE = new ClipPaste();

	public static final Action CLIP_CUT = new ClipCut();

	public static final Action SELECT_ALL = new SelectAll();

	public static final Action SEARCH_AND_REPLACE = new SearchAction(IconSize.SMALL);

	private static Hashtable<String, ActionListener> actions;

	static {
		actions = new Hashtable<>();
		actions.put("backspace", BACKSPACE);
		actions.put("backspace-word", BACKSPACE_WORD);
		actions.put("delete", DELETE);
		actions.put("delete-word", DELETE_WORD);
		actions.put("end", END);
		actions.put("select-end", SELECT_END);
		actions.put("document-end", DOCUMENT_END);
		actions.put("select-doc-end", SELECT_DOC_END);
		actions.put("insert-break", INSERT_BREAK);
		actions.put("insert-tab", INSERT_TAB);
		actions.put("home", HOME);
		actions.put("select-home", SELECT_HOME);
		actions.put("document-home", DOCUMENT_HOME);
		actions.put("select-doc-home", SELECT_DOC_HOME);
		actions.put("next-char", NEXT_CHAR);
		actions.put("next-line", NEXT_LINE);
		actions.put("next-page", NEXT_PAGE);
		actions.put("next-word", NEXT_WORD);
		actions.put("select-next-char", SELECT_NEXT_CHAR);
		actions.put("select-next-line", SELECT_NEXT_LINE);
		actions.put("select-next-page", SELECT_NEXT_PAGE);
		actions.put("select-next-word", SELECT_NEXT_WORD);
		actions.put("overwrite", OVERWRITE);
		actions.put("prev-char", PREV_CHAR);
		actions.put("prev-line", PREV_LINE);
		actions.put("prev-page", PREV_PAGE);
		actions.put("prev-word", PREV_WORD);
		actions.put("select-prev-char", SELECT_PREV_CHAR);
		actions.put("select-prev-line", SELECT_PREV_LINE);
		actions.put("select-prev-page", SELECT_PREV_PAGE);
		actions.put("select-prev-word", SELECT_PREV_WORD);
		actions.put("repeat", REPEAT);
		actions.put("toggle-rect", TOGGLE_RECT);
		actions.put("insert-char", INSERT_CHAR);

		actions.put("clipboard-copy", CLIP_COPY);
		actions.put("clipboard-paste", CLIP_PASTE);
		actions.put("clipboard-cut", CLIP_CUT);
		actions.put("select-all", SELECT_ALL);
	}

	/**
	 * Returns a named text area action.
	 * 
	 * @param name
	 *            The action name
	 */
	public static ActionListener getAction(String name) {
		return actions.get(name);
	}

	/**
	 * Returns the name of the specified text area action.
	 * 
	 * @param listener
	 *            The action
	 */
	public static String getActionName(ActionListener listener) {
		Enumeration<String> enumeration = getActions();
		while (enumeration.hasMoreElements()) {
			String name = enumeration.nextElement();
			ActionListener _listener = getAction(name);
			if (_listener == listener) {
				return name;
			}
		}
		return null;
	}

	/**
	 * Returns an enumeration of all available actions.
	 */
	public static Enumeration<String> getActions() {
		return actions.keys();
	}

	/**
	 * Adds the default key bindings to this input handler. This should not be called in the
	 * constructor of this input handler, because applications might load the key bindings from a
	 * file, etc.
	 */
	public abstract void addDefaultKeyBindings();

	/**
	 * Adds a key binding to this input handler.
	 * 
	 * @param keyBinding
	 *            The key binding (the format of this is input-handler specific)
	 * @param action
	 *            The action
	 */
	public abstract void addKeyBinding(String keyBinding, ActionListener action);

	/**
	 * Removes a key binding from this input handler.
	 * 
	 * @param keyBinding
	 *            The key binding
	 */
	public abstract void removeKeyBinding(String keyBinding);

	/**
	 * Removes all key bindings from this input handler.
	 */
	public abstract void removeAllKeyBindings();

	/**
	 * Grabs the next key typed event and invokes the specified action with the key as a the action
	 * command.
	 */
	public void grabNextKeyStroke(ActionListener listener) {
		grabAction = listener;
	}

	/**
	 * Returns if repeating is enabled. When repeating is enabled, actions will be executed multiple
	 * times. This is usually invoked with a special key stroke in the input handler.
	 */
	public boolean isRepeatEnabled() {
		return repeat;
	}

	/**
	 * Enables repeating. When repeating is enabled, actions will be executed multiple times. Once
	 * repeating is enabled, the input handler should read a number from the keyboard.
	 */
	public void setRepeatEnabled(boolean repeat) {
		this.repeat = repeat;
	}

	/**
	 * Returns the number of times the next action will be repeated.
	 */
	public int getRepeatCount() {
		return repeat ? Math.max(1, repeatCount) : 1;
	}

	/**
	 * Sets the number of times the next action will be repeated.
	 * 
	 * @param repeatCount
	 *            The repeat count
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * Returns the macro recorder. If this is non-null, all executed actions should be forwarded to
	 * the recorder.
	 */
	public InputHandler.MacroRecorder getMacroRecorder() {
		return recorder;
	}

	/**
	 * Sets the macro recorder. If this is non-null, all executed actions should be forwarded to the
	 * recorder.
	 * 
	 * @param recorder
	 *            The macro recorder
	 */
	public void setMacroRecorder(InputHandler.MacroRecorder recorder) {
		this.recorder = recorder;
	}

	/**
	 * Returns a copy of this input handler that shares the same key bindings. Setting key bindings
	 * in the copy will also set them in the original.
	 */
	public abstract InputHandler copy();

	/**
	 * Executes the specified action, repeating and recording it as necessary.
	 * 
	 * @param listener
	 *            The action listener
	 * @param source
	 *            The event source
	 * @param actionCommand
	 *            The action command
	 */
	public void executeAction(ActionListener listener, Object source, String actionCommand) {
		// create event
		ActionEvent evt = new ActionEvent(source, ActionEvent.ACTION_PERFORMED, actionCommand);

		// don't do anything if the action is a wrapper
		// (like EditAction.Wrapper)
		if (listener instanceof Wrapper) {
			listener.actionPerformed(evt);
			return;
		}

		// remember old values, in case action changes them
		boolean _repeat = repeat;
		int _repeatCount = getRepeatCount();

		// execute the action
		if (listener instanceof InputHandler.NonRepeatable) {
			listener.actionPerformed(evt);
		} else {
			for (int i = 0; i < Math.max(1, repeatCount); i++) {
				listener.actionPerformed(evt);
			}
		}

		// do recording. Notice that we do no recording whatsoever
		// for actions that grab keys
		if (grabAction == null) {
			if (recorder != null) {
				if (!(listener instanceof InputHandler.NonRecordable)) {
					if (_repeatCount != 1) {
						recorder.actionPerformed(REPEAT, String.valueOf(_repeatCount));
					}

					recorder.actionPerformed(listener, actionCommand);
				}
			}

			// If repeat was true originally, clear it
			// Otherwise it might have been set by the action, etc
			if (_repeat) {
				repeat = false;
				repeatCount = 0;
			}
		}
	}

	/**
	 * Returns the text area that fired the specified event.
	 * 
	 * @param evt
	 *            The event
	 */
	public static JEditTextArea getTextArea(EventObject evt) {
		if (evt != null) {
			Object o = evt.getSource();
			if (o instanceof Component) {
				// find the parent text area
				Component c = (Component) o;
				for (;;) {
					if (c instanceof JEditTextArea) {
						return (JEditTextArea) c;
					} else if (c == null) {
						break;
					}
					if (c instanceof JPopupMenu) {
						c = ((JPopupMenu) c).getInvoker();
					} else {
						c = c.getParent();
					}
				}
			}
		}

		// this shouldn't happen
		// System.err.println("BUG: getTextArea() returning null");
		// System.err.println("Report this to Slava Pestov <sp@gjt.org>");
		return null;
	}

	// protected members

	/**
	 * If a key is being grabbed, this method should be called with the appropriate key event. It
	 * executes the grab action with the typed character as the parameter.
	 */
	protected void handleGrabAction(KeyEvent evt) {
		// Clear it *before* it is executed so that executeAction()
		// resets the repeat count
		ActionListener _grabAction = grabAction;
		grabAction = null;
		executeAction(_grabAction, evt.getSource(), String.valueOf(evt.getKeyChar()));
	}

	// protected members
	protected ActionListener grabAction;

	protected boolean repeat;

	protected int repeatCount;

	protected InputHandler.MacroRecorder recorder;

	/**
	 * If an action implements this interface, it should not be repeated. Instead, it will handle
	 * the repetition itself.
	 */
	public interface NonRepeatable {
	}

	/**
	 * If an action implements this interface, it should not be recorded by the macro recorder.
	 * Instead, it will do its own recording.
	 */
	public interface NonRecordable {
	}

	/**
	 * For use by EditAction.Wrapper only.
	 * 
	 * @since jEdit 2.2final
	 */
	public interface Wrapper {
	}

	/**
	 * Macro recorder.
	 */
	public interface MacroRecorder {

		void actionPerformed(ActionListener listener, String actionCommand);
	}

	public static class Backspace implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable()) {
				textArea.getToolkit().beep();
				return;
			}

			if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
				textArea.setSelectedText("");
			} else {
				int caret = textArea.getCaretPosition();
				if (caret == 0) {
					textArea.getToolkit().beep();
					return;
				}
				try {
					textArea.getDocument().remove(caret - 1, 1);
				} catch (BadLocationException bl) {
					bl.printStackTrace();
				}
			}
		}
	}

	public static class BackspaceWord implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int start = textArea.getSelectionStart();
			if (start != textArea.getSelectionEnd()) {
				textArea.setSelectedText("");
			}

			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			int caret = start - lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if (caret == 0) {
				if (lineStart == 0) {
					textArea.getToolkit().beep();
					return;
				}
				caret--;
			} else {
				String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
				caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
			}

			try {
				textArea.getDocument().remove(caret + lineStart, start - (caret + lineStart));
			} catch (BadLocationException bl) {
				bl.printStackTrace();
			}
		}
	}

	public static class Delete implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable()) {
				textArea.getToolkit().beep();
				return;
			}

			if (textArea.getSelectionStart() != textArea.getSelectionEnd()) {
				textArea.setSelectedText("");
			} else {
				int caret = textArea.getCaretPosition();
				if (caret == textArea.getDocumentLength()) {
					textArea.getToolkit().beep();
					return;
				}
				try {
					textArea.getDocument().remove(caret, 1);
				} catch (BadLocationException bl) {
					bl.printStackTrace();
				}
			}
		}
	}

	public static class DeleteWord implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int start = textArea.getSelectionStart();
			if (start != textArea.getSelectionEnd()) {
				textArea.setSelectedText("");
			}

			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			int caret = start - lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if (caret == lineText.length()) {
				if (lineStart + caret == textArea.getDocumentLength()) {
					textArea.getToolkit().beep();
					return;
				}
				caret++;
			} else {
				String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
				caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
			}

			try {
				textArea.getDocument().remove(start, caret + lineStart - start);
			} catch (BadLocationException bl) {
				bl.printStackTrace();
			}
		}
	}

	public static class End implements ActionListener {

		private boolean select;

		public End(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			int caret = textArea.getCaretPosition();

			int lastOfLine = textArea.getLineEndOffset(textArea.getCaretLine()) - 1;
			int lastVisibleLine = textArea.getFirstLine() + textArea.getVisibleLines();
			if (lastVisibleLine >= textArea.getLineCount()) {
				lastVisibleLine = Math.min(textArea.getLineCount() - 1, lastVisibleLine);
			} else {
				lastVisibleLine -= textArea.getElectricScroll() + 1;
			}

			int lastVisible = textArea.getLineEndOffset(lastVisibleLine) - 1;
			int lastDocument = textArea.getDocumentLength();

			if (caret == lastDocument) {
				textArea.getToolkit().beep();
				return;
			} else if (!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY))) {
				caret = lastOfLine;
			} else if (caret == lastVisible) {
				caret = lastDocument;
			} else if (caret == lastOfLine) {
				caret = lastVisible;
			} else {
				caret = lastOfLine;
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
		}
	}

	public static class DocumentEnd implements ActionListener {

		private boolean select;

		public DocumentEnd(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			if (select) {
				textArea.select(textArea.getMarkPosition(), textArea.getDocumentLength());
			} else {
				textArea.setCaretPosition(textArea.getDocumentLength());
			}
		}
	}

	public static class Home implements ActionListener {

		private boolean select;

		public Home(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			int caret = textArea.getCaretPosition();

			int firstLine = textArea.getFirstLine();

			int firstOfLine = textArea.getLineStartOffset(textArea.getCaretLine());
			int firstVisibleLine = firstLine == 0 ? 0 : firstLine + textArea.getElectricScroll();
			int firstVisible = textArea.getLineStartOffset(firstVisibleLine);

			if (caret == 0) {
				textArea.getToolkit().beep();
				return;
			} else if (!Boolean.TRUE.equals(textArea.getClientProperty(SMART_HOME_END_PROPERTY))) {
				caret = firstOfLine;
			} else if (caret == firstVisible) {
				caret = 0;
			} else if (caret == firstOfLine) {
				caret = firstVisible;
			} else {
				caret = firstOfLine;
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
		}
	}

	public static class DocumentHome implements ActionListener {

		private boolean select;

		public DocumentHome(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			if (select) {
				textArea.select(textArea.getMarkPosition(), 0);
			} else {
				textArea.setCaretPosition(0);
			}
		}
	}

	public static class InsertBreak implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable()) {
				textArea.getToolkit().beep();
				return;
			}

			textArea.setSelectedText(Tools.getLineSeparator());
		}
	}

	public static class InsertTab implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);

			if (!textArea.isEditable()) {
				textArea.getToolkit().beep();
				return;
			}

			textArea.overwriteSetSelectedText("\t");
		}
	}

	public static class NextChar implements ActionListener {

		private boolean select;

		public NextChar(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if (caret == textArea.getDocumentLength()) {
				textArea.getToolkit().beep();
				return;
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), caret + 1);
			} else {
				textArea.setCaretPosition(caret + 1);
			}
		}
	}

	public static class NextLine implements ActionListener {

		private boolean select;

		public NextLine(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if (line == textArea.getLineCount() - 1) {
				textArea.getToolkit().beep();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if (magic == -1) {
				magic = textArea.offsetToX(line, caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line + 1) + textArea.xToOffset(line + 1, magic);
			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class NextPage implements ActionListener {

		private boolean select;

		public NextPage(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int lineCount = textArea.getLineCount();
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			firstLine += visibleLines;

			if (firstLine + visibleLines >= lineCount - 1) {
				firstLine = lineCount - visibleLines;
			}

			textArea.setFirstLine(firstLine);

			int caret = textArea.getLineStartOffset(Math.min(textArea.getLineCount() - 1, line + visibleLines));
			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
		}
	}

	public static class NextWord implements ActionListener {

		private boolean select;

		public NextWord(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if (caret == lineText.length()) {
				if (lineStart + caret == textArea.getDocumentLength()) {
					textArea.getToolkit().beep();
					return;
				}
				caret++;
			} else {
				String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
				caret = TextUtilities.findWordEnd(lineText, caret, noWordSep);
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), lineStart + caret);
			} else {
				textArea.setCaretPosition(lineStart + caret);
			}
		}
	}

	public static class Overwrite implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.setOverwriteEnabled(!textArea.isOverwriteEnabled());
		}
	}

	public static class PrevChar implements ActionListener {

		private boolean select;

		public PrevChar(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			if (caret == 0) {
				textArea.getToolkit().beep();
				return;
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), caret - 1);
			} else {
				textArea.setCaretPosition(caret - 1);
			}
		}
	}

	public static class PrevLine implements ActionListener {

		private boolean select;

		public PrevLine(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();

			if (line == 0) {
				textArea.getToolkit().beep();
				return;
			}

			int magic = textArea.getMagicCaretPosition();
			if (magic == -1) {
				magic = textArea.offsetToX(line, caret - textArea.getLineStartOffset(line));
			}

			caret = textArea.getLineStartOffset(line - 1) + textArea.xToOffset(line - 1, magic);
			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
			textArea.setMagicCaretPosition(magic);
		}
	}

	public static class PrevPage implements ActionListener {

		private boolean select;

		public PrevPage(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int firstLine = textArea.getFirstLine();
			int visibleLines = textArea.getVisibleLines();
			int line = textArea.getCaretLine();

			if (firstLine < visibleLines) {
				firstLine = visibleLines;
			}

			textArea.setFirstLine(firstLine - visibleLines);

			int caret = textArea.getLineStartOffset(Math.max(0, line - visibleLines));
			if (select) {
				textArea.select(textArea.getMarkPosition(), caret);
			} else {
				textArea.setCaretPosition(caret);
			}
		}
	}

	public static class PrevWord implements ActionListener {

		private boolean select;

		public PrevWord(boolean select) {
			this.select = select;
		}

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			int caret = textArea.getCaretPosition();
			int line = textArea.getCaretLine();
			int lineStart = textArea.getLineStartOffset(line);
			caret -= lineStart;

			String lineText = textArea.getLineText(textArea.getCaretLine());

			if (caret == 0) {
				if (lineStart == 0) {
					textArea.getToolkit().beep();
					return;
				}
				caret--;
			} else {
				String noWordSep = (String) textArea.getDocument().getProperty("noWordSep");
				caret = TextUtilities.findWordStart(lineText, caret, noWordSep);
			}

			if (select) {
				textArea.select(textArea.getMarkPosition(), lineStart + caret);
			} else {
				textArea.setCaretPosition(lineStart + caret);
			}
		}
	}

	public static class Repeat implements ActionListener, InputHandler.NonRecordable {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.getInputHandler().setRepeatEnabled(true);
			String actionCommand = evt.getActionCommand();
			if (actionCommand != null) {
				textArea.getInputHandler().setRepeatCount(Integer.parseInt(actionCommand));
			}
		}
	}

	public static class ToggleRect implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.setSelectionRectangular(!textArea.isSelectionRectangular());
		}
	}

	public static class insert_char implements ActionListener, InputHandler.NonRepeatable {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			String str = evt.getActionCommand();
			int repeatCount = textArea.getInputHandler().getRepeatCount();

			if (textArea.isEditable()) {
				StringBuffer buf = new StringBuffer();
				for (int i = 0; i < repeatCount; i++) {
					buf.append(str);
				}
				textArea.overwriteSetSelectedText(buf.toString());
			} else {
				textArea.getToolkit().beep();
			}
		}
	}

	public static class ClipCopy extends LoggedAbstractAction {

		private static final long serialVersionUID = 2942143617686299824L;

		public ClipCopy() {
			super("Copy");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
		}

		@Override
		public void loggedActionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.copy();
		}
	}

	public static class ClipPaste extends LoggedAbstractAction {

		private static final long serialVersionUID = 1961399179178333327L;

		public ClipPaste() {
			super("Paste");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
		}

		@Override
		public void loggedActionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.paste();
		}
	}

	public static class ClipCut extends LoggedAbstractAction {

		private static final long serialVersionUID = 8199702559544536825L;

		public ClipCut() {
			super("Cut");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
		}

		@Override
		public void loggedActionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.cut();
		}
	}

	public static class SelectAll extends LoggedAbstractAction {

		private static final long serialVersionUID = -5706102409245450060L;

		public SelectAll() {
			super("Select All");
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
		}

		@Override
		public void loggedActionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			textArea.selectAll();
		}
	}

	private static class SearchAction extends LoggedAbstractAction {

		private static final long serialVersionUID = -8380073257252178693L;

		private SearchAction(IconSize size) {
			super("Search and Replace...");
			putValue(SHORT_DESCRIPTION, "Searches and replaces text occurances.");
			putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_F));
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
		}

		@Override
		public void loggedActionPerformed(ActionEvent evt) {
			JEditTextArea textArea = getTextArea(evt);
			new SearchDialog(textArea, new SearchableJEditTextArea(textArea), true).setVisible(true);
		}
	}
}

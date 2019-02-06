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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


/**
 * This class is capable of batch line inserts which only trigger one GUI refresh and only uses a
 * single lock. This gives a significant performance boost compared to the
 * {@link DefaultStyledDocument} that refreshes the GUI each time a {@link String} is inserted via
 * {@link StyledDocument#insertString(int, String, AttributeSet)}.
 * <p>
 * Usage: Call {@link #appendLineForBatch(String, AttributeSet)} as many times as desired, then call
 * {@link #executeBatch(int)} or {@link #executeBatchAppend()} to update the document with all
 * changes at once. This will only trigger one GUI update.
 * </p>
 * <p>
 * This document is thread safe in the same way as the regular {@link StyledDocument}.
 * </p>
 *
 * @author Marco Boeck
 *
 */
public class ExtendedStyledDocument extends DefaultStyledDocument {

	private static final long serialVersionUID = 1L;

	/** name of the default font family for batch strings */
	private static final String DEFAULT_FONT_FAMILY = "SansSerif";

	/** this list contains the elements that should be inserted with the next batch update */
	private final LinkedList<ElementSpec> listToInsert;

	/** this list contains the length of each line in the batch */
	private final LinkedList<Integer> lineLength;

	/** the max number of rows per batch. If exceeded, starts discarding oldest entries for new ones */
	private int maxRows;

	private final Object LOCK = new Object();

	/**
	 * Creates an empty styled document which supports batch insertion.
	 *
	 * @param maxRows
	 *            if set to > 0, will limit the max batch row amount. Oldest entries will be
	 *            discarded for new entries.
	 */
	public ExtendedStyledDocument(int maxRows) {
		this.listToInsert = new LinkedList<>();
		this.lineLength = new LinkedList<>();
		this.maxRows = maxRows;
	}

	/**
	 * Stores the given {@link String} line for the next batch update. If the number of elements
	 * awaiting batch update are >= maxRows, will discard the oldest element. Call
	 * {@link #executeBatch(int)} or {@link #executeBatchAppend()} to execute the batch update.
	 * <p>
	 * <strong>Attention:</strong> Every {@link String} is considered as one line so a line
	 * separator will be added into the document after it.
	 * </p>
	 * <p>
	 * This method is thread safe.
	 * </p>
	 *
	 * @param str
	 *            the {@link String} to add to the document.
	 * @param a
	 *            the style formatting settings
	 */
	public void appendLineForBatch(String str, SimpleAttributeSet a) {
		if (str == null || str.isEmpty()) {
			throw new IllegalArgumentException("str must not be null or empty!");
		}
		if (!str.endsWith(System.lineSeparator())) {
			str += System.lineSeparator();
		}

		char[] txt = str.toCharArray();
		a = a != null ? (SimpleAttributeSet) a.copyAttributes() : new SimpleAttributeSet();
		// set font family if not set
		if (a.getAttribute(StyleConstants.FontFamily) == null) {
			StyleConstants.setFontFamily(a, DEFAULT_FONT_FAMILY);
		}

		synchronized (LOCK) {
			// make sure batch size does not exceed maxRows *3 (*3 because we add the str and 2 line
			// separator tags)
			if (maxRows > 0) {
				while (listToInsert.size() >= maxRows * 3) {
					// remove element itself and both line separator elements)
					// we start at the beginning because we discard oldest first
					listToInsert.removeFirst();
					listToInsert.removeFirst();
					listToInsert.removeFirst();
					lineLength.removeFirst();
				}
			}

			// close previous paragraph tag, start new one, add text
			// yes the order is correct; no you cannot change to start/text/end
			// if you do, linebreaks get messed up
			listToInsert.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.EndTagType));
			listToInsert.add(new ElementSpec(new SimpleAttributeSet(), ElementSpec.StartTagType));
			listToInsert.add(new ElementSpec(a, ElementSpec.ContentType, txt, 0, txt.length));

			// store length of each row we add
			lineLength.add(txt.length);
		}
	}

	/**
	 * Executes a batch update. This takes all previously stored {@link String}s and appends them to
	 * the document together. If no {@link String}s have been appended since the last batch update,
	 * does nothing.
	 * <p>
	 * This method is thread safe.
	 * </p>
	 *
	 * @return a list with the length of each added row. Note that this might return one more row
	 *         than added which is of length 0
	 * @throws BadLocationException
	 */
	public List<Integer> executeBatchAppend() throws BadLocationException {
		return executeBatch(getLength());
	}

	/**
	 * Executes a batch update. This takes all previously stored {@link String}s and adds them to
	 * the document together at the specified offset. If no {@link String}s have been appended since
	 * the last batch update, does nothing.
	 * <p>
	 * This method is thread safe.
	 * </p>
	 *
	 * @param offset
	 *            the offset from the beginning of the document where the batch should be added
	 * @return a list with the length of each added row. Note that this might return one more row
	 *         than added which is of length 0
	 * @throws BadLocationException
	 */
	public List<Integer> executeBatch(int offset) throws BadLocationException {
		ElementSpec[] data = null;
		List<Integer> toReturn = new LinkedList<>();
		synchronized (LOCK) {
			if (listToInsert.isEmpty()) {
				// nothing to do
				return Collections.emptyList();
			}

			data = new ElementSpec[listToInsert.size()];
			listToInsert.toArray(data);
			listToInsert.clear();

			toReturn.addAll(lineLength);
			lineLength.clear();
		}

		super.insert(offset, data);
		return toReturn;
	}

	/**
	 * Returns the size of the current batch update, i.e. how many batch elements have been added
	 * since the last batch update.
	 * <p>
	 * This method is thread safe.
	 * </p>
	 *
	 * @return the current number of added batch updates
	 */
	public int getBatchSize() {
		int size = 0;
		synchronized (LOCK) {
			// we take the linelength list because the other one contains start/end tags
			size = lineLength.size();
		}

		return size;
	}

	/**
	 * Discards the accumulated strings for a batch update. Does not change the {@link Document}.
	 * After this method has been called, {@link #isBatchUpdateReady()} will return
	 * <code>false</code>.
	 * <p>
	 * This method is thread safe.
	 * </p>
	 */
	public void clearBatch() {
		synchronized (LOCK) {
			listToInsert.clear();
			lineLength.clear();
		}
	}

	/**
	 * Sets the maximum amount of lines allowed in a batch before oldest ones get discarded for new
	 * ones.
	 *
	 * @param maxRows
	 *            if set to > 0, limits the number of batch items
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}
}

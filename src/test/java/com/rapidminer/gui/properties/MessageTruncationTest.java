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

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests {@link ExpressionPropertyDialog#truncateMessage(String[], int)}
 *
 * @author Kevin Majchrzak
 * @since 9.2.2
 */
public class MessageTruncationTest {

	@Test
	public void emptyMessage() {
		String[] message = {"", "", ""};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals(message[0], "");
		Assert.assertEquals(message[1], "");
		Assert.assertEquals(message[2], "");
	}

	@Test
	public void withoutErrorMarkerNotTruncated() {
		String[] message = {"Some terrible cause", "Some short but invalid input"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("Some short but invalid input", message[1]);
	}

	@Test
	public void withoutErrorMarkerTruncated() {
		String[] message = {"Some terrible cause", "Long invalid text that should be truncated at the end! " +
				"Long invalid text that should be truncated at the end! " +
				"Long invalid text that should be truncated at the end!"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("Long invalid text that should be truncated at the end! Long invalid text that should be truncated at the end! Long invalid t[...]", message[1]);
	}

	@Test
	public void withErrorMarkerTruncatedAtEnd() {
		String[] message = {"Some terrible cause", "Long invalid text that should be truncated at the end! " +
				"Long invalid text that should be truncated at the end! " +
				"Long invalid text that should be truncated at the end!", "     ^^^^^^^"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("Long invalid text that should be truncated at the end! Long invalid text that should be truncated at the end! Long invalid [...]", message[1]);
		Assert.assertEquals("     ^^^^^^^", message[2]); // marks the word invalid
	}


	@Test
	public void withErrorMarkerTruncatedAtBeginning() {
		String[] message = {"Some terrible cause", "Long invalid text that should be truncated at the beginning! " +
				"Long invalid text that should be truncated at the beginning! " +
				"Long invalid text that should be truncated at the beginning!", "                                                                                         " +
				"                                                                  ^^^^^^^^^"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("[...]! Long invalid text that should be truncated at the beginning! Long invalid text that should be truncated at the beginning!", message[1]);
		Assert.assertEquals("                                                                                                     ^^^^^^^^^", message[2]); // marks the word "truncated"
	}

	@Test
	public void withErrorMarkerTruncatedAtBothSides() {
		String[] message = {"Some terrible cause", "Long invalid text that should be truncated at both sides! " +
				"Long invalid text that should be truncated at both sides! " +
				"Long invalid text that should be truncated at both sides!",
				"                                                                                        ^^"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("[...]ld be truncated at both sides! Long invalid text that should be truncated at both sides! Long invalid text that should be tr[...]", message[1]);
		Assert.assertEquals("                                                                  ^^", message[2]); // marks the word "be"
	}

	@Test
	public void veryLongErrorWord() {
		String[] message = {"Some terrible cause", "Long wooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
				"oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooord that should be partly output",
				"     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("[...]wooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo[...]", message[1]);
		Assert.assertEquals("     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", message[2]); // marks the long woooord
	}

	@Test
	public void errorWordLengthMatchesMaxChars() {
		String[] message = {"Some terrible cause", "Long woooooooooooooooooooooooooooooooooooooooooooooooooooooooooo" +
				"ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooord that should exclusively be output",
				"     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"};
		ExpressionPropertyDialog.truncateMessage(message, 124);
		Assert.assertEquals("Some terrible cause", message[0]);
		Assert.assertEquals("[...]wooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooord[...]", message[1]);
		Assert.assertEquals("     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^", message[2]); // marks the long woooord
	}

}
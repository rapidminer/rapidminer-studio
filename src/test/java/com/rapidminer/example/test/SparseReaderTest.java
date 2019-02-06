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
package com.rapidminer.example.test;

import static junit.framework.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.junit.Test;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.SparseFormatDataRowReader;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeSet;


/**
 * Tests all formats of the {@link SparseFormatDataRowReader}
 *
 * @author Simon Fischer, Ingo Mierswa
 */
public class SparseReaderTest {

	private static final String[] ATTRIBUTE_STRINGS = { "5:3.0 2:8.0", "1:cat 3:2.5 4:1.5e-1", "5:1.0", "1:dog 4:7.3e1" };

	private static final String[] LABEL = { "yes", "no", "no", "yes" };

	public void readerTest(int format, Reader input, Reader labelInput) throws Exception {
		AttributeSet attributeSet = new AttributeSet();
		Attribute att1 = ExampleTestTools.attributeDogCatMouse();
		Attribute att2 = ExampleTestTools.attributeReal(1);
		Attribute att3 = ExampleTestTools.attributeReal(2);
		Attribute att4 = ExampleTestTools.attributeReal(3);
		Attribute att5 = ExampleTestTools.attributeReal(4);
		Attribute att6 = ExampleTestTools.attributeYesNo();

		attributeSet.addAttribute(att1);
		attributeSet.addAttribute(att2);
		attributeSet.addAttribute(att3);
		attributeSet.addAttribute(att4);
		attributeSet.addAttribute(att5);
		attributeSet.setSpecialAttribute("label", att6);

		java.util.Map<String, String> prefixMap = new java.util.HashMap<String, String>();
		prefixMap.put("l", "label");
		SparseFormatDataRowReader reader = new SparseFormatDataRowReader(
				new DataRowFactory(DataRowFactory.TYPE_SPARSE_MAP, '.'), format, prefixMap, attributeSet, input, labelInput,
				-1, false, '"');
		ExampleSet exampleSet = ExampleSets.from(attributeSet.getAllAttributes()).withDataRowReader(reader)
				.withRole(att6, Attributes.LABEL_NAME).build();
		Iterator<Example> r = exampleSet.iterator();
		Example e = r.next();
		assertEquals("example 1, column 1", "dog", e.getValueAsString(att1));
		assertEquals("example 1, column 2", 8.0, e.getValue(att2), 0.00000001);
		assertEquals("example 1, column 3", 0.0, e.getValue(att3), 0.00000001);
		assertEquals("example 1, column 4", 0.0, e.getValue(att4), 0.00000001);
		assertEquals("example 1, column 5", 3.0, e.getValue(att5), 0.00000001);
		assertEquals("example 1, label", "yes", e.getValueAsString(att6));

		e = r.next();
		assertEquals("example 2, column 1", "cat", e.getValueAsString(att1));
		assertEquals("example 2, column 2", 0.0, e.getValue(att2), 0.00000001);
		assertEquals("example 2, column 3", 2.5, e.getValue(att3), 0.00000001);
		assertEquals("example 2, column 4", 0.15, e.getValue(att4), 0.00000001);
		assertEquals("example 2, column 5", 0.0, e.getValue(att5), 0.00000001);
		assertEquals("example 2, label", "no", e.getValueAsString(att6));

		e = r.next();
		assertEquals("example 3, column 1", "dog", e.getValueAsString(att1));
		assertEquals("example 3, column 2", 0.0, e.getValue(att2), 0.00000001);
		assertEquals("example 3, column 3", 0.0, e.getValue(att3), 0.00000001);
		assertEquals("example 3, column 4", 0.0, e.getValue(att4), 0.00000001);
		assertEquals("example 3, column 5", 1.0, e.getValue(att5), 0.00000001);
		assertEquals("example 3, label", "no", e.getValueAsString(att6));

		e = r.next();
		assertEquals("example 4, column 1", "dog", e.getValueAsString(att1));
		assertEquals("example 4, column 2", 0.0, e.getValue(att2), 0.00000001);
		assertEquals("example 4, column 3", 0.0, e.getValue(att3), 0.00000001);
		assertEquals("example 4, column 4", 73, e.getValue(att4), 0.00000001);
		assertEquals("example 4, column 5", 0.0, e.getValue(att5), 0.00000001);
		assertEquals("example 4, label", "yes", e.getValueAsString(att6));
	}

	@Test
	public void testFormatXY() throws Exception {
		StringBuffer input = new StringBuffer("# comment" + Tools.getLineSeparator());
		for (int i = 0; i < ATTRIBUTE_STRINGS.length; i++) {
			input.append(ATTRIBUTE_STRINGS[i] + " " + LABEL[i] + Tools.getLineSeparator());
		}
		readerTest(SparseFormatDataRowReader.FORMAT_XY, new StringReader(input.toString()), null);
	}

	@Test
	public void testFormatYX() throws Exception {
		StringBuffer input = new StringBuffer("# comment" + Tools.getLineSeparator());
		for (int i = 0; i < ATTRIBUTE_STRINGS.length; i++) {
			input.append(LABEL[i] + " " + ATTRIBUTE_STRINGS[i] + Tools.getLineSeparator());
		}
		readerTest(SparseFormatDataRowReader.FORMAT_YX, new StringReader(input.toString()), null);
	}

	@Test
	public void testFormatPrefix() throws Exception {
		StringBuffer input = new StringBuffer("# comment" + Tools.getLineSeparator());
		for (int i = 0; i < ATTRIBUTE_STRINGS.length; i++) {
			input.append("l:" + LABEL[i] + " " + ATTRIBUTE_STRINGS[i] + Tools.getLineSeparator());
		}
		readerTest(SparseFormatDataRowReader.FORMAT_PREFIX, new StringReader(input.toString()), null);
	}

	@Test
	public void testFormatSeparate() throws Exception {
		StringBuffer input = new StringBuffer("# comment" + Tools.getLineSeparator());
		StringBuffer label = new StringBuffer();
		for (int i = 0; i < ATTRIBUTE_STRINGS.length; i++) {
			label.append(LABEL[i] + Tools.getLineSeparator());
			input.append(ATTRIBUTE_STRINGS[i] + Tools.getLineSeparator());
		}
		readerTest(SparseFormatDataRowReader.FORMAT_SEPARATE_FILE, new StringReader(input.toString()), new StringReader(label.toString()));
	}
}

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import com.rapidminer.parameter.ParameterTypeDateFormat;


/**
 * A simple log formatter for dates. It outputs the format "yyyy-MM-dd HH:mm:ss"
 * 
 * @author Simon Fischer
 * 
 */
public class LeanFormatter extends Formatter {

	private final DateFormat dateFormat = new SimpleDateFormat(ParameterTypeDateFormat.DATE_TIME_FORMAT_YYYY_MM_DD_HH_MM_SS);

	@Override
	public String format(LogRecord record) {
		StringBuilder b = new StringBuilder();
		b.append(dateFormat.format(new Date(record.getMillis())));
		b.append(" ");
		b.append(record.getLevel().getLocalizedName());
		b.append(": ");
		b.append(formatMessage(record));
		String className = record.getSourceClassName();
		if (className != null) {
			int dot = className.lastIndexOf('.');
			if (dot != -1) {
				className = className.substring(dot + 1);
			}
			b.append(" (").append(className).append(".").append(record.getSourceMethodName()).append("())");
		}
		b.append("\n");
		append(record.getThrown(), b);
		return b.toString();
	}

	private void append(Throwable t, StringBuilder b) {
		if (t != null) {
			b.append("  ").append(t.toString()).append("\n");
			for (StackTraceElement elem : t.getStackTrace()) {
				b.append("      " + elem.toString()).append("\n");
			}
			if (t.getCause() != null) {
				b.append("Caused by:\n");
				append(t.getCause(), b);
			}
		}
	}
}

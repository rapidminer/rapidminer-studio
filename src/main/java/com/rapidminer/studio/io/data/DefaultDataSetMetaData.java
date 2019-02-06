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
package com.rapidminer.studio.io.data;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.ColumnMetaData.ColumnType;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.tools.Tools;


/**
 * A simple implementation of the {@link DataSetMetaData} interface.
 *
 * @author Nils Woehler, Gisa Schaefer
 * @since 7.0.0
 */
public class DefaultDataSetMetaData implements DataSetMetaData {

	private final List<ColumnMetaData> columnList;
	private ThreadLocal<DateFormat> dateFormat;
	private boolean isFaultTolerant;

	/**
	 * Creates a {@link DataSetMetaData} from the given names and types, which must have the same
	 * size. All columns will have the default role ({@code null}). To add a role to a column use
	 * {@code getColumnMetaData(index).setRole()}.
	 *
	 * @param names
	 *            a list of column names
	 * @param types
	 *            a list of column types
	 * @throws IllegalArgumentException
	 *             if the two lists do not have the same size
	 */
	public DefaultDataSetMetaData(List<String> names, List<ColumnType> types) {
		int listSize = names.size();
		if (listSize != types.size()) {
			throw new IllegalArgumentException("names and types lists must have the same size");
		}
		columnList = new ArrayList<>(listSize);
		for (int i = 0; i < listSize; i++) {
			columnList.add(new DefaultColumnMetaData(names.get(i), types.get(i)));
		}

	}

	/**
	 * Copy constructor.
	 *
	 * @param other
	 *            the instance to copy.
	 */
	private DefaultDataSetMetaData(DefaultDataSetMetaData other) {
		columnList = new ArrayList<>(other.getColumnMetaData().size());
		this.configure(other);
	}

	@Override
	public DateFormat getDateFormat() {
		if (dateFormat == null) {
			this.dateFormat = ThreadLocal.withInitial(Tools.DATE_TIME_FORMAT::get);
		}
		return this.dateFormat.get();
	}

	@Override
	public void setDateFormat(final DateFormat dateFormat) {
		this.dateFormat = ThreadLocal.withInitial(() -> dateFormat);
	}

	@Override
	public DataSetMetaData copy() {
		return new DefaultDataSetMetaData(this);
	}

	@Override
	public void configure(DataSetMetaData other) {
		this.columnList.clear();
		for (ColumnMetaData column : other.getColumnMetaData()) {
			columnList.add(new DefaultColumnMetaData(column));
		}
		DateFormat otherDateFormat = other.getDateFormat();
		this.setDateFormat(otherDateFormat != null ? (DateFormat) otherDateFormat.clone() : null);
		this.setFaultTolerant(other.isFaultTolerant());
	}

	@Override
	public List<ColumnMetaData> getColumnMetaData() {
		return columnList;
	}

	@Override
	public ColumnMetaData getColumnMetaData(int index) {
		return columnList.get(index);
	}

	@Override
	public boolean isFaultTolerant() {
		return isFaultTolerant;
	}

	@Override
	public void setFaultTolerant(boolean faultTolerant) {
		this.isFaultTolerant = faultTolerant;
	}
}

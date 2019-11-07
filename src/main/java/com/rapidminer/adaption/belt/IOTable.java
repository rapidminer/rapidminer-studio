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
package com.rapidminer.adaption.belt;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.util.List;
import java.util.Objects;

import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.operator.ResultObjectAdapter;
import com.rapidminer.tools.Tools;


/**
 * Wraps a {@link Table} into an {@link com.rapidminer.operator.IOObject} so that it can be returned at and retrieved
 * from ports.
 *
 * Please note that this class is not part of any public API and might be modified or removed in future releases without
 * prior warning.
 *
 * @author Gisa Meier
 * @since 9.0.0
 */
public final class IOTable extends ResultObjectAdapter {

	private static final long serialVersionUID = -6999955402797722996L;

	/**
	 * {@link Table} is not serializable, but we replace it by an example set on serialization anyway, see
	 * {@link #writeReplace()}.
	 */
	private final transient Table table;

	/**
	 * Creates a new IOTable that wraps a {@link Table}.
	 *
	 * @param table
	 * 		the table to wrap
	 * @throws NullPointerException
	 * 		if table is {@code null}
	 */
	public IOTable(Table table) {
		this.table = Objects.requireNonNull(table, "Table must not be null");
	}

	/**
	 * @return the wrapped {@link Table}
	 */
	public Table getTable() {
		return table;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder(this.getClass().getSimpleName() + ":" + Tools.getLineSeparator());
		str.append(table.height()).append(" examples,").append(Tools.getLineSeparator());

		List<String> withRoles = table.select().withMetaData(ColumnRole.class).labels();
		str.append(table.width() - withRoles.size()).append(" regular attributes,").append(Tools.getLineSeparator());

		boolean first = true;
		for (String label : withRoles) {
			if (first) {
				str.append("special attributes = {").append(Tools.getLineSeparator());
				first = false;
			} else {
				str.append(',');
			}
			str.append("    ").append(BeltConverter.convertRole(table, label)).append(" = ").append(label)
					.append(Tools.getLineSeparator());
		}

		if (!first) {
			str.append("}");
		} else {
			str.append("no special attributes").append(Tools.getLineSeparator());
		}

		return str.toString();
	}

	@Override
	public String toResultString() {
		return table.toString();
	}

	@Override
	public String getName() {
		return "DataTable";
	}

	/**
	 * When serializing, convert to an example set.
	 */
	private Object writeReplace() throws ObjectStreamException {
		try {
			return BeltConverter.convertSequentially(this);
		} catch (BeltConverter.ConversionException e) {
			throw new InvalidObjectException("Custom column " + e.getColumnName()
					+ " of type " + e.getType().customTypeID() + " not serializable");
		}
	}

}
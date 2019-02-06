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
package com.rapidminer.operator.nio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.IntStream;

import com.rapidminer.core.io.data.ColumnMetaData;
import com.rapidminer.core.io.data.DataSet;
import com.rapidminer.core.io.data.DataSetException;
import com.rapidminer.core.io.data.DataSetMetaData;
import com.rapidminer.core.io.data.DataSetRow;
import com.rapidminer.operator.nio.model.CSVResultSet;


/**
 * Detects if a Excel DateTime is either a Date, Time or just a DateTime
 *
 * @author Jonas Wilms-Pfau
 * @since 9.1.0
 */
public final class ExcelDateTimeTypeGuesser {

	private static final int HAS_DATE = 0x01;
	private static final int HAS_TIME = 0x02;
	private static final int DATETIME = HAS_DATE | HAS_TIME;
	/** XLS time is stored on the 30.12.1899 */
	private static final long TIME_1900_XLS = LocalDate.of(1899, 12, 30).getLong(ChronoField.EPOCH_DAY);
	/** XLSX time is stored on the 31.12.1899 */
	private static final long TIME_1900_XLSX = TIME_1900_XLS + 1;
	/** 1904 times are stored on the 1.1.1904 for both XLS and XLSX */
	private static final long TIME_1904 = LocalDate.ofYearDay(1904, 1).getLong(ChronoField.EPOCH_DAY);
	/** Dates have no time */
	private static final long DATE_MICRO_OF_DAY = 0;

	private ExcelDateTimeTypeGuesser() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Determines if the DateTime columns of the given DataSet are actually Date or Time columns
	 *
	 * @param data The DataSet
	 * @param metaData The original MetaData
	 * @return The adjusted MetaData
	 * @throws DataSetException in case the data could not be loaded
	 */
	public static DataSetMetaData guessDateTimeColumnType(DataSet data, DataSetMetaData metaData) throws DataSetException {
		DataSetMetaData copiedMetaData = metaData.copy();
		int[] dateColumns = IntStream.range(0, copiedMetaData.getColumnMetaData().size()).filter(c -> copiedMetaData.getColumnMetaData().get(c).getType() == ColumnMetaData.ColumnType.DATETIME).toArray();
		ColumnMetaData.ColumnType[] dateMetaData = guessDateTimeColumnType(data, dateColumns);
		for (int dateColumn : dateColumns) {
			copiedMetaData.getColumnMetaData(dateColumn).setType(dateMetaData[dateColumn]);
		}
		return copiedMetaData;
	}

	/**
	 * Checks if the given dateTimeColumns are ColumnType.DATE, ColumnType.TIME or ColumnType.DATETIME
	 *
	 * @param data
	 * 		the data
	 * @param dateTimeColumns
	 * 		index of datetime columns to predict
	 * @return array containing ColumnType.DATE, ColumnType.TIME or ColumnType.DATETIME for each column
	 * @throws DataSetException in case the data could not be loaded
	 */
	public static ColumnMetaData.ColumnType[] guessDateTimeColumnType(DataSet data, int... dateTimeColumns) throws DataSetException {
		int initialIndex = data.getCurrentRowIndex();
		data.reset();
		int[] type = new int[data.getNumberOfColumns()];
		for (int lineCount = 0; lineCount < CSVResultSet.LINES_FOR_GUESSING && data.hasNext(); lineCount++) {
			DataSetRow row = data.nextRow();
			for (int col : dateTimeColumns) {
				if (type[col] == DATETIME || col >= data.getNumberOfColumns() || row.isMissing(col)) {
					continue;
				}
				try {
					type[col] |= guessDateTimeType(row.getDate(col), type[col]);
				} catch (Exception e) {
					// do nothing
				}
			}
		}
		// Rewind iterator to initial position
		data.reset();
		while (data.getCurrentRowIndex() < initialIndex && data.hasNext()) {
			data.nextRow();
		}
		return Arrays.stream(type).mapToObj(ExcelDateTimeTypeGuesser::toColumnType).toArray(ColumnMetaData.ColumnType[]::new);
	}

	/**
	 * Guess the type for the given date
	 *
	 * @param date the date
	 * @param type the currently guessed type
	 * @return the guessed type
	 */
	private static int guessDateTimeType(Date date, int type) {
		LocalDateTime localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

		// Check if it has a time
		if (type != HAS_TIME && localDate.getLong(ChronoField.MICRO_OF_DAY) != DATE_MICRO_OF_DAY) {
			type |= HAS_TIME;
		}

		// Check if it has a date
		if (type != HAS_DATE) {
			long day = localDate.getLong(ChronoField.EPOCH_DAY);
			if (day != TIME_1900_XLSX && day != TIME_1900_XLS && day != TIME_1904) {
				type |= HAS_DATE;
			}
		}
		return type;
	}

	/**
	 * Converts an internal type to an ColumnType
	 *
	 * @param type
	 * 		the internal type HAS_DATE, HAS_TIME or DATE_TIME
	 * @return a ColumnType
	 */
	private static ColumnMetaData.ColumnType toColumnType(int type) {
		switch (type) {
			case HAS_TIME:
				return ColumnMetaData.ColumnType.TIME;
			case HAS_DATE:
				return ColumnMetaData.ColumnType.DATE;
			default:
				return ColumnMetaData.ColumnType.DATETIME;
		}
	}
}
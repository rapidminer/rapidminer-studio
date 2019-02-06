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
package com.rapidminer.example.table;

import com.rapidminer.example.Attribute;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.att.AttributeDataSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;


/**
 * <p>
 * FileDataRowReader implements a DataRowReader that reads DataRows from a file. This is the main
 * data reader for many file formats (including csv) and is used by the ExampleSource operator and
 * the attribute editor.
 * </p>
 * 
 * <p>
 * This class supports the reading of data from multiple source files. Each attribute (including
 * special attributes like labels, weights, ...) might be read from another file. Please note that
 * only the minimum number of lines of all files will be read, i.e. if one of the data source files
 * has less lines than the others, only this number of data rows will be read.
 * </p>
 * 
 * <p>
 * The split points can be defined with regular expressions (please refer to the Java API). Quoting
 * is possible but not suggested since the runtime is higher. The user should ensure that the split
 * characters are not included in the data columns. Please refer to {@link RapidMinerLineReader} for
 * further information.
 * </p>
 * 
 * <p>
 * Unknown attribute values can be marked with empty strings or &quot;?&quot;.
 * </p>
 * 
 * @author Ingo Mierswa Exp $
 */
public class FileDataRowReader extends AbstractDataRowReader {

	private static final int FILE_NR = 0;

	private static final int COLUMN_NR = 1;

	/** The file readers. */
	private BufferedReader[] fileReader;

	/** The attribute descriptions. */
	private Attribute[] attributes;

	/** Remember if an end of file has occured. */
	private boolean eof;

	/** Remember if a line has already been read. */
	private boolean lineRead;

	/** The sample ratio. */
	private double sampleRatio = 1.0d;

	/** The maximum number of examples to read (sampling). */
	private int maxNumber = -1;

	/** The number of lines read so far (i.e. the number of examples). */
	private int linesRead = 0;

	/**
	 * This array hold the current data. The first dimension is used for distinguishing different
	 * sources and the second for data read from the corresponding source.
	 */
	private String[][] currentData;

	/**
	 * This array holds the information how many columns each data source should provide. Otherwise
	 * an IOException will be thrown. This information is only used for checks and error
	 * improvement.
	 */
	private int[] expectedNumberOfColumns;

	/** This reader maps lines read from a file to RapidMiner columns. */
	private RapidMinerLineReader rapidMinerLineReader;

	/** The random generator used for sampling. */
	private RandomGenerator random;

	/**
	 * Array of size [number of attributes][2]. For each attribute i the value of
	 * dataSourceIndex[i][FILE_NR] is used as an index to {@link #fileReader} and the value of
	 * dataSourceIndex[i][TOKEN_NR] specifies the index of the column to use for attribute i.
	 */
	private int[][] dataSourceIndex;

	/**
	 * Constructs a new FileDataRowReader.
	 * 
	 * @param factory
	 *            Factory used to create data rows.
	 * @param attributeDataSources
	 *            List of {@link AttributeDataSource}s.
	 * @param sampleRatio
	 *            the ratio of examples which will be read. Only used if sampleSize is -1.
	 * @param sampleSize
	 *            Limit sample to the first sampleSize lines read from files. -1 for no limit, then
	 *            the sampleRatio will be used.
	 * @param separatorsRegExpr
	 *            a regular expression describing the separator characters for the columns of each
	 *            line
	 * @param commentChars
	 *            defines which characters are used to comment the rest of a line
	 * @param useQuotes
	 *            indicates if quotes should be used and parsed. Slows down reading and should be
	 *            avoided if possible
	 * @param random
	 *            the random generator used for sampling
	 */
	public FileDataRowReader(DataRowFactory factory, List<AttributeDataSource> attributeDataSources, double sampleRatio,
			int sampleSize, String separatorsRegExpr, char[] commentChars, boolean useQuotes, char quoteChar,
			char escapeChar, boolean trimLines, boolean skipErrorLines, Charset encoding, RandomGenerator random)
			throws IOException {
		super(factory);
		this.sampleRatio = sampleRatio;
		this.maxNumber = sampleSize;
		this.attributes = new Attribute[attributeDataSources.size()];
		this.dataSourceIndex = new int[attributeDataSources.size()][2];
		this.rapidMinerLineReader = new RapidMinerLineReader(separatorsRegExpr, commentChars, useQuotes, quoteChar,
				escapeChar, trimLines, skipErrorLines);
		this.random = random;
		initReader(factory, attributeDataSources, sampleSize, separatorsRegExpr, useQuotes, encoding);
	}

	/** Read the complete data. */
	private void initReader(DataRowFactory factory, List<AttributeDataSource> attributeDataSources, int sampleSize,
			String separatorsRegExpr, boolean useQuotes, Charset encoding) throws IOException {
		// map all files used to indices
		List<BufferedReader> readerList = new LinkedList<BufferedReader>();
		Map<File, Integer> fileMap = new HashMap<File, Integer>();
		Iterator<AttributeDataSource> i = attributeDataSources.iterator();
		int attribute = 0;
		int greatestFileIndex = -1;
		List<AtomicInteger> columnCounters = new ArrayList<AtomicInteger>();
		while (i.hasNext()) {
			AttributeDataSource ads = i.next();
			attributes[attribute] = ads.getAttribute();
			File file = ads.getFile();
			Integer fileIndex = fileMap.get(file);
			// new file found? -> create reader and map to index number
			if (fileIndex == null) {
				fileIndex = Integer.valueOf(++greatestFileIndex);
				fileMap.put(file, fileIndex);
				readerList.add(Tools.getReader(file, encoding));
				columnCounters.add(new AtomicInteger(1));
			} else {
				AtomicInteger counter = columnCounters.get(fileIndex.intValue());
				counter.incrementAndGet();
			}
			dataSourceIndex[attribute][FILE_NR] = fileIndex.intValue();
			dataSourceIndex[attribute][COLUMN_NR] = ads.getColumn();
			attribute++;
		}

		this.fileReader = new BufferedReader[readerList.size()];
		readerList.toArray(this.fileReader);
		currentData = new String[this.fileReader.length][];

		// create counters
		expectedNumberOfColumns = new int[columnCounters.size()];
		Iterator<AtomicInteger> j = columnCounters.iterator();
		int k = 0;
		while (j.hasNext()) {
			expectedNumberOfColumns[k++] = j.next().intValue();
		}
	}

	/** Skips the next line, if present. */
	public void skipLine() {
		try {
			readLine();
		} catch (Exception e) {
			// LogService.getGlobal().log("Problem during skipping of line: " + e.getMessage(),
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.example.table.FileDataRowReader.problem_during_skipping_of_line", e.getMessage());
		}
	}

	/**
	 * Reads a line of data from all file readers. Returns true if the line was readable, i.e. the
	 * end of the source files was not yet reached.
	 */
	private boolean readLine() throws IOException {
		boolean eofReached = false;
		boolean ok = false;
		while (!ok) {
			for (int i = 0; i < fileReader.length; i++) {
				currentData[i] = rapidMinerLineReader.readLine(fileReader[i], expectedNumberOfColumns[i]);
				if (currentData[i] == null) {
					eofReached = true;
					break;
				}
			}
			if ((eofReached) || (maxNumber != -1) || (sampleRatio == 1.0d) || (random.nextDouble() < sampleRatio)) {
				ok = true;
			}
		}
		if (eofReached) {
			for (int i = 0; i < fileReader.length; i++) {
				fileReader[i].close();
			}
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Checks if another line exists and reads. The next line is only read once even if this method
	 * is invoked more than once.
	 */
	@Override
	public boolean hasNext() {
		if ((maxNumber > -1) && (linesRead >= maxNumber)) {
			return false;
		}

		if (lineRead) {
			return !eof;
		}

		try {
			eof = !readLine();
		} catch (IOException e) {
			LogService.getRoot().severe(e.getMessage());
			return false;
		}
		lineRead = true;

		return !eof;
	}

	/** Returns the next Example. */
	@Override
	public DataRow next() {
		if (eof == true) {
			return null;
		}
		if (!lineRead) {
			if (!hasNext()) {
				return null;
			}
		}

		String[] data = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			if (dataSourceIndex[i][1] == -1) {
				data[i] = null;
			} else {
				data[i] = currentData[dataSourceIndex[i][0]][dataSourceIndex[i][1]];
			}
		}

		DataRow dataRow = getFactory().create(data, attributes);
		linesRead++;
		lineRead = false;
		return dataRow;
	}
}

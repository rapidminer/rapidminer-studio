/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * http://www.gnu.org/licenses/.
 */
package com.rapidminer.search;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;


/**
 * Utility class for using Global Search features.
 *
 * @author Marco Boeck
 * @since 8.1
 */
@SuppressWarnings("unused")
public enum GlobalSearchUtilities {

	INSTANCE;


	/**
	 * name of the {@link StringField} each document <strong>MUST</strong> have which
	 * represents the name/title/header of a document.
	 */
	public static final String FIELD_NAME = "name";

	/**
	 * name of the {@link StringField} each document <strong>MUST</strong> have which also
	 * must be unique across the entire category
	 */
	public static final String FIELD_UNIQUE_ID = "id";

	/**
	 * name of the category {@link org.apache.lucene.document.StringField} each document is given automatically
	 */
	public static final String FIELD_CATEGORY = "category";

	/**
	 * name of the optional {@link org.apache.lucene.document.SortedNumericDocValuesField} a document can have which is used to sort search results.
	 * The field must be numeric and contain long values!
	 */
	public static final String FIELD_SORTING = "sort";

	/** wildcard character in lucene */
	public static final String QUERY_WILDCARD = "*";

	/** searching for a field in lucene works by using fieldname followed by this delimiter and then the search term */
	public static final String QUERY_FIELD_SPECIFIER = ":";

	/** default boost of the name field in search results */
	public static final float DEFAULT_NAME_FIELD_BOOST = 1f;

	/** default boost of other fields in search results */
	public static final float DEFAULT_OTHER_FIELD_BOOST = 0.5f;

	/**
	 * The analyzer that should be used for indexing and searching.
	 * It uses a per-field analyzer where the {@link #FIELD_NAME} is analyzed without stop words, while all other fields are analyzed with stop words.
	 */
	public static final Analyzer ANALYZER;

	static {
		Map<String, Analyzer> analyzerMap = new HashMap<>();
		analyzerMap.put(FIELD_NAME, new StandardAnalyzer(CharArraySet.EMPTY_SET));

		ANALYZER = new PerFieldAnalyzerWrapper(new StandardAnalyzer(), analyzerMap);
	}


	private static final String PATH_REPLACEMENT = "REPOSITORYPATHDELIMITER";
	private static final String WHITESPACE_REPLACEMENT = "WHITESPACEINPATH";
	private static final String PLUS_REPLACEMENT = "PLUS";
	private static final String MINUS_REPLACEMENT = "MINUS";
	private static final String AND_REPLACEMENT = "AMPERSAND";
	private static final String EXCLAMATION_MARK_REPLACEMENT = "EXCLAMATIONMARK";
	private static final String BRACKET_OPEN_REPLACEMENT = "BRACKETOPEN";
	private static final String BRACKET_CLOSE_REPLACEMENT = "BRACKETCLOSE";
	private static final String BRACKET_EDGED_OPEN_REPLACEMENT = "BRACKETEDGEDOPEN";
	private static final String BRACKET_EDGED_CLOSED_REPLACEMENT = "BRACKETEDGEDCLOSE";
	private static final String BRACKET_CURLY_OPEN_REPLACEMENT = "BRACKETCURLYOPEN";
	private static final String BRACKET_CURLY_CLOSED_REPLACEMENT = "BRACKETCURLYCLOSE";
	private static final String POWER_REPLACEMENT = "POWER";
	private static final String TILDE_REPLACEMENT = "TILDE";

	/** key is what will be replaced, value is the replacement */
	private static final Map<String, String> PATH_REPLACEMENT_MAP ;

	static {
		PATH_REPLACEMENT_MAP = new HashMap<>();

		// add things that need to be escaped from repository paths for the Global Search to not interfere with Lucene syntax
		PATH_REPLACEMENT_MAP.put("\\/", PATH_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\s", WHITESPACE_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\+", PLUS_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\-", MINUS_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\&", AND_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\!", EXCLAMATION_MARK_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\(", BRACKET_OPEN_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\)", BRACKET_CLOSE_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\[", BRACKET_EDGED_OPEN_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\]", BRACKET_EDGED_CLOSED_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\{", BRACKET_CURLY_OPEN_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\}", BRACKET_CURLY_CLOSED_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("\\^", POWER_REPLACEMENT);
		PATH_REPLACEMENT_MAP.put("~", TILDE_REPLACEMENT);
	}

	private final FieldType titleFieldType;
	private final FieldType textFieldType;


	GlobalSearchUtilities() {
		titleFieldType = new FieldType();
		titleFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		titleFieldType.setStored(true);
		titleFieldType.setTokenized(true);

		textFieldType = new FieldType();
		textFieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		textFieldType.setStored(true);
		textFieldType.setTokenized(true);
		textFieldType.setStoreTermVectors(true);
		textFieldType.setStoreTermVectorPositions(true);
		textFieldType.setStoreTermVectorOffsets(true);
		textFieldType.setStoreTermVectorPayloads(true);
	}

	/**
	 * Creates a {@link Document} instance in a way that can be used by the Global Search {@link GlobalSearchHandler} while you
	 * can also supply all the advanced fields you deem necessary. <p> See utility methods in this class to create
	 * fields: <ul> <li>{@link #createFieldForTitles(String, String)}</li> <li>{@link #createFieldForTexts(String,
	 * String)}</li> </ul> </p>
	 *
	 * @param uniqueId
	 * 		this is a unique id across the search category. To later access it again, call {@link Document#get(String)}
	 * 		with {@link #FIELD_UNIQUE_ID} as parameter.
	 * @param title
	 * 		this is the title/name/header of a document
	 * @param fields
	 * 		Optional fields to add further search information, e.g. tags, timestamps, etc
	 * @return the document instance, never {@code null}
	 */
	public Document createDocument(final String uniqueId, final String title, final Field... fields) {
		Document doc = new Document();
		doc.add(createFieldForIdentifiers(FIELD_UNIQUE_ID, uniqueId));
		doc.add(createFieldForTitles(FIELD_NAME, title));
		for (Field field : fields) {
			doc.add(field);
		}

		return doc;
	}

	/**
	 * Tries to get the id of the {@link GlobalSearchCategory} from which this {@link Document} originates.
	 *
	 * @param document
	 * 		the document for which the category id should be returned
	 * @return the category id or {@code null} if it does not belong to any known category or to none at all
	 */
	public String getSearchCategoryIdForDocument(final Document document) {
		if (document == null) {
			throw new IllegalArgumentException("document must not be null!");
		}

		IndexableField categoryField = document.getField(FIELD_CATEGORY);
		if (categoryField != null) {
			String categoryId = categoryField.stringValue();
			if (GlobalSearchRegistry.INSTANCE.isSearchCategoryRegistered(categoryId)) {
				return categoryId;
			}
		}

		return null;
	}

	/**
	 * Creates a search document {@link Field} for very short strings like titles, names, etc.
	 *
	 * @param key the name of the field, e.g. {@link #FIELD_NAME}
	 * @param value
	 * 		the value string, e.g. a title
	 * @return the field, never {@code null}
	 */
	public Field createFieldForTitles(final String key, final String value) {
		return new Field(key, value, titleFieldType);
	}

	/**
	 * Creates a search document {@link Field} for longer search strings like texts, descriptions, etc.
	 *
	 * @param key the name of the field, e.g. {@code content}
	 * @param value
	 * 		the value string, e.g. a description text
	 * @return the field, never {@code null}
	 */
	public Field createFieldForTexts(final String key, final String value) {
		return new Field(key, value, textFieldType);
	}

	/**
	 * Creates a search document {@link Field} for date values (ms since epoch).
	 *
	 * @param key
	 * 		the name of the field, e.g. {@code modified}
	 * @param value
	 * 		the ms since epoch
	 * @return the field, never {@code null}
	 */
	public Field createFieldForDateValues(final String key, final long value) {
		return createFieldForIdentifiers(key, DateTools.timeToString(value, DateTools.Resolution.DAY));
	}

	/**
	 * Creates a search document {@link Field} for identifier strings like tags, location, etc.
	 *
	 * @param key the name of the field, e.g. {@code location}
	 * @param value
	 * 		the value string, e.g. a description text
	 * @return the field, never {@code null}
	 */
	public Field createFieldForIdentifiers(final String key, final String value) {
		return new StringField(key, value, Field.Store.YES);
	}

	/**
	 * Creates a search document {@link Field} for binary data that should be stored but not searched. This can then later be retrieved again.
	 *
	 * @param key the name of the field, e.g. {@code data}
	 * @param bytes
	 * 		the byte array
	 * @return the field, never {@code null}
	 */
	public Field createFieldForBinary(final String key, final byte[] bytes) {
		return new StoredField(key, bytes);
	}

	/**
	 * Creates a search document {@link Field} for sorting with long values.
	 *
	 * @param value
	 * 		the long value. Higher values are sorted higher than lower values
	 * @return the field which is used for sorting, never {@code null}
	 */
	public Field createSortingField(final long value) {
		return new SortedNumericDocValuesField(FIELD_SORTING, value);
	}

	/**
	 * Encodes a repository path (see {@link com.rapidminer.repository.RepositoryLocation}) for searching.
	 * This means that {@link com.rapidminer.repository.RepositoryLocation#REPOSITORY_PREFIX} and
	 * {@link com.rapidminer.repository.RepositoryLocation#SEPARATOR} will be replaced by a string and all whitespaces will be replaced by another string.
	 *
	 * @param path
	 * 		the full repository path
	 * @return the full repository path where path seperators and whitespaces have been replaced with a specific string
	 */
	public String encodeRepositoryPath(final String path) {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null!");
		}

		String modifiedPath = path;
		for (Map.Entry<String, String> replacement : PATH_REPLACEMENT_MAP.entrySet()) {
			modifiedPath = modifiedPath.replaceAll(replacement.getKey(), replacement.getValue());
		}
		return modifiedPath;
	}

	/**
	 * Encodes a query string so special syntax characters used by Lucene are encoded.
	 *
	 * @param query
	 * 		the query string to encode
	 * @return the encoded string, ready to be used by Lucene as a query string
	 */
	public String escapeQueryString(final String query) {
		if (query == null) {
			throw new IllegalArgumentException("query must not be null!");
		}

		return QueryParser.escape(query);
	}
}

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
package com.rapidminer.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.search.highlight.TokenSources;

import com.rapidminer.tools.LogService;


/**
 * The class to run search queries for the Global Search feature against. Use the {@link GlobalSearchResultBuilder} to setup the
 * search and then run it via {@link GlobalSearchResultBuilder#runSearch()}.
 * <p>
 * To register a {@link GlobalSearchable} and thus make it available for searching, use {@link GlobalSearchRegistry#registerSearchCategory(GlobalSearchable)}.
 * </p>
 *
 * @author Marco Boeck
 * @since 8.1
 */
enum GlobalSearchHandler {

	INSTANCE;


	/**
	 * name of the {@link org.apache.lucene.document.StringField} each document gets added which is unique across the
	 * entire application.
	 */
	protected static final String FIELD_INTERNAL_UNIQUE_ID = "internal_unique_id";

	private static final Formatter HIGHLIGHT_FORMATTER = new SimpleHTMLFormatter("<font color=#ff6600>", "</font>");
	private static final int MAX_NUMBER_OF_FRAGMENTS = 5;
	private static final int FRAGMENT_SIZE = 10;

	/**
	 * special characters/sequences: if they occur in a query, advanced functionality was used and we cannot simplify the query anymore
	 */
	private static final String[] SPECIAL_CHARACTERS = new String[]{GlobalSearchUtilities.QUERY_WILDCARD, "AND", "OR", "NOT", "+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "?", "\\"};

	private static final Sort SORT = new Sort(new SortedNumericSortField(GlobalSearchUtilities.FIELD_SORTING, SortField.Type.LONG, true), SortField.FIELD_SCORE);
	private static final GlobalSearchDefaultField DEFAULT_SEARCH_FIELD = new GlobalSearchDefaultField(GlobalSearchUtilities.FIELD_NAME, GlobalSearchUtilities.DEFAULT_NAME_FIELD_BOOST);


	/**
	 * Executes the search with the given parameters.
	 *
	 * @param searchQueryString
	 * 		the search query string, must neither be {@code null} nor empty
	 * @param categories
	 * 		<i>Optional</i>. If {@code null} or not specified, all registered search categories are included in the
	 * 		search.
	 * @param simpleMode
	 * 		if {@code true}, will try to make the query automatically user-friendly; {@code false} will not make any changes to the query
	 * @param maxNumberOfResults
	 * 		the maximum number of results
	 * @param moreResults
	 * 		the number of optional additional search results. Will add these results if there are no additional results.
	 * 	 	Will not be used if parameter 'after' is set.
	 * @param highlightResult
	 * 		if {@code true}, the {@link GlobalSearchResult#getBestFragments()} will be created
	 * @param after
	 * 		<i>Optional</i>. If not {@code null}, then the search results are retrieved from ranks lower than the given
	 * 		{@link ScoreDoc}.
	 * @return the search result, never {@code null}
	 * @throws ParseException
	 * 		if the searchQuery was invalid
	 */
	protected GlobalSearchResult search(final String searchQueryString, final List<GlobalSearchCategory> categories, final boolean simpleMode, final int maxNumberOfResults, final int moreResults, final boolean highlightResult, final ScoreDoc after) throws ParseException {
		if (searchQueryString == null || searchQueryString.trim().isEmpty()) {
			return GlobalSearchResult.createEmptyResult(searchQueryString);
		}
		// build the query based on the default search fields, which can be specified in the search string anyway though
		try (IndexReader reader = GlobalSearchIndexer.INSTANCE.createIndexReader()) {
			IndexSearcher searcher = new IndexSearcher(reader);
			TopDocs result;
			Query finalQuery;

			Query parsedQuery = parseDefaultQuery(searchQueryString, categories, simpleMode);
			if (categories == null) {
				finalQuery = parsedQuery;
			} else {
				// categories are restricted, create query for that and combine with original query via AND
				BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
				Query categoryQuery = createCategoryQuery(categories);
				finalQuery = booleanQueryBuilder.add(parsedQuery, BooleanClause.Occur.MUST).add(categoryQuery, BooleanClause.Occur.MUST).build();
			}

			// sort by the sort field, if provided
			if (after == null) {
				if (moreResults > 0 && (searcher.count(finalQuery) <= maxNumberOfResults + moreResults)) {
					result = searcher.search(finalQuery, maxNumberOfResults + moreResults, SORT, true, false);
				} else {
					result = searcher.search(finalQuery, maxNumberOfResults, SORT, true, false);
				}
			} else {
				result = searcher.searchAfter(after, finalQuery, maxNumberOfResults, SORT, true, false);
			}

			return createSearchResult(searchQueryString, parsedQuery, searcher, result, highlightResult);
		} catch (ParseException e) {
			// re-throw parse exceptions
			throw e;
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "com.rapidminer.global_search.searchhandler.search_failed", e);
			return GlobalSearchResult.createEmptyResult(searchQueryString);
		}
	}


	/**
	 * Parses the given search query for the given search categories and returns a {@link Query} object.
	 *
	 * @param searchString
	 * 		the search query string, must not be {@code null} or empty
	 * @param searchCategories
	 * 		the search categories to search in. If {@code null}, all search categories are searched
	 * @param simpleMode
	 * 		if {@code true}, will try to make the query automatically user-friendly (e.g. by adding wildcards after words, etc); {@code false} will not make any changes to the query.
	 * 		Note that if a simplified query cannot be parsed, the original (non-simplified) query will be created.
	 * @return the query, never {@code null}
	 * @throws ParseException
	 * 		if the search string cannot be parsed
	 */
	private Query parseDefaultQuery(final String searchString, final List<GlobalSearchCategory> searchCategories, final boolean simpleMode) throws ParseException {
		String newSearchString = searchString;

		// can the query be simplified? If so, do it
		boolean simplified = false;
		if (simpleMode) {
			StringBuilder result = new StringBuilder();
			simplified = simplifySearchString(searchString, result);
			newSearchString = result.toString();
		}

		Collection<GlobalSearchDefaultField> fieldSet = new HashSet<>();

		fieldSet.add(DEFAULT_SEARCH_FIELD);
		// get all fields which should be queried in addition to the default name field
		List<GlobalSearchCategory> categoryList = searchCategories;
		if (categoryList == null) {
			categoryList = GlobalSearchRegistry.INSTANCE.getAllSearchCategories();
		}

		for (GlobalSearchCategory category : categoryList) {
			fieldSet.addAll(category.getManager().getAdditionalDefaultSearchFields());
		}

		try {
			// if we have one query, then just return it, otherwise it was a multi-field query, construct Boolean query for them
			// we don't use MultiFieldQueryParser here because that cannot be boosted for our PrefixQueries
			List<Query> queries = buildSubQueries(newSearchString, fieldSet, simpleMode);
			if (queries.size() == 1) {
				return queries.get(0);
			} else {
				BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
				for (Query query : queries) {
					booleanQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
				}
				return booleanQueryBuilder.build();
			}
		} catch (ParseException e) {
			// if we modified the query and then a ParseException occurs, try original query
			if (simplified) {
				return parseDefaultQuery(searchString, categoryList, false);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Try to simplify the search string (i.e. append wildcards etc).
	 *
	 * @param searchString
	 * 		the original search string
	 * @param result
	 * 		the builder that will contain the final search string, either simplified or not
	 * @return {@code true} if the search string was simplified; {@code false} otherwise
	 */
	private boolean simplifySearchString(String searchString, StringBuilder result) {
		boolean wildcardAppendForEveryWordPossible = true;

		// special characters used? Cannot simplify at all now.
		for (String specialCharacter : SPECIAL_CHARACTERS) {
			if (searchString.contains(specialCharacter)) {
				wildcardAppendForEveryWordPossible = false;
				break;
			}
		}

		if (!wildcardAppendForEveryWordPossible) {
			// no simplification possible, return original search term
			result.append(searchString);
			return false;
		}

		// can append wildcard to every word that was searched (special case for ':' applies, see below)
		for (String word : searchString.split("\\s+")) {
			result.append(word);

			// this is a specific field that was searched, cannot append wildcard behind the field specifier -> skip
			if (word.endsWith(GlobalSearchUtilities.QUERY_FIELD_SPECIFIER)) {
				continue;
			}

			// add wildcard at end of word
			result.append(GlobalSearchUtilities.QUERY_WILDCARD);

			result.append(' ');
		}
		// remove last whitespace again
		result.deleteCharAt(result.length() - 1);

		return true;
	}

	/**
	 * Builds the individual sub queries based on the given fields that should be searched.
	 *
	 * @param searchString
	 * 		the search term
	 * @param fieldSet
	 * 		the collection of fields that should be searched
	 * @param simpleMode
	 * 		if {@code true}, will boost the query for the name field and additional fields according to their weight;{@code false} will not boost any query.
	 * @throws ParseException
	 * 		if the search string cannot be parsed
	 */
	private List<Query> buildSubQueries(final String searchString, final Collection<GlobalSearchDefaultField> fieldSet, final boolean simpleMode) throws ParseException {
		// create queries for each field. Always at least the NAME_FIELD
		QueryParser parser;
		List<Query> queries = new ArrayList<>(fieldSet.size());
		for (GlobalSearchDefaultField field : fieldSet) {
			String name = field.getName();

			parser = new QueryParser(name, GlobalSearchUtilities.ANALYZER);
			parser.setDateResolution(DateTools.Resolution.DAY);
			parser.setAllowLeadingWildcard(true);
			parser.setDefaultOperator(QueryParser.Operator.AND);

			Query parsedQuery = parser.parse(searchString);
			Query query;
			// in simple mode, boost search for default name field. Set lower boost to additional fields
			if (simpleMode) {
				float boost;
				if (GlobalSearchUtilities.FIELD_NAME.equals(name)) {
					boost = GlobalSearchUtilities.DEFAULT_NAME_FIELD_BOOST;
				} else {
					boost = field.getBoost();
				}
				query = new BoostQuery(parsedQuery, boost);
			} else {
				query = parsedQuery;
			}

			queries.add(query);
		}

		return queries;
	}

	/**
	 * Creates the query for searching in the given {@link GlobalSearchCategory}s and returns the {@link Query} object.
	 *
	 * @param categories
	 * 		the search categories which are to be included in the search. Must not be {@code null}
	 * @return the query, never {@code null}
	 * @throws ParseException
	 * 		if the search string cannot be parsed
	 */
	private Query createCategoryQuery(final List<GlobalSearchCategory> categories) throws ParseException {
		// simply create "cat1 cat2 ... catN" string which is all that is needed for an OR search on the category field
		String categoryQueryString = categories.stream().map(GlobalSearchCategory::getCategoryId).collect(Collectors.joining(" "));

		QueryParser parser = new QueryParser(GlobalSearchUtilities.FIELD_CATEGORY, GlobalSearchUtilities.ANALYZER);
		return parser.parse(categoryQueryString);
	}

	/**
	 * Creates the search result for search methods.
	 *
	 * @param searchTerm
	 * 		the search string
	 * @param searcher
	 * 		the index searcher instance which was used to search
	 * @param result
	 * 		the result of the search
	 * @param highlightResult
	 * 		if {@code true}, the {@link GlobalSearchResult#getBestFragments()} will be created
	 * @return the search result instance, never {@code null}
	 * @throws IOException
	 * 		if something goes wrong
	 */
	private GlobalSearchResult createSearchResult(final String searchTerm, final Query parsedQuery, final IndexSearcher searcher, final TopDocs result, final boolean highlightResult) throws IOException {
		int resultNumber = result.scoreDocs.length;
		List<Document> resultList = new ArrayList<>(resultNumber);
		List<String[]> highlights = highlightResult ? new LinkedList<>() : null;
		ScoreDoc lastResult = resultNumber > 0 ? result.scoreDocs[result.scoreDocs.length - 1] : null;
		for (ScoreDoc scoreDoc : result.scoreDocs) {
			Document doc = searcher.doc(scoreDoc.doc);
			resultList.add(doc);

			if (highlightResult) {
				// search result highlighting best match on name field
				QueryScorer scorer = new QueryScorer(parsedQuery);
				Highlighter highlighter = new Highlighter(HIGHLIGHT_FORMATTER, scorer);
				Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, FRAGMENT_SIZE);
				highlighter.setTextFragmenter(fragmenter);
				try {
					TokenStream stream = TokenSources.getTokenStream(GlobalSearchUtilities.FIELD_NAME, searcher.getIndexReader().getTermVectors(scoreDoc.doc), doc.get(GlobalSearchUtilities.FIELD_NAME), GlobalSearchUtilities.ANALYZER, Highlighter.DEFAULT_MAX_CHARS_TO_ANALYZE - 1);
					if (stream != null) {
						highlights.add(highlighter.getBestFragments(stream, doc.get(GlobalSearchUtilities.FIELD_NAME), MAX_NUMBER_OF_FRAGMENTS));
					} else {
						highlights.add(null);
					}
				} catch (InvalidTokenOffsetsException e) {
					highlights.add(null);
				}
			}
		}
		return new GlobalSearchResult(resultList, searchTerm, lastResult, result.totalHits, highlights);
	}
}

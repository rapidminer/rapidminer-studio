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
package com.rapidminer.tools.expression.internal.antlr;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;


/**
 * Listener for errors in the expression parser. Stores the error line and its message.
 *
 * @author Gisa Schaefer
 *
 */
class ExpressionErrorListener extends BaseErrorListener {

	private String errorMessage;
	private int line;

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		super.syntaxError(recognizer, offendingSymbol, line, charPositionInLine, msg, e);

		this.line = line;

		if (recognizer.getInputStream() instanceof CommonTokenStream) {
			StringBuilder errorBuilder = new StringBuilder(msg);

			CommonTokenStream tokenStream = (CommonTokenStream) recognizer.getInputStream();
			errorBuilder.append("\n");

			String expression = tokenStream.getTokenSource().getInputStream().toString();
			String[] split = expression.split("(\r\n|\n)");
			String error = null;
			if (split.length > 0) {
				if (line - 1 >= 0 && line - 1 < split.length) {
					error = split[line - 1].replace("\t", " ");
				} else {
					error = split[split.length - 1].replace("\t", " ");
				}
				errorBuilder.append(error);
				errorBuilder.append("\n");
			} else {
				errorBuilder.append(expression);
				errorBuilder.append("\n");
			}

			for (int i = 0; i < charPositionInLine; i++) {
				errorBuilder.append(" ");
			}

			if (offendingSymbol instanceof Token) {
				Token token = (Token) offendingSymbol;
				int startIndex = token.getStartIndex();
				int stopIndex = token.getStopIndex();
				if (startIndex >= 0 && stopIndex >= 0 && startIndex <= stopIndex) {
					for (int i = token.getStartIndex(); i <= token.getStopIndex(); i++) {
						errorBuilder.append("^");
					}
				} else {
					errorBuilder.append("^");
				}
			} else {
				errorBuilder.append("^");
			}

			errorMessage = errorBuilder.toString();
		} else {
			errorMessage = msg;
		}
	}

	/**
	 * @return the message describing the syntax error
	 */
	String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @return the line containing the error, starting with 1
	 */
	int getErrorLine() {
		return line;
	}

	/**
	 * @return {@code true} if a syntax error occurred
	 */
	boolean containsError() {
		return errorMessage != null;
	}

}

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

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;


/**
 * Error strategy that does not recover after encountering an unrecognizable symbol and adjusts the
 * error messages of the {@link DefaultErrorStrategy}.
 *
 * @author Gisa Schaefer
 *
 */
class CapitulatingErrorStrategy extends DefaultErrorStrategy {

	@Override
	public void recover(Parser recognizer, RecognitionException e) {
		// stop parsing when encountering the first error
		throw new CapitulatingRuntimeException();
	}

	@Override
	protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {
		// change error message from default implementation
		TokenStream tokens = recognizer.getInputStream();
		String input;
		if (tokens != null) {
			if (e.getStartToken().getType() == Token.EOF) {
				input = "the end";
			} else {
				input = escapeWSAndQuote(tokens.getText(e.getStartToken(), e.getOffendingToken()));
			}
		} else {
			input = escapeWSAndQuote("<unknown input>");
		}
		String msg = "inadmissible input at " + input;
		recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
	}

	@Override
	protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
		// change error message from default implementation
		String msg = "mismatched input " + getTokenErrorDisplay(e.getOffendingToken()) + " expecting operator";
		recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
	}

	@Override
	protected void reportUnwantedToken(Parser recognizer) {
		// change error message from default implementation
		if (inErrorRecoveryMode(recognizer)) {
			return;
		}

		beginErrorCondition(recognizer);

		Token t = recognizer.getCurrentToken();
		String tokenName = getTokenErrorDisplay(t);
		String msg = "extraneous input " + tokenName + " expecting operator";
		recognizer.notifyErrorListeners(t, msg, null);
	}

	@Override
	protected String getTokenErrorDisplay(Token t) {
		// overwrite standard behavior to use "the end" instead of <EOF>
		if (t == null) {
			return "<no token>";
		}
		String s = getSymbolText(t).replace("<EOF>", "the end");
		if (s == null) {
			if (getSymbolType(t) == Token.EOF) {
				s = "the end";
			} else {
				s = escapeWSAndQuote("<" + getSymbolType(t) + ">");
			}
		}
		return s;
	}

}

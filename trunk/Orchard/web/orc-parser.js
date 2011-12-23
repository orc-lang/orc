//
// orc-parser.js -- JavaScript source to parse Orc text for the CodeMirror editor
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/**
 * Syntax-highlighting parser for Orc.
 * - This depends on the CodeMirror library: http://marijn.haverbeke.nl/codemirror
 * - Recognizes the following syntactic entities
 *   - combinator, pattern, site, variable, keyword, literal, comment
 * - The actual parsing is very shallow, mostly we just tokenize
 * - sites are distinguished from variables based on whether they
 *   are followed by a "(".
 * - The actual styles are defined in orc-syntax.css
 */
Editor.Parser = (function() {

// 2-character symbolic tokens
var ops2 = {
"{-": readCommentML,
"<:": readOperator,
":=": readOperator,
":>": readOperator,
"<=": readOperator,
">=": readOperator,
"||": readOperator,
"&&": readOperator,
"--": readCommentSL,
"/=": readOperator,
"{.": readOperator,
".}": readOperator };

// 1-character symbolic tokens
var ops1 = {
"{": readOperator,
"<": readCombinator,
">": readCombinator,
"|": readCombinator,
":": readOperator,
"&": readOperator,
"-": readOperator,
"/": readOperator,
";": readCombinator,
'}': readOperator,
',': readOperator,
'!': readOperator,
'=': readOperator,
'(': readOperator,
')': readOperator,
'.': readOperator,
'[': readOperator,
']': readOperator,
'~': readOperator,
'+': readOperator,
'*': readOperator,
'%': readOperator,
'@': readOperator,
'?': readOperator,
'"': readString };

// useful matchers
var isWord = matcher(/[\w_0-9']/);
var isDigit = matcher(/[0-9]/);

/**
 * Custom tokenizer. This always separates whitespace and
 * non-whitespace tokens so we can get more accurate information
 * for indentation. We may have to rethink that if it makes the parser
 * inefficient.
 */
function tokenizer(source, state) {
	function isSpace(ch) {
		// The messy regexp is because IE's regexp matcher is of the
		// opinion that non-breaking spaces are no whitespace.
		return ch != "\n" && /^[\s\u00a0]$/.test(ch);
	}

	function out(token) {
		token.value = token.content = (token.content || "") + source.get();
		return token;
	}

	function next() {
		var token;
		if (!source.more()) throw StopIteration;
		if (source.peek() == "\n") {
			source.next();
			return out({ type:"whitespace", style:"whitespace" });
		} else if (source.applies(isSpace)) {
			source.nextWhile(isSpace);
			return out({ type:"whitespace", style:"whitespace" });
		} else {
			while (!token) token = state(source, function (s) { state = s; });
			return out(token);
		}
	}

	return { next: next, state: function() { return state; } };
}

/**
 * Read one token.
 */
function readToken(source, setState) {
	var ch1 = source.next();
	var ch2 = ch1 + source.peek();
	var reader;
	// try to match 2 and 1 character symbolic tokens,
	// then try numbers, then try words
	if ((reader = ops2[ch2])) {
		source.next();
		return reader(source, setState, ch2);
	} else if ((reader = ops1[ch1])) {
		return reader(source, setState, ch1);
	} else if (isDigit(ch1)) {
		return readNumber(source, setState, ch1);
	} else {
		return readWord(source, setState, ch1);
	}
}

function readOperator(_1, _2, type) {
	return { type:type, style:"operator" };
}

function readCombinator(_1, _2, type) {
	return { type:type, style:"combinator" };
}

function readNumber(source, _, ch1) {
	source.nextWhile(isDigit);
	if (source.peek() == ".") {
		source.next();
		source.nextWhile(isDigit);
	}
	if (source.peek() == "E" || source.peek() == "e") {
		source.next();
		switch (source.peek()) {
		case "-": case "+":
			source.next();
		}
		source.nextWhile(isDigit);
	}
	// FIXME: handle decimals
	return { type:"number", style:"literal" };
}

function readWord(source, _, ch1) {
	source.nextWhile(isWord);
	var word = source.get();
	switch (word) {
	// literals
	case "true": case "false": case "null":
		return { type:word, content:word, style:"literal" };
	// keywords
	case "as": case "def": case "else": case "if": case "import":
	case "include": case "lambda": case "signal": case "stop":
	case "then": case "type": case "val": case "_":
		return { type:word, content:word, style:"keyword" };
	default:
		return { type:"variable", content:word, style:"variable" };
	}
}

function readString(source, setState, _) {
	setState(readString);
	while (!source.endOfLine()) {
		var ch = source.next();
		if (ch == '"') {
			setState(readToken);
			break;
		} else if (ch == '\\' && !source.endOfLine()) {
			source.next();
		}
	}
	return { type:"string", style:"literal" };
}

function readCommentML(source, setState) {
	setState(readCommentML);
	while (!source.endOfLine()) {
		var ch = source.next();
		if (ch == "-" && source.peek() == "}") {
			source.next();
			setState(readToken);
			break;
		}
	}
	return { type:"comment", style:"comment" };
}

function readCommentSL(source, setState, _) {
	while (!source.endOfLine()) source.next();
	return { type:"comment", style:"comment" };
}

/**
 * Orc parser factory. A codemirror parser is just a token stream
 * with a copy method that is used to cache the parser state at the
 * start of each line.
 */
function newParser(source) {
	var tokensState = readToken;
	var tokens = tokenizer(source, tokensState);
	// buffer for look-ahead tokens; holds {token, state} records
	var lookahead = [];
	// while looking, where are we in the lookahead buffer?
	var lookaheadIndex = 0;
	// the current tabstop column
	var tabstop = null;
	// are we inside a pattern?
	var inPattern = false;
	// are we immediately preceeded by whitspace?
	var inWhitespace = false;
	/** Reset the lookahead pointer to the current token. */
	function startLooking() {
		lookaheadIndex = 0;
	}
	/** Get the next token for real and move the "current" token pointer. */
	function nextToken() {
		startLooking();
		if (lookahead.length) {
			var r = lookahead.shift();
			tokensState = r.state;
			return r.token;
		} else {
			var out = tokens.next();
			tokensState = tokens.state();
			return out;
		}
	}
	/**
	 * Get the next token speculatively. Repeated calls
	 * will move forward in the token stream. May be reset
	 * to the "current" token using startLooking().
	 */
	function look() {
		try {
			if (lookahead.length > lookaheadIndex) {
				return lookahead[lookaheadIndex++].token;
			} else {
				lookahead.push({token: tokens.next(), state: tokens.state()});
				return look();
			}
		} catch (e) {}
	}
	/** Get the next token and change its style based on the parse state. */
	function next() {
		var out = nextToken();
		// if we're in a pattern, apply the "pattern" style
		if (inPattern) out.style += " pattern";
		if (out.content == "\n") {
			if (tabstop == null) tabstop = 0;
			var _tabstop = tabstop;
			out.indentation = function (start) {
				return _tabstop;
			};
			tabstop = null;
		} else {
			// if the variable is in function call position,
			// change it to a "site"
			if (out.type == "variable") {
				startLooking();
                var tmp;
				do {
					tmp = look();
					if (!tmp) break;
					if (tmp.type == "(" || tmp.type == "[") {
						out.style = "site";
						break;
					}
				} while (tmp.type == "whitespace");
			}
			// if no tabstop has been set yet, use the
			// length of the first whitespace token
			if (tabstop == null) {
				if (out.type == "whitespace") {
					tabstop = out.value.length;
				} else {
					tabstop = 0;
				}
			}
		}
		inWhitespace = (out.type == "whitespace");
		return out;
	}
	function copy() {
		var _tokensState = tokensState;
		var _inPattern = inPattern;
		return function (source) {
			tokens = tokenizer(source, _tokensState);
			tokensState = _tokensState;
			inPattern = _inPattern;
			// the copy starts at the beginning
			// of the line, so most of the within-line
			// state should just be reset
			lookahead = [];
			lookaheadIndex = 0;
			tabstop = null;
			inWhitespace = true;
			return parser;
		};
	}
	var parser = { next: next, copy: copy };
	return parser;
}

return { make: newParser };

})();

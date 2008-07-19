/**
 * Parser for Orc.
 */
Editor.Parser = (function() {

// 2-character symbolic tokens
var ops2 = {
"{-": readCommentML,
"<:": readOperator,
":>": readOperator,
"<=": readOperator,
">=": readOperator,
"||": readOperator,
"&&": readOperator,
"--": readCommentSL,
"/=": readOperator };

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
'"': readString };

// useful matchers
var isWord = matcher(/[\w_0-9]/);
var isDigit = matcher(/[0-9]/);

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

	return { next: next, state: state };
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
	if (reader = ops2[ch2]) {
		source.next();
		return reader(source, setState, ch2);
	} else if (reader = ops1[ch1]) {
		return reader(source, setState, ch1);
	} else if (isDigit(ch1)) {
		return readNumber(source, setState, ch1);
	} else {
		return readWord(source, setState, ch1);
	}
}

function readOperator(_, _, type) {
	return { type:type, style:"operator" };
}

function readCombinator(_, _, type) {
	return { type:type, style:"combinator" };
}

function readNumber(source, _, ch1) {
	source.nextWhile(isDigit);
	// FIXME: handle decimals
	return { type:"number", style:"literal" };
}

function readWord(source, _, ch1) {
	source.nextWhile(isWord);
	var word = source.get();
	switch (word) {
	// literals
	case "true": case "false": case "null":
		return { type:"boolean", content:word, style:"literal" };
	// keywords
	case "val": case "def": case "at": case "include": case "site": case "class":
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
		} else if (ch == '\\') {
			source.next();
		}
	}
	return { type:"string", style:"literal" }
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
	return { type:"comment", style:"comment" }
}

function readCommentSL(source, setState, _) {
	while (!source.endOfLine()) source.next();
	return { type:"comment", style:"comment" };
}

/**
 * Constructor for a Parser object.
 */
function newParser(source) {
	var tokens = tokenizer(source, readToken);
	var lookahead = [];
	var lookaheadIndex = 0;
	var tabstop = null;
	function startLooking() {
		lookaheadIndex = 0;
	}
	function nextToken() {
		startLooking();
		if (lookahead.length) return lookahead.shift();
		else return tokens.next();
	}
	function look() {
		try {
			if (lookahead.length > lookaheadIndex) {
				return lookahead[lookaheadIndex++];
			} else {
				lookahead.push(tokens.next());
				return look();
			}
		} catch (e) {}
	}
	function next() {
		var out = nextToken();
		if (out.content == "\n") {
			if (tabstop == null) tabstop = 0;
			var _tabstop = tabstop;
			out.indentation = function (start) {
				return _tabstop;
			};
			tabstop = null;
		} else {
			if (out.type == "variable") {
				startLooking();
				// check if the variable should be a function
				do {
					var tmp = look();
					if (!tmp) break;
					if (tmp.type == "(") {
						out.style = "site";
						break;
					}
				} while (tmp.type == "whitespace");
			}
			if (tabstop == null) {
				if (out.type == "whitespace") {
					tabstop = out.value.length;
				} else {
					tabstop = 0;
				}
			}
		}
		return out;
	}
	function copy() {
		var _state = tokens.state;
		return function (source) {
			tokens = tokenizer(source, _state);
			lookahead = [];
			lookaheadIndex = 0;
			tabstop = null;
			return parser;
		}
	}
	var parser = { next: next, copy: copy };
	return parser;
}

return { make: newParser };

})();

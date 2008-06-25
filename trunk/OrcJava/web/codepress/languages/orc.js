Language.syntax = [
	// single-line comments
	{ input : /(--.*)/g, output : '<i>$1</i>' },
	// multi-line comments
	{ input : /(\{-[^]*?-\})/g, output: '<i>$1</i>' },
	// strings
	{ input : /([^\\]|^)("([^"\\\n]|\\[^])*")/g, output : '$1<s>$2</s>' },
	// reserved words
	{ input : /\b(val|def|class|site|include|lambda|as)\b/g, output : '<b>$1</b>' },
	// builtins
	{ input : /\b(if|let|some|none|isSome|isNone|Rtimer|Ltimer|null|true|false)\b/g, output : '<u>$1</u>' },
	// combinators
	{ input : /(&gt;.*?&gt;|&lt;.*?&lt;|\|)/g, output : '<tt>$1</tt>' }
];

Language.snippets = [
	{ input : 'def', output : 'def $0() = ' },
	{ input : 'if', output : 'if($0) >> ' }
];

Language.complete = [
	{ input : '>', output : '>$0>' },
	{ input : '<', output : '<$0<' },
	{ input : '"', output : '"$0"' },
	{ input : '(', output : '\($0\)' },
	{ input : '[', output : '\[$0\]' },
];

Language.shortcuts = [];

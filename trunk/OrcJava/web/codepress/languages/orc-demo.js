Language.syntax = [
	// single-line comments
	{ input : /(--.*)/g, output : '<i>$1</i>' },
	// multi-line comments
	{ input : /(\{-[^]*?-\})/g, output: '<i>$1</i>' },
	// strings
	{ input : /([^\\]|^)("([^"\\\n]|\\[^])*")/g, output : '$1<s>$2</s>' },
	// literals
	{ input : /\b(true|false|[1-9][0-9]*|0)\b/g, output : '<s>$1</s>' },
	// keywords
	{ input : /(\bval\b|\bdef\b|\bclass\b|\bsite\b|\binclude\b|\blambda\b|\bas\b|&gt;|&lt;|\|)/g, output : '<u>$1</u>' },
	// operators
	{ input : /([+*()=,-]+)/g, output: '<tt>$1</tt>' }
];

Language.snippets = [];

Language.complete = [];

Language.shortcuts = [];

Language.syntax = [
	// comments
	{ input : /(--.*?)<br>/g, output : '<i>$1</i><br>' },
	// strings
	{ input : /([^\\]|^)("([^"\\]|\\[^])*")/g, output : '$1<s>$2</s>' },
	// reserved words
	{ input : /\b(var|def|class|site)\b/g, output : '<b>$1</b>' },
	// builtins
	{ input : /\b(if|let|Rtimer|Ltimer)\b/g, output : '<u>$1</u>' },
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
	{ input : '{', output : '{\n\t$0\n}' }
];

Language.shortcuts = [];

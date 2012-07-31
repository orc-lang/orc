//
// orc-ready.js -- JavaScript source for the "Try Orc" Orchard Web interface
// Project Orchard
//
// $Id$
//
// Copyright (c) 2012 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/**
 * This is loaded automatically by orc.js after jQuery.
 *
 * Actually orc.js loads a minified version of this
 * generated with the following command:
 *
 * java -jar ../lib/yuicompressor-2.3.6.jar --type js orc-ready.js > orc-ready-min.js
 */
jQuery(function ($) {

// don't want to conflict with other libraries
$.noConflict();

// always cache!
$.ajaxSetup({cache: true});

/**
 * Load the executor on demand.
 */
function withExecutor(f) {
	var executor;
	if (executor) return f(executor);
	$.getScript(executorUrl, function () {
		executor = executorService;

		$(window).unload(function () {
			if (currentWidget) currentWidget.stopOrc();
		});

		f(executor);
	});
}

/**
 * Opens a new browser window.
 */
function browse(url) {
	if (!open(url)) {
		if (currentWidget) currentWidget.stopOrc();
		alert("This Orc program needs to open a browser window.\n\n"+
			"Please disable any popup blockers and run the program again.");
	}
}

/**
 * Our current JSON serializer unwraps arrays with one element.  This function
 * re-wraps such values so we can treat them consistently as arrays.
 */
function toArray(object) {
	if (!object) return [];
	else if (object.constructor == Array) return object;
	else return [object];
}

function escapeHtml(v) {
	return v
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;')
        .replace(/"/g, '&quot;')
        .replace(/ /g, '&nbsp;')
        .replace(/\t/g, '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;')
        .replace(/\n/g, '<br />\n');
}

/**
 * Convert arbitrary JSON values to
 * pretty-printed HTML
 */
function jsonToHtml(v) {
	switch (typeof v) {
		case 'boolean':
		case 'number':
			return v+'';
		case 'string':
			return escapeHtml('"' + v
					.replace(/\\/g, '\\\\')
					.replace(/"/g, '\\"')
					.replace(/\f/g, '\\f')
					.replace(/\n/g, '\\n')
					.replace(/\r/g, '\\r')
					.replace(/\t/g, '\\t')
				+ '"');
		case 'object':
			if (v == null) return 'null';
			if (v.constructor == Array) {
				var tmp = [];
				for (var k in v) {
					tmp[k] = jsonToHtml(v[k]);
				}
				return '[' + tmp.join(', ') + ']';
			}
			if (v.constructor == Object) {
				var out = '{';
				for (var k in v) {
					out += jsonToHtml(k)+': ' + jsonToHtml(v[k]) + ', ';
				}
				return out.substring(0, out.length-2) + '}';
			}
			return v+""; // toString
	}
}

/**
 * Convert publication values to HTML.
 * These have a bit more structure than arbitrary JSON.
 */
function publicationToHtml(v) {
	if (v == null) return jsonToHtml(null);
	if (v["@xsi.nil"]) return jsonToHtml(null);
	switch (v["@xsi.type"]) {
		// XSD types
		case 'xs:string': return jsonToHtml(v.$);
		case 'xs:integer':
		case 'xs:long':
		case 'xs:short':
		case 'xs:int': return jsonToHtml(parseInt(v.$));
		case 'xs:double':
		case 'xs:decimal':
		case 'xs:float': return jsonToHtml(parseFloat(v.$));
		case 'xs:boolean': return jsonToHtml(v.$ == 'true');
		// Orc types
		case 'ns2:orcValue':
			return escapeHtml(v.$);
		case 'ns2:otherValue':
			return '<i>' + escapeHtml(v["@typeName"]) + ': ' + escapeHtml(v.$) + '</i>';
		case 'ns2:list':
			var tmp = [];
			$.each(toArray(v.elements), function (i, e) {
				tmp[i] = publicationToHtml(e);
			});
			return '[' + tmp.join(', ') + ']';
		default: return '<i>' + jsonToHtml(v) + '</i>';
	}
}

/**
 * Return a mousemove function for a drag-resize.
 * Takes the mousedown event and the element to resize.
 */
function dragResize(e0, top) {
	var $top = $(top);
	var th = $top.height();

	function onmove(e) {
		var dy = e.pageY - e0.pageY;
		$top.height(th + dy);
	}

	// without this div, some mousemove events are
	// lost to the iframe edit area
	var $dragCover = $('<div style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; cursor: s-resize;" />');
	$(document.body).append($dragCover);

	$(document).mouseup(function (e) {
		$(document).unbind('mousemove', onmove);
		$dragCover.remove();
	});

	return onmove;
}

function getCodeFrom(elem) {
	if (!elem) return "";
	if (elem.value) return elem.value;
	else return $(elem).text();
}

/**
 * Turn an element into a non-runnable Orc widget.
 * Since all this does is call CodeMirror, it's much simpler
 * than the full-blown orcify.
 */
function orcifySnippet(code, defaultConfig) {
	var $code = $(code);
	var height = $code.height();
	// put a wrapper around the code area, to add a border
	$code.wrap('<div class="orc-code" />');
	$code = $code.parent();
	$code.wrap('<div class="orc-wrapper" />');
	$code = $code.parent();
	// replace the code with a codemirror editor
	var config = $.extend({}, defaultConfig, {
		content: getCodeFrom(code),
		readOnly: true,
		height: height + "px"
	});
	var codemirror = new CodeMirror(CodeMirror.replace(code), config);
}

/**
 * Turn an element into a runnable Orc widget. The basic lifecycle of the
 * widget is this:
 *
 * 1. widget is created
 * 2. user clicks "Run"
 * 3. create a new job
 * 4. wait for events
 *    a. if there are no more events, go to step 6.
 *    b. otherwise, handle the events, purge them, and repeat step 4.
 * 5. user clicks "Stop"
 * 6. finish the job
 *
 * Race conditions are a concern because we can have pending callbacks which
 * become invalid after the job finishes, and the job may finish at any time.
 * The solution is to package all the callbacks into a closure which tracks the
 * state of the job, so we can ignore callbacks which happen after the job
 * finishes.
 */
function orcify(code, defaultConfig) {
	var prelude;
	var postlude;
	function extractLude(program) {
		var tmp = program.split(/\s*{- EXAMPLE -}\s*/);
		if (tmp.length == 1) {
			prelude = "";
			postlude = "";
			return tmp[0];
		} else {
			prelude = tmp[0];
			postlude = tmp[2] ? tmp[2] : "";
			return tmp[1];
		}
	}

	function getCode() {
		return (prelude==""?"":prelude+"\n") + codemirror.getCode() + (postlude==""?"":"\n"+postlude);
	}

	function startOfLine(node) {
		while (node && node.nodeName != "BR")
			node = node.previousSibling;
		return node;
	}
	function endOfLine(node) {
		while (node && node.nextSibling && node.nextSibling.nodeName != "BR")
			node = node.nextSibling;
		return node;
	}
	function toggleComment(editor) {
		var selection = editor.selectedText();
		if (selection) {
			// toggle multi-line-comment around selected area
			if (/^\s*{-/.test(selection)) {
				selection = selection.replace(/^(\s*){-/, "$1");
				selection = selection.replace(/-}(\s*)$/, "$1");
			} else {
				selection = selection.replace(/{-/, "");
				selection = selection.replace(/-}/, "");
				selection = "{-" + selection + "-}";
			}
			editor.replaceSelection(selection);
		} else {
			// toggle single-line-comment at current line;
			// this works by selecting the whole line and replacing it,
			// because I couldn't find an easy way to just insert stuff
			// using the codemirror API. as a side effect, this loses
			// the current selection
			var select = editor.win.select;
			// copied in part from indentAtCursor
			if (!editor.container.firstChild) return;
			var cursor = select.selectionTopNode(editor.container, false)
				|| editor.container.firstChild;
			// select the line
			select.setCursorPos(editor.container, {node: startOfLine(cursor), offset: 0}, {node: endOfLine(cursor), offset: 0});
			var selection = editor.selectedText();
			if (/^\s*--/.test(selection)) {
				selection = selection.replace(/^(\s*)--/, "$1");
			} else {
				selection = "--" + selection;
			}
			editor.replaceSelection(selection);
			// reset cursor to the start of the line
			cursor = select.cursorPos(editor.container, true);
			select.setCursorPos(editor.container, cursor, cursor);
		}
	}

	function displayHelp() {
		window.open("/orchard/help.html", "OrchardHelp", "width=500,height=500,scrollbars=yes");
	}

	function appendEventHtml(html) {
		var $html = $(html);
		$events.show();
		$events.append($html);
		var isauto = $events.css("height") == "auto";
		if (isauto && $events.height() + $html.height() > 250) {
			// simulate max-height for IE's benefit
			$events.height(250);
			isauto = false;
		}
		$html.show();
		if (!isauto) $events[0].scrollTop = $events[0].scrollHeight;
	}

	function renderTokenError(p) {
		appendEventHtml('<div class="orc-error">'
			+ escapeHtml(p.message)
			+ (p.location && p.location.file
				? ' at '
					+ escapeHtml(p.location.file)
					+ ':' + p.location.line
					+ ':' + p.location.column
				: '')
			+ '</div>');
	}

	function renderPublication(p) {
		appendEventHtml('<div class="orc-publication">' + publicationToHtml(p.value) + '</div>');
	}

	function renderPrintln(p) {
		appendEventHtml('<div class="orc-print">' + escapeHtml(p.line) + '</div>');
	}

	function prompt(message, onSubmit) {
		var isFirst = $prompts.is(":empty");
		function hide(onReady) {
			$prompt.remove();
			if ($prompts.is(":empty")) {
				$prompts.hide();
			} else {
				// setTimeout resolves a weird layout bug with IE
				setTimeout(function () {
					$prompts.find(":input:first").focus();
				}, 0);
			}
			onReady();
		}
		function ok() {
			var response = $input[0].value;
			hide(function () {
				onSubmit(response);
			});
		}
		function cancel() {
			hide(onSubmit);
		}
		var $prompt = $('<div class="orc-prompt">'+
			'<p>'+escapeHtml(message)+'</p>'+
			'<div class="orc-prompt-input"><input type="text" value="" />'+
				'<div class="orc-prompt-input-send" />'+
				'<div class="orc-prompt-input-close" /></div>'+
			'</div>');
		$prompts.append($prompt);
		var $input = $prompt.find("input")
			.keydown(function(event){
				switch (event.keyCode) {
				case 13: ok(); return false;
				case 27: cancel(); return false;
				}
			});
		$prompt.find(".orc-prompt-input-close").click(cancel);
		$prompt.find(".orc-prompt-input-send").click(ok);
		if (isFirst) $prompts.show();
		$prompt.slideDown("fast", function () {
			// setTimeout resolves a weird layout bug with IE
			setTimeout(function () {
				if (isFirst) $input.focus();
			}, 0);
		});
	}

    var orcWikiUri = "https://orc.csres.utexas.edu/wiki/Wiki.jsp?page="

	function renderError(response, code, exception) {
        if (response && response.detail && response.detail.exception && (response.detail.exception["@class"] == "orc.orchard.errors.InvalidProgramException" || response.detail.exception["@class"] == "orc.orchard.errors.InvalidOilException")) {
            var problems = response.detail[response.detail.exception["@class"].substring(response.detail.exception["@class"].lastIndexOf(".")+1)].problems;
            if (problems) {
                problems = toArray(problems);
                for (var i in problems) {
                    var errmsg = "";
                    if (problems[i].severity >= 5) errmsg += "Problem ";
                    if (problems[i].severity == 4) errmsg += "Warning ";
                    var filenamelength = 0;
                    if (problems[i].filename && problems[i].filename.length && problems[i].filename.length > 0) {
                        errmsg += "in file "+problems[i].filename + " ";
                        filenamelength = problems[i].filename.length;
                    }
                    errmsg += "near line "+problems[i].line+", column "+problems[i].column;
                    errmsg += problems[i].longMessage.substring(filenamelength+problems[0].line.toString().length+problems[0].column.toString().length+2);
                    var $eventMessage = $('<div class="orc-error">' + escapeHtml(errmsg) + '</div>');
                    if (problems[i].orcWikiHelpPageName && problems[i].orcWikiHelpPageName.length && problems[i].orcWikiHelpPageName.length > 0) {
                        $helpLink = $('<button class="orc-error-help" title="Orc wiki: ' + problems[i].orcWikiHelpPageName + '">&nbsp;?&nbsp;</button>')
                            .click(function() {
                                window.open(orcWikiUri + problems[i].orcWikiHelpPageName, problems[i].orcWikiHelpPageName, "width=875,height=750,scrollbars=yes");
                            })
                            .mousedown(nobubble);
                        $eventMessage.prepend($helpLink);
                    }
                    appendEventHtml($eventMessage);
                }
                if (!problems[i].filename || !problems[i].filename.length || problems[i].filename.length == 0) {
                    codemirror.selectLines(codemirror.nthLine(problems[0].line), problems[0].column-1, codemirror.nthLine(problems[0].line), problems[0].column-1);
                }
            } else {
                appendEventHtml('<div class="orc-error">' + escapeHtml(response.faultstring) + '</div>');
            }
        } else {
            // unwrap response if possible
            if (response) {
                if (response.faultstring) {
                    response = response.faultstring;
                }
            } else {
                response = exception;
            }
            appendEventHtml('<div class="orc-error">Service error: ' + jsonToHtml(response) + '</div>');
        }
	}

	/**
	 * Start a job and return a function to stop it.
	 */
	function startJob(executor, jobID) {
		var finished = false;

		/**
		 * Call the job webservice.
		 */
		function job(method, args, onReady) {
			if (finished) return;
			args.devKey = devKey;
			args.job = jobID;
			executor[method](args, onReady, onError);
		}

		/**
		 * Stop the job; this is idempotent.
		 */
		function stop() {
			if (finished) return;
			job('finishJob', {});
			// it's safe to assume that finish
			// will succeed, or if it doesn't
			// we can't recover so we may as
			// well pretend it did
			finished = true;
			$run.show();
			$stop.hide();
			if (!$events.is(":empty")) $close.show();
			$loading.hide();
			$prompts.empty();
			$prompts.hide();
		}

		function onPrompt(v) {
			prompt(v.message, function (response) {
				if (response == null)
					job('cancelPrompt', { promptID: v.promptID });
				else job('respondToPrompt', { promptID: v.promptID, response: response });
			});
		}

		/**
		 * Handle job events.
		 */
		function onEvents(vs) {
			// if the job was aborted by the user, we should
			// suppress any further publications, because we
			// don't want for example a new prompt to show
			// up after the job has finished
			if (finished) return;
			if (!vs) return stop();
			$.each(toArray(vs), function (_, v) {
				switch (v["@xsi.type"]) {
				case "ns2:tokenErrorEvent":
					renderTokenError(v);
					break;
				case "ns2:publicationEvent":
					renderPublication(v);
					break;
				case "ns2:printlnEvent":
					renderPrintln(v);
					break;
				case "ns2:promptEvent":
					onPrompt(v);
					break;
				case "ns2:browseEvent":
					browse(v.url);
					break;
				}
			});
			job('purgeJobEvents', {}, function () {
				job('jobEvents', {}, onEvents);
			});
		}

		/**
		 * Handle errors.
		 */
		function onError(response, code, exception) {
			// ignore errors which happened after we stopped the job
			if (finished) return;
			// Try to stop the job
			stop();
			renderError(response, code, exception);
		}

		// start the job
		job('startJob', {}, function () {
			$stop.show();
			job('jobEvents', {}, onEvents);
		});

		return stop;
	}

	/**
	 * This will be mutated when a job starts.
	 */
	function stopCurrentJob() {}

	/**
	 * Start the job.
	 */
	function run() {
		$close.hide();
		$run.hide();
		$loading.show();
		$events[0].innerHTML = '';
		$events.hide();
		$events.css("height", "auto");
		// If another job is running, it should stop
		if (currentWidget) currentWidget.stopOrc();
		currentWidget = $widget[0];
		withExecutor(function (executor) {
			executor.compileAndSubmit({devKey: devKey, program: getCode()}, function (id) {
				stopCurrentJob = startJob(executor, id);
			}, function (r,c,e) {
				renderError(r,c,e);
				$run.show();
				$stop.hide();
				$loading.hide();
			});
		});
	}

	function nobubble() { return false; }

	// private members
	var $code = $(code);
	
	var hiddenParents = $code.parents(":hidden")
	hiddenParents.show()
	
	var $loading = $('<div class="orc-loading" style="display: none"/>');
	var $widget = $('<div class="orc-wrapper" />');
		//.width($code.width()+2);
	var $prompts = $('<div class="orc-prompts" style="display: none"/>');
	var $events = $('<div class="orc-events" style="display: none"/>');
	var $close = $('<button class="orc-close" style="display: none">close</button>')
		.click(function () {
			$close.hide();
			$events.slideUp("fast");
		})
		.mousedown(nobubble);
	var $stop = $('<button class="orc-stop" style="display: none">stop</button>')
		.click(function() {
			stopCurrentJob();
		})
		.mousedown(nobubble);
	var $run = $('<button class="orc-run">run</button>')
		.click(run)
		.mousedown(nobubble);
	var $controls = $('<div class="orc-controls" />')
		.append($loading).append($close).append($stop).append($run);
	var editable = (code.tagName == "TEXTAREA");
	var id = $code.attr("id");

	var program = extractLude(getCodeFrom(code));
	// redraw the program in case the height changes due to hidden code
	if (!editable) $code.text(program);
	var height = $code.height();

	// put a wrapper around the code area, to add a border
	$code.wrap('<div class="orc-code" />');
	$code = $code.parent();
	$code.wrap($widget).after($events).after($prompts).after($controls);
	// for some reason wrap() makes a copy of $widget.
	$widget = $code.parent();
	var onReady = code.onOrcReady;
	var config = $.extend({}, defaultConfig, {
		content: program,
		readOnly: !editable,
		height: height + "px",
		initCallback: function (cm) {
			// monkey-patch keyDown to handle our own hotkeys
			var _keyDown = cm.editor.keyDown;
			cm.editor.keyDown = function (event) {
				if (event.ctrlKey) {
					if (event.keyCode == 13) { // enter
						run();
						event.stop();
						return;
					} else if (event.keyCode == 191) { // forward slash
						toggleComment(this);
						event.stop();
						return;
					} else if (event.keyCode == 76) { // L
						this.reparseBuffer();
						event.stop();
						return;
					}
				} else if (event.keyCode == 27) { // escape
					stopCurrentJob();
					event.stop();
					return;
				} else if (event.keyCode == 112) { // F1
					displayHelp();
					event.stop();
					return;
				}
				_keyDown.apply(this, arguments);
			}
			if (onReady) {
				onReady($widget[0]);
				onReady = null;
			}
		}
	});
	program = null;
	// replace the code with a codemirror editor
	var codemirror = new CodeMirror(CodeMirror.replace(code), config);
	// if the code had an id, move it to the surrounding div
	// (since we replaced the code element with codemirror)
	if (id) $widget.attr("id", id);

	// resizable fonts
	var fontSize = 13;
	function setFontSize(size) {
		fontSize = size;
		codemirror.win.document.body.style.fontSize = size + "px";
		$widget.css("font-size", size + "px");
	}
	if (editable) {
		// implement drag-resize
		$controls.css("cursor", "s-resize");
		$controls.mousedown(function (e) {
			$(document).mousemove(dragResize(e, codemirror.frame.parentNode));
		});
	}

	// public members
	$widget[0].stopOrc = function() {
		stopCurrentJob();
	};
	$widget[0].displayOrcHelp = function() {
		displayHelp();
	};
	$widget[0].setOrcCode = function(code) {
		stopCurrentJob();
		$close.hide();
		$events.slideUp("fast");
		codemirror.setCode(extractLude(code));
	};
	$widget[0].orcFontSizeUp = function() {
		setFontSize(fontSize * 1.25);
	};
	$widget[0].orcFontSizeDown = function() {
		setFontSize(fontSize * 0.8);
	};
	hiddenParents.hide()
}

/** Widget which is currently running. */
var currentWidget;
var devKey = Orc.devKey;
var executorUrl = Orc.mock
	? Orc.mock + "mock-executor.js"
	: "/orchard/json/executor?js";

var config = {
	stylesheet: Orc.baseUrl + "orc-syntax.css",
	path: Orc.baseUrl,
	parserfile: Orc.mock
		? ["orc-parser.js"]
		: ["orc-parser-min.js"],
	basefiles: Orc.mock
		? ["codemirror/util.js", "codemirror/stringstream.js", "codemirror/select.js", "codemirror/undo.js", "codemirror/editor.js", "codemirror/tokenize.js"]
		: ["codemirror-extra-min.js"],
	autoMatchParens: true,
	/**
	 * FIXME: although I don't like text wrapping, without this option
	 * we encounter some weird copy/paste bugs with newlines:
	 * http://groups.google.com/group/codemirror/browse_thread/thread/283d4da359dc2b9b
	 */
	textWrapping: true

};

$("pre.orc-snippet").each(function (_, code) {
	orcifySnippet(code, config);
});

$(".orc").each(function (_, code) {
	orcify(code, config);
});

}); // end module

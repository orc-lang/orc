/**
 * This is loaded automatically by orc.js after jQuery.
 */
jQuery(function ($) {

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
		executor.onError = function (response, code, exception) {
			// If the job failed while stopping, job may not be set.
			if (job) stop();
			// unwrap response if possible
			if (response) {
				if (response.faultstring) response = response.faultstring;
			} else {
				response = exception;
			}
			alert(response);
		}

		$(window).unload(function () {
			if (currentWidget) currentWidget.stop();
		});

		f(executor);
	});
}

/**
 * Open a prompt-like dialog box. Unlike the standard prompt function, this one
 * does not suspend execution, so the user can see values being published while
 * they enter their repsonse.
 */
function prompt(message, handler) {
	var $message = $('<p>'+escapeHtml(message).replace(/\n/g, '<br />\n')+'</p>');
	var $input = $('<input type="text" value="" />')
		.keydown(function(event){
			switch (event.keyCode) {
			case 13: ok(); break;
			case 27: cancel(); break;
			}
		});
	var $root = $('<div/>').append($message).append($input);
	function close() {
		$root.dialog('destroy');
		$root.remove();
	}
	function cancel() {
		close();
		handler();
	}
	function ok() {
		var response = $input.get(0).value;
		close();
		handler(response);
	}
	function init() {
		$root.dialog({
			dialogClass: 'orc-prompt',
			title: "Orc Prompt",
			resizable: false,
			width: "460",
			height: "", // automatic height
			buttons: {
				"Cancel": cancel,
				"OK": ok
			}
		});
		$input.focus();
	}
	if ($root.dialog) {
		init();
	} else {
		$.getScript(baseUrl + 'jquery-ui-dialog-20080717-min.js', init);
	}
	return {
		close: close
	};
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
	v = v.replace(/&/g, '&amp;');
	v = v.replace(/</g, '&lt;');
	// FIXME: escape other special characters
	return v;
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
			return '"' + escapeHtml(v)
					.replace('"', '\\"')
					.replace('\\', '\\\\')
				+ '"';
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
			return '';
	}
}

/**
 * Convert publication values to HTML.
 * These have a bit more structure than arbitrary JSON.
 */
function publicationToHtml(v) {
	if (v == null) return null;
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
		// OIL types
		// FIXME: server should use better namespace than ns2
		case 'ns2:constant': return publicationToHtml(v.value);
		case 'ns2:list':
			var tmp = [];
			$.each(toArray(v.element), function (i, e) {
				tmp[i] = publicationToHtml(e);
			});
			return '[' + tmp.join(', ') + ']';
		case 'ns2:tuple':
			var tmp = [];
			$.each(toArray(v.element), function (i, e) {
				tmp[i] = publicationToHtml(e);
			});
			return '(' + tmp.join(', ') + ')';
		default: return '<i>' + jsonToHtml(v) + '</i>';
	}
}

/**
 * Class which encapsulates the behavior of one widget.
 */
function OrcWidget(code) {
	var _this = this;
	// private members

	/** Wrapper for the current job; unset as soon as the job stops. */
	var job;
	/** If codemirror is used, this is the editor. */
	var codemirror;
	/** A table of active prompts to close when the job terminates. */
	var prompts = {};
	var $loading = $('<div class="orc-loading" style="display: none"/>');
	var $wrapper = $('<div class="orc-wrapper" />')
		.width($(code).width()+2);
	var $events = $('<div class="orc-events" style="display: none"/>');
	var $close = $('<button class="orc-close" style="display: none">close</button>')
		.click(function () {
			$close.hide();
			$events.slideUp("fast");
		});
	var $stop = $('<button class="orc-stop" style="display: none">stop</button>')
		.click(stop);
	var $run = $('<button class="orc-run">run</button>')
		.click(run);
	var $controls = $('<div class="orc-controls" />')
		.append($loading).append($close).append($stop).append($run);

	function getCodeFrom(elem) {
		if (!elem) return "";
		if (elem.value) return elem.value;
		else return $(elem).text();
	}

	function getCode() {
		return getCodeFrom($(code).parent().prev(".orc-prelude").get(0)) + "\n" +
			(codemirror ? codemirror.getCode() : getCodeFrom(code));
	}

	function appendEventHtml(html) {
		$events.append(html);
		$events.get(0).scrollTop = $events.get(0).scrollHeight;
	}

	function renderTokenError(p) {
		appendEventHtml('<div class="orc-error">'
			+ p.message
			+ (p.location
				? ' at '
					+ p.location.filename
					+ ':' + p.location.line
					+ '(' + p.location.column + ')'
				: '')
			+ '</div>');
	}

	function renderPublication(p) {
		appendEventHtml('<div class="orc-publication">' + publicationToHtml(p.value) + '</div>');
	}

	function renderPrintln(p) {
		appendEventHtml('<div class="orc-print">' + p.line + '</div>');
	}

	function handlePrompt(v) {
		prompts[v.promptID] = prompt(v.message, function (response) {
			delete prompts[v.promptID];
			if (response != null) {
				job('respondToPrompt', { promptID: v.promptID, response: response });
			} else {
				job('cancelPrompt', { promptID: v.promptID });
			}
		});
	}

	function onEvents(vs) {
		if (!vs || !job) return stop();
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
				handlePrompt(v);
				break;
			}
		});
		job('purgeJobEvents', {}, function () {
			job('jobEvents', {}, onEvents);
		});
	}

	function onError(response, code, exception) {
		// If the job failed while stopping, job may not be set.
		if (job) stop();
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

	function run() {
		$close.hide();
		$run.hide();
		$loading.show();
		$events.get(0).innerHTML = '';
		$events.slideDown("fast");
		// If another job is running, it should stop
		if (currentWidget) currentWidget.stop();
		currentWidget = _this;
		jobsService = null;
		withExecutor(function (executor) {
			executor.compileAndSubmit({devKey: devKey, program: getCode()}, function (id) {
				job = function (method, args, onReady) {
					args.devKey = devKey;
					args.job = id;
					executor[method](args, onReady, onError);
				};
				job('startJob', {}, function () {
					$stop.show();
					job('jobEvents', {}, onEvents);
				});
			}, function (r,c,e) {
				onError(r,c,e);
				$run.show();
				$stop.hide();
				$close.show();
				$loading.hide();
			});
		});
	}

	function stop() {
		if (!job) return;
		job('finishJob', {});
		job = null;
		// close any open prompts
		$.each(prompts, function (i, e) {
			e.close();
		});
		prompts = {};
		$run.show();
		$stop.hide();
		$close.show();
		$loading.hide();
	}

	// public members
	this.stop = stop;
	this.codemirror = function(defaultConfig) {
		var config = $.extend({}, defaultConfig, {
			content: getCodeFrom(code),
			readOnly: (code.tagName != "TEXTAREA"),
			height: $(code).height() + "px"
		});
		// weird DOM manipulation to:
		// 1. put a border around the CodeMirror editor
		// 2. get a handle to an element around the editor
		//    (we'll use it to look for orc-prelude in getCode)
		$(code).wrap("<div class='orc'></div>");
		var div = $(code).parent().get(0);
		codemirror = new CodeMirror(CodeMirror.replace(code), config);
		code = div;
	}

	$(code).wrap($wrapper).after($controls).after($events);
}

/** Widget which is currently running. */
var currentWidget;
var devKey = Orc.query.k ? Orc.query.k : "";
var baseUrl = Orc.baseUrl;

var widgets = [];

$(".orc").each(function (_, code) {
	widgets[widgets.length] = new OrcWidget(code);
});

var executorUrl = Orc.query.mock
	? "mock-executor.js"
	: "/orchard/json/executor?js";

var config = {
	stylesheet: baseUrl + "orc-syntax.css",
	path: baseUrl,
	parserfile: ["orc-parser.js"],
	basefiles: ["codemirror-20080715-extra-min.js"],
	textWrapping: false
};
for (var i in widgets) widgets[i].codemirror(config);

}); // end module

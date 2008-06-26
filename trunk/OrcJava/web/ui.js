//////////////////////////////////////////////////////////
// Utility functions
function createXmlHttpRequest() {
	if (window.XMLHttpRequest) {
		return new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		return new ActiveXObject("Microsoft.XMLHTTP");
	} else {
		throw "XMLHttpRequest not available";
	}
}
function parseQuery() {
	var parts = document.location.search.substr(1).split("&");
	var out = {};
	var tmp;
	for (var i in parts) {
		tmp = parts[i].split("=");
		out[tmp[0]] = tmp[1] ? tmp[1] : true;
	}
	return out;
}
function foreach(vs, f) {
	vs = toArray(vs);
	for (var i in vs) {
		f(vs[i]);
	}
}
function toArray(object) {
	if (!object) return [];
	else if (object.constructor == Array) return object;
	else return [object];
}
/**
 * Load a service into the current page.
 * If a service with the same name was loaded, it will be replaced.
 * If onLoad is provided, it names a function which will be called
 * when the service is ready.
 */
function loadService(name, url, onReady) {
	var old = document.getElementById(name);
	if (old != null) {
		old.parentNode.removeChild(old);
		delete old;
	}
	var head = document.getElementsByTagName("head")[0];
	var script = document.createElement('script');
	script.id = name;
	script.type = "text/javascript";
	script.src = url + "?js" + (onReady ? "&func="+encodeURIComponent(onReady) : "");
	head.appendChild(script);
}
function renderTimestamp(value) {
	document.getElementById("timestamp").innerHTML = value;
}
function renderPublication(p) {
	var pubs = document.getElementById("publications");
	pubs.innerHTML += '<div class="publication">' + publicationToHtml(p.value) + '</div>';
	renderTimestamp(p.timestamp);
	pubs.scrollTop = pubs.scrollHeight;
}
function renderTokenError(p) {
	var pubs = document.getElementById("publications");
	pubs.innerHTML += '<div class="error">'
		+ p.message
		+ (p.location
			? ' at '
				+ p.location.filename
				+ ':' + p.location.line
				+ '(' + p.location.column + ')'
			: '')
		+ '</div>';
	renderTimestamp(p.timestamp);
	pubs.scrollTop = pubs.scrollHeight;
}
function renderPrintln(p) {
	var pubs = document.getElementById("publications");
	pubs.innerHTML += '<div class="print">' + p.line + '</div>';
	renderTimestamp(p.timestamp);
	pubs.scrollTop = pubs.scrollHeight;
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
			return '<font color="blue">' + v + '</font>'
		case 'number':
			return v+'';
		case 'string':
			return '<font color="grey">"'
				+ escapeHtml(v)
					.replace('"', '\\"')
					.replace('\\', '\\\\')
				+ '"</font>';
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
			foreach (v.element, function (e) {
				tmp[tmp.length] = publicationToHtml(e);
			});
			return '[' + tmp.join(', ') + ']';
		case 'ns2:tuple':
			var tmp = [];
			foreach (v.element, function (e) {
				tmp[tmp.length] = publicationToHtml(e);
			});
			return '(' + tmp.join(', ') + ')';
		default: return '<i>' + jsonToHtml(v) + '</i>';
	}
}
/**
 * Extract leading block comments from a program.
 */
function extractComments(text) {
	var comments = "";
	var m;
	// extract block comments
	m = text.match(/^\s*\{-\s*([^]*?)\s*-\}\s*/);
	if (m) return [m[1], text.substr(m[0].length)];
	// extract line comments
	m = text.match(/^\s*((--.*\n)+)\s*/);
	if (m) return [m[1].replace(/--\s*/g, ""), text.substr(m[0].length)];	
	return ["", text];
}

//////////////////////////////////////////////////////////
// Main event handlers

/**
 * Flag used to indicate that publications should
 * be wiped after the current job completes.
 */
var suppressPublications = false;

/**
 * The currently-running job.
 * Anybody who calls finish on it should unset
 * this variable to indicate that the job is no
 * longer valid.
 */
var currentJob = null;
function onJobServiceReady(job) {
	currentJob = job;
	suppressPublications = false;
	currentJob.onError = onError;
	/**
	 * Recursively listen for values
	 * until the job finishes.
	 */
	function onEvents(vs) {
		if (!vs) {
			if (currentJob) {
				// finish the job
				currentJob.finish({}, onJobFinish);
				currentJob = null;
			} else {
				// the job was already finished
				// (i.e. by onUnload)
			}
			return;
		}
		if (suppressPublications) return;
		foreach(vs, function (v) {
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
			}
		});
		currentJob.listen({}, onEvents);
	}
	// Start the job and then listen for published values.
	currentJob.start({}, function () {
		document.getElementById("stopButton").disabled = false;
		currentJob.listen({}, onEvents);
	});
}
function onJobFinish() {
	document.getElementById("runButton").disabled = false;
	document.getElementById("stopButton").disabled = true;
	document.getElementById("loading").style.visibility = "hidden";
	document.getElementById("timestamp").style.visibility = "hidden";
	currentJob = null;
}
function onRunButton() {
	document.getElementById("runButton").disabled = true;
	document.getElementById("publications").innerHTML = "";
	document.getElementById("loading").style.visibility = "";
	document.getElementById("timestamp").style.visibility = "";
	executorService.compileAndSubmit({program: program.getCode()}, function (url) {
		loadService("jobService", url, "onJobServiceReady");
	});
}
function onStopButton() {
	document.getElementById("stopButton").disabled = true;
	currentJob.halt({});
	// finish will be called by
	// the publish listener
}
function onUnload() {
	if (currentJob) currentJob.finish({});
}
var executorService = null;
function onExecutorServiceReady(service) {
	executorService = service;
	executorService.onError = onError;
	document.getElementById("runButton").disabled = false;
}
function onError(response, code, exception) {
	document.getElementById("stopButton").disabled = true;
	document.getElementById("loading").style.visibility = "hidden";
	document.getElementById("timestamp").style.visibility = "hidden";
	// stop the current job, if possible
	var job = currentJob;
	onJobFinish();
	if (job) {
		// prevent this from running again
		// even if finish has an error
		currentJob = null;
		job.finish({});
	}
	// unwrap response if possible
	if (response) {
		if (response.faultstring) {
			response = response.faultstring;
		}
	} else {
		response = exception;
	}
	var pubs = document.getElementById("publications");
	pubs.innerHTML += '<div class="error">Service error: ' + jsonToHtml(response) + '</div>';
	pubs.scrollTop = pubs.scrollHeight;
	document.getElementById("runButton").disabled = false;
}
function loadCode(filename) {
	if (currentJob) {
		suppressPublications = true;
		currentJob.halt({});
	}
	document.getElementById("publications").innerHTML = "";
	try {
		var xhr = createXmlHttpRequest();
		xhr.onreadystatechange = function () {
			if (xhr.readyState == 4) {
				var parts = extractComments(xhr.responseText);
				document.getElementById("comments").innerHTML =
					escapeHtml(parts[0]).replace("\n", "<br>");
				program.setCode(parts[1]);
				program.editor.syntaxHighlight('init');
			}
		};
		xhr.open("GET", filename, true);
		xhr.send("");
	} catch (e) {
		alert("Unable to load " + filename + " due to exception: " + e);
	}
}

//////////////////////////////////////////////////////////
// Configuration
CodePress.languages = { "orc-demo": "Orc-Demo", "orc": "Orc" };
var query = parseQuery();
var executorServiceUrl = query.mock
	? "mock-executor.js"
	: "json/executor";

//////////////////////////////////////////////////////////
// Go go gadget executor

loadService("executorService", executorServiceUrl, "onExecutorServiceReady");

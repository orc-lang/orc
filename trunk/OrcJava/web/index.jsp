<html>
<head>
<title>Orchard Demo</title>
<style type="text/css">
#publications {
	border: 1px solid gray;
	overflow: auto;
}
div.publication {
	border-bottom: 1px solid gray;
	font-family: monospace;
	font-size: 13px;
}
div.error {
	border: 3px solid red;
	font-family: monospace;
	color: red;
	font-size: larger;
}
</style>
</head>
<body onunload="onUnload()">
<textarea id="program" class="codepress orc" style="width: 600px; height: 200px" wrap="off">
-- Metronome which publishes every second
def M(n) = ("Metronome", n) | Rtimer(1000) >> M(n+1)

M(1)
</textarea>
<p><input type="submit" value="Run" onClick="onRunButton()" id="runButton" disabled="true">
&nbsp;<input type="submit" value="Stop" onClick="onStopButton()" id="stopButton" disabled="true">
&nbsp;<img id="loading" src="loading.gif" width="126" height="22" style="visibility: hidden" align="top">
&nbsp;<span id="timestamp" style="visibility: hidden"></span>
<div id="publications" style="width: 600px; height: 200px"></div>
<script src="codepress/codepress.js" type="text/javascript"></script>
<script language="javascript">
//////////////////////////////////////////////////////////
// Configuration
CodePress.languages = { orc: "Orc" };
var executorServiceUrl;
if (document.location.search == "?mock") {
	executorServiceUrl = "mock-executor.js";
} else {
	executorServiceUrl = "json/executor";
}

//////////////////////////////////////////////////////////
// Utility functions
function foreach(vs, f) {
	vs = toArray(vs);
	for (var i in vs) {
		f(vs[i]);
	}
}
function toArray(object) {
	if (object.constructor == Array) return object;
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
/**
 * Convert arbitrary JSON values to
 * pretty-printed HTML
 */
function jsonToHtml(v) {
	function fromString(v) {
		v = v.replace(/&/g, '&amp;');
		v = v.replace(/</g, '&lt;');
		return '<b>'+v+'</b>';
	}
	function fromArray(v) {
		if (v.length == 0) return '[]';
		var out = '[';
		out += jsonToHtml(v[0]);
		for (var i = 1; i < v.length; ++i) {
			out += ', ' + jsonToHtml(v[i]);
		}
		return out + ']';
	}
	function fromObject(v) {
		var out = '{';
		for (var k in v) {
			out += '<i>'+k+'</i>: ' + jsonToHtml(v[k]) + ', ';
		}
		return out.substring(0, out.length-2) + '}';
	}
	switch (typeof v) {
		case 'boolean':
		case 'number': return v+'';
		case 'string': return fromString(v);
		case 'object':
			if (v == null) return 'null';
			if (v.constructor == Array) return fromArray(v);
			if (v.constructor == Object) return fromObject(v);
	}
}
/**
 * Convert publication values to JSON.
 */
function publicationToJson(v) {
	if (v == null) return v;
	switch (v["@xsi.type"]) {
		// XSD types
		case 'xs:string': return v.$;
		case 'xs:integer':
		case 'xs:long':
		case 'xs:short':
		case 'xs:int': return parseInt(v.$);
		case 'xs:double':
		case 'xs:decimal':
		case 'xs:float': return parseFloat(v.$);
		case 'xs:boolean': return v.$ == 'true';
		// OIL types
		// FIXME: server should use better namespace than ns2
		case 'ns2:constant': return publicationToJson(v.value);
		case 'ns2:list':
		case 'ns2:tuple':
			var tmp = [];
			foreach (v.element, function (e) {
				tmp[tmp.length] = publicationToJson(e);
			});
			return tmp;
		default: return v;
	}
}
/**
 * Convert publication values to HTML.
 * These have a bit more structure than arbitrary JSON.
 */
function publicationToHtml(v) {
	return jsonToHtml(publicationToJson(v))
}

//////////////////////////////////////////////////////////
// Main event handlers

/**
 * The currently-running job.
 * Anybody who calls finish on it should unset
 * this variable to indicate that the job is no
 * longer valid.
 */
var currentJob = null;
function onJobServiceReady(job) {
	currentJob = job;
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
		foreach(vs, function (v) {
			switch (v["@xsi.type"]) {
			case "ns2:tokenErrorEvent":
				renderTokenError(v);
				break;
			case "ns2:publicationEvent":
				renderPublication(v);
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


//////////////////////////////////////////////////////////
// Go go gadget executor

loadService("executorService", executorServiceUrl, "onExecutorServiceReady");
</script>
</body>

<html>
<head>
<title>Orchard Demo</title>
<style type="text/css">
#program {
	border: 2px inset gray;
	width: 600px;
	height: 200px;
}
#publications {
	border: 2px inset gray;
	width: 600px;
	height: 200px;
	overflow: auto;
}
div.publication {
	border-bottom: 1px solid gray;
	font-family: monospace;
}
</style>
<script language="javascript">
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
var currentJob = null;
function onJobServiceReady(job) {
	currentJob = job;
	/**
	 * Recursively listen for values
	 * until the job finishes.
	 */
	function onPublish(v) {
		if (!v) {
				job.finish({}, onJobFinish);
				return;
		}
		renderPublications(v);
		job.listen({}, onPublish);
	}
	// Start the job and then listen for published values.
	job.start({}, function () {
		document.getElementById("stopButton").disabled = false;
		job.listen({}, onPublish);
	});
}
function onJobFinish() {
	document.getElementById("runButton").disabled = false;
	document.getElementById("loading").style.visibility = "hidden";
	currentJob = null;
}
function onRunButton() {
	document.getElementById("runButton").disabled = true;
	document.getElementById("publications").innerHTML = "";
	document.getElementById("loading").style.visibility = "";
	var textarea = document.getElementById("program");
	executorService.compileAndSubmit({program: textarea.value}, function (url) {
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
function renderPublications(ps) {
	var pubs = document.getElementById("publications");
	foreach(ps, function (p) {
		pubs.innerHTML += '<div class="publication">' + publicationToHtml(p.value) + '</div>';
	});
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
</script>
<script language="javascript" src="json/executor?js"/></script>
<!-- <script language="javascript" src="mock-executor.js"/></script> -->
</head>
<body onunload="onUnload()">
<textarea id="program" rows="5" cols="80">
def M(n) = n | Rtimer(2000) >> M(n+1)
M(1)
</textarea>
<p><input type="submit" value="Run" onClick="onRunButton()" id="runButton">
&nbsp;<input type="submit" value="Stop" onClick="onStopButton()" id="stopButton" disabled="true">
&nbsp;<img id="loading" src="loading.gif" width="126" height="22" style="visibility: hidden" align="top">
<div id="publications"></div>
</body>
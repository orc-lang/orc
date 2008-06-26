<html>
<head>
<title></title>
<style type="text/css">
#publications {
	border: 1px solid gray;
	overflow: auto;
	font-size: 20pt;
}
div.publication {
	border-bottom: 1px solid gray;
	font-family: monospace;
	font-weight: bold;
}
div.error {
	border: 3px solid red;
	color: red;
	font-size: larger;
}
div.print {
	border-bottom: 1px solid gray;
	font-color: #666666;
}
body {
	font-family: sans-serif;
	font-size: 20pt;
}
ul {
	list-style-type: none;
	padding-left: 0px;
	padding-right: 1em;
	text-align: right;
}
li {
	margin-bottom: 1ex;
}
a {
	color: black;
	text-decoration: none;
	font-variant: small-caps;
}
#program_cp {
	font-weight: bold;
	font-size: 20pt;
}
#comments {
	padding-bottom: 1em;
	font-size: 20pt;
}
</style>
</head>
<body onunload="onUnload()">
<table width="100%" height="100%"><tr><td valign="top" width="100">
<ul>
<li><a href="#" onclick="loadCode('demo/call.orc')">Call</a></li>
<li><a href="#" onclick="loadCode('demo/bar.orc')">Bar</a></li>
<li><a href="#" onclick="loadCode('demo/push.orc')">Push</a></li>
<li><a href="#" onclick="loadCode('demo/pushbind.orc')">Push/Bind</a></li>
<li><a href="#" onclick="loadCode('demo/barpush.orc')">Bar/Push</a></li>
<li><a href="#" onclick="loadCode('demo/pull.orc')">Pull</a></li>
<li><a href="#" onclick="loadCode('demo/pulldoesnotwait.orc')">Pull Doesn't Wait</a></li>
<li><a href="#" onclick="loadCode('demo/fundamentals.orc')">Fundamental Sites</a></li>
<li><a href="#" onclick="loadCode('demo/defs.orc')">Expressions</a></li>
<li><a href="#" onclick="loadCode('demo/metronome.orc')">Metronome</a></li>
<li><a href="#" onclick="loadCode('demo/queryaccept.orc')">Expression Use</a></li>
<li><a href="#" onclick="loadCode('demo/timeout.orc')">Timeout</a></li>
<li><a href="#" onclick="loadCode('demo/forkjoin.orc')">Fork/Join</a></li>
<li><a href="#" onclick="loadCode('demo/priority.orc')">Priority</a></li>
<li><a href="#" onclick="loadCode('demo/parallelor.orc')">Parallel Or</a></li>
<li><a href="#" onclick="loadCode('demo/tally.orc')">Tally</a></li>
<li><a href="#" onclick="loadCode('demo/music_calendar.orc')">Music Calendar</a></li>
</ul>
</td><td valign="top">
<div id="comments"></div>
<textarea spellcheck="false" id="program" class="codepress orc-demo linenumbers-off" style="width: 100%; height: 40%" wrap="off">
</textarea>
<p><input type="submit" value="Run" onClick="onRunButton()" id="runButton" disabled="true">
&nbsp;<input type="submit" value="Stop" onClick="onStopButton()" id="stopButton" disabled="true">
&nbsp;<input type="checkbox" onClick="program.toggleEditor()" checked>&nbsp;Syntax
&nbsp;<img id="loading" src="loading.gif" width="126" height="22" style="visibility: hidden" align="top">
&nbsp;<span id="timestamp" style="visibility: hidden"></span>
<div id="publications" style="width: 100%; height: 30%"></div>
<script src="codepress/codepress.js" type="text/javascript"></script>
<script src="ui.js" type="text/javascript"></script>
</td></tr></table>
</body>
</html>

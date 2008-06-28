<html>
<!--
To play with this in your browser without running an Orchard server,
load it as demo.jsp?mock
-->
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
#menu {
	list-style-type: none;
	padding-right: 1.1em;
	direction: rtl;
}
#menu li {
	margin-bottom: 1ex;
	font-size: 10pt;
	color: black;
	text-decoration: none;
	font-variant: small-caps;
	cursor: pointer;
}
#menu li.selected {
	list-style-type: square;
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
<ul id="menu">
<li onclick="onMenu(this,'demo/call.orc')">Call</li>
<li onclick="onMenu(this,'demo/bar.orc')">Bar</li>
<li onclick="onMenu(this,'demo/push.orc')">Push</li>
<li onclick="onMenu(this,'demo/pushbind.orc')">Push/Bind</li>
<li onclick="onMenu(this,'demo/barpush.orc')">Bar/Push</li>
<li onclick="onMenu(this,'demo/pull.orc')">Pull</li>
<li onclick="onMenu(this,'demo/pulldoesnotwait.orc')">Blocking</li>
<li onclick="onMenu(this,'demo/fundamentals.orc')">Fundamental Sites</li>
<li onclick="onMenu(this,'demo/metronome.orc')">Metronome</li>
<li onclick="onMenu(this,'demo/metronome2.orc')">Two Metronomes</li>
<li onclick="onMenu(this,'demo/queryaccept.orc')">Using Metronome</li>
<li onclick="onMenu(this,'demo/timeout.orc')">Timeout</li>
<li onclick="onMenu(this,'demo/delay.orc')">Delay</li>
<li onclick="onMenu(this,'demo/priority.orc')">Priority</li>
<li onclick="onMenu(this,'demo/forkjoin.orc')">Fork/Join</li>
<li onclick="onMenu(this,'demo/parallelor.orc')">Parallel Or</li>
<li onclick="onMenu(this,'demo/tally.orc')">Tally</li>
<li onclick="onMenu(this,'demo/music_calendar.orc')">Music Calendar</li>
</ul>
</td><td valign="top">
<div id="comments"></div>
<textarea spellcheck="false" id="program" class="codepress orc-demo linenumbers-off" style="width: 100%; height: 320px" wrap="off">
</textarea>
<p><input type="submit" value="Run" onClick="onRunButton()" id="runButton" disabled="true">
&nbsp;<input type="submit" value="Stop" onClick="onStopButton()" id="stopButton" disabled="true" style="visibility: hidden">
&nbsp;<input type="checkbox" onClick="program.toggleEditor()" checked>&nbsp;Syntax
&nbsp;<img id="loading" src="loading.gif" style="visibility: hidden" align="top">
&nbsp;<span id="timestamp" style="visibility: hidden"></span>
<div id="publications" style="width: 100%; height: 180px"></div>
<script src="codepress/codepress.js" type="text/javascript"></script>
<script src="ui.js" type="text/javascript"></script>
<script type="text/javascript">
function onMenu(link, file) {
	var menu = document.getElementById('menu');
	var items = menu.getElementsByTagName('li');
	for (var i in items) {
		items[i].className = "";
	}
	link.className = "selected";
	loadCode(file);
}
</script>
</td></tr></table>
</body>
</html>

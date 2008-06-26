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
	color: red;
	font-size: larger;
}
div.print {
	border-bottom: 1px solid gray;
	font-color: gray;
}
</style>
</head>
<body onunload="onUnload()">
<textarea spellcheck="false" id="program" class="codepress orc" style="width: 600px; height: 300px" wrap="off">
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
<script src="ui.js" type="text/javascript"></script>
</body>
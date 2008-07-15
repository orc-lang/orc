/*
Copyright (c) 2007 Marijn Haverbeke

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any
damages arising from the use of this software.

Permission is granted to anyone to use this software for any
purpose, including commercial applications, and to alter it and
redistribute it freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must
   not claim that you wrote the original software. If you use this
   software in a product, an acknowledgment in the product
   documentation would be appreciated but is not required.

2. Altered source versions must be plainly marked as such, and must
   not be misrepresented as being the original software.

3. This notice may not be removed or altered from any source
   distribution.

Marijn Haverbeke
marijnh at gmail

ATTENTION: this version has been minified and combined into
a single file by Adrian Quark, but is otherwise unaltered.
*/

var CodeMirrorConfig=window.CodeMirrorConfig||{};var CodeMirror=(function(){function C(F,E){for(var D in E){if(!F.hasOwnProperty(D)){F[D]=E[D]}}}function B(F,E){for(var D=0;D<F.length;D++){E(F[D])}}C(CodeMirrorConfig,{stylesheet:"",path:"",parserfile:[],basefiles:["util.js","stringstream.js","select.js","undo.js","editor.js","tokenize.js"],linesPerPass:15,passDelay:200,continuousScanning:false,saveFunction:null,undoDepth:20,undoDelay:800,disableSpellcheck:true,textWrapping:true,readOnly:false,width:"100%",height:"300px",parserConfig:null});function A(F,E){this.options=E=E||{};C(E,CodeMirrorConfig);frame=document.createElement("IFRAME");frame.style.border="0";frame.style.width=E.width;frame.style.height=E.height;frame.style.display="block";if(F.appendChild){F.appendChild(frame)}else{F(frame)}frame.CodeMirror=this;this.win=frame.contentWindow;if(typeof E.parserfile=="string"){E.parserfile=[E.parserfile]}var D=['<html><head><link rel="stylesheet" type="text/css" href="'+E.stylesheet+'"/>'];B(E.basefiles.concat(E.parserfile),function(H){D.push('<script type="text/javascript" src="'+E.path+H+'"><\/script>')});D.push('</head><body style="border-width: 0;" class="editbox" spellcheck="'+(E.disableSpellcheck?"false":"true")+'"></body></html>');var G=this.win.document;G.open();G.write(D.join(""));G.close()}A.prototype={getCode:function(){return this.editor.getCode()},setCode:function(D){this.editor.importCode(D)},focus:function(){this.win.focus()},jumpToChar:function(E,D){this.editor.jumpToChar(E,D);this.focus()},jumpToLine:function(D){this.editor.jumpToLine(D);this.focus()},currentLine:function(){return this.editor.currentLine()},selection:function(){return this.editor.selectedText()},reindent:function(){this.editor.reindent()},replaceSelection:function(F,E){var D=this.editor.replaceSelection(F);if(E){this.focus()}return D},replaceChars:function(F,E,D){this.editor.replaceChars(F,E,D)},getSearchCursor:function(E,D){return this.editor.getSearchCursor(E,D)}};A.replace=function(D){if(typeof D=="string"){D=document.getElementById(D)}return function(E){D.parentNode.replaceChild(E,D)}};A.fromTextArea=function(G,F){if(typeof G=="string"){G=document.getElementById(G)}F=F||{};if(G.style.width){F.width=G.style.width}if(G.style.height){F.height=G.style.height}if(F.content==null){F.content=G.value}if(G.form){function D(){G.value=E.getCode()}if(typeof G.form.addEventListener=="function"){G.form.addEventListener("submit",D,false)}else{G.form.attachEvent("onsubmit",D)}}function H(I){if(G.nextSibling){G.parentNode.insertBefore(I,G.nextSibling)}else{G.parentNode.appendChild(I)}}G.style.display="none";var E=new A(H,F);return E};return A})();

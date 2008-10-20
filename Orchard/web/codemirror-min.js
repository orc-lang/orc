/**
 * CodeMirror 0.58 http://marijn.haverbeke.nl/codemirror/
 *
 * This is a minified version created by Adrian Quark for the Orc project:
 * http://orc.csres.utexas.edu. It is otherwise unmodified.
 *
 * ------------ ORIGINAL LICENSE FOLLOWS -----------------
 *
 * Copyright (c) 2007 Marijn Haverbeke
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any
 * damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any
 * purpose, including commercial applications, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must
 *    not claim that you wrote the original software. If you use this
 *    software in a product, an acknowledgment in the product
 *    documentation would be appreciated but is not required.
 *
 * 2. Altered source versions must be plainly marked as such, and must
 *    not be misrepresented as being the original software.
 *
 * 3. This notice may not be removed or altered from any source
 *    distribution.
 *
 * Marijn Haverbeke
 * marijnh at gmail
 */
var CodeMirrorConfig=window.CodeMirrorConfig||{};var CodeMirror=(function(){function C(D,F){for(var E in F){if(!D.hasOwnProperty(E)){D[E]=F[E]}}}function B(F,E){for(var D=0;D<F.length;D++){E(F[D])}}C(CodeMirrorConfig,{stylesheet:"",path:"",parserfile:[],basefiles:["util.js","stringstream.js","select.js","undo.js","editor.js","tokenize.js"],linesPerPass:15,passDelay:200,continuousScanning:false,saveFunction:null,onChange:null,undoDepth:20,undoDelay:800,disableSpellcheck:true,textWrapping:true,readOnly:false,width:"100%",height:"300px",autoMatchParens:false,parserConfig:null,dumbTabs:false});function A(D,E){this.options=E=E||{};C(E,CodeMirrorConfig);var H=this.frame=document.createElement("IFRAME");H.style.border="0";H.style.width=E.width;H.style.height=E.height;H.style.display="block";if(D.appendChild){D.appendChild(H)}else{D(H)}H.CodeMirror=this;this.win=H.contentWindow;if(typeof E.parserfile=="string"){E.parserfile=[E.parserfile]}if(typeof E.stylesheet=="string"){E.stylesheet=[E.stylesheet]}var F=["<html><head>"];B(E.stylesheet,function(I){F.push('<link rel="stylesheet" type="text/css" href="'+I+'"/>')});B(E.basefiles.concat(E.parserfile),function(I){F.push('<script type="text/javascript" src="'+E.path+I+'"><\/script>')});F.push('</head><body style="border-width: 0;" class="editbox" spellcheck="'+(E.disableSpellcheck?"false":"true")+'"></body></html>');var G=this.win.document;G.open();G.write(F.join(""));G.close()}A.prototype={getCode:function(){return this.editor.getCode()},setCode:function(D){this.editor.importCode(D)},focus:function(){this.win.focus()},jumpToChar:function(E,D){this.editor.jumpToChar(E,D);this.focus()},jumpToLine:function(D){this.editor.jumpToLine(D);this.focus()},currentLine:function(){return this.editor.currentLine()},selection:function(){return this.editor.selectedText()},reindent:function(){this.editor.reindent()},replaceSelection:function(F,E){var D=this.editor.replaceSelection(F);if(E){this.focus()}return D},replaceChars:function(E,F,D){this.editor.replaceChars(E,F,D)},getSearchCursor:function(E,D){return this.editor.getSearchCursor(E,D)}};A.replace=function(D){if(typeof D=="string"){D=document.getElementById(D)}return function(E){D.parentNode.replaceChild(E,D)}};A.fromTextArea=function(F,E){if(typeof F=="string"){F=document.getElementById(F)}E=E||{};if(F.style.width){E.width=F.style.width}if(F.style.height){E.height=F.style.height}if(E.content==null){E.content=F.value}if(F.form){function D(){F.value=H.getCode()}if(typeof F.form.addEventListener=="function"){F.form.addEventListener("submit",D,false)}else{F.form.attachEvent("onsubmit",D)}}function G(I){if(F.nextSibling){F.parentNode.insertBefore(I,F.nextSibling)}else{F.parentNode.appendChild(I)}}F.style.display="none";var H=new A(G,E);return H};A.isProbablySupported=function(){var D;if(window.opera){return Number(window.opera.version())>=9.52}else{if(/Apple Computers, Inc/.test(navigator.vendor)&&(D=navigator.userAgent.match(/Version\/(\d+(?:\.\d+)?)\./))){return Number(D[1])>=3}else{if(document.selection&&window.ActiveXObject&&(D=navigator.userAgent.match(/MSIE (\d+(?:\.\d*)?)\b/))){return Number(D[1])>=6}else{if(D=navigator.userAgent.match(/gecko\/(\d{8})/i)){return Number(D[1])>=20050901}else{if(/Chrome\//.test(navigator.userAgent)){return true}else{return null}}}}}};return A})();
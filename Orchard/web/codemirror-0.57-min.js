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

var CodeMirrorConfig=window.CodeMirrorConfig||{};
var CodeMirror=(function(){
function setDefaults(_1,_2){
for(var _3 in _2){
if(!_1.hasOwnProperty(_3)){
_1[_3]=_2[_3];
}
}
};
function forEach(_4,_5){
for(var i=0;i<_4.length;i++){
_5(_4[i]);
}
};
setDefaults(CodeMirrorConfig,{stylesheet:"",path:"",parserfile:[],basefiles:["util.js","stringstream.js","select.js","undo.js","editor.js","tokenize.js"],linesPerPass:15,passDelay:200,continuousScanning:false,saveFunction:null,undoDepth:20,undoDelay:800,disableSpellcheck:true,textWrapping:true,readOnly:false,width:"100%",height:"300px",parserConfig:null});
function CodeMirror(_7,_8){
this.options=_8=_8||{};
setDefaults(_8,CodeMirrorConfig);
frame=document.createElement("IFRAME");
frame.style.border="0";
frame.style.width=_8.width;
frame.style.height=_8.height;
frame.style.display="block";
if(_7.appendChild){
_7.appendChild(frame);
}else{
_7(frame);
}
frame.CodeMirror=this;
this.win=frame.contentWindow;
if(typeof _8.parserfile=="string"){
_8.parserfile=[_8.parserfile];
}
var _9=["<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\""+_8.stylesheet+"\"/>"];
forEach(_8.basefiles.concat(_8.parserfile),function(_a){
_9.push("<script type=\"text/javascript\" src=\""+_8.path+_a+"\"></script>");
});
_9.push("</head><body style=\"border-width: 0;\" class=\"editbox\" spellcheck=\""+(_8.disableSpellcheck?"false":"true")+"\"></body></html>");
var _b=this.win.document;
_b.open();
_b.write(_9.join(""));
_b.close();
};
CodeMirror.prototype={getCode:function(){
return this.editor.getCode();
},setCode:function(_c){
this.editor.importCode(_c);
},focus:function(){
this.win.focus();
},jumpToChar:function(_d,_e){
this.editor.jumpToChar(_d,_e);
this.focus();
},jumpToLine:function(_f){
this.editor.jumpToLine(_f);
this.focus();
},currentLine:function(){
return this.editor.currentLine();
},selection:function(){
return this.editor.selectedText();
},reindent:function(){
this.editor.reindent();
},replaceSelection:function(_10,_11){
var _12=this.editor.replaceSelection(_10);
if(_11){
this.focus();
}
return _12;
},replaceChars:function(_13,_14,end){
this.editor.replaceChars(_13,_14,end);
},getSearchCursor:function(_16,_17){
return this.editor.getSearchCursor(_16,_17);
}};
CodeMirror.replace=function(_18){
if(typeof _18=="string"){
_18=document.getElementById(_18);
}
return function(_19){
_18.parentNode.replaceChild(_19,_18);
};
};
CodeMirror.fromTextArea=function(_1a,_1b){
if(typeof _1a=="string"){
_1a=document.getElementById(_1a);
}
_1b=_1b||{};
if(_1a.style.width){
_1b.width=_1a.style.width;
}
if(_1a.style.height){
_1b.height=_1a.style.height;
}
if(_1b.content==null){
_1b.content=_1a.value;
}
if(_1a.form){
function updateField(){
_1a.value=_1c.getCode();
};
if(typeof _1a.form.addEventListener=="function"){
_1a.form.addEventListener("submit",updateField,false);
}else{
_1a.form.attachEvent("onsubmit",updateField);
}
}
function insert(_1d){
if(_1a.nextSibling){
_1a.parentNode.insertBefore(_1d,_1a.nextSibling);
}else{
_1a.parentNode.appendChild(_1d);
}
};
_1a.style.display="none";
var _1c=new CodeMirror(insert,_1b);
return _1c;
};
return CodeMirror;
})();

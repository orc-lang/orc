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

var Editor=(function(){
var _1e={"P":true,"DIV":true,"LI":true};
function safeWhiteSpace(n){
var _20=[],nb=true;
for(;n>0;n--){
_20.push((nb||n==1)?nbsp:" ");
nb=!nb;
}
return _20.join("");
};
function splitSpaces(_22){
return _22.replace(/[\t \u00a0]{2,}/g,function(s){
return safeWhiteSpace(s.length);
});
};
function asEditorLines(_24){
return splitSpaces(_24.replace(/\u00a0/g," ")).replace(/\r\n?/g,"\n").split("\n");
};
function simplifyDOM(_25){
var doc=_25.ownerDocument;
var _27=[];
var _28=false;
function simplifyNode(_29){
if(_29.nodeType==3){
var _2a=_29.nodeValue=splitSpaces(_29.nodeValue.replace(/[\n\r]/g,""));
if(_2a.length){
_28=false;
}
_27.push(_29);
}else{
if(_29.nodeName=="BR"&&_29.childNodes.length==0){
_28=false;
_27.push(_29);
}else{
forEach(_29.childNodes,simplifyNode);
if(!_28&&_1e.hasOwnProperty(_29.nodeName)){
_28=true;
_27.push(doc.createElement("BR"));
}
}
}
};
simplifyNode(_25);
return _27;
};
function traverseDOM(_2b){
function yield(_2c,c){
cc=c;
return _2c;
};
function push(fun,arg,c){
return function(){
return fun(arg,c);
};
};
function stop(){
cc=stop;
throw StopIteration;
};
var cc=push(scanNode,_2b,stop);
var _32=_2b.ownerDocument;
var _33=[];
function pointAt(_34){
var _35=_34.parentNode;
var _36=_34.nextSibling;
if(_36){
return function(_37){
_35.insertBefore(_37,_36);
};
}else{
return function(_38){
_35.appendChild(_38);
};
}
};
var _39=null;
function insertPart(_3a){
var _3b="\n";
if(_3a.nodeType==3){
_3b=_3a.nodeValue;
var _3c=_32.createElement("SPAN");
_3c.className="part";
_3c.appendChild(_3a);
_3a=_3c;
_3a.currentText=_3b;
}
_3a.dirty=true;
_33.push(_3a);
_39(_3a);
return _3b;
};
function writeNode(_3d,c){
var _3f=[];
forEach(simplifyDOM(_3d),function(_40){
_3f.push(insertPart(_40));
});
return yield(_3f.join(""),c);
};
function partNode(_41){
if(_41.nodeName=="SPAN"&&_41.childNodes.length==1&&_41.firstChild.nodeType==3){
_41.currentText=_41.firstChild.nodeValue;
return true;
}
return false;
};
function scanNode(_42,c){
if(_42.nextSibling){
c=push(scanNode,_42.nextSibling,c);
}
if(partNode(_42)){
_33.push(_42);
return yield(_42.currentText,c);
}else{
if(_42.nodeName=="BR"){
_33.push(_42);
return yield("\n",c);
}else{
_39=pointAt(_42);
removeElement(_42);
return writeNode(_42,c);
}
}
};
return {next:function(){
return cc();
},nodes:_33};
};
function nodeSize(_44){
if(_44.nodeName=="BR"){
return 1;
}else{
return _44.currentText.length;
}
};
function startOfLine(_45){
while(_45&&_45.nodeName!="BR"){
_45=_45.previousSibling;
}
return _45;
};
function cleanText(_46){
return _46.replace(/\u00a0/g," ");
};
function SearchCursor(_47,_48,_49){
this.editor=_47;
this.history=_47.history;
this.history.commit();
this.atOccurrence=false;
this.fallbackSize=15;
var _4a;
if(_49&&(_4a=select.cursorPos(this.editor.container))){
this.line=_4a.node;
this.offset=_4a.offset;
}else{
this.line=null;
this.offset=0;
}
this.valid=!!_48;
var _4b=_48.split("\n"),_4c=this;
this.matches=(_4b.length==1)?function(){
var _4d=cleanText(_4c.history.textAfter(_4c.line).slice(_4c.offset)).indexOf(_48);
if(_4d>-1){
return {from:{node:_4c.line,offset:_4c.offset+_4d},to:{node:_4c.line,offset:_4c.offset+_4d+_48.length}};
}
}:function(){
var _4e=cleanText(_4c.history.textAfter(_4c.line).slice(_4c.offset));
var _4f=_4e.lastIndexOf(_4b[0]);
if(_4f==-1||_4f!=_4e.length-_4b[0].length){
return false;
}
var _50=_4c.offset+_4f;
var _51=_4c.history.nodeAfter(_4c.line);
for(var i=1;i<_4b.length-1;i++){
if(cleanText(_4c.history.textAfter(_51))!=_4b[i]){
return false;
}
_51=_4c.history.nodeAfter(_51);
}
if(cleanText(_4c.history.textAfter(_51)).indexOf(_4b[_4b.length-1])!=0){
return false;
}
return {from:{node:_4c.line,offset:_50},to:{node:_51,offset:_4b[_4b.length-1].length}};
};
};
SearchCursor.prototype={findNext:function(){
if(!this.valid){
return false;
}
this.atOccurrence=false;
var _53=this;
if(this.line&&!this.line.parentNode){
this.line=null;
this.offset=0;
}
function saveAfter(pos){
if(_53.history.textAfter(pos.node).length<pos.offset){
_53.line=pos.node;
_53.offset=pos.offset+1;
}else{
_53.line=_53.history.nodeAfter(pos.node);
_53.offset=0;
}
};
while(true){
var _55=this.matches();
if(_55){
this.atOccurrence=_55;
saveAfter(_55.from);
return true;
}
this.line=this.history.nodeAfter(this.line);
this.offset=0;
if(!this.line){
this.valid=false;
return false;
}
}
},select:function(){
if(this.atOccurrence){
select.setCursorPos(this.editor.container,this.atOccurrence.from,this.atOccurrence.to);
select.scrollToCursor(this.editor.container);
}
},replace:function(_56){
if(this.atOccurrence){
this.editor.replaceRange(this.atOccurrence.from,this.atOccurrence.to,_56);
this.line=this.atOccurrence.from.node;
this.offset=this.atOccurrence.from.offset;
this.atOccurrence=false;
}
}};
function Editor(_57){
this.options=_57;
this.parent=parent;
this.doc=document;
this.container=this.doc.body;
this.win=window;
this.history=new History(this.container,this.options.undoDepth,this.options.undoDelay,this);
if(!Editor.Parser){
throw "No parser loaded.";
}
if(_57.parserConfig&&Editor.Parser.configure){
Editor.Parser.configure(_57.parserConfig);
}
if(!_57.textWrapping){
this.container.style.whiteSpace="pre";
}
select.setCursorPos(this.container,{node:null,offset:0});
this.dirty=[];
if(_57.content){
this.importCode(_57.content);
}else{
this.container.appendChild(this.doc.createElement("SPAN"));
}
if(!_57.readOnly){
if(_57.continuousScanning!==false){
this.scanner=this.documentScanner(_57.linesPerPass);
this.delayScanning();
}
function setEditable(){
if(document.body.contentEditable!=undefined&&/MSIE/.test(navigator.userAgent)){
document.body.contentEditable="true";
}else{
document.designMode="on";
}
};
try{
setEditable();
}
catch(e){
var _58=addEventHandler(document,"focus",function(){
removeEventHandler(_58);
setEditable();
});
}
addEventHandler(document,"keydown",method(this,"keyDown"));
addEventHandler(document,"keypress",method(this,"keyPress"));
addEventHandler(document,"keyup",method(this,"keyUp"));
addEventHandler(document.body,"paste",method(this,"markCursorDirty"));
addEventHandler(document.body,"cut",method(this,"markCursorDirty"));
}
};
function isSafeKey(_59){
return (_59>=16&&_59<=18)||(_59>=33&&_59<=40);
};
Editor.prototype={importCode:function(_5a){
this.history.push(null,null,asEditorLines(_5a));
this.history.reset();
},getCode:function(){
if(!this.container.firstChild){
return "";
}
var _5b=[];
forEach(traverseDOM(this.container.firstChild),method(_5b,"push"));
return cleanText(_5b.join(""));
},jumpToLine:function(_5c){
if(_5c<=1||!this.container.firstChild){
select.focusAfterNode(null,this.container);
}else{
var pos=this.container.firstChild;
while(true){
if(pos.nodeName=="BR"){
_5c--;
}
if(_5c<=1||!pos.nextSibling){
break;
}
pos=pos.nextSibling;
}
select.focusAfterNode(pos,this.container);
}
select.scrollToCursor(this.container);
},currentLine:function(){
var pos=select.cursorPos(this.container,true),_5f=1;
if(!pos){
return 1;
}
for(cursor=pos.node;cursor;cursor=cursor.previousSibling){
if(cursor.nodeName=="BR"){
_5f++;
}
}
return _5f;
},selectedText:function(){
var h=this.history;
h.commit();
var _61=select.cursorPos(this.container,true),end=select.cursorPos(this.container,false);
if(!_61||!end){
return "";
}
if(_61.node==end.node){
return h.textAfter(_61.node).slice(_61.offset,end.offset);
}
var _63=[h.textAfter(_61.node).slice(_61.offset)];
for(pos=h.nodeAfter(_61.node);pos!=end.node;pos=h.nodeAfter(pos)){
_63.push(h.textAfter(pos));
}
_63.push(h.textAfter(end.node).slice(0,end.offset));
return cleanText(_63.join("\n"));
},replaceSelection:function(_64){
this.history.commit();
var _65=select.cursorPos(this.container,true),end=select.cursorPos(this.container,false);
if(!_65||!end){
return false;
}
end=this.replaceRange(_65,end,_64);
select.setCursorPos(this.container,_65,end);
return true;
},replaceRange:function(_67,to,_69){
var _6a=asEditorLines(_69);
_6a[0]=this.history.textAfter(_67.node).slice(0,_67.offset)+_6a[0];
var _6b=_6a[_6a.length-1];
_6a[_6a.length-1]=_6b+this.history.textAfter(to.node).slice(to.offset);
var end=this.history.nodeAfter(to.node);
this.history.push(_67.node,end,_6a);
return {node:this.history.nodeBefore(end),offset:_6b.length};
},getSearchCursor:function(_6d,_6e){
return new SearchCursor(this,_6d,_6e);
},reindent:function(){
if(this.container.firstChild){
this.indentRegion(null,this.container.lastChild);
}
},keyDown:function(_6f){
this.delayScanning();
if(_6f.keyCode==13){
if(_6f.ctrlKey){
this.reparseBuffer();
}else{
select.insertNewlineAtCursor(this.win);
this.indentAtCursor();
select.scrollToCursor(this.container);
}
_6f.stop();
}else{
if(_6f.keyCode==9){
this.handleTab();
_6f.stop();
}else{
if(_6f.ctrlKey){
if(_6f.keyCode==90||_6f.keyCode==8){
this.history.undo();
_6f.stop();
}else{
if(_6f.keyCode==89){
this.history.redo();
_6f.stop();
}else{
if(_6f.keyCode==83&&this.options.saveFunction){
this.options.saveFunction();
_6f.stop();
}
}
}
}
}
}
},keyPress:function(_70){
var _71=Editor.Parser.electricChars;
if(_70.code==13||_70.code==9){
_70.stop();
}else{
if(_71&&_71.indexOf(_70.character)!=-1){
this.parent.setTimeout(method(this,"indentAtCursor"),0);
}
}
},keyUp:function(_72){
if(!isSafeKey(_72.keyCode)){
this.markCursorDirty();
}
},indentLineAfter:function(_73){
var _74=_73?_73.nextSibling:this.container.firstChild;
if(_74&&!hasClass(_74,"whitespace")){
_74=null;
}
var _75=_74?_74.nextSibling:(_73?_73.nextSibling:this.container.firstChild);
var _76=(_73&&_75&&_75.currentText)?_75.currentText:"";
var _77=_73?_73.indentation(_76):0;
var _78=_77-(_74?_74.currentText.length:0);
if(_78<0){
if(_77==0){
removeElement(_74);
_74=null;
}else{
_74.currentText=safeWhiteSpace(_77);
_74.firstChild.nodeValue=_74.currentText;
}
}else{
if(_78>0){
if(_74){
_74.currentText=safeWhiteSpace(_77);
_74.firstChild.nodeValue=_74.currentText;
}else{
_74=this.doc.createElement("SPAN");
_74.className="part whitespace";
_74.appendChild(this.doc.createTextNode(safeWhiteSpace(_77)));
if(_73){
insertAfter(_74,_73);
}else{
insertAtStart(_74,this.containter);
}
}
}
}
return _74;
},highlightAtCursor:function(){
var pos=select.selectionTopNode(this.container,true);
var to=select.selectionTopNode(this.container,false);
if(pos===false||!to){
return;
}
if(to.nextSibling){
to=to.nextSibling;
}
var sel=select.markSelection(this.win);
var _7c=to.nodeType==3;
if(!_7c){
to.dirty=true;
}
while(to.parentNode==this.container&&(_7c||to.dirty)){
var _7d=this.highlight(pos,1,true);
if(_7d){
pos=_7d.node;
}
if(!_7d||_7d.left){
break;
}
}
select.selectMarked(sel);
},handleTab:function(){
var _7e=select.selectionTopNode(this.container,true),end=select.selectionTopNode(this.container,false);
if(_7e===false||end===false){
return;
}
if(_7e==end){
this.indentAtCursor();
}else{
this.indentRegion(_7e,end);
}
},indentAtCursor:function(){
if(!this.container.firstChild){
return;
}
this.highlightAtCursor();
var _80=select.selectionTopNode(this.container,false);
if(_80===false){
return;
}
var _81=startOfLine(_80);
var _82=this.indentLineAfter(_81);
if(_80==_81&&_82){
_80=_82;
}
if(_80==_82){
select.focusAfterNode(_80,this.container);
}
},indentRegion:function(_83,end){
var sel=select.markSelection(this.win);
if(!_83){
this.indentLineAfter(_83);
}else{
_83=startOfLine(_83.previousSibling);
}
end=startOfLine(end);
while(true){
var _86=this.highlight(_83,1);
var _87=_86?_86.node:null;
while(_83!=_87){
_83=_83?_83.nextSibling:this.container.firstChild;
}
if(_87){
this.indentLineAfter(_87);
}
if(_83==end){
break;
}
}
select.selectMarked(sel);
},markCursorDirty:function(){
var _88=select.selectionTopNode(this.container,false);
if(_88!==false&&this.container.firstChild){
this.scheduleHighlight();
this.addDirtyNode(_88||this.container.firstChild);
}
},reparseBuffer:function(){
forEach(this.container.childNodes,function(_89){
_89.dirty=true;
});
if(this.container.firstChild){
this.addDirtyNode(this.container.firstChild);
}
},addDirtyNode:function(_8a){
_8a=_8a||this.container.firstChild;
if(!_8a){
return;
}
for(var i=0;i<this.dirty.length;i++){
if(this.dirty[i]==_8a){
return;
}
}
if(_8a.nodeType!=3){
_8a.dirty=true;
}
this.dirty.push(_8a);
},scheduleHighlight:function(){
this.parent.clearTimeout(this.highlightTimeout);
this.highlightTimeout=this.parent.setTimeout(method(this,"highlightDirty"),this.options.passDelay);
},getDirtyNode:function(){
while(this.dirty.length>0){
var _8c=this.dirty.pop();
if((_8c.dirty||_8c.nodeType==3)&&_8c.parentNode){
return _8c;
}
}
return null;
},highlightDirty:function(all){
var _8e=all?Infinity:this.options.linesPerPass;
var sel=select.markSelection(this.win);
var _90;
while(_8e>0&&(_90=this.getDirtyNode())){
var _91=this.highlight(_90,_8e);
if(_91){
_8e=_91.left;
if(_91.node&&_91.dirty){
this.addDirtyNode(_91.node);
}
}
}
select.selectMarked(sel);
if(_90){
this.scheduleHighlight();
}
},documentScanner:function(_92){
var _93=this,pos=null;
return function(){
if(pos&&pos.parentNode!=_93.container){
pos=null;
}
var sel=select.markSelection(_93.win);
var _96=_93.highlight(pos,_92,true);
select.selectMarked(sel);
pos=_96?(_96.node&&_96.node.nextSibling):null;
_93.delayScanning();
};
},delayScanning:function(){
if(this.scanner){
this.parent.clearTimeout(this.documentScan);
this.documentScan=this.parent.setTimeout(this.scanner,this.options.continuousScanning);
}
},highlight:function(_97,_98,_99){
var _9a=this.container,_9b=this;
if(!_9a.firstChild){
return;
}
while(_97&&(!_97.parserFromHere||_97.dirty)){
_97=_97.previousSibling;
}
if(_97&&!_97.nextSibling){
return;
}
function correctPart(_9c,_9d){
return !_9d.reduced&&_9d.currentText==_9c.value&&hasClass(_9d,_9c.style);
};
function shortenPart(_9e,_9f){
_9e.currentText=_9e.currentText.substring(_9f);
_9e.reduced=true;
};
function tokenPart(_a0){
var _a1=_9b.doc.createElement("SPAN");
_a1.className="part "+_a0.style;
_a1.appendChild(_9b.doc.createTextNode(_a0.value));
_a1.currentText=_a0.value;
return _a1;
};
var _a2=traverseDOM(_97?_97.nextSibling:_9a.firstChild),_a3=multiStringStream(_a2),_a4=_97?_97.parserFromHere(_a3):Editor.Parser.make(_a3);
var _a5={current:null,get:function(){
if(!this.current){
this.current=_a2.nodes.shift();
}
return this.current;
},next:function(){
this.current=null;
},remove:function(){
_9a.removeChild(this.get());
this.current=null;
},getNonEmpty:function(){
var _a6=this.get();
while(_a6&&_a6.nodeName=="SPAN"&&_a6.currentText==""){
var old=_a6;
if((!_a6.previousSibling||_a6.previousSibling.nodeName=="BR")&&(!_a6.nextSibling||_a6.nextSibling.nodeName=="BR")){
this.next();
}else{
this.remove();
}
_a6=this.get();
select.replaceSelection(old.firstChild,_a6.firstChild||_a6,0,0);
}
return _a6;
}};
var _a8=false,_a9=false;
this.history.touch(_97);
forEach(_a4,function(_aa){
var _ab=_a5.getNonEmpty();
if(_aa.value=="\n"){
if(_ab.nodeName!="BR"){
throw "Parser out of sync. Expected BR.";
}
if(_ab.dirty||!_ab.indentation){
_a8=true;
}
_9b.history.touch(_ab);
_ab.parserFromHere=_a4.copy();
_ab.indentation=_aa.indentation;
_ab.dirty=false;
if((_98!==undefined&&--_98<=0)||(!_a8&&_a9&&!_99)){
throw StopIteration;
}
_a8=false;
_a9=false;
_a5.next();
}else{
if(_ab.nodeName!="SPAN"){
throw "Parser out of sync. Expected SPAN.";
}
if(_ab.dirty){
_a8=true;
}
_a9=true;
if(correctPart(_aa,_ab)){
_ab.dirty=false;
_a5.next();
}else{
_a8=true;
var _ac=tokenPart(_aa);
_9a.insertBefore(_ac,_ab);
var _ad=_aa.value.length;
var _ae=0;
while(_ad>0){
_ab=_a5.get();
var _af=_ab.currentText.length;
select.replaceSelection(_ab.firstChild,_ac.firstChild,_ad,_ae);
if(_af>_ad){
shortenPart(_ab,_ad);
_ad=0;
}else{
_ad-=_af;
_ae+=_af;
_a5.remove();
}
}
}
}
});
return {left:_98,node:_a5.get(),dirty:_a8};
}};
return Editor;
})();
addEventHandler(window,"load",function(){
var _b0=window.frameElement.CodeMirror;
_b0.editor=new Editor(_b0.options);
if(_b0.options.initCallback){
this.parent.setTimeout(function(){
_b0.options.initCallback(_b0);
},0);
}
});
var select={};
(function(){
var _b1=document.selection&&document.selection.createRangeCollection;
function topLevelNodeAt(_b2,top){
while(_b2&&_b2.parentNode!=top){
_b2=_b2.parentNode;
}
return _b2;
};
function topLevelNodeBefore(_b4,top){
while(!_b4.previousSibling&&_b4.parentNode!=top){
_b4=_b4.parentNode;
}
return topLevelNodeAt(_b4.previousSibling,top);
};
var _b6=false;
if(_b1){
select.markSelection=function(win){
var _b8=win.document.selection;
var _b9=_b8.createRange(),end=_b9.duplicate();
var _bb=_b9.getBookmark();
_b9.collapse(true);
end.collapse(false);
_b6=false;
var _bc=win.document.body;
return {start:{x:_b9.boundingLeft+_bc.scrollLeft-1,y:_b9.boundingTop+_bc.scrollTop},end:{x:end.boundingLeft+_bc.scrollLeft-1,y:end.boundingTop+_bc.scrollTop},window:win,bookmark:_bb};
};
select.selectMarked=function(sel){
if(!sel||!_b6){
return;
}
_b6=false;
var _be=sel.window.document.body.createTextRange(),_bf=_be.duplicate();
var _c0=false;
if(sel.start.y>=0&&sel.end.y<sel.window.document.body.clientHeight){
try{
_be.moveToPoint(sel.start.x,sel.start.y);
_bf.moveToPoint(sel.end.x,sel.end.y);
_be.setEndPoint("EndToStart",_bf);
_c0=true;
}
catch(e){
}
}
if(!_c0){
_be.moveToBookmark(sel.bookmark);
}
_be.select();
};
select.replaceSelection=function(){
_b6=true;
};
select.selectionTopNode=function(_c1,_c2){
var _c3=_c1.ownerDocument.selection;
if(!_c3){
return false;
}
var _c4=_c3.createRange();
_c4.collapse(_c2);
var _c5=_c4.parentElement();
if(_c5&&isAncestor(_c1,_c5)){
var _c6=_c4.duplicate();
_c6.moveToElementText(_c5);
if(_c4.compareEndPoints("StartToStart",_c6)==-1){
return topLevelNodeAt(_c5,_c1);
}
}
_c4.pasteHTML("<span id='xxx-temp-xxx'></span>");
var _c7=_c1.ownerDocument.getElementById("xxx-temp-xxx");
if(_c7){
var _c8=topLevelNodeBefore(_c7,_c1);
removeElement(_c7);
return _c8;
}
};
select.focusAfterNode=function(_c9,_ca){
var _cb=_ca.ownerDocument.body.createTextRange();
_cb.moveToElementText(_c9||_ca);
_cb.collapse(!_c9);
_cb.select();
};
function insertAtCursor(_cc,_cd){
var _ce=_cc.document.selection;
if(_ce){
var _cf=_ce.createRange();
_cf.pasteHTML(_cd);
_cf.collapse(false);
_cf.select();
}
};
select.insertNewlineAtCursor=function(_d0){
insertAtCursor(_d0,"<br/>");
};
select.cursorPos=function(_d1,_d2){
var _d3=_d1.ownerDocument.selection;
if(!_d3){
return null;
}
var _d4=select.selectionTopNode(_d1,_d2);
while(_d4&&_d4.nodeName!="BR"){
_d4=_d4.previousSibling;
}
var _d5=_d3.createRange(),_d6=_d5.duplicate();
_d5.collapse(_d2);
if(_d4){
_d6.moveToElementText(_d4);
_d6.collapse(false);
}else{
try{
_d6.moveToElementText(_d1);
}
catch(e){
return null;
}
_d6.collapse(true);
}
_d5.setEndPoint("StartToStart",_d6);
return {node:_d4,offset:_d5.text.length};
};
select.setCursorPos=function(_d7,_d8,to){
function rangeAt(pos){
var _db=_d7.ownerDocument.body.createTextRange();
if(!pos.node){
_db.moveToElementText(_d7);
_db.collapse(true);
}else{
_db.moveToElementText(pos.node);
_db.collapse(false);
}
_db.move("character",pos.offset);
return _db;
};
var _dc=rangeAt(_d8);
if(to&&to!=_d8){
_dc.setEndPoint("EndToEnd",rangeAt(to));
}
_dc.select();
};
select.scrollToCursor=function(_dd){
var _de=_dd.ownerDocument.selection;
if(!_de){
return null;
}
_de.createRange().scrollIntoView();
};
}else{
var _df=!window.scrollX&&!window.scrollY;
select.markSelection=function(win){
_b6=false;
var _e1=win.getSelection();
if(!_e1||_e1.rangeCount==0){
return null;
}
var _e2=_e1.getRangeAt(0);
var _e3={start:{node:_e2.startContainer,offset:_e2.startOffset},end:{node:_e2.endContainer,offset:_e2.endOffset},window:win,scrollX:_df&&win.document.body.scrollLeft,scrollY:_df&&win.document.body.scrollTop};
function normalize(_e4){
while(_e4.node.nodeType!=3&&_e4.node.nodeName!="BR"){
var _e5=_e4.node.childNodes[_e4.offset]||_e4.node.nextSibling;
_e4.offset=0;
while(!_e5&&_e4.node.parentNode){
_e4.node=_e4.node.parentNode;
_e5=_e4.node.nextSibling;
}
_e4.node=_e5;
if(!_e5){
break;
}
}
};
normalize(_e3.start);
normalize(_e3.end);
if(_e3.start.node){
_e3.start.node.selectStart=_e3.start;
}
if(_e3.end.node){
_e3.end.node.selectEnd=_e3.end;
}
return _e3;
};
select.selectMarked=function(sel){
if(!sel||!_b6){
return;
}
var win=sel.window;
var _e8=win.document.createRange();
function setPoint(_e9,_ea){
if(_e9.node){
delete _e9.node["select"+_ea];
if(_e9.offset==0){
_e8["set"+_ea+"Before"](_e9.node);
}else{
_e8["set"+_ea](_e9.node,_e9.offset);
}
}else{
_e8.setStartAfter(win.document.body.lastChild||win.document.body);
}
};
if(_df){
sel.window.document.body.scrollLeft=sel.scrollX;
sel.window.document.body.scrollTop=sel.scrollY;
}
setPoint(sel.end,"End");
setPoint(sel.start,"Start");
selectRange(_e8,win);
};
select.replaceSelection=function(_eb,_ec,_ed,_ee){
_b6=true;
function replace(_ef){
var _f0=_eb["select"+_ef];
if(_f0){
if(_f0.offset>_ed){
_f0.offset-=_ed;
}else{
_ec["select"+_ef]=_f0;
delete _eb["select"+_ef];
_f0.node=_ec;
_f0.offset+=(_ee||0);
}
}
};
replace("Start");
replace("End");
};
function selectRange(_f1,_f2){
var _f3=_f2.getSelection();
_f3.removeAllRanges();
_f3.addRange(_f1);
};
function selectionRange(_f4){
var _f5=_f4.getSelection();
if(!_f5||_f5.rangeCount==0){
return false;
}else{
return _f5.getRangeAt(0);
}
};
select.selectionTopNode=function(_f6,_f7){
var _f8=selectionRange(_f6.ownerDocument.defaultView);
if(!_f8){
return false;
}
var _f9=_f7?_f8.startContainer:_f8.endContainer;
var _fa=_f7?_f8.startOffset:_f8.endOffset;
if(_f9.nodeType==3){
if(_fa>0){
return topLevelNodeAt(_f9,_f6);
}else{
return topLevelNodeBefore(_f9,_f6);
}
}else{
if(_f9.nodeName=="HTML"){
return (_fa==1?null:_f6.lastChild);
}else{
if(_f9==_f6){
return (_fa==0)?null:_f9.childNodes[_fa-1];
}else{
if(_fa==_f9.childNodes.length){
return topLevelNodeAt(_f9,_f6);
}else{
if(_fa==0){
return topLevelNodeBefore(_f9,_f6);
}else{
return topLevelNodeAt(_f9.childNodes[_fa-1],_f6);
}
}
}
}
}
};
select.focusAfterNode=function(_fb,_fc){
var win=_fc.ownerDocument.defaultView,_fe=win.document.createRange();
_fe.setStartBefore(_fc.firstChild||_fc);
if(_fb&&!_fb.firstChild){
_fe.setEndAfter(_fb);
}else{
if(_fb){
_fe.setEnd(_fb,_fb.childNodes.length);
}else{
_fe.setEndBefore(_fc.firstChild||_fc);
}
}
_fe.collapse(false);
selectRange(_fe,win);
};
insertNodeAtCursor=function(_ff,node){
var _101=selectionRange(_ff);
if(!_101){
return;
}
if(_ff.opera&&_101.startContainer.nodeType==3&&_101.startOffset!=0){
var _102=_101.startContainer,text=_102.nodeValue;
_102.parentNode.insertBefore(_ff.document.createTextNode(text.substr(0,_101.startOffset)),_102);
_102.nodeValue=text.substr(_101.startOffset);
_102.parentNode.insertBefore(node,_102);
}else{
_101.insertNode(node);
}
_101.setEndAfter(node);
_101.collapse(false);
selectRange(_101,_ff);
return node;
};
var _104=/Gecko/.test(navigator.userAgent);
select.insertNewlineAtCursor=function(_105){
insertNodeAtCursor(_105,_105.document.createElement("BR"));
if(_104){
insertNodeAtCursor(_105,_105.document.createTextNode(""));
}
};
select.cursorPos=function(_106,_107){
var _108=selectionRange(window);
if(!_108){
return;
}
var _109=select.selectionTopNode(_106,_107);
while(_109&&_109.nodeName!="BR"){
_109=_109.previousSibling;
}
_108=_108.cloneRange();
_108.collapse(_107);
if(_109){
_108.setStartAfter(_109);
}else{
_108.setStartBefore(_106);
}
return {node:_109,offset:_108.toString().length};
};
select.setCursorPos=function(_10a,from,to){
var win=_10a.ownerDocument.defaultView,_10e=win.document.createRange();
function setPoint(node,_110,side){
if(!node){
node=_10a.firstChild;
}else{
node=node.nextSibling;
}
if(!node){
return;
}
if(_110==0){
_10e["set"+side+"Before"](node);
return true;
}
var _112=[];
function decompose(node){
if(node.nodeType==3){
_112.push(node);
}else{
forEach(node.childNodes,decompose);
}
};
while(true){
while(node&&!_112.length){
decompose(node);
node=node.nextSibling;
}
var cur=_112.shift();
if(!cur){
return false;
}
var _115=cur.nodeValue.length;
if(_115>=_110){
_10e["set"+side](cur,_110);
return true;
}
_110-=_115;
}
};
to=to||from;
if(setPoint(to.node,to.offset,"End")&&setPoint(from.node,from.offset,"Start")){
selectRange(_10e,win);
}
};
select.scrollToCursor=function(_116){
var body=_116.ownerDocument.body,win=_116.ownerDocument.defaultView;
var _119=select.selectionTopNode(_116,true)||_116.firstChild;
while(_119&&!_119.offsetTop){
_119=_119.previousSibling;
}
var y=0,pos=_119;
while(pos&&pos.offsetParent){
y+=pos.offsetTop;
pos=pos.offsetParent;
}
var _11c=y-body.scrollTop;
if(_11c<0||_11c>win.innerHeight-10){
win.scrollTo(0,y);
}
};
}
}());
(function(){
var base={more:function(){
return this.peek()!==null;
},applies:function(test){
var next=this.peek();
return (next!==null&&test(next));
},nextWhile:function(test){
while(this.applies(test)){
this.next();
}
},equals:function(ch){
return ch===this.peek();
},endOfLine:function(){
var next=this.peek();
return next==null||next=="\n";
}};
window.singleStringStream=function(_123){
var pos=0,_125=0;
return update({peek:function(){
if(pos<_123.length){
return _123.charAt(pos);
}else{
return null;
}
},next:function(){
if(pos>=_123.length){
if(pos<_125){
throw "End of stringstream reached without emptying buffer.";
}else{
throw StopIteration;
}
}
return _123.charAt(pos++);
},get:function(){
var _126=_123.slice(_125,pos);
_125=pos;
return _126;
}},base);
};
window.multiStringStream=function(_127){
_127=iter(_127);
var _128="",pos=0;
var _12a=null,_12b="";
return update({peek:function(){
if(!_12a){
try{
_12a=this.step();
}
catch(e){
if(e!=StopIteration){
throw e;
}else{
_12a=null;
}
}
}
return _12a;
},step:function(){
if(_12a){
var temp=_12a;
_12a=null;
return temp;
}
while(pos==_128.length){
_12b+=_128;
_128="";
pos=0;
_128=_127.next();
}
return _128.charAt(pos++);
},next:function(){
try{
return this.step();
}
catch(e){
if(e==StopIteration&&_12b.length>0){
throw "End of stringstream reached without emptying buffer ('"+_12b+"').";
}else{
throw e;
}
}
},get:function(){
var temp=_12b;
var _12e=_12a?pos-1:pos;
_12b="";
if(_12e>0){
temp+=_128.slice(0,_12e);
_128=_128.slice(_12e);
pos=_12a?1:0;
}
return temp;
}},base);
};
})();
function History(_12f,_130,_131,_132){
this.container=_12f;
this.maxDepth=_130;
this.commitDelay=_131;
this.editor=_132;
this.parent=_132.parent;
var _133={text:"",from:null,to:null};
this.first=_133;
this.last=_133;
this.firstTouched=false;
this.history=[];
this.redoHistory=[];
this.touched=[];
};
History.prototype={touch:function(node){
this.setTouched(node);
this.parent.clearTimeout(this.commitTimeout);
this.commitTimeout=this.parent.setTimeout(method(this,"commit"),this.commitDelay);
},undo:function(){
this.commit();
if(this.history.length){
this.redoHistory.push(this.updateTo(this.history.pop(),"applyChain"));
}
},redo:function(){
this.commit();
if(this.redoHistory.length){
this.addUndoLevel(this.updateTo(this.redoHistory.pop(),"applyChain"));
}
},push:function(from,to,_137){
var _138=[];
for(var i=0;i<_137.length;i++){
var end=(i==_137.length-1)?to:this.container.ownerDocument.createElement("BR");
_138.push({from:from,to:end,text:_137[i]});
from=end;
}
this.pushChains([_138]);
},pushChains:function(_13b){
this.commit();
this.addUndoLevel(this.updateTo(_13b,"applyChain"));
this.redoHistory=[];
},reset:function(){
this.commit();
this.history=[];
this.redoHistory=[];
},textAfter:function(br){
return this.after(br).text;
},nodeAfter:function(br){
return this.after(br).to;
},nodeBefore:function(br){
return this.before(br).from;
},commit:function(){
this.parent.clearTimeout(this.commitTimeout);
this.editor.highlightDirty(true);
var _13f=this.touchedChains(),self=this;
if(_13f.length){
this.addUndoLevel(this.updateTo(_13f,"linkChain"));
this.redoHistory=[];
}
},updateTo:function(_141,_142){
var _143=[],_144=[];
for(var i=0;i<_141.length;i++){
_143.push(this.shadowChain(_141[i]));
_144.push(this[_142](_141[i]));
}
if(_142=="applyChain"){
this.notifyDirty(_144);
}
return _143;
},notifyDirty:function(_146){
forEach(_146,method(this.editor,"addDirtyNode"));
this.editor.scheduleHighlight();
},linkChain:function(_147){
for(var i=0;i<_147.length;i++){
var line=_147[i];
if(line.from){
line.from.historyAfter=line;
}else{
this.first=line;
}
if(line.to){
line.to.historyBefore=line;
}else{
this.last=line;
}
}
},after:function(node){
return node?node.historyAfter:this.first;
},before:function(node){
return node?node.historyBefore:this.last;
},setTouched:function(node){
if(node){
if(!node.historyTouched){
this.touched.push(node);
node.historyTouched=true;
}
}else{
this.firstTouched=true;
}
},addUndoLevel:function(_14d){
this.history.push(_14d);
if(this.history.length>this.maxDepth){
this.history.shift();
}
},touchedChains:function(){
var self=this;
function compareText(a,b){
return a.replace(/\u00a0/g," ")==b.replace(/\u00a0/g," ");
};
var _151=null;
function temp(node){
return node?node.historyTemp:_151;
};
function setTemp(node,line){
if(node){
node.historyTemp=line;
}else{
_151=line;
}
};
var _155=[];
if(self.firstTouched){
self.touched.push(null);
}
forEach(self.touched,function(node){
if(node){
node.historyTouched=false;
if(node.parentNode!=self.container){
return;
}
}else{
self.firstTouched=false;
}
var text=[];
for(var cur=node?node.nextSibling:self.container.firstChild;cur&&cur.nodeName!="BR";cur=cur.nextSibling){
if(cur.currentText){
text.push(cur.currentText);
}
}
var line={from:node,to:cur,text:text.join("")};
var _15a=self.after(node);
if(!_15a||!compareText(_15a.text,line.text)||_15a.to!=line.to){
_155.push(line);
setTemp(node,line);
}
});
function nextBR(node,dir){
var link=dir+"Sibling",_15e=node[link];
while(_15e&&_15e.nodeName!="BR"){
_15e=_15e[link];
}
return _15e;
};
var _15f=[];
self.touched=[];
forEach(_155,function(line){
if(!temp(line.from)){
return;
}
var _161=[],_162=line.from;
while(true){
var _163=temp(_162);
if(!_163){
break;
}
_161.unshift(_163);
setTemp(_162,null);
if(!_162){
break;
}
_162=nextBR(_162,"previous");
}
_162=line.to;
while(true){
var _163=temp(_162);
if(!_163||!_162){
break;
}
_161.push(_163);
setTemp(_162,null);
_162=nextBR(_162,"next");
}
if(self.after(_161[0].from)&&self.before(_161[_161.length-1].to)){
_15f.push(_161);
}else{
forEach(_161,function(line){
self.setTouched(line.from);
});
}
});
return _15f;
},recordChange:function(_165,_166){
if(this.onChange){
this.onChange(_165,_166);
}
},shadowChain:function(_167){
var _168=[],next=this.after(_167[0].from),end=_167[_167.length-1].to;
while(true){
_168.push(next);
var _16b=next.to;
if(!_16b||_16b==end){
break;
}else{
next=_16b.historyAfter;
}
}
return _168;
},applyChain:function(_16c){
var _16d=select.cursorPos(this.container,false),self=this;
function removeRange(from,to){
var pos=from?from.nextSibling:self.container.firstChild;
while(pos!=to){
var temp=pos.nextSibling;
removeElement(pos);
pos=temp;
}
};
var _173=_16c[0].from,end=_16c[_16c.length-1].to;
removeRange(_173,end);
var _175=end?function(node){
self.container.insertBefore(node,end);
}:function(node){
self.container.appendChild(node);
};
for(var i=0;i<_16c.length;i++){
var line=_16c[i];
if(i>0){
_175(line.from);
}
var _17a=this.container.ownerDocument.createTextNode(line.text);
_175(_17a);
if(_16d&&_16d.node==line.from){
var _17b=0;
var prev=this.after(line.from);
if(prev&&i==_16c.length-1){
for(var _17d=0;_17d<_16d.offset&&line.text.charAt(_17d)==prev.text.charAt(_17d);_17d++){
}
if(_16d.offset>_17d){
_17b=line.text.length-prev.text.length;
}
}
select.setCursorPos(this.container,{node:line.from,offset:Math.max(0,_16d.offset+_17b)});
}else{
if(_16d&&(i==_16c.length-1)&&_16d.node&&_16d.node.parentNode!=this.container){
select.setCursorPos(this.container,{node:line.from,offset:line.text.length});
}
}
}
this.linkChain(_16c);
return _173;
}};
function method(obj,name){
return function(){
obj[name].apply(obj,arguments);
};
};
function update(obj,from){
for(var name in from){
obj[name]=from[name];
}
return obj;
};
var StopIteration={toString:function(){
return "StopIteration";
}};
function iter(seq){
var i=0;
if(seq.next){
return seq;
}else{
return {next:function(){
if(i>=seq.length){
throw StopIteration;
}else{
return seq[++i];
}
}};
}
};
function forEach(iter,f){
if(iter.next){
try{
while(true){
f(iter.next());
}
}
catch(e){
if(e!=StopIteration){
throw e;
}
}
}else{
for(var i=0;i<iter.length;i++){
f(iter[i]);
}
}
};
function map(iter,f){
var _18a=[];
forEach(iter,function(val){
_18a.push(f(val));
});
return _18a;
};
function matcher(_18c){
return function(_18d){
return _18c.test(_18d);
};
};
function hasClass(_18e,_18f){
var _190=_18e.className;
return _190&&new RegExp("(^| )"+_18f+"($| )").test(_190);
};
function insertAfter(_191,_192){
var _193=_192.parentNode;
var next=_192.nextSibling;
if(next){
_193.insertBefore(_191,next);
}else{
_193.appendChild(_191);
}
return _191;
};
function insertAtStart(node,_196){
if(_196.firstChild){
_196.insertBefore(node,_196.firstChild);
}else{
_196.appendChild(node);
}
return node;
};
function removeElement(node){
if(node.parentNode){
node.parentNode.removeChild(node);
}
};
function clearElement(node){
while(node.firstChild){
node.removeChild(node.firstChild);
}
};
function isAncestor(node,_19a){
while(_19a=_19a.parentNode){
if(node==_19a){
return true;
}
}
return false;
};
var nbsp=" ";
function normalizeEvent(_19b){
if(!_19b.stopPropagation){
_19b.stopPropagation=function(){
this.cancelBubble=true;
};
_19b.preventDefault=function(){
this.returnValue=false;
};
}
if(!_19b.stop){
_19b.stop=function(){
this.stopPropagation();
this.preventDefault();
};
}
if(_19b.type=="keypress"){
if(_19b.charCode===0||_19b.charCode==undefined){
_19b.code=_19b.keyCode;
}else{
_19b.code=_19b.charCode;
}
_19b.character=String.fromCharCode(_19b.code);
}
return _19b;
};
function addEventHandler(node,type,_19e){
function wrapHandler(_19f){
_19e(normalizeEvent(_19f||window.event));
};
if(typeof node.addEventListener=="function"){
node.addEventListener(type,wrapHandler,false);
return function(){
node.removeEventListener(type,wrapHandler,false);
};
}else{
node.attachEvent("on"+type,wrapHandler);
return function(){
node.detachEvent("on"+type,wrapHandler);
};
}
};
function removeEventHandler(_1a0){
_1a0();
};


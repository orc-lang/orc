var Editor=(function(){var H={P:true,DIV:true,LI:true};function C(O){var N=[],M=true;for(;O>0;O--){N.push((M||O==1)?nbsp:" ");M=!M}return N.join("")}function D(M){if(M==" "){return"\u00a0"}else{return M.replace(/[\t \u00a0]{2,}/g,function(N){return C(N.length)})}}function B(M){return D(M.replace(/\u00a0/g," ")).replace(/\r\n?/g,"\n").split("\n")}function E(N){var P=N.ownerDocument;var M=[];var Q=false;function O(R){if(R.nodeType==3){var S=R.nodeValue=D(R.nodeValue.replace(/[\n\r]/g,""));if(S.length){Q=false}M.push(R)}else{if(R.nodeName=="BR"&&R.childNodes.length==0){Q=false;M.push(R)}else{forEach(R.childNodes,O);if(!Q&&H.hasOwnProperty(R.nodeName)){Q=true;M.push(P.createElement("BR"))}}}}O(N);return M}function F(M){function N(Z,a){P=a;return Z}function U(a,Z,b){return function(){return a(Z,b)}}function V(){P=V;throw StopIteration}var P=U(W,M,V);var O=M.ownerDocument;var Q=[];function T(b){var a=b.parentNode;var Z=b.nextSibling;if(Z){return function(c){a.insertBefore(c,Z)}}else{return function(c){a.appendChild(c)}}}var X=null;function R(Z){var b="\n";if(Z.nodeType==3){b=Z.nodeValue;var a=O.createElement("SPAN");a.className="part";a.appendChild(Z);Z=a;Z.currentText=b}Z.dirty=true;Q.push(Z);X(Z);return b}function S(a,b){var Z=[];forEach(E(a),function(c){Z.push(R(c))});return N(Z.join(""),b)}function Y(Z){if(Z.nodeName=="SPAN"&&Z.childNodes.length==1&&Z.firstChild.nodeType==3){Z.currentText=Z.firstChild.nodeValue;return true}return false}function W(Z,a){if(Z.nextSibling){a=U(W,Z.nextSibling,a)}if(Y(Z)){Q.push(Z);return N(Z.currentText,a)}else{if(Z.nodeName=="BR"){Q.push(Z);return N("\n",a)}else{X=T(Z);removeElement(Z);return S(Z,a)}}}return{next:function(){return P()},nodes:Q}}function J(M){if(M.nodeName=="BR"){return 1}else{return M.currentText.length}}function L(M){while(M&&M.nodeName!="BR"){M=M.previousSibling}return M}function I(M){return M.replace(/\u00a0/g," ")}function K(P,O,N){this.editor=P;this.history=P.history;this.history.commit();this.atOccurrence=false;this.fallbackSize=15;var R;if(N&&(R=select.cursorPos(this.editor.container))){this.line=R.node;this.offset=R.offset}else{this.line=null;this.offset=0}this.valid=!!O;var Q=O.split("\n"),M=this;this.matches=(Q.length==1)?function(){var S=I(M.history.textAfter(M.line).slice(M.offset)).indexOf(O);if(S>-1){return{from:{node:M.line,offset:M.offset+S},to:{node:M.line,offset:M.offset+S+O.length}}}}:function(){var V=I(M.history.textAfter(M.line).slice(M.offset));var U=V.lastIndexOf(Q[0]);if(U==-1||U!=V.length-Q[0].length){return false}var S=M.offset+U;var T=M.history.nodeAfter(M.line);for(var W=1;W<Q.length-1;W++){if(I(M.history.textAfter(T))!=Q[W]){return false}T=M.history.nodeAfter(T)}if(I(M.history.textAfter(T)).indexOf(Q[Q.length-1])!=0){return false}return{from:{node:M.line,offset:S},to:{node:T,offset:Q[Q.length-1].length}}}}K.prototype={findNext:function(){if(!this.valid){return false}this.atOccurrence=false;var M=this;if(this.line&&!this.line.parentNode){this.line=null;this.offset=0}function O(P){if(M.history.textAfter(P.node).length<P.offset){M.line=P.node;M.offset=P.offset+1}else{M.line=M.history.nodeAfter(P.node);M.offset=0}}while(true){var N=this.matches();if(N){this.atOccurrence=N;O(N.from);return true}this.line=this.history.nodeAfter(this.line);this.offset=0;if(!this.line){this.valid=false;return false}}},select:function(){if(this.atOccurrence){select.setCursorPos(this.editor.container,this.atOccurrence.from,this.atOccurrence.to);select.scrollToCursor(this.editor.container)}},replace:function(M){if(this.atOccurrence){this.editor.replaceRange(this.atOccurrence.from,this.atOccurrence.to,M);this.line=this.atOccurrence.from.node;this.offset=this.atOccurrence.from.offset;this.atOccurrence=false}}};function G(N){this.options=N;this.parent=parent;this.doc=document;this.container=this.doc.body;this.win=window;this.history=new History(this.container,this.options.undoDepth,this.options.undoDelay,this);if(!G.Parser){throw"No parser loaded."}if(N.parserConfig&&G.Parser.configure){G.Parser.configure(N.parserConfig)}if(!N.textWrapping){this.container.style.whiteSpace="pre"}select.setCursorPos(this.container,{node:null,offset:0});this.dirty=[];if(N.content){this.importCode(N.content)}else{this.container.appendChild(this.doc.createElement("SPAN"))}if(!N.readOnly){if(N.continuousScanning!==false){this.scanner=this.documentScanner(N.linesPerPass);this.delayScanning()}function O(){if(document.body.contentEditable!=undefined&&/MSIE/.test(navigator.userAgent)){document.body.contentEditable="true"}else{document.designMode="on"}}try{O()}catch(P){var M=addEventHandler(document,"focus",function(){removeEventHandler(M);O()})}addEventHandler(document,"keydown",method(this,"keyDown"));addEventHandler(document,"keypress",method(this,"keyPress"));addEventHandler(document,"keyup",method(this,"keyUp"));addEventHandler(document.body,"paste",method(this,"markCursorDirty"));addEventHandler(document.body,"cut",method(this,"markCursorDirty"))}}function A(M){return(M>=16&&M<=18)||(M>=33&&M<=40)}G.prototype={importCode:function(M){this.history.push(null,null,B(M));this.history.reset()},getCode:function(){if(!this.container.firstChild){return""}var M=[];forEach(F(this.container.firstChild),method(M,"push"));return I(M.join(""))},jumpToLine:function(M){if(M<=1||!this.container.firstChild){select.focusAfterNode(null,this.container)}else{var N=this.container.firstChild;while(true){if(N.nodeName=="BR"){M--}if(M<=1||!N.nextSibling){break}N=N.nextSibling}select.focusAfterNode(N,this.container)}select.scrollToCursor(this.container)},currentLine:function(){var N=select.cursorPos(this.container,true),M=1;if(!N){return 1}for(cursor=N.node;cursor;cursor=cursor.previousSibling){if(cursor.nodeName=="BR"){M++}}return M},selectedText:function(){var N=this.history;N.commit();var P=select.cursorPos(this.container,true),M=select.cursorPos(this.container,false);if(!P||!M){return""}if(P.node==M.node){return N.textAfter(P.node).slice(P.offset,M.offset)}var O=[N.textAfter(P.node).slice(P.offset)];for(pos=N.nodeAfter(P.node);pos!=M.node;pos=N.nodeAfter(pos)){O.push(N.textAfter(pos))}O.push(N.textAfter(M.node).slice(0,M.offset));return I(O.join("\n"))},replaceSelection:function(N){this.history.commit();var O=select.cursorPos(this.container,true),M=select.cursorPos(this.container,false);if(!O||!M){return false}M=this.replaceRange(O,M,N);select.setCursorPos(this.container,O,M);return true},replaceRange:function(R,Q,P){var O=B(P);O[0]=this.history.textAfter(R.node).slice(0,R.offset)+O[0];var N=O[O.length-1];O[O.length-1]=N+this.history.textAfter(Q.node).slice(Q.offset);var M=this.history.nodeAfter(Q.node);this.history.push(R.node,M,O);return{node:this.history.nodeBefore(M),offset:N.length}},getSearchCursor:function(N,M){return new K(this,N,M)},reindent:function(){if(this.container.firstChild){this.indentRegion(null,this.container.lastChild)}},keyDown:function(M){this.delayScanning();if(M.keyCode==13){if(M.ctrlKey){this.reparseBuffer()}else{select.insertNewlineAtCursor(this.win);this.indentAtCursor();select.scrollToCursor(this.container)}M.stop()}else{if(M.keyCode==9){this.handleTab();M.stop()}else{if(M.ctrlKey){if(M.keyCode==90||M.keyCode==8){this.history.undo();M.stop()}else{if(M.keyCode==89){this.history.redo();M.stop()}else{if(M.keyCode==83&&this.options.saveFunction){this.options.saveFunction();M.stop()}}}}}}},keyPress:function(N){var M=G.Parser.electricChars;if(N.code==13||N.code==9){N.stop()}else{if(M&&M.indexOf(N.character)!=-1){this.parent.setTimeout(method(this,"indentAtCursor"),0)}}},keyUp:function(M){if(!A(M.keyCode)){this.markCursorDirty()}},indentLineAfter:function(R){var Q=R?R.nextSibling:this.container.firstChild;if(Q&&!hasClass(Q,"whitespace")){Q=null}var N=Q?Q.nextSibling:(R?R.nextSibling:this.container.firstChild);var O=(R&&N&&N.currentText)?N.currentText:"";var M=R?R.indentation(O):0;var P=M-(Q?Q.currentText.length:0);if(P<0){if(M==0){removeElement(Q);Q=null}else{Q.currentText=C(M);Q.firstChild.nodeValue=Q.currentText}}else{if(P>0){if(Q){Q.currentText=C(M);Q.firstChild.nodeValue=Q.currentText}else{Q=this.doc.createElement("SPAN");Q.className="part whitespace";Q.appendChild(this.doc.createTextNode(C(M)));if(R){insertAfter(Q,R)}else{insertAtStart(Q,this.containter)}}}}return Q},highlightAtCursor:function(){var Q=select.selectionTopNode(this.container,true);var P=select.selectionTopNode(this.container,false);if(Q===false||!P){return }if(P.nextSibling){P=P.nextSibling}var O=select.markSelection(this.win);var N=P.nodeType==3;if(!N){P.dirty=true}while(P.parentNode==this.container&&(N||P.dirty)){var M=this.highlight(Q,1,true);if(M){Q=M.node}if(!M||M.left){break}}select.selectMarked(O)},handleTab:function(){var N=select.selectionTopNode(this.container,true),M=select.selectionTopNode(this.container,false);if(N===false||M===false){return }if(N==M){this.indentAtCursor()}else{this.indentRegion(N,M)}},indentAtCursor:function(){if(!this.container.firstChild){return }this.highlightAtCursor();var O=select.selectionTopNode(this.container,false);if(O===false){return }var M=L(O);var N=this.indentLineAfter(M);if(O==M&&N){O=N}if(O==N){select.focusAfterNode(O,this.container)}},indentRegion:function(Q,N){var P=select.markSelection(this.win);if(!Q){this.indentLineAfter(Q)}else{Q=L(Q.previousSibling)}N=L(N);while(true){var M=this.highlight(Q,1);var O=M?M.node:null;while(Q!=O){Q=Q?Q.nextSibling:this.container.firstChild}if(O){this.indentLineAfter(O)}if(Q==N){break}}select.selectMarked(P)},markCursorDirty:function(){var M=select.selectionTopNode(this.container,false);if(M!==false&&this.container.firstChild){this.scheduleHighlight();this.addDirtyNode(M||this.container.firstChild)}},reparseBuffer:function(){forEach(this.container.childNodes,function(M){M.dirty=true});if(this.container.firstChild){this.addDirtyNode(this.container.firstChild)}},addDirtyNode:function(N){N=N||this.container.firstChild;if(!N){return }for(var M=0;M<this.dirty.length;M++){if(this.dirty[M]==N){return }}if(N.nodeType!=3){N.dirty=true}this.dirty.push(N)},scheduleHighlight:function(){this.parent.clearTimeout(this.highlightTimeout);this.highlightTimeout=this.parent.setTimeout(method(this,"highlightDirty"),this.options.passDelay)},getDirtyNode:function(){while(this.dirty.length>0){var M=this.dirty.pop();if((M.dirty||M.nodeType==3)&&M.parentNode){return M}}return null},highlightDirty:function(O){var N=O?Infinity:this.options.linesPerPass;var P=select.markSelection(this.win);var Q;while(N>0&&(Q=this.getDirtyNode())){var M=this.highlight(Q,N);if(M){N=M.left;if(M.node&&M.dirty){this.addDirtyNode(M.node)}}}select.selectMarked(P);if(Q){this.scheduleHighlight()}},documentScanner:function(M){var N=this,O=null;return function(){if(O&&O.parentNode!=N.container){O=null}var Q=select.markSelection(N.win);var P=N.highlight(O,M,true);select.selectMarked(Q);O=P?(P.node&&P.node.nextSibling):null;N.delayScanning()}},delayScanning:function(){if(this.scanner){this.parent.clearTimeout(this.documentScan);this.documentScan=this.parent.setTimeout(this.scanner,this.options.continuousScanning)}},highlight:function(V,Z,Q){var N=this.container,X=this;if(!N.firstChild){return }while(V&&(!V.parserFromHere||V.dirty)){V=V.previousSibling}if(V&&!V.nextSibling){return }function M(b,a){return !a.reduced&&a.currentText==b.value&&hasClass(a,b.style)}function Y(a,b){a.currentText=a.currentText.substring(b);a.reduced=true}function O(b){var a=X.doc.createElement("SPAN");a.className="part "+b.style;a.appendChild(X.doc.createTextNode(b.value));a.currentText=b.value;return a}var S=F(V?V.nextSibling:N.firstChild),W=multiStringStream(S),T=V?V.parserFromHere(W):G.Parser.make(W);var P={current:null,get:function(){if(!this.current){this.current=S.nodes.shift()}return this.current},next:function(){this.current=null},remove:function(){N.removeChild(this.get());this.current=null},getNonEmpty:function(){var b=this.get();while(b&&b.nodeName=="SPAN"&&b.currentText==""){var a=b;if((!b.previousSibling||b.previousSibling.nodeName=="BR")&&(!b.nextSibling||b.nextSibling.nodeName=="BR")){this.next()}else{this.remove()}b=this.get();select.replaceSelection(a.firstChild,b.firstChild||b,0,0)}return b}};var R=false,U=false;this.history.touch(V);forEach(T,function(c){var b=P.getNonEmpty();if(c.value=="\n"){if(b.nodeName!="BR"){throw"Parser out of sync. Expected BR."}if(b.dirty||!b.indentation){R=true}X.history.touch(b);b.parserFromHere=T.copy();b.indentation=c.indentation;b.dirty=false;if((Z!==undefined&&--Z<=0)||(!R&&U&&!Q)){throw StopIteration}R=false;U=false;P.next()}else{if(b.nodeName!="SPAN"){throw"Parser out of sync. Expected SPAN."}if(b.dirty){R=true}U=true;if(M(c,b)){b.dirty=false;P.next()}else{R=true;var a=O(c);N.insertBefore(a,b);var f=c.value.length;var e=0;while(f>0){b=P.get();var d=b.currentText.length;select.replaceSelection(b.firstChild,a.firstChild,f,e);if(d>f){Y(b,f);f=0}else{f-=d;e+=d;P.remove()}}}}});return{left:Z,node:P.get(),dirty:R}}};return G})();addEventHandler(window,"load",function(){var A=window.frameElement.CodeMirror;A.editor=new Editor(A.options);if(A.options.initCallback){this.parent.setTimeout(function(){A.options.initCallback(A)},0)}});var select={};(function(){var D=document.selection&&document.selection.createRangeCollection;function E(J,K){while(J&&J.parentNode!=K){J=J.parentNode}return J}function I(J,K){while(!J.previousSibling&&J.parentNode!=K){J=J.parentNode}return E(J.previousSibling,K)}var H=false;if(D){select.markSelection=function(N){var M=N.document.selection;var O=M.createRange(),K=O.duplicate();var L=O.getBookmark();O.collapse(true);K.collapse(false);H=false;var J=N.document.body;return{start:{x:O.boundingLeft+J.scrollLeft-1,y:O.boundingTop+J.scrollTop},end:{x:K.boundingLeft+J.scrollLeft-1,y:K.boundingTop+J.scrollTop},window:N,bookmark:L}};select.selectMarked=function(M){if(!M||!H){return }H=false;var L=M.window.document.body.createTextRange(),J=L.duplicate();var K=false;if(M.start.y>=0&&M.end.y<M.window.document.body.clientHeight){try{L.moveToPoint(M.start.x,M.start.y);J.moveToPoint(M.end.x,M.end.y);L.setEndPoint("EndToStart",J);K=true}catch(N){}}if(!K){L.moveToBookmark(M.bookmark)}L.select()};select.replaceSelection=function(){H=true};select.selectionTopNode=function(L,Q){var O=L.ownerDocument.selection;if(!O){return false}var N=O.createRange();N.collapse(Q);var P=N.parentElement();if(P&&isAncestor(L,P)){var K=N.duplicate();K.moveToElementText(P);if(N.compareEndPoints("StartToStart",K)==-1){return E(P,L)}}N.pasteHTML("<span id='xxx-temp-xxx'></span>");var M=L.ownerDocument.getElementById("xxx-temp-xxx");if(M){var J=I(M,L);removeElement(M);return J}};select.focusAfterNode=function(L,J){var K=J.ownerDocument.body.createTextRange();K.moveToElementText(L||J);K.collapse(!L);K.select()};function B(M,K){var L=M.document.selection;if(L){var J=L.createRange();J.pasteHTML(K);J.collapse(false);J.select()}}select.insertNewlineAtCursor=function(J){B(J,"<br/>")};select.cursorPos=function(K,P){var N=K.ownerDocument.selection;if(!N){return null}var M=select.selectionTopNode(K,P);while(M&&M.nodeName!="BR"){M=M.previousSibling}var L=N.createRange(),J=L.duplicate();L.collapse(P);if(M){J.moveToElementText(M);J.collapse(false)}else{try{J.moveToElementText(K)}catch(O){return null}J.collapse(true)}L.setEndPoint("StartToStart",J);return{node:M,offset:L.text.length}};select.setCursorPos=function(K,N,M){function J(P){var O=K.ownerDocument.body.createTextRange();if(!P.node){O.moveToElementText(K);O.collapse(true)}else{O.moveToElementText(P.node);O.collapse(false)}O.move("character",P.offset);return O}var L=J(N);if(M&&M!=N){L.setEndPoint("EndToEnd",J(M))}L.select()};select.scrollToCursor=function(J){var K=J.ownerDocument.selection;if(!K){return null}K.createRange().scrollIntoView()}}else{var G=!window.scrollX&&!window.scrollY;select.markSelection=function(N){H=false;var M=N.getSelection();if(!M||M.rangeCount==0){return null}var L=M.getRangeAt(0);var J={start:{node:L.startContainer,offset:L.startOffset},end:{node:L.endContainer,offset:L.endOffset},window:N,scrollX:G&&N.document.body.scrollLeft,scrollY:G&&N.document.body.scrollTop};function K(O){while(O.node.nodeType!=3&&O.node.nodeName!="BR"){var P=O.node.childNodes[O.offset]||O.node.nextSibling;O.offset=0;while(!P&&O.node.parentNode){O.node=O.node.parentNode;P=O.node.nextSibling}O.node=P;if(!P){break}}}K(J.start);K(J.end);if(J.start.node){J.start.node.selectStart=J.start}if(J.end.node){J.end.node.selectEnd=J.end}return J};select.selectMarked=function(L){if(!L||!H){return }var M=L.window;var K=M.document.createRange();function J(N,O){if(N.node){delete N.node["select"+O];if(N.offset==0){K["set"+O+"Before"](N.node)}else{K["set"+O](N.node,N.offset)}}else{K.setStartAfter(M.document.body.lastChild||M.document.body)}}if(G){L.window.document.body.scrollLeft=L.scrollX;L.window.document.body.scrollTop=L.scrollY}J(L.end,"End");J(L.start,"Start");F(K,M)};select.replaceSelection=function(M,K,L,N){H=true;function J(P){var O=M["select"+P];if(O){if(O.offset>L){O.offset-=L}else{K["select"+P]=O;delete M["select"+P];O.node=K;O.offset+=(N||0)}}}J("Start");J("End")};function F(J,L){var K=L.getSelection();K.removeAllRanges();K.addRange(J)}function C(K){var J=K.getSelection();if(!J||J.rangeCount==0){return false}else{return J.getRangeAt(0)}}select.selectionTopNode=function(J,N){var K=C(J.ownerDocument.defaultView);if(!K){return false}var L=N?K.startContainer:K.endContainer;var M=N?K.startOffset:K.endOffset;if(L.nodeType==3){if(M>0){return E(L,J)}else{return I(L,J)}}else{if(L.nodeName=="HTML"){return(M==1?null:J.lastChild)}else{if(L==J){return(M==0)?null:L.childNodes[M-1]}else{if(M==L.childNodes.length){return E(L,J)}else{if(M==0){return I(L,J)}else{return E(L.childNodes[M-1],J)}}}}}};select.focusAfterNode=function(L,J){var M=J.ownerDocument.defaultView,K=M.document.createRange();K.setStartBefore(J.firstChild||J);if(L&&!L.firstChild){K.setEndAfter(L)}else{if(L){K.setEnd(L,L.childNodes.length)}else{K.setEndBefore(J.firstChild||J)}}K.collapse(false);F(K,M)};insertNodeAtCursor=function(L,K){var J=C(L);if(!J){return }if(L.opera&&J.startContainer.nodeType==3&&J.startOffset!=0){var N=J.startContainer,M=N.nodeValue;N.parentNode.insertBefore(L.document.createTextNode(M.substr(0,J.startOffset)),N);N.nodeValue=M.substr(J.startOffset);N.parentNode.insertBefore(K,N)}else{J.insertNode(K)}J.setEndAfter(K);J.collapse(false);F(J,L);return K};var A=/Gecko/.test(navigator.userAgent);select.insertNewlineAtCursor=function(J){insertNodeAtCursor(J,J.document.createElement("BR"));if(A){insertNodeAtCursor(J,J.document.createTextNode(""))}};select.cursorPos=function(J,M){var K=C(window);if(!K){return }var L=select.selectionTopNode(J,M);while(L&&L.nodeName!="BR"){L=L.previousSibling}K=K.cloneRange();K.collapse(M);if(L){K.setStartAfter(L)}else{K.setStartBefore(J)}return{node:L,offset:K.toString().length}};select.setCursorPos=function(J,O,N){var M=J.ownerDocument.defaultView,L=M.document.createRange();function K(S,V,Q){if(!S){S=J.firstChild}else{S=S.nextSibling}if(!S){return }if(V==0){L["set"+Q+"Before"](S);return true}var T=[];function P(W){if(W.nodeType==3){T.push(W)}else{forEach(W.childNodes,P)}}while(true){while(S&&!T.length){P(S);S=S.nextSibling}var U=T.shift();if(!U){return false}var R=U.nodeValue.length;if(R>=V){L["set"+Q](U,V);return true}V-=R}}N=N||O;if(K(N.node,N.offset,"End")&&K(O.node,O.offset,"Start")){F(L,M)}};select.scrollToCursor=function(L){var K=L.ownerDocument.body,N=L.ownerDocument.defaultView;var M=select.selectionTopNode(L,true)||L.firstChild;while(M&&!M.offsetTop){M=M.previousSibling}var P=0,O=M;while(O&&O.offsetParent){P+=O.offsetTop;O=O.offsetParent}var J=P-K.scrollTop;if(J<0||J>N.innerHeight-10){N.scrollTo(0,P)}}}}());(function(){var A={more:function(){return this.peek()!==null},applies:function(C){var B=this.peek();return(B!==null&&C(B))},nextWhile:function(B){while(this.applies(B)){this.next()}},equals:function(B){return B===this.peek()},endOfLine:function(){var B=this.peek();return B==null||B=="\n"}};window.singleStringStream=function(B){var D=0,C=0;return update({peek:function(){if(D<B.length){return B.charAt(D)}else{return null}},next:function(){if(D>=B.length){if(D<C){throw"End of stringstream reached without emptying buffer."}else{throw StopIteration}}return B.charAt(D++)},get:function(){var E=B.slice(C,D);C=D;return E}},A)};window.multiStringStream=function(C){C=iter(C);var D="",F=0;var E=null,B="";return update({peek:function(){if(!E){try{E=this.step()}catch(G){if(G!=StopIteration){throw G}else{E=null}}}return E},step:function(){if(E){var G=E;E=null;return G}while(F==D.length){B+=D;D="";F=0;D=C.next()}return D.charAt(F++)},next:function(){try{return this.step()}catch(G){if(G==StopIteration&&B.length>0){throw"End of stringstream reached without emptying buffer ('"+B+"')."}else{throw G}}},get:function(){var G=B;var H=E?F-1:F;B="";if(H>0){G+=D.slice(0,H);D=D.slice(H);F=E?1:0}return G}},A)}})();function History(B,E,A,D){this.container=B;this.maxDepth=E;this.commitDelay=A;this.editor=D;this.parent=D.parent;var C={text:"",from:null,to:null};this.first=C;this.last=C;this.firstTouched=false;this.history=[];this.redoHistory=[];this.touched=[]}History.prototype={touch:function(A){this.setTouched(A);this.parent.clearTimeout(this.commitTimeout);this.commitTimeout=this.parent.setTimeout(method(this,"commit"),this.commitDelay)},undo:function(){this.commit();if(this.history.length){this.redoHistory.push(this.updateTo(this.history.pop(),"applyChain"))}},redo:function(){this.commit();if(this.redoHistory.length){this.addUndoLevel(this.updateTo(this.redoHistory.pop(),"applyChain"))}},push:function(F,E,B){var D=[];for(var C=0;C<B.length;C++){var A=(C==B.length-1)?E:this.container.ownerDocument.createElement("BR");D.push({from:F,to:A,text:B[C]});F=A}this.pushChains([D])},pushChains:function(A){this.commit();this.addUndoLevel(this.updateTo(A,"applyChain"));this.redoHistory=[]},reset:function(){this.commit();this.history=[];this.redoHistory=[]},textAfter:function(A){return this.after(A).text},nodeAfter:function(A){return this.after(A).to},nodeBefore:function(A){return this.before(A).from},commit:function(){this.parent.clearTimeout(this.commitTimeout);this.editor.highlightDirty(true);var B=this.touchedChains(),A=this;if(B.length){this.addUndoLevel(this.updateTo(B,"linkChain"));this.redoHistory=[]}},updateTo:function(E,A){var D=[],C=[];for(var B=0;B<E.length;B++){D.push(this.shadowChain(E[B]));C.push(this[A](E[B]))}if(A=="applyChain"){this.notifyDirty(C)}return D},notifyDirty:function(A){forEach(A,method(this.editor,"addDirtyNode"));this.editor.scheduleHighlight()},linkChain:function(C){for(var B=0;B<C.length;B++){var A=C[B];if(A.from){A.from.historyAfter=A}else{this.first=A}if(A.to){A.to.historyBefore=A}else{this.last=A}}},after:function(A){return A?A.historyAfter:this.first},before:function(A){return A?A.historyBefore:this.last},setTouched:function(A){if(A){if(!A.historyTouched){this.touched.push(A);A.historyTouched=true}}else{this.firstTouched=true}},addUndoLevel:function(A){this.history.push(A);if(this.history.length>this.maxDepth){this.history.shift()}},touchedChains:function(){var C=this;function H(J,I){return J.replace(/\u00a0/g," ")==I.replace(/\u00a0/g," ")}var D=null;function B(I){return I?I.historyTemp:D}function E(J,I){if(J){J.historyTemp=I}else{D=I}}var A=[];if(C.firstTouched){C.touched.push(null)}forEach(C.touched,function(J){if(J){J.historyTouched=false;if(J.parentNode!=C.container){return }}else{C.firstTouched=false}var L=[];for(var K=J?J.nextSibling:C.container.firstChild;K&&K.nodeName!="BR";K=K.nextSibling){if(K.currentText){L.push(K.currentText)}}var I={from:J,to:K,text:L.join("")};var M=C.after(J);if(!M||!H(M.text,I.text)||M.to!=I.to){A.push(I);E(J,I)}});function G(L,I){var K=I+"Sibling",J=L[K];while(J&&J.nodeName!="BR"){J=J[K]}return J}var F=[];C.touched=[];forEach(A,function(J){if(!B(J.from)){return }var K=[],L=J.from;while(true){var I=B(L);if(!I){break}K.unshift(I);E(L,null);if(!L){break}L=G(L,"previous")}L=J.to;while(true){var I=B(L);if(!I||!L){break}K.push(I);E(L,null);L=G(L,"next")}if(C.after(K[0].from)&&C.before(K[K.length-1].to)){F.push(K)}else{forEach(K,function(M){C.setTouched(M.from)})}});return F},recordChange:function(A,B){if(this.onChange){this.onChange(A,B)}},shadowChain:function(C){var E=[],D=this.after(C[0].from),B=C[C.length-1].to;while(true){E.push(D);var A=D.to;if(!A||A==B){break}else{D=A.historyAfter}}return E},applyChain:function(A){var J=select.cursorPos(this.container,false),L=this;function B(Q,P){var O=Q?Q.nextSibling:L.container.firstChild;while(O!=P){var N=O.nextSibling;removeElement(O);O=N}}var C=A[0].from,G=A[A.length-1].to;B(C,G);var K=G?function(N){L.container.insertBefore(N,G)}:function(N){L.container.appendChild(N)};for(var H=0;H<A.length;H++){var M=A[H];if(H>0){K(M.from)}var F=this.container.ownerDocument.createTextNode(M.text);K(F);if(J&&J.node==M.from){var E=0;var D=this.after(M.from);if(D&&H==A.length-1){for(var I=0;I<J.offset&&M.text.charAt(I)==D.text.charAt(I);I++){}if(J.offset>I){E=M.text.length-D.text.length}}select.setCursorPos(this.container,{node:M.from,offset:Math.max(0,J.offset+E)})}else{if(J&&(H==A.length-1)&&J.node&&J.node.parentNode!=this.container){select.setCursorPos(this.container,{node:M.from,offset:M.text.length})}}}this.linkChain(A);return C}};function method(B,A){return function(){B[A].apply(B,arguments)}}function update(B,C){for(var A in C){B[A]=C[A]}return B}var StopIteration={toString:function(){return"StopIteration"}};function iter(A){var B=0;if(A.next){return A}else{return{next:function(){if(B>=A.length){throw StopIteration}else{return A[++B]}}}}}function forEach(A,C){if(A.next){try{while(true){C(A.next())}}catch(D){if(D!=StopIteration){throw D}}}else{for(var B=0;B<A.length;B++){C(A[B])}}}function map(A,C){var B=[];forEach(A,function(D){B.push(C(D))});return B}function matcher(A){return function(B){return A.test(B)}}function hasClass(B,C){var A=B.className;return A&&new RegExp("(^| )"+C+"($| )").test(A)}function insertAfter(A,D){var C=D.parentNode;var B=D.nextSibling;if(B){C.insertBefore(A,B)}else{C.appendChild(A)}return A}function insertAtStart(B,A){if(A.firstChild){A.insertBefore(B,A.firstChild)}else{A.appendChild(B)}return B}function removeElement(A){if(A.parentNode){A.parentNode.removeChild(A)}}function clearElement(A){while(A.firstChild){A.removeChild(A.firstChild)}}function isAncestor(A,B){while(B=B.parentNode){if(A==B){return true}}return false}var nbsp="\u00a0";function normalizeEvent(A){if(!A.stopPropagation){A.stopPropagation=function(){this.cancelBubble=true};A.preventDefault=function(){this.returnValue=false}}if(!A.stop){A.stop=function(){this.stopPropagation();this.preventDefault()}}if(A.type=="keypress"){if(A.charCode===0||A.charCode==undefined){A.code=A.keyCode}else{A.code=A.charCode}A.character=String.fromCharCode(A.code)}return A}function addEventHandler(C,B,A){function D(E){A(normalizeEvent(E||window.event))}if(typeof C.addEventListener=="function"){C.addEventListener(B,D,false);return function(){C.removeEventListener(B,D,false)}}else{C.attachEvent("on"+B,D);return function(){C.detachEvent("on"+B,D)}}}function removeEventHandler(A){A()};
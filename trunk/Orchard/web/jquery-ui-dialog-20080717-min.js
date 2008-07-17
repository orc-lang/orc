(function(C){jQuery.extend(jQuery.expr[":"],{data:"jQuery.data(a, m[3])"});C.ui={plugin:{add:function(E,F,H){var G=C.ui[E].prototype;for(var D in H){G.plugins[D]=G.plugins[D]||[];G.plugins[D].push([F,H[D]])}},call:function(D,F,E){var H=D.plugins[F];if(!H){return }for(var G=0;G<H.length;G++){if(D.options[H[G][0]]){H[G][1].apply(D.element,E)}}}},cssCache:{},css:function(D){if(C.ui.cssCache[D]){return C.ui.cssCache[D]}var E=C('<div class="ui-gen">').addClass(D).css({position:"absolute",top:"-5000px",left:"-5000px",display:"block"}).appendTo("body");C.ui.cssCache[D]=!!((!(/auto|default/).test(E.css("cursor"))||(/^[1-9]/).test(E.css("height"))||(/^[1-9]/).test(E.css("width"))||!(/none/).test(E.css("backgroundImage"))||!(/transparent|rgba\(0, 0, 0, 0\)/).test(E.css("backgroundColor"))));try{C("body").get(0).removeChild(E.get(0))}catch(F){}return C.ui.cssCache[D]},disableSelection:function(D){C(D).attr("unselectable","on").css("MozUserSelect","none")},enableSelection:function(D){C(D).attr("unselectable","off").css("MozUserSelect","")},hasScroll:function(G,E){var D=(E&&E=="left")?"scrollLeft":"scrollTop",F=false;if(G[D]>0){return true}G[D]=1;F=(G[D]>0);G[D]=0;return F}};var B=C.fn.remove;C.fn.remove=function(){C("*",this).add(this).triggerHandler("remove");return B.apply(this,arguments)};function A(E,F,G){var D=C[E][F].getter||[];D=(typeof D=="string"?D.split(/,?\s+/):D);return(C.inArray(G,D)!=-1)}C.widget=function(E,D){var F=E.split(".")[0];E=E.split(".")[1];C.fn[E]=function(J){var H=(typeof J=="string"),I=Array.prototype.slice.call(arguments,1);if(H&&A(F,E,J)){var G=C.data(this[0],E);return(G?G[J].apply(G,I):undefined)}return this.each(function(){var K=C.data(this,E);if(H&&K&&C.isFunction(K[J])){K[J].apply(K,I)}else{if(!H){C.data(this,E,new C[F][E](this,J))}}})};C[F][E]=function(I,H){var G=this;this.widgetName=E;this.widgetEventPrefix=C[F][E].eventPrefix||E;this.widgetBaseClass=F+"-"+E;this.options=C.extend({},C.widget.defaults,C[F][E].defaults,H);this.element=C(I).bind("setData."+E,function(L,J,K){return G.setData(J,K)}).bind("getData."+E,function(K,J){return G.getData(J)}).bind("remove",function(){return G.destroy()});this.init()};C[F][E].prototype=C.extend({},C.widget.prototype,D)};C.widget.prototype={init:function(){},destroy:function(){this.element.removeData(this.widgetName)},getData:function(D){return this.options[D]},setData:function(D,E){this.options[D]=E;if(D=="disabled"){this.element[E?"addClass":"removeClass"](this.widgetBaseClass+"-disabled")}},enable:function(){this.setData("disabled",false)},disable:function(){this.setData("disabled",true)},trigger:function(E,G,F){var D=(E==this.widgetEventPrefix?E:this.widgetEventPrefix+E);G=G||C.event.fix({type:D,target:this.element[0]});return this.element.triggerHandler(D,[G,F],this.options[E])}};C.widget.defaults={disabled:false};C.ui.mouse={mouseInit:function(){var D=this;this.element.bind("mousedown."+this.widgetName,function(E){return D.mouseDown(E)});if(C.browser.msie){this._mouseUnselectable=this.element.attr("unselectable");this.element.attr("unselectable","on")}this.started=false},mouseDestroy:function(){this.element.unbind("."+this.widgetName);(C.browser.msie&&this.element.attr("unselectable",this._mouseUnselectable))},mouseDown:function(F){(this._mouseStarted&&this.mouseUp(F));this._mouseDownEvent=F;var E=this,G=(F.which==1),D=(typeof this.options.cancel=="string"?C(F.target).parents().add(F.target).filter(this.options.cancel).length:false);if(!G||D||!this.mouseCapture(F)){return true}this._mouseDelayMet=!this.options.delay;if(!this._mouseDelayMet){this._mouseDelayTimer=setTimeout(function(){E._mouseDelayMet=true},this.options.delay)}if(this.mouseDistanceMet(F)&&this.mouseDelayMet(F)){this._mouseStarted=(this.mouseStart(F)!==false);if(!this._mouseStarted){F.preventDefault();return true}}this._mouseMoveDelegate=function(H){return E.mouseMove(H)};this._mouseUpDelegate=function(H){return E.mouseUp(H)};C(document).bind("mousemove."+this.widgetName,this._mouseMoveDelegate).bind("mouseup."+this.widgetName,this._mouseUpDelegate);return false},mouseMove:function(D){if(C.browser.msie&&!D.button){return this.mouseUp(D)}if(this._mouseStarted){this.mouseDrag(D);return false}if(this.mouseDistanceMet(D)&&this.mouseDelayMet(D)){this._mouseStarted=(this.mouseStart(this._mouseDownEvent,D)!==false);(this._mouseStarted?this.mouseDrag(D):this.mouseUp(D))}return !this._mouseStarted},mouseUp:function(D){C(document).unbind("mousemove."+this.widgetName,this._mouseMoveDelegate).unbind("mouseup."+this.widgetName,this._mouseUpDelegate);if(this._mouseStarted){this._mouseStarted=false;this.mouseStop(D)}return false},mouseDistanceMet:function(D){return(Math.max(Math.abs(this._mouseDownEvent.pageX-D.pageX),Math.abs(this._mouseDownEvent.pageY-D.pageY))>=this.options.distance)},mouseDelayMet:function(D){return this._mouseDelayMet},mouseStart:function(D){},mouseDrag:function(D){},mouseStop:function(D){},mouseCapture:function(D){return true}};C.ui.mouse.defaults={cancel:null,distance:1,delay:0}})(jQuery);(function(B){var A={dragStart:"start.draggable",drag:"drag.draggable",dragStop:"stop.draggable",maxHeight:"maxHeight.resizable",minHeight:"minHeight.resizable",maxWidth:"maxWidth.resizable",minWidth:"minWidth.resizable",resizeStart:"start.resizable",resize:"drag.resizable",resizeStop:"stop.resizable"};B.widget("ui.dialog",{init:function(){this.options.title=this.options.title||this.element.attr("title");var J=this,K=this.options,D=typeof K.resizable=="string"?K.resizable:"n,e,s,w,se,sw,ne,nw",E=this.element.addClass("ui-dialog-content").wrap("<div/>").wrap("<div/>"),G=(this.uiDialogContainer=E.parent()).addClass("ui-dialog-container").css({position:"relative",width:"100%",height:"100%"}),H=K.title||"&nbsp;",C=(this.uiDialogTitlebar=B('<div class="ui-dialog-titlebar"/>')).append('<span class="ui-dialog-title">'+H+"</span>").append('<a href="#" class="ui-dialog-titlebar-close"><span>X</span></a>').prependTo(G),I=(this.uiDialog=G.parent()).appendTo(document.body).hide().addClass("ui-dialog").addClass(K.dialogClass).addClass(E.attr("className")).removeClass("ui-dialog-content").css({position:"absolute",width:K.width,height:K.height,overflow:"hidden",zIndex:K.zIndex}).attr("tabIndex",-1).css("outline",0).keydown(function(L){if(K.closeOnEscape){var M=27;(L.keyCode&&L.keyCode==M&&J.close())}}).mousedown(function(){J.moveToTop()}),F=(this.uiDialogButtonPane=B("<div/>")).addClass("ui-dialog-buttonpane").css({position:"absolute",bottom:0}).appendTo(I);this.uiDialogTitlebarClose=B(".ui-dialog-titlebar-close",C).hover(function(){B(this).addClass("ui-dialog-titlebar-close-hover")},function(){B(this).removeClass("ui-dialog-titlebar-close-hover")}).mousedown(function(L){L.stopPropagation()}).click(function(){J.close();return false});C.find("*").add(C).each(function(){B.ui.disableSelection(this)});if(B.fn.draggable){I.draggable({cancel:".ui-dialog-content",helper:K.dragHelper,handle:".ui-dialog-titlebar",start:function(){J.moveToTop();(K.dragStart&&K.dragStart.apply(J.element[0],arguments))},drag:function(){(K.drag&&K.drag.apply(J.element[0],arguments))},stop:function(){(K.dragStop&&K.dragStop.apply(J.element[0],arguments));B.ui.dialog.overlay.resize()}});(K.draggable||I.draggable("disable"))}if(B.fn.resizable){I.resizable({cancel:".ui-dialog-content",helper:K.resizeHelper,maxWidth:K.maxWidth,maxHeight:K.maxHeight,minWidth:K.minWidth,minHeight:K.minHeight,start:function(){(K.resizeStart&&K.resizeStart.apply(J.element[0],arguments))},resize:function(){(K.autoResize&&J.size.apply(J));(K.resize&&K.resize.apply(J.element[0],arguments))},handles:D,stop:function(){(K.autoResize&&J.size.apply(J));(K.resizeStop&&K.resizeStop.apply(J.element[0],arguments));B.ui.dialog.overlay.resize()}});(K.resizable||I.resizable("disable"))}this.createButtons(K.buttons);this.isOpen=false;(K.bgiframe&&B.fn.bgiframe&&I.bgiframe());(K.autoOpen&&this.open())},setData:function(C,D){(A[C]&&this.uiDialog.data(A[C],D));switch(C){case"buttons":this.createButtons(D);break;case"draggable":this.uiDialog.draggable(D?"enable":"disable");break;case"height":this.uiDialog.height(D);break;case"position":this.position(D);break;case"resizable":(typeof D=="string"&&this.uiDialog.data("handles.resizable",D));this.uiDialog.resizable(D?"enable":"disable");break;case"title":B(".ui-dialog-title",this.uiDialogTitlebar).html(D||"&nbsp;");break;case"width":this.uiDialog.width(D);break}B.widget.prototype.setData.apply(this,arguments)},position:function(H){var D=B(window),E=B(document),F=E.scrollTop(),C=E.scrollLeft(),G=F;if(B.inArray(H,["center","top","right","bottom","left"])>=0){H=[H=="right"||H=="left"?H:"center",H=="top"||H=="bottom"?H:"middle"]}if(H.constructor!=Array){H=["center","middle"]}if(H[0].constructor==Number){C+=H[0]}else{switch(H[0]){case"left":C+=0;break;case"right":C+=D.width()-this.uiDialog.width();break;default:case"center":C+=(D.width()-this.uiDialog.width())/2}}if(H[1].constructor==Number){F+=H[1]}else{switch(H[1]){case"top":F+=0;break;case"bottom":F+=D.height()-this.uiDialog.height();break;default:case"middle":F+=(D.height()-this.uiDialog.height())/2}}F=Math.max(F,G);this.uiDialog.css({top:F,left:C})},size:function(){var D=this.uiDialogContainer,G=this.uiDialogTitlebar,E=this.element,F=(parseInt(E.css("margin-top"),10)||0)+(parseInt(E.css("margin-bottom"),10)||0),C=(parseInt(E.css("margin-left"),10)||0)+(parseInt(E.css("margin-right"),10)||0);E.height(D.height()-G.outerHeight()-F);E.width(D.width()-C)},open:function(){if(this.isOpen){return }this.overlay=this.options.modal?new B.ui.dialog.overlay(this):null;(this.uiDialog.next().length&&this.uiDialog.appendTo("body"));this.position(this.options.position);this.uiDialog.show(this.options.show);(this.options.autoResize&&this.size());this.moveToTop(true);this.trigger("open",null,{options:this.options});this.isOpen=true},moveToTop:function(E){if((this.options.modal&&!E)||(!this.options.stack&&!this.options.modal)){return this.trigger("focus",null,{options:this.options})}var D=this.options.zIndex,C=this.options;B(".ui-dialog:visible").each(function(){D=Math.max(D,parseInt(B(this).css("z-index"),10)||C.zIndex)});(this.overlay&&this.overlay.$el.css("z-index",++D));this.uiDialog.css("z-index",++D);this.trigger("focus",null,{options:this.options})},close:function(){(this.overlay&&this.overlay.destroy());this.uiDialog.hide(this.options.hide);this.trigger("close",null,{options:this.options});B.ui.dialog.overlay.resize();this.isOpen=false},destroy:function(){(this.overlay&&this.overlay.destroy());this.uiDialog.hide();this.element.unbind(".dialog").removeData("dialog").removeClass("ui-dialog-content").hide().appendTo("body");this.uiDialog.remove()},createButtons:function(F){var E=this,C=false,D=this.uiDialogButtonPane;D.empty().hide();B.each(F,function(){return !(C=true)});if(C){D.show();B.each(F,function(G,H){B("<button/>").text(G).click(function(){H.apply(E.element[0],arguments)}).appendTo(D)})}},fakeEvent:function(C){return B.event.fix({type:C,target:this.element[0]})}});B.extend(B.ui.dialog,{defaults:{autoOpen:true,autoResize:true,bgiframe:false,buttons:{},closeOnEscape:true,draggable:true,height:200,minHeight:100,minWidth:150,modal:false,overlay:{},position:"center",resizable:true,stack:true,width:300,zIndex:1000},overlay:function(C){this.$el=B.ui.dialog.overlay.create(C)}});B.extend(B.ui.dialog.overlay,{instances:[],events:B.map("focus,mousedown,mouseup,keydown,keypress,click".split(","),function(C){return C+".dialog-overlay"}).join(" "),create:function(D){if(this.instances.length===0){setTimeout(function(){B("a, :input").bind(B.ui.dialog.overlay.events,function(){var F=false;var H=B(this).parents(".ui-dialog");if(H.length){var E=B(".ui-dialog-overlay");if(E.length){var G=parseInt(E.css("z-index"),10);E.each(function(){G=Math.max(G,parseInt(B(this).css("z-index"),10))});F=parseInt(H.css("z-index"),10)>G}else{F=true}}return F})},1);B(document).bind("keydown.dialog-overlay",function(E){var F=27;(E.keyCode&&E.keyCode==F&&D.close())});B(window).bind("resize.dialog-overlay",B.ui.dialog.overlay.resize)}var C=B("<div/>").appendTo(document.body).addClass("ui-dialog-overlay").css(B.extend({borderWidth:0,margin:0,padding:0,position:"absolute",top:0,left:0,width:this.width(),height:this.height()},D.options.overlay));(D.options.bgiframe&&B.fn.bgiframe&&C.bgiframe());this.instances.push(C);return C},destroy:function(C){this.instances.splice(B.inArray(this.instances,C),1);if(this.instances.length===0){B("a, :input").add([document,window]).unbind(".dialog-overlay")}C.remove()},height:function(){if(B.browser.msie&&B.browser.version<7){var D=Math.max(document.documentElement.scrollHeight,document.body.scrollHeight);var C=Math.max(document.documentElement.offsetHeight,document.body.offsetHeight);if(D<C){return B(window).height()+"px"}else{return D+"px"}}else{if(B.browser.opera){return Math.max(window.innerHeight,B(document).height())+"px"}else{return B(document).height()+"px"}}},width:function(){if(B.browser.msie&&B.browser.version<7){var C=Math.max(document.documentElement.scrollWidth,document.body.scrollWidth);var D=Math.max(document.documentElement.offsetWidth,document.body.offsetWidth);if(C<D){return B(window).width()+"px"}else{return C+"px"}}else{if(B.browser.opera){return Math.max(window.innerWidth,B(document).width())+"px"}else{return B(document).width()+"px"}}},resize:function(){var C=B([]);B.each(B.ui.dialog.overlay.instances,function(){C=C.add(this)});C.css({width:0,height:0}).css({width:B.ui.dialog.overlay.width(),height:B.ui.dialog.overlay.height()})}});B.extend(B.ui.dialog.overlay.prototype,{destroy:function(){B.ui.dialog.overlay.destroy(this.$el)}})})(jQuery);(function(A){A.widget("ui.draggable",A.extend({},A.ui.mouse,{init:function(){if(this.options.helper=="original"&&!(/^(?:r|a|f)/).test(this.element.css("position"))){this.element[0].style.position="relative"}(this.options.disabled&&this.element.addClass("ui-draggable-disabled"));this.mouseInit()},mouseStart:function(F){var H=this.options;if(this.helper||H.disabled||A(F.target).is(".ui-resizable-handle")){return false}var C=!this.options.handle||!A(this.options.handle,this.element).length?true:false;A(this.options.handle,this.element).find("*").andSelf().each(function(){if(this==F.target){C=true}});if(!C){return false}if(A.ui.ddmanager){A.ui.ddmanager.current=this}this.helper=A.isFunction(H.helper)?A(H.helper.apply(this.element[0],[F])):(H.helper=="clone"?this.element.clone():this.element);if(!this.helper.parents("body").length){this.helper.appendTo((H.appendTo=="parent"?this.element[0].parentNode:H.appendTo))}if(this.helper[0]!=this.element[0]&&!(/(fixed|absolute)/).test(this.helper.css("position"))){this.helper.css("position","absolute")}this.margins={left:(parseInt(this.element.css("marginLeft"),10)||0),top:(parseInt(this.element.css("marginTop"),10)||0)};this.cssPosition=this.helper.css("position");this.offset=this.element.offset();this.offset={top:this.offset.top-this.margins.top,left:this.offset.left-this.margins.left};this.offset.click={left:F.pageX-this.offset.left,top:F.pageY-this.offset.top};this.offsetParent=this.helper.offsetParent();var B=this.offsetParent.offset();if(this.offsetParent[0]==document.body&&A.browser.mozilla){B={top:0,left:0}}this.offset.parent={top:B.top+(parseInt(this.offsetParent.css("borderTopWidth"),10)||0),left:B.left+(parseInt(this.offsetParent.css("borderLeftWidth"),10)||0)};var E=this.element.position();this.offset.relative=this.cssPosition=="relative"?{top:E.top-(parseInt(this.helper.css("top"),10)||0)+this.offsetParent[0].scrollTop,left:E.left-(parseInt(this.helper.css("left"),10)||0)+this.offsetParent[0].scrollLeft}:{top:0,left:0};this.originalPosition=this.generatePosition(F);this.helperProportions={width:this.helper.outerWidth(),height:this.helper.outerHeight()};if(H.cursorAt){if(H.cursorAt.left!=undefined){this.offset.click.left=H.cursorAt.left+this.margins.left}if(H.cursorAt.right!=undefined){this.offset.click.left=this.helperProportions.width-H.cursorAt.right+this.margins.left}if(H.cursorAt.top!=undefined){this.offset.click.top=H.cursorAt.top+this.margins.top}if(H.cursorAt.bottom!=undefined){this.offset.click.top=this.helperProportions.height-H.cursorAt.bottom+this.margins.top}}if(H.containment){if(H.containment=="parent"){H.containment=this.helper[0].parentNode}if(H.containment=="document"||H.containment=="window"){this.containment=[0-this.offset.relative.left-this.offset.parent.left,0-this.offset.relative.top-this.offset.parent.top,A(H.containment=="document"?document:window).width()-this.offset.relative.left-this.offset.parent.left-this.helperProportions.width-this.margins.left-(parseInt(this.element.css("marginRight"),10)||0),(A(H.containment=="document"?document:window).height()||document.body.parentNode.scrollHeight)-this.offset.relative.top-this.offset.parent.top-this.helperProportions.height-this.margins.top-(parseInt(this.element.css("marginBottom"),10)||0)]}if(!(/^(document|window|parent)$/).test(H.containment)){var D=A(H.containment)[0];var G=A(H.containment).offset();this.containment=[G.left+(parseInt(A(D).css("borderLeftWidth"),10)||0)-this.offset.relative.left-this.offset.parent.left,G.top+(parseInt(A(D).css("borderTopWidth"),10)||0)-this.offset.relative.top-this.offset.parent.top,G.left+Math.max(D.scrollWidth,D.offsetWidth)-(parseInt(A(D).css("borderLeftWidth"),10)||0)-this.offset.relative.left-this.offset.parent.left-this.helperProportions.width-this.margins.left-(parseInt(this.element.css("marginRight"),10)||0),G.top+Math.max(D.scrollHeight,D.offsetHeight)-(parseInt(A(D).css("borderTopWidth"),10)||0)-this.offset.relative.top-this.offset.parent.top-this.helperProportions.height-this.margins.top-(parseInt(this.element.css("marginBottom"),10)||0)]}}this.propagate("start",F);this.helperProportions={width:this.helper.outerWidth(),height:this.helper.outerHeight()};if(A.ui.ddmanager&&!H.dropBehaviour){A.ui.ddmanager.prepareOffsets(this,F)}this.helper.addClass("ui-draggable-dragging");this.mouseDrag(F);return true},convertPositionTo:function(C,D){if(!D){D=this.position}var B=C=="absolute"?1:-1;return{top:(D.top+this.offset.relative.top*B+this.offset.parent.top*B-(this.cssPosition=="fixed"||(this.cssPosition=="absolute"&&this.offsetParent[0]==document.body)?0:this.offsetParent[0].scrollTop)*B+(this.cssPosition=="fixed"?A(document).scrollTop():0)*B+this.margins.top*B),left:(D.left+this.offset.relative.left*B+this.offset.parent.left*B-(this.cssPosition=="fixed"||(this.cssPosition=="absolute"&&this.offsetParent[0]==document.body)?0:this.offsetParent[0].scrollLeft)*B+(this.cssPosition=="fixed"?A(document).scrollLeft():0)*B+this.margins.left*B)}},generatePosition:function(E){var F=this.options;var B={top:(E.pageY-this.offset.click.top-this.offset.relative.top-this.offset.parent.top+(this.cssPosition=="fixed"||(this.cssPosition=="absolute"&&this.offsetParent[0]==document.body)?0:this.offsetParent[0].scrollTop)-(this.cssPosition=="fixed"?A(document).scrollTop():0)),left:(E.pageX-this.offset.click.left-this.offset.relative.left-this.offset.parent.left+(this.cssPosition=="fixed"||(this.cssPosition=="absolute"&&this.offsetParent[0]==document.body)?0:this.offsetParent[0].scrollLeft)-(this.cssPosition=="fixed"?A(document).scrollLeft():0))};if(!this.originalPosition){return B}if(this.containment){if(B.left<this.containment[0]){B.left=this.containment[0]}if(B.top<this.containment[1]){B.top=this.containment[1]}if(B.left>this.containment[2]){B.left=this.containment[2]}if(B.top>this.containment[3]){B.top=this.containment[3]}}if(F.grid){var D=this.originalPosition.top+Math.round((B.top-this.originalPosition.top)/F.grid[1])*F.grid[1];B.top=this.containment?(!(D<this.containment[1]||D>this.containment[3])?D:(!(D<this.containment[1])?D-F.grid[1]:D+F.grid[1])):D;var C=this.originalPosition.left+Math.round((B.left-this.originalPosition.left)/F.grid[0])*F.grid[0];B.left=this.containment?(!(C<this.containment[0]||C>this.containment[2])?C:(!(C<this.containment[0])?C-F.grid[0]:C+F.grid[0])):C}return B},mouseDrag:function(B){this.position=this.generatePosition(B);this.positionAbs=this.convertPositionTo("absolute");this.position=this.propagate("drag",B)||this.position;if(!this.options.axis||this.options.axis!="y"){this.helper[0].style.left=this.position.left+"px"}if(!this.options.axis||this.options.axis!="x"){this.helper[0].style.top=this.position.top+"px"}if(A.ui.ddmanager){A.ui.ddmanager.drag(this,B)}return false},mouseStop:function(C){var D=false;if(A.ui.ddmanager&&!this.options.dropBehaviour){var D=A.ui.ddmanager.drop(this,C)}if((this.options.revert=="invalid"&&!D)||(this.options.revert=="valid"&&D)||this.options.revert===true){var B=this;A(this.helper).animate(this.originalPosition,parseInt(this.options.revert,10)||500,function(){B.propagate("stop",C);B.clear()})}else{this.propagate("stop",C);this.clear()}return false},clear:function(){this.helper.removeClass("ui-draggable-dragging");if(this.options.helper!="original"&&!this.cancelHelperRemoval){this.helper.remove()}this.helper=null;this.cancelHelperRemoval=false},plugins:{},uiHash:function(B){return{helper:this.helper,position:this.position,absolutePosition:this.positionAbs,options:this.options}},propagate:function(C,B){A.ui.plugin.call(this,C,[B,this.uiHash()]);if(C=="drag"){this.positionAbs=this.convertPositionTo("absolute")}return this.element.triggerHandler(C=="drag"?C:"drag"+C,[B,this.uiHash()],this.options[C])},destroy:function(){if(!this.element.data("draggable")){return }this.element.removeData("draggable").unbind(".draggable").removeClass("ui-draggable-dragging ui-draggable-disabled");this.mouseDestroy()}}));A.extend(A.ui.draggable,{defaults:{appendTo:"parent",axis:false,cancel:":input",delay:0,distance:1,helper:"original"}});A.ui.plugin.add("draggable","cursor",{start:function(D,C){var B=A("body");if(B.css("cursor")){C.options._cursor=B.css("cursor")}B.css("cursor",C.options.cursor)},stop:function(C,B){if(B.options._cursor){A("body").css("cursor",B.options._cursor)}}});A.ui.plugin.add("draggable","zIndex",{start:function(D,C){var B=A(C.helper);if(B.css("zIndex")){C.options._zIndex=B.css("zIndex")}B.css("zIndex",C.options.zIndex)},stop:function(C,B){if(B.options._zIndex){A(B.helper).css("zIndex",B.options._zIndex)}}});A.ui.plugin.add("draggable","opacity",{start:function(D,C){var B=A(C.helper);if(B.css("opacity")){C.options._opacity=B.css("opacity")}B.css("opacity",C.options.opacity)},stop:function(C,B){if(B.options._opacity){A(B.helper).css("opacity",B.options._opacity)}}});A.ui.plugin.add("draggable","iframeFix",{start:function(C,B){A(B.options.iframeFix===true?"iframe":B.options.iframeFix).each(function(){A('<div class="ui-draggable-iframeFix" style="background: #fff;"></div>').css({width:this.offsetWidth+"px",height:this.offsetHeight+"px",position:"absolute",opacity:"0.001",zIndex:1000}).css(A(this).offset()).appendTo("body")})},stop:function(C,B){A("div.DragDropIframeFix").each(function(){this.parentNode.removeChild(this)})}});A.ui.plugin.add("draggable","scroll",{start:function(D,C){var E=C.options;var B=A(this).data("draggable");E.scrollSensitivity=E.scrollSensitivity||20;E.scrollSpeed=E.scrollSpeed||20;B.overflowY=function(F){do{if(/auto|scroll/.test(F.css("overflow"))||(/auto|scroll/).test(F.css("overflow-y"))){return F}F=F.parent()}while(F[0].parentNode);return A(document)}(this);B.overflowX=function(F){do{if(/auto|scroll/.test(F.css("overflow"))||(/auto|scroll/).test(F.css("overflow-x"))){return F}F=F.parent()}while(F[0].parentNode);return A(document)}(this);if(B.overflowY[0]!=document&&B.overflowY[0].tagName!="HTML"){B.overflowYOffset=B.overflowY.offset()}if(B.overflowX[0]!=document&&B.overflowX[0].tagName!="HTML"){B.overflowXOffset=B.overflowX.offset()}},drag:function(D,C){var E=C.options;var B=A(this).data("draggable");if(B.overflowY[0]!=document&&B.overflowY[0].tagName!="HTML"){if((B.overflowYOffset.top+B.overflowY[0].offsetHeight)-D.pageY<E.scrollSensitivity){B.overflowY[0].scrollTop=B.overflowY[0].scrollTop+E.scrollSpeed}if(D.pageY-B.overflowYOffset.top<E.scrollSensitivity){B.overflowY[0].scrollTop=B.overflowY[0].scrollTop-E.scrollSpeed}}else{if(D.pageY-A(document).scrollTop()<E.scrollSensitivity){A(document).scrollTop(A(document).scrollTop()-E.scrollSpeed)}if(A(window).height()-(D.pageY-A(document).scrollTop())<E.scrollSensitivity){A(document).scrollTop(A(document).scrollTop()+E.scrollSpeed)}}if(B.overflowX[0]!=document&&B.overflowX[0].tagName!="HTML"){if((B.overflowXOffset.left+B.overflowX[0].offsetWidth)-D.pageX<E.scrollSensitivity){B.overflowX[0].scrollLeft=B.overflowX[0].scrollLeft+E.scrollSpeed}if(D.pageX-B.overflowXOffset.left<E.scrollSensitivity){B.overflowX[0].scrollLeft=B.overflowX[0].scrollLeft-E.scrollSpeed}}else{if(D.pageX-A(document).scrollLeft()<E.scrollSensitivity){A(document).scrollLeft(A(document).scrollLeft()-E.scrollSpeed)}if(A(window).width()-(D.pageX-A(document).scrollLeft())<E.scrollSensitivity){A(document).scrollLeft(A(document).scrollLeft()+E.scrollSpeed)}}}});A.ui.plugin.add("draggable","snap",{start:function(D,C){var B=A(this).data("draggable");B.snapElements=[];A(C.options.snap.constructor!=String?(C.options.snap.items||":data(draggable)"):C.options.snap).each(function(){var F=A(this);var E=F.offset();if(this!=B.element[0]){B.snapElements.push({item:this,width:F.outerWidth(),height:F.outerHeight(),top:E.top,left:E.left})}})},drag:function(P,K){var E=A(this).data("draggable");var Q=K.options.snapTolerance||20;var O=K.absolutePosition.left,N=O+E.helperProportions.width,D=K.absolutePosition.top,C=D+E.helperProportions.height;for(var M=E.snapElements.length-1;M>=0;M--){var L=E.snapElements[M].left,J=L+E.snapElements[M].width,I=E.snapElements[M].top,S=I+E.snapElements[M].height;if(!((L-Q<O&&O<J+Q&&I-Q<D&&D<S+Q)||(L-Q<O&&O<J+Q&&I-Q<C&&C<S+Q)||(L-Q<N&&N<J+Q&&I-Q<D&&D<S+Q)||(L-Q<N&&N<J+Q&&I-Q<C&&C<S+Q))){if(E.snapElements[M].snapping){(E.options.snap.release&&E.options.snap.release.call(E.element,null,A.extend(E.uiHash(),{snapItem:E.snapElements[M].item})))}E.snapElements[M].snapping=false;continue}if(K.options.snapMode!="inner"){var B=Math.abs(I-C)<=20;var R=Math.abs(S-D)<=20;var G=Math.abs(L-N)<=20;var H=Math.abs(J-O)<=20;if(B){K.position.top=E.convertPositionTo("relative",{top:I-E.helperProportions.height,left:0}).top}if(R){K.position.top=E.convertPositionTo("relative",{top:S,left:0}).top}if(G){K.position.left=E.convertPositionTo("relative",{top:0,left:L-E.helperProportions.width}).left}if(H){K.position.left=E.convertPositionTo("relative",{top:0,left:J}).left}}var F=(B||R||G||H);if(K.options.snapMode!="outer"){var B=Math.abs(I-D)<=20;var R=Math.abs(S-C)<=20;var G=Math.abs(L-O)<=20;var H=Math.abs(J-N)<=20;if(B){K.position.top=E.convertPositionTo("relative",{top:I,left:0}).top}if(R){K.position.top=E.convertPositionTo("relative",{top:S-E.helperProportions.height,left:0}).top}if(G){K.position.left=E.convertPositionTo("relative",{top:0,left:L}).left}if(H){K.position.left=E.convertPositionTo("relative",{top:0,left:J-E.helperProportions.width}).left}}if(!E.snapElements[M].snapping&&(B||R||G||H||F)){(E.options.snap.snap&&E.options.snap.snap.call(E.element,null,A.extend(E.uiHash(),{snapItem:E.snapElements[M].item})))}E.snapElements[M].snapping=(B||R||G||H||F)}}});A.ui.plugin.add("draggable","connectToSortable",{start:function(D,C){var B=A(this).data("draggable");B.sortables=[];A(C.options.connectToSortable).each(function(){if(A.data(this,"sortable")){var E=A.data(this,"sortable");B.sortables.push({instance:E,shouldRevert:E.options.revert});E.refreshItems();E.propagate("activate",D,B)}})},stop:function(D,C){var B=A(this).data("draggable");A.each(B.sortables,function(){if(this.instance.isOver){this.instance.isOver=0;B.cancelHelperRemoval=true;this.instance.cancelHelperRemoval=false;if(this.shouldRevert){this.instance.options.revert=true}this.instance.mouseStop(D);this.instance.element.triggerHandler("sortreceive",[D,A.extend(this.instance.ui(),{sender:B.element})],this.instance.options.receive);this.instance.options.helper=this.instance.options._helper}else{this.instance.propagate("deactivate",D,B)}})},drag:function(F,E){var D=A(this).data("draggable"),B=this;var C=function(K){var H=K.left,J=H+K.width,I=K.top,G=I+K.height;return(H<(this.positionAbs.left+this.offset.click.left)&&(this.positionAbs.left+this.offset.click.left)<J&&I<(this.positionAbs.top+this.offset.click.top)&&(this.positionAbs.top+this.offset.click.top)<G)};A.each(D.sortables,function(G){if(C.call(D,this.instance.containerCache)){if(!this.instance.isOver){this.instance.isOver=1;this.instance.currentItem=A(B).clone().appendTo(this.instance.element).data("sortable-item",true);this.instance.options._helper=this.instance.options.helper;this.instance.options.helper=function(){return E.helper[0]};F.target=this.instance.currentItem[0];this.instance.mouseCapture(F,true);this.instance.mouseStart(F,true,true);this.instance.offset.click.top=D.offset.click.top;this.instance.offset.click.left=D.offset.click.left;this.instance.offset.parent.left-=D.offset.parent.left-this.instance.offset.parent.left;this.instance.offset.parent.top-=D.offset.parent.top-this.instance.offset.parent.top;D.propagate("toSortable",F)}if(this.instance.currentItem){this.instance.mouseDrag(F)}}else{if(this.instance.isOver){this.instance.isOver=0;this.instance.cancelHelperRemoval=true;this.instance.options.revert=false;this.instance.mouseStop(F,true);this.instance.options.helper=this.instance.options._helper;this.instance.currentItem.remove();if(this.instance.placeholder){this.instance.placeholder.remove()}D.propagate("fromSortable",F)}}})}});A.ui.plugin.add("draggable","stack",{start:function(D,B){var C=A.makeArray(A(B.options.stack.group)).sort(function(F,E){return(parseInt(A(F).css("zIndex"),10)||B.options.stack.min)-(parseInt(A(E).css("zIndex"),10)||B.options.stack.min)});A(C).each(function(E){this.style.zIndex=B.options.stack.min+E});this[0].style.zIndex=B.options.stack.min+C.length}})})(jQuery);
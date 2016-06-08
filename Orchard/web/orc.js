//
// orc.js -- JavaScript source for the "Try Orc" Orchard Web interface
// Project Orchard
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

/**
 * This library augments every element with class="orc" with a run button.
 * It works on textareas and anything else as well.
 *
 * If there is an element with class="orc-prelude" preceeding the class="orc"
 * element, its contents will be prepended to the code whenever it is compiled.
 * Be warned that using this feature will throw off line numbers so you probably
 * shouldn't use it for editable programs.
 *
 * @author quark
 */
var Orc = (function () {

function parseQuery() {
	var parts = document.location.search.substr(1).split("&");
	var out = {};
	var tmp;
	// var i in parts breaks when used with some js libraries
	// which extend the Array prototype
	for (var i = 0; i < parts.length; ++i) {
		tmp = parts[i].split("=");
		out[tmp[0]] = tmp[1] ? tmp[1] : true;
	}
	return out;
}

docCookies = {
    getItem: function (sKey) {
        if (!sKey || !this.hasItem(sKey)) { return null; }
        return unescape(document.cookie.replace(new RegExp("(?:^|.*;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=\\s*((?:[^;](?!;))*[^;]?).*"), "$1"));
    },
    hasItem: function (sKey) { return (new RegExp("(?:^|;\\s*)" + escape(sKey).replace(/[\-\.\+\*]/g, "\\$&") + "\\s*\\=")).test(document.cookie); }
};


var query = parseQuery();
var mock = query.mock;
//mock = true;
var baseUrl = mock ? mock : "/orchard/";
var devKey = docCookies.hasItem("OrchardDevKey") ? docCookies.getItem("OrchardDevKey") : "";

// load our dependencies
document.write("<script src='", baseUrl, "jquery-1.12.4.min.js'><\/script>");
document.write("<script src='", baseUrl, (mock?"codemirror/codemirror.js":"codemirror-min.js"), "'><\/script>");
// load the rest of our code after jQuery and other services are ready
document.write("<script src='", baseUrl, (mock?"orc-ready.js":"orc-ready-min.js"), "'><\/script>");

// public exports
return {
	query: query,
	baseUrl: baseUrl,
	mock: mock,
	devKey: devKey };

})(); // end module

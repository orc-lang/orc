/**
 * This library augments every element with class="orc" with a run button.
 * It works on textareas and anything else as well.
 *
 * If there is an element with class="orc-prelude" preceeding the class="orc"
 * element, its contents will be prepended to the code whenever it is compiled.
 *
 * @author quark
 */
var Orc = (function () {

function parseQuery() {
	var parts = document.location.search.substr(1).split("&");
	var out = {};
	var tmp;
	for (var i in parts) {
		tmp = parts[i].split("=");
		out[tmp[0]] = tmp[1] ? tmp[1] : true;
	}
	return out;
}

var query = parseQuery();
var executorServiceUrl = query.mock
	? "mock-executor.js"
	: "/orchard/json/executor?js";
var baseUrl = query.mock ? "" : "/orchard/";

// load our dependencies
document.write("<script src='", baseUrl, "jquery-1.2.6-min.js'><\/script>");
document.write("<script src='", baseUrl, "codemirror-20080715-min.js'><\/script>");
document.write("<script src='", executorServiceUrl, "'><\/script>");
// load the rest of our code after jQuery and other services are ready
document.write("<script src='", baseUrl, "orc-ready.js'><\/script>");

// public exports
return {query: query, baseUrl: baseUrl}

})(); // end module

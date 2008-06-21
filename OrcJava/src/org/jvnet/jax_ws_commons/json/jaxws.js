
	createXmlHttpRequest : function () {
		if (window.XMLHttpRequest) {
			return new XMLHttpRequest();
		} else if (window.ActiveXObject) {
			return new ActiveXObject("Microsoft.XMLHTTP");
		} else {
			throw "XMLHttpRequest not available";
		}
	},

	/*  Prototype JavaScript framework, version 1.5.1_rc2
	 *  (c) 2005-2007 Sam Stephenson
	 *
	 *  Prototype is freely distributable under the terms of an MIT-style license.
	 *  For details, see the Prototype web site: http://www.prototypejs.org/
	 *
	/*--------------------------------------------------------------------------*/
	toJSON: function(object) {
		var type = typeof object;
		switch (type) {
		case 'undefined':
		case 'function':
		case 'unknown': return;
		case 'object': break;
		case 'string': return this.stringToJSON(object);
		default: return object.toString();
		}
		if (object === null) return 'null';
		if (object.ownerDocument === document) return;
		var results = [];
		for (var property in object) {
			var value = this.toJSON(object[property]);
			if (value !== undefined)
				results.push(property + ':' + value);
		}
		return '{' + results.join(',') + '}';
	},
	
	stringToJSON: function(string) {
		out = "'";
		for (var i = 0; i < string.length; i++) {
			var ch = string[i]
			switch (ch) {
			case '\\':
				out += '\\\\';
				break;
			case "'":
				out += "\\'";
				break;
			case "\b":
				out += "\\b";
				break;
			case "\t":
				out += "\\t";
				break;
			case "\f":
				out += "\\f";
				break;
			case "\r":
				out += "\\r";
				break;
			case "\n":
				out += "\\n";
				break;
			// FIXME: handle other special characters
			default:
				out += ch;
				break;
			}
		}
		return out + "'";
	},


	post : function(obj, func) {
		var req = this.createXmlHttpRequest();
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.responseText) {
					if (func) func(eval('('+req.responseText+')'));
				} else {
					throw "Error: no response (Status "+req.status+": "+req.statusText + ")";
				}
			}
		};
		req.open("POST", this.url, true);
		req.setRequestHeader("Content-Type", "application/json");
		req.send(this.toJSON(obj));
	},


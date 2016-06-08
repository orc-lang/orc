
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
			var ch = string.charAt(i);
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
	
	/** Reassign this to change the default error handler. */
	onError : function(response, status, exception) {
		if (exception) throw exception;
		else throw {
			response: response,
			status: status
		};
	},

	post : function(message, onResult, onError) {
		var req = this.createXmlHttpRequest();
		if (!onError) onError = this.onError;
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				try {
					var response = req.response
						? window.JSON.parse(req.response + "")
						: null;
					if (req.status == 200) {
						if (onResult) onResult(response);
					} else {
						onError(response, req.status);
					}
				} catch (e) {
					onError(null, null, e);
				}
			}
		};
		try {
			req.open("POST", this.url, true);
			req.setRequestHeader("Content-Type", "application/json");
			req.send(this.toJSON(message));
		} catch (e) {
			onError(null, null, e);
		}
	},


/**
 * Basic functions to display and read dates in various formats. The module is
 * packaged in an object to avoid namespace collisions; for public functions,
 * see the end of the module.
 */
var net_sixfingeredman_date = new (function() {

/**
 * Parse a human-readable date using simple heuristics.
 * Understands formats like:
 * 2003-12-5; 5 dec 2003; 12/5/2003; 12/5; December 5 2003; Dec 5
 */
function fromHumanDate(string) {
	if (!string) return null;
	if (string.toLowerCase() == 'none') return 'None'; // "None" is an allowable date
	var m, d, y, tmp;
	var parts = trim(string).split(/[-:/., ]+/);
	// handle trivial undelimited formats
	if (parts.length == 1) parts = split_fl(parts[0], [2,2,4]);
	// I miss destructuring assignment
	// most likely order (in the US): m-d-y
	m = parts[0];
	d = parts[1];
	y = parts[2];

	if (d && !isNumeric(d)) {
		// day looks like a month; assume d-m-y
		tmp = d;
		d = m;
		m = tmp;
	} else if (isNumeric(m) && m > 12) {
		// month looks like a year; assume y-m-d
		tmp = y;
		y = m;
		m = d;
		d = tmp;
	}
	if (!d || !m) return null;

	// now we have decided upon the order, for better or worse;
	// let's clean up the values slightly
	d = parseInt(d);
	// month could be a name
	if (isNumeric(m)) m = parseInt(m);
	else m = monthNumbers[m.substr(0,3).toLowerCase()] + 1;
	// missing year is OK, we'll just assume the current year
	if (!y) y = new Date().getFullYear();
	else {
		y = parseInt(y);
		// try and fix 2-digit years
		if (y < 40) y += 2000;
		else if (y < 100) y += 1900;
	}

	// does the date look unreasonable? we'd better give up
	if (m < 1 || m > 12 || d < 1 || d > 31 || y < 1000 || y > 3000)
		return null;

	return new Date(y, m-1, d);
}

function fromSoapDate(string) {
	if (null == string) return null;
	var matches = string.match(/^([0-9]{4})-([0-9]{2})-([0-9]{2})/);
	if (null == matches) return null;
	return new Date(matches[1], matches[2]-1, matches[3]);
}

function fromSoapDateTime(string) {
	if (null == string) return null;
	var pattern = new RegExp("^([0-9]{4})-([0-9]{2})-([0-9]{2})"
		+ "T([0-9]{2}):([0-9]{2}):([0-9]{2})(\.[0-9]+)?"
		+ "(Z|([+\-])([0-9]{2}):([0-9]{2}))");
	var matches = string.match(pattern);
	if (null == matches) return null;
	if (matches[8] != 'Z') {
		if (matches[9] == '-') {
			matches[10] *= -1;
			matches[11] *= -1;
		}
		matches[4] = matches[4] + matches[10];
		matches[5] = matches[5] + matches[11];
	}
	return new Date(Date.UTC(matches[1], matches[2]-1, matches[3],
		matches[4], matches[5], matches[6]));
}

function format(date, fstring) {
	return _format(date, fstring);
}

function gmformat(date, fstring) {
	return _format(new UTCDate(date), fstring);
}

function UTCDate(date) {
	this.getFullYear = function() { return date.getUTCFullYear(); }
	this.getMonth = function() { return date.getUTCMonth(); }
	this.getDate = function() { return date.getUTCDate(); }
	this.getDay = function() { return date.getUTCDay(); }
	this.getHours = function() { return date.getUTCHours(); }
	this.getMinutes = function() { return date.getUTCMinutes(); }
	this.getSeconds = function() { return date.getUTCSeconds(); }
	this.getMilliseconds = function() { return date.getUTCMilliseconds(); }
}

function _format(date, fstring) {
	if (date == "None") return date;
	var out = '';
	for (var i = 0; i < fstring.length; i++) {
		var char = fstring.charAt(i);
		if (char == "\\") {
			out += fstring.charAt(++$i);
		} else {
			out += _format_char(date, char);
		}
	}
	return out;
}

var months = ['January', 'February', 'March', 'April', 'May', 'June', 'July',
'August', 'September', 'October', 'November', 'December'];

var monthNumbers = new Object();
for (var i in months) monthNumbers[months[i].substr(0,3).toLowerCase()] = i-0;

function _format_char(date, char) {
	switch(char) {
	case 'a': return (date.getHours() >= 12) ? 'pm' : 'am';
	case 'A': return (date.getHours() >= 12) ? 'PM' : 'AM';
	case 'H': return zeropad(2, date.getHours());

	case 'g':
		var h = date.getHours() % 12;
		return h?h:12;
	case 'h':
		var h = date.getHours() % 12;
		return zeropad(2, h?h:12);

	case 'i': return zeropad(2, date.getMinutes());
	case 's': return zeropad(2, date.getSeconds());

	case 'Y': return zeropad(4, date.getFullYear());
	case 'y': return zeropad(2, date.getFullYear() % 100);

	case 'm': return zeropad(2, date.getMonth()+1);
	case 'n': return date.getMonth()+1;
	case 'F': return months[date.getMonth()];
	case 'M': return months[date.getMonth()].substr(0, 3);

	case 'd': return zeropad(2, date.getDate());
	case 'j': return date.getDate();

	default: return char;
	}
}

function zeropad(size, number) {
	var out = number+"";
	while (out.length < size) {
		out = "0" + out;
	}
	return out;
}

function trim(string) {
	return string.replace(/^\s*(.*?)\s*$/, "$1");
}

function isNumeric(string) {
	return !isNaN(parseInt(string));
}

function split_fl(string, sizes) {
	var out = new Array();
	for (var i in sizes) {
		out.push(string.substr(0, sizes[i]));
		string = string.substr(sizes[i]);
	}
	if (string.length) out.push(string);
	return out;
}

// export public functions
this.fromHumanDate = fromHumanDate;
this.fromSoapDate = fromSoapDate;
this.fromSoapDateTime = fromSoapDateTime;
this.format = format;
this.gmformat = gmformat;
this.UTCDate = UTCDate;
})(); // end package

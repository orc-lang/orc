/*
 * Basic cookie manipulation and load/unload handlers to ensure that
 * navigating away from the orc code window does not wipe out its contents.
 * 
 * Derived from code at http://www.dynamicdrive.com/forums/showthread.php?t=34032
 * 
 */
window.onload = 
	function() { 
		document.getElementById('orc').value = cookieLoad('orc'); 
	}

window.onunload = 
	function() { 
		cookieSave('orc', document.getElementById('orc').value);  
	}

function cookieSave(name, k)
{
	document.cookie = name + "=" + escape(k);
}

function cookieLoad(name)
{
	var search = name + "=";
	if (document.cookie.length > 0)
	{
		offset = document.cookie.indexOf(search);
		if (offset != -1) {
			offset += search.length;
			end = document.cookie.indexOf(";", offset);
			if (end == -1) {
				end = document.cookie.length;
			}
			return unescape(document.cookie.substring(offset, end));
		}
	}
	return '';
}
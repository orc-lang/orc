/**
 * Simple hack to protect email addresses from SPAM harvesters.
 * Look for links with class="obfuscated-email" and add a mailto:.
 */
(function () {
function fixE(address) {
	return address.replace(' at ', '@');
}
function fixL(link) {
	var address;
	if (link.href) {
		address = fixE(link.href.replace('mailto:', ''));
	} else if (link.innerText) {
		address = fixE(link.innerText);
		link.innerText = address;
	} else if (link.textContent) {
		address = fixE(link.textContent);
		link.textContent = address;
	}
	link.href="mailto:" + address;
}
var onload = window.onload;
window.onload = function () {
	if (onload) onload();
	var links = document.getElementsByTagName('A');
	for (var i in links) {
		var link = links[i];
		if (link.className == 'obfuscated-email') fixL(link);
	}
}
})();

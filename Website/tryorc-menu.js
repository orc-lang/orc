//
// tryorc-menu.js -- JavaScript source for the "Try Orc" Orchard Web interface
// Project Orchard
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

jQuery(function ($) {
$("#menu").treeview({
	animated: "fast",
	unique: true
});

$("#menu .demo-link").each(function () {
	var $this = $(this);
	var file = this.getAttribute('href');
	this.href = "#" + file;
	$this.click(function () {
		loadOrcCode(file);
		$("#menu .demo-link").removeClass("selected");
		$this.addClass("selected");
	});
});

window.clearProgram = function() {
	document.getElementById("orc").setOrcCode("");
}

window.toggleExamples = function() {
	$('#menu-td').toggle();
	$('#header').toggle();
	$('#homelink').toggle();
}


});

function loadOrcCode(url) {
	jQuery.ajax({
		url: url,
        dataType: "text",
		success: function (data) {
			document.getElementById("orc").setOrcCode(data);
		},
		error: function () {
			alert("There was an error loading the example program.");
		}
	});
}

document.getElementById("orc").onOrcReady = function () {
	var file = document.location.hash ? document.location.hash.substring(1) : null;
	if (file) loadOrcCode(file);
};

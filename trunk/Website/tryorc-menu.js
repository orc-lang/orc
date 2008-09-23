(function ($) {
$("#menu").treeview({
	animated: "fast",
	unique: true
});

$("#menu .demo-link").each(function () {
	var $this = $(this);
	var file = this.href;
	this.href = "#";
	$this.click(function () {
		loadOrcCode(file);
		$("#menu .demo-link").removeClass("selected");
		$this.addClass("selected");
	});
});

window.toggleExamples = function() {
	$('#menu-td').toggle();
	$('#header').toggle();
	$('#homelink').toggle();
}

function escapeHtml(v) {
	v = v.replace(/&/g, '&amp;');
	v = v.replace(/</g, '&lt;');
	// FIXME: escape other special characters
	return v;
}
function loadOrcCode(url) {
	$.ajax({
		url: url,
		success: function (data) {
			document.getElementById("orc").setOrcCode(data);
		},
		error: function () {
			alert("There was an error loading the example program.");
		}
	});
}
})(jQuery);

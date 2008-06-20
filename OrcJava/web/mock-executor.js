/**
 * Used for testing the HTML UI without having to talk
 * to an actual webservice.
 */
executorService = {
	compileAndSubmit: function (_, f) {
		var n = 1;
		var halt = false;
		f({
			start: function (_, f) {
				f();
			},
			listen: function (_, f) {
				setTimeout(function () {
					f(halt ? null : {value:{"@xsi:type":"int", "$":(n++)+""}});
				}, 2000);
			},
			halt: function () {
				halt = true;
			},
			finish: function (_, f) {
				f();
			}
		});
	}
};

function loadService(_, url, f) {
	window[f](url);
}
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
					f(halt ? null :
						{"value":{"@xsi.type": "ns2:tuple", "@size": "2",
							"element": [{"@xsi.type": "ns2:constant",
									"value": {"@xsi.type": "xs:int", "$": (n++)+""}},
								{"@xsi.type": "ns2:list",
									"element": {"@xsi.type": "ns2:constant",
										"value": {"@xsi.type": "xs:string", "$": "hi"}}}]}});					
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
(function () {
	var n = 1;
	var halt = false;
	onJobServiceReady({
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
										"value": {"@xsi.type": "xs:string", "$": "hi"}}}]},
						"timestamp":"xxx"});
			}, 2000);
		},
		halt: function () {
			halt = true;
		},
		finish: function (_, f) {
			if (f) f();
		}
	});
})();
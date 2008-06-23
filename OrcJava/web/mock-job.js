(function () {
	var n = 1;
	var halt = false;
	onJobServiceReady({
		start: function (_, f) {
			f();
		},
		nextPublications: function (_, f) {
			if (halt) return f(null);
			setTimeout(function () {
				f({
					"value": {
						"@xsi.type": "ns2:tuple",
						"@size": "2",
						"element": [
							{
								"@xsi.type": "ns2:constant",
								"value": {"@xsi.type": "xs:int", "$": (n++)+""}
							},
							{
								"@xsi.type": "ns2:list",
								"element": {
									"@xsi.type": "ns2:constant",
									"value": {"@xsi.type": "xs:string", "$": "hi"}
								}
							}
						]
					},
					"timestamp":"publication timestamp "+n
				});
			}, 2000);
		},
		nextErrors: function (_, f) {
			if (halt) return f(null);
			if (n % 3 == 0) {
				n++;
				f({
					timestamp:"error timestamp "+n,
					message: "Error " + n,
					location: {
						filename: "test.orc",
						line: 100,
						column: 5
					}
				});
			} else {
				var _this = this;
				setTimeout(function () {
					_this.nextErrors(_, f);
				}, 1000);
			}
		},
		halt: function () {
			halt = true;
		},
		finish: function (_, f) {
			if (f) f();
		}
	});
})();

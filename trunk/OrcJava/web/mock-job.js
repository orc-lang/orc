(function () {
	var n = 1;
	var halt = false;
	onJobServiceReady({
		start: function (_, f) {
			f();
		},
		listen: function (_, f) {
			if (halt) return f(null);
			if (n%4 == 0) {
				// simulate occasional token errors
				n++;
				return f([{
					"@xsi.type": "ns2:tokenErrorEvent",
					timestamp:"error timestamp "+n,
					message: "Error " + n,
					location: {
						filename: "test.orc",
						line: 100,
						column: 5
					}
				}, {
					"@xsi.type": "ns2:printlnEvent",
					timestamp:"println timestamp "+n,
					line: "Printed line " + n
				}]);
			}
			setTimeout(function () {
				f({
					"@xsi.type": "ns2:publicationEvent",
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
							},
							{ "unexpected": [1,2] }
						]
					},
					"timestamp":"publication timestamp "+n
				});
			}, 1000);
		},
		halt: function (_, f) {
			halt = true;
			if (f) f();
		},
		finish: function (_, f) {
			if (f) f();
		}
	});
})();

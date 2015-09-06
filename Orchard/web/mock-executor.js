//
// mock-executor.js -- JavaScript source to simulate the executor service
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

executorService = (function () {
	var n = 1;
	var cancel = false;
	return {
		compileAndSubmit: function (x, f) {
			try {
				console.log(x.program);
			} catch (e) {}
			f("1");
		},
		startJob: function (_, f) {
			f();
		},
		respondToPrompt: function (x) {
			try {
				console.log(x);
			} catch (e) {}
		},
		cancelPrompt: function (x) {
			try {
				console.log(x);
			} catch (e) {}
		},
		jobEvents: function (_, f) {
			if (cancel) return f(null);
			if (n == 1) {
				n++;
				return f({
					"@xsi.type": "ns2:promptEvent",
					timestamp:"error timestamp "+n,
					promptID: 1,
					message: "Say something\nSay something\nSay something\nSay something",
					sequence: 3
				});
			}
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
					},
					sequence: 1
				}, {
					"@xsi.type": "ns2:printlnEvent",
					timestamp:"println timestamp "+n,
					line: "Printed line " + n,
					sequence: 2
				}, {
					"@xsi.type": "ns2:promptEvent",
					timestamp:"error timestamp "+n,
					promptID: 1,
					message: "Say something",
					sequence: 3
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
					"timestamp":"publication timestamp "+n,
					sequence: 3
				});
			}, 1000);
		},
		purgeJobEvents: function(_, f) {
			if (f) f();
		},
		cancelJob: function (_, f) {
			cancel = true;
			if (f) f();
		},
		finishJob: function (_, f) {
			if (f) f();
		}
	};
})();

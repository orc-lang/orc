{- json.orc -- Orc program json
 - 
 - Created by jthywiss on Dec 30, 2010 11:10:21 AM
 -}

import site JSON = "orc.lib.web.ReadJSON"

  "ECMAScript 5 Conformance Suite 15.12.1.1-0-9 " + (if
    JSON("\t\r \n{\t\r \n" +
         "\"property\"\t\r \n:\t\r \n{\t\r \n}\t\r \n,\t\r \n" +
         "\"prop2\"\t\r \n:\t\r \n" +
         "[\t\r \ntrue\t\r \n,\t\r \nnull\t\r \n,123.456\t\r \n]" +
         "\t\r \n}\t\r \n")
       = {. property = {.  .}, prop2 = [true, null, 123.456] .}
    then "pass" else "FAIL")
| "ECMAScript 5 Conformance Suite 15.12.1.1-g6 " + (if
    JSON("{ \"test\": \"\\\"\\/\\\\\\b\\f\\n\\r\\t\" }")
       = {. test = "\"/\\\f\n\r\t" .}
    then "pass" else "FAIL")
| "ECMAScript 5 Conformance Suite 15.12.1.1-g2-5 " + (if
    JSON("{ \"test\": \"\" }")
       = {. test = "" .}
    then "pass" else "FAIL")
| "JSON numeric literals " + (if
    JSON("[ 1, -2, 0, 0.4, -0.500, 6e7, 7.8e9, -8e9, -9.10e+11, -10.11e-12, 11.12e+00013 ]")
       = [1, -2, 0, 0.4, -0.5, 6E+7, 7.8E+9, -8E+9, -9.1E+11, -1.011E-11, 1.112E+14]
    then "pass" else "FAIL")
| "JSON object literal (with repeated key) " + (if
    JSON("{ \"z_a\": 0, \"x\": 1, \"x\": 2, \"a\": 3, \"m\": 4 }")
       = {. z_a = 0, x = 2, a = 3, m = 4 .}
    then "pass" else "FAIL")
| "JSON null & boolean literals " + (if
    JSON("[ null, false, true, null ]")
       = [null, false, true, null]
    then "pass" else "FAIL")
| "JSON empty object and array 1 " + (if
    JSON("[ { }, [ ] ]")
       = [{.  .}, []]
    then "pass" else "FAIL")
| "JSON empty object and array 2 " + (if
    JSON("{ \"a\": { }, \"b\": [ ] }")
       = {. a = {.  .}, b = [] .}
    then "pass" else "FAIL")

{-
OUTPUT:PERMUTABLE
"ECMAScript 5 Conformance Suite 15.12.1.1-0-9 pass"
"ECMAScript 5 Conformance Suite 15.12.1.1-g6 pass"
"ECMAScript 5 Conformance Suite 15.12.1.1-g2-5 pass"
"JSON numeric literals pass"
"JSON object literal (with repeated key) pass"
"JSON null & boolean literals pass"
"JSON empty object and array 1 pass"
"JSON empty object and array 2 pass"
-}

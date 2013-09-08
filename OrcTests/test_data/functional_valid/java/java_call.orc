{- java_call.orc -- Test Orc to Java bridge facility
 - 
 - $Id$
 - 
 - Created by jthywiss on Jul 14, 2010 8:48:56 PM
 -}

import class JavaBoolean = "java.lang.Boolean"
import class JavaString = "java.lang.String"
import class FieldTestClass = "org.omg.CORBA.portable.ServantObject"
import class AccessTestClass = "javax.swing.JTextField"

-- 1. Static field access
(if JavaBoolean.TRUE? then "1 pass" else "1 FAIL") |

-- 2. Constructor access
(if JavaBoolean("true") then "2 pass" else "2 FAIL") |

-- 3. Static method access
(if JavaBoolean.parseBoolean("true") then "3 pass" else "3 FAIL") |

-- 4. No-arg constructor, field assign, and field deref test
(FieldTestClass() >t> t.servant := "test 4" >> ( if t.servant?.equals("test 4") then "4 pass" else "4 FAIL" ) ) |

-- 5. Misuse instance field as method test
((FieldTestClass() >t> t.servant() >> "5 FAIL" ) :!: Bot ) |

-- 6. Misuse instance method as field test
((JavaBoolean.TRUE.toString? >> "6 FAIL" ) :!: Bot ) |

-- 7. Misuse static field as method test
((JavaBoolean.TRUE() >> "7 FAIL" ) :!: Bot ) |

-- 8. Misuse static method as field test
((JavaBoolean.parseBoolean? >> "8 FAIL" ) :!: Bot ) |

-- 9. Attempt to access protected method
((JavaBoolean.TRUE.clone() >> "9 FAIL" ) :!: Bot ) |

-- 10. An overloading & Orc conversion test -- make sure String.valueOf(double) is chosen
(if (JavaString.valueOf(2e108).equalsIgnoreCase("2E+108") || JavaString.valueOf(2e108).equalsIgnoreCase("2.0E108")) then "10 pass" else "10 FAIL") |

-- 11. Another overloading & Orc conversion test -- make sure String.valueOf(long) is chosen
(if JavaString.valueOf(9223372036854770000).equalsIgnoreCase("9223372036854770000") then "11 pass" else "11 FAIL") |

-- 12. Method accessibility tricky case: access a public method of a non-public class that is declared (publicly) in an interface
(
  AccessTestClass("Hello, world")  >o>
  o.getKeymap()  >km>
  km.getResolveParent() >r>
  r.getName() >>
  "12 pass"
) |

-- 13. Conversion tests: Test Java-Orc conversion
(if (
	    126.byteValue() = 126 &&
	    8e307.doubleValue() :> 7.99e307 &&
	    1e38.floatValue() :> 0.99e38 &&
	    2147483640.intValue() = 2147483640 &&
	    9223372036854775800.longValue() = 9223372036854775800 &&
	    32766.shortValue() = 32766
  ) then "13 pass" else "13 FAIL") |


--TODO: These require a custom Java class to test against:
-- "apply" default method
-- identical member name -- method vs. field
-- Invocation conversions.... prim widen, subclass, box, box-subclass, unbox, unbox-widen
-- Overloading where boxing would select a different method (i.e. JLS 15.12.2 Phase 1 & 2 different)
-- Check "most specific" picks right method
-- Ambiguous method invocation

stop


{-
OUTPUT:PERMUTABLE
"1 pass"
"2 pass"
"3 pass"
"4 pass"
Error: orc.error.runtime.JavaException: java.lang.NoSuchMethodException: No accessible servant in org.omg.CORBA.portable.ServantObject
Error: orc.error.runtime.JavaException: java.lang.NoSuchFieldException: toString
Error: orc.error.runtime.JavaException: java.lang.NoSuchMethodException: No accessible TRUE in java.lang.Boolean
Error: orc.error.runtime.JavaException: java.lang.NoSuchFieldException: parseBoolean
Error: orc.error.runtime.JavaException: java.lang.NoSuchMethodException: No accessible clone in java.lang.Boolean
"10 pass"
"11 pass"
"12 pass"
"13 pass"
-}

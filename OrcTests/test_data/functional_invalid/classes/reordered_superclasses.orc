-- Test reordered superclasses

class B1 { }
class B2 { }
class B3 { }

class C1 extends B1 with B2  { }
class C2 extends B2 with B3  { }
class C3 extends B3 with B1  { }

class D extends C1 with C2 with C3 {}

signal

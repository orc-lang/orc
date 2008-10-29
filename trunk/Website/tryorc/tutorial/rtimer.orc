-- Publish one string immediately, one 3 seconds later, and one 5 seconds later.
  "immediately"
| Rtimer(3000) >> "...three seconds later..."
| Rtimer(5000) >> "...five seconds later..."

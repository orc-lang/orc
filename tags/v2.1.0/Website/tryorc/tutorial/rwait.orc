-- Publish one string immediately, one 3 seconds later, and one 5 seconds later.
  "immediately"
| Rwait(3000) >> "...three seconds later..."
| Rwait(5000) >> "...five seconds later..."

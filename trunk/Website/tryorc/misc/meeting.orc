include "forms.inc"

val span = DateRange(Date(2008, 8, 10), Date(2008, 8, 15))
val invitees = [
  "Adrian Quark",
  "David Kitchin" ]

def invite(span)(name) =
WebPrompt(name + ": Meeting Request", [
  DateRangesField("times", "When Are You Available?", span, 9, 17),
  Button("submit", "Submit") ]) >data>
  data.get("times")

map(invite(span), invitees) >times:rest>
foldl(lambda (next, times) = times.intersect(next), rest, times) >>
times.getRanges() >ranges>
if ranges.size() > 0 then
  println("Meeting will start at: " + ranges.first().getStart()) >>
  stop
else
  println("No acceptable meeting time found.") >>
  stop

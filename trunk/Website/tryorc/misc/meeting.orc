include "forms.inc"

-- Configuration

val span = DateRange(Date(108, 8, 10), Date(108, 8, 15))
val quorum = 2
val invitees = [
  "Adrian Quark",
  "David Kitchin",
  "Jayadev Misra",
  "William Cook" ]

-- Utility functions

def getN(channel, n) =
  if n > 0 then
    channel.get():getN(channel, n-1)
  else []

def mergeRanges(accum:rest) =
  def f(next, accum) = accum.intersect(next)
  foldl(f, rest, accum) >>
  accum

def pickMeetingTime(times) =
  times.getRanges() >ranges>
  if ranges.size() > 0 then ranges.first().getStart()

def invite(span, name) =
  WebPrompt(name + ": Meeting Request", [
    DateRangesField("times", "When Are You Available?", span, 9, 17),
    Button("submit", "Submit") ]) >data>
    data.get("times")

def inviteQuorum(invitees, quorum) =
  let(
    val c = Buffer()
    getN(c, quorum)
    | each(invitees) >invitee>
      invite(span, invitee) >response>
      c.put(response) >> stop
  )

-- Main orchestration

inviteQuorum(invitees, quorum) >responses>
mergeRanges(responses) >times>
let("Meeting starts: " + pickMeetingTime(times)
    ; "No acceptable meeting found")

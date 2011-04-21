{-
Schedule a meeting. A window will pop up with a calendar for each invitee to
choose when they are available. Once enough responses have been received, Orc
will print a message for each invitee informing them of the meeting time.

Normally the invitees would receive a link via email and a notification via
email of the scheduled meeting, but that doesn't work very well for demos,
hence the popups.

See the variables under "Configuration" below to customize.
-}
include "forms.inc"

-- Configuration

-- The range of possible dates (Year, Month, Day), with an exclusive upper bound
val span = Interval(LocalDate(2008, 9, 29), LocalDate(2008, 10, 4))
val format = DateTimeFormat.forStyle("SS")
-- Number of responses required to schedule the meeting
val quorum = 2
-- Names of invitees
val invitees = [
  "John Thywissen",
  "David Kitchin",
  "Jayadev Misra" ]

-- Utility functions

def invite(span, name) =
  def send(span, name) =
    WebPrompt(name + ": Meeting Request", [
    DateTimeRangesField("times", "When Are You Available?", span, 9, 17),
    Button("submit", "Submit") ]) >data>
    data.get("times")
  send(span, name)

def getN(channel, n) =
  if n :> 0 then
    channel.get():getN(channel, n-1)
  else []

def member(item, []) = false
def member(item, h:t) =
  if item = h then true
  else member(item, t)

def pickMeetingTime(first:_) = first.getStart()
def pickMeetingTime(_) = stop

def inviteQuorum(invitees, quorum) =
  Let(
    val c = Channel()
    getN(c, quorum)
    | each(invitees) >invitee>
      invite(span, invitee) >response>
      c.put((invitee, response)) >> stop
  )

def notify(time, invitees, responders) =
  each(invitees) >invitee> (
    if member(invitee, responders) then
      Println(invitee + ": meeting is at " + time) >>
      stop
    else
      Println(invitee + ": if possible, come to meeting at " + time) >>
      stop
  ) ; "DONE"

-- Main orchestration

inviteQuorum(invitees, quorum) >responses>
unzip(responses) >(responders,ranges)>
afold(lambda (a,b) = a.intersect(b), ranges) >times>
Let(
  pickMeetingTime(iterableToList(times)) >time>
  format.print(time) >time>
  notify(time, invitees, responders)
  ; "No acceptable meeting found")


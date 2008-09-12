include "forms.inc"
include "mail.inc"

-- Configuration

-- dates are given in the form (Year-1900, Month-1, Day)
-- date range is inclusive on the bottom and exclusive on the top
val span = DateRange(Date(108, 8, 11), Date(108, 8, 12))
val requestor = Prompt("Your email address:")
val invitees = [ ("Full Name", "test@example.com") ]
val quorum = 1 -- number of responses required
def requestBody(name, url) =
  "Greetings " + name + ",\n" +
  "Click on the below URL to choose time slots when you are available for a 1-hour meeting.\n" +
  "After all invitees have responded, everyone will receive an email with the chosen meeting time.\n" +
  "\n" + url + "\n\n" +
  "Thank you, and if you have any questions, contact " + requestor + " for more information.\n"
def notificationBody(name, time) =
  "Greetings " + name + ",\n" +
  "All invitees have responded and the chosen time slot was:\n" +
  time + "\n" +
  "Thank you, and if you have any questions, contact " + requestor + " for more information.\n"

-- Utility functions

def getN(channel, n) =
  if n > 0 then
    channel.get():getN(channel, n-1)
  else []

def mergeRanges(accum:rest) =
  def f(next, accum) = accum.intersect(next) >> accum
  foldl(f, rest, accum) >> accum

def pickMeetingTime(times) =
  times.getRanges() >ranges>
  if ranges.size() > 0 then ranges.first().getStart()

def buildForm() =
  Form() >form>
  FieldGroup("data", "Meeting Request") >group>
  group.addPart(DateRangesField("times", "When are you available for a meeting?", span, 9, 17)) >>
  group.addPart(Button("submit", "Submit")) >>
  form.addPart(group) >>
  form

def invite(span, (name, email)) =
  println("Inviting " + name + " at " + email) >>
  buildForm() >form>
  SendForm(form) >receiver>
  SendMail(email, "Meeting Request", requestBody(name, receiver.getURL())) >>
  receiver.get() >>
  println("Received response from " + name + " at " + email) >>
  form.getValue().get("data").get("times")

def inviteQuorum(invitees, quorum) =
  let(
    val c = Buffer()
    getN(c, quorum)
    | each(invitees) >invitee>
      invite(span, invitee) >response>
      c.put((invitee, response)) >> stop
  )

def notify(time, invitees, responders) =
  each(invitees) >(name, email)>
  SendMail(email, "Meeting Notification", notificationBody(name, time))

-- Main orchestration

inviteQuorum(invitees, quorum) >responses>
unzip(responses) >(responders,ranges)>
mergeRanges(ranges) >times>
let(
  pickMeetingTime(times) >time>
  println("Chosen time: " + time) >>
  notify(time, invitees, responders)
  ; SendMail(requestor, "Meeting Request Failed", "The invitees could not agree on a meeting time.")) >>
"DONE"

{-
Schedules a meeting via email.
When you run this program, you will be prompted
to enter information about the meeting and invitees.
-}
include "forms.inc"
include "mail.inc"

val dateFormat = DateTimeFormat.forStyle("SS")
val timeFormat = DateTimeFormat.forStyle("-S")

-- returns [(name, address)]
def parseInvitees(text) =
  def nonemptyLine(line) = (line.trim().length() > 0)
  def parseInvitee(line) =
    val line = line.trim()
    val space = line.lastIndexOf(" ")
    val tab = line.lastIndexOf("\t")
    val splitAt = if space < tab then tab else space
    if splitAt > 0 then
    (line.substring(0, splitAt).trim(),
     line.substring(splitAt+1))
    else ("", line)
  val filtered = filter(nonemptyLine, lines(text))
  map(parseInvitee, filtered)

val (from, span, invitees, quorum, timeLimit, requestTemplate, notificationTemplate) =
  WebPrompt("Meeting Parameters", [
    Mandatory(Textbox("from", "Your Email Address")),
    Mandatory(DateField("start", "First Possible Meeting Date")),
    Mandatory(DateField("end", "Last Possible Meeting Date")),
    FormInstructions("limiti",
      "The meeting will be scheduled once a quorum of invitees have
      responded or the time limit is reached, whichever comes first."),
    Mandatory(IntegerField("quorum", "Quorum")),
    Mandatory(IntegerField("timeLimit", "Time Limit (hours)")),
    FormInstructions("inviteesi",
      "Invitees should be one per line: name and email address, separated by space.
       You may either upload the invitees or enter them in the text box below."),
    UploadField("inviteesUpload", "Upload Invitees"),
    Textarea("inviteesText", "Enter Invitees", "", false),
    Mandatory(Textarea("requestTemplate", "Request Message", 
      "Greetings {{NAME}},\n"
      + "Click on the below URL to choose time slots when you"
      + " are available for a 1-hour meeting.\n"
      + "After enough invitees have responded, everyone will receive"
      + " an email with the chosen meeting time.\n\n{{URL}}\n\n"
      + "Thank you, and if you have any questions, contact {{FROM}}"
      + " for more information.\n")),
    Mandatory(Textarea("notificationTemplate", "Notification Message",
      "Greetings {{NAME}},\n"
      + "The chosen time slot was:\n{{TIME}}\n"
      + "Thank you, and if you have any questions, contact {{FROM}}"
      + " for more information.\n")),
    Button("submit", "Submit") ])
  >data> (
    val inviteesText =
      if data.get("inviteesText") = null
      then data.get("inviteesUpload").getString()
      else data.get("inviteesUpload").getString()
           + "\n" + data.get("inviteesText")
    val invitees =
      parseInvitees(inviteesText) >(_:_) as x> x
      ; error("No invitees found. Please try again.")
    val span =
      Interval(data.get("start"), data.get("end").plusDays(1)) >span>
      if span.isEmpty()
      then error("Empty date range. Please try again.")
      else span
    (data.get("from"), span, invitees,
     data.get("quorum"), data.get("timeLimit"),
     data.get("requestTemplate"),
     data.get("notificationTemplate"))
  )

def requestBody(name, url) =
  requestTemplate
  .replace("{{NAME}}", name)
  .replace("{{FROM}}", from)
  .replace("{{URL}}", url)

def notificationBody(name, time) =
  notificationTemplate
  .replace("{{NAME}}", name)
  .replace("{{FROM}}", from)
  .replace("{{TIME}}", time)

-- get the first n items from the channel
-- until it is closed
def getN(channel, 0) = []
def getN(channel, n) =
  channel.get() >x>
  x:getN(channel, n-1)
  ; []

def inviteQuorum(invitees) =
  let(
    val c = Buffer()
    getN(c, quorum)
    -- invite invitees
    | each(invitees) >(name,_) as invitee>
      invite(invitee) >response>
      c.put((name, response)) >> stop
    -- close the buffer once the time limit is up
    | Rtimer(timeLimit*3600000) >> c.closenb() >> stop
  )

def pickMeetingTime(first:_) = first.getStart()
def pickMeetingTime(_) = stop

def buildForm() =
  Form() >form>
  FieldGroup("data", "Meeting Request") >group>
  group.addPart(DateTimeRangesField("times",
    "When are you available for a meeting?", span, 9, 17)) >>
  group.addPart(Button("submit", "Submit")) >>
  form.addPart(group) >>
  form

def invite((name, email)) =
  println("Inviting " + name + " at " + email) >>
  buildForm() >form>
  SendForm(form) >receiver>
  SendMail(email, "Meeting Request", requestBody(name, receiver.getURL())) >>
  receiver.get() >>
  println("Received response from " + name + " at " + email) >>
  form.getValue().get("data").get("times")

def notify(time, invitees, responders) =
  each(invitees) >(name, email)>
  SendMail(email, "Meeting Notification", notificationBody(name, time))

def formatResponses(responses) =
  def formatResponse((responder, times)) =
    def toString(x) =
      dateFormat.print(x.getStart())
      + " -- " + timeFormat.print(x.getEnd())
    responder + ":\n" + unlines(map(toString, times.getRanges()))
  unlines(map(formatResponse, responses))

def fail(message) =
  SendMail(from, "Meeting Request Failed",
    "The invitees were unable to agree on a meeting time. "
    + "Their responses follow:\n\n" + message)

-- Main orchestration

def handleResponses((_:_) as responses) =
  formatResponses(responses) >msg>
  unzip(responses) >(responders,ranges)>
  afold(lambda (a,b) = a.intersect(b), ranges) >times>
  let(
    pickMeetingTime(times) >time>
    dateFormat.print(time) >time>
    println("Chosen time: " + time) >>
    notify(time, invitees, responders)
    ; println("Request failed") >>
      fail(msg) )
  >> "DONE"
def handleResponses([]) =
  SendMail(from, "Meeting Request Failed",
    "Nobody responded to the meeting request.")

handleResponses(inviteQuorum(invitees))

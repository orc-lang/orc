{-
Schedules a meeting via email.
When you run this program, you will be prompted
to enter information about the meeting and invitees.
-}
include "forms.inc"
include "mail.inc"

val dateFormat = DateTimeFormat.forStyle("MS")
val timeFormat = DateTimeFormat.forStyle("-S")

-- returns [(name, address)]
def parseInvitees(text) =
  def nonemptyLine(line) = (line.trim().length() :> 0)
  def parseInvitee(line) =
    val line = line.trim()
    val space = line.lastIndexOf(" ")
    val tab = line.lastIndexOf("\t")
    val splitAt = if space <: tab then tab else space
    if splitAt :> 0 then
    (line.substring(0, splitAt).trim(),
     line.substring(splitAt+1))
    else ("", line)
  val filtered = filter(nonemptyLine, lines(text))
  map(parseInvitee, filtered)

val (from, meetingTopic, duration, span, invitees, quorum, timeLimit, requestTemplate, notificationTemplate) =
  WebPrompt("Meeting Parameters", [
    Mandatory(Textbox("fromName", "Your Name")),
    Mandatory(Textbox("fromEmail", "Your E-mail Address")),
    Mandatory(Textbox("meetingTopic", "Meeting Title/Topic")),
    Mandatory(IntegerField("duration", "Meeting Duration (minutes)")),
    Mandatory(DateField("start", "First Allowed Meeting Date")),
    Mandatory(DateField("end", "Last Allowed Meeting Date")),
    FormInstructions("limiti",
      "The meeting will be scheduled once a quorum of invitees have responded or the response time limit is reached, whichever comes first."),
    Mandatory(IntegerField("quorum", "Quorum")),
    Mandatory(IntegerField("timeLimit", "Response Time Limit (hours)")),
    FormInstructions("inviteesi",
      "Invitees should be one per line: name and e-mail address, separated by space. You may either upload the invitees or enter them in the text box below."),
    UploadField("inviteesUpload", "Upload Invitees"),
    Textarea("inviteesText", "Enter Invitees", "", false),
    Mandatory(Textarea("requestTemplate", "Request Message", 
      "Greetings {{NAME}},\n\n"
      + "We are organizing a \"{{TOPIC}}\" meeting.\n\n"
      + "Click on the below URL to choose time slots when you"
      + " are available to start a {{DURATION}}-minute meeting.\n"
      + "After enough invitees have responded, everyone will receive"
      + " an e-mail with the chosen meeting time.\n\n{{URL}}\n\n"
      + "Thank you, and if you have any questions, contact {{FROM}}"
      + " for more information.\n"
      + "\n\n--\nPowered by Orc -- https://orc.csres.utexas.edu/")),
    Mandatory(Textarea("notificationTemplate", "Notification Message",
      "Greetings {{NAME}},\n\n"
      + "Regarding the \"{{TOPIC}}\" meeting:\n"
      + "The chosen time slot is: {{START_TIME}} to {{END_TIME}}\n\n"
      + "Thank you, and if you have any questions, contact {{FROM}}"
      + " for more information.\n"
      + "\n\n--\nPowered by Orc -- https://orc.csres.utexas.edu/")),
    Button("submit", "Submit") ])
  >data> (
    val inviteesText =
      if data.get("inviteesText") = null
      then data.get("inviteesUpload").getString()
      else data.get("inviteesUpload").getString()
           + "\n" + data.get("inviteesText")
    val invitees =
      parseInvitees(inviteesText) >(_:_) as x> x
      ; Error("No invitees found. Please try again.")
    val span =
      Interval(data.get("start"), data.get("end").plusDays(1)) >span>
      (if span.isEmpty()
      then Error("Empty date range. Please try again.")
      else span)
    # (data.get("fromName")+" <"+data.get("fromEmail")+">",
     data.get("meetingTopic"), data.get("duration"),
     span, invitees,
     data.get("quorum"), data.get("timeLimit"),
     data.get("requestTemplate"),
     data.get("notificationTemplate"))
  )

def requestBody(name, url) =
  requestTemplate
  .replace("{{NAME}}", name)
  .replace("{{FROM}}", from)
  .replace("{{TOPIC}}", meetingTopic)
  .replace("{{DURATION}}", duration.toString())
  .replace("{{URL}}", url)

def notificationBody(name, startTime, endTime) =
  notificationTemplate
  .replace("{{NAME}}", name)
  .replace("{{FROM}}", from)
  .replace("{{TOPIC}}", meetingTopic)
  .replace("{{DURATION}}", duration.toString())
  .replace("{{START_TIME}}", startTime)
  .replace("{{END_TIME}}", endTime)

-- get the first n items from the channel
-- until it is closed
def getN(channel, 0) = []
def getN(channel, n) =
  channel.get() >x>
  x:getN(channel, n-1)
  ; []

def inviteQuorum(invitees) =
  Let(
    val c = Channel()
    getN(c, quorum)
    -- invite invitees
    | each(invitees) >(name,_) as invitee>
      invite(invitee) >response>
      c.put((name, response)) >> stop
    -- close the buffer once the time limit is up
    | Rwait(timeLimit*3600000) >> c.closeD() >> stop
  )

def pickMeetingTime(first:_) = first.getStart()
def pickMeetingTime(_) = stop

def buildForm() =
  Form() >form>
  FieldGroup("data", "Meeting Request") >group>
  group.addPart(DateTimeRangesField("times",
    "When are you available to start a "+duration+"-minute meeting?", span, 9, 17)) >>
  group.addPart(Button("submit", "Submit")) >>
  form.addPart(group) >>
  form

def invite((name, email)) =
  Println("Inviting " + name + " at " + email) >>
  buildForm() >form>
  SendForm(form) >receiver>
  SendMailFrom(from, email, "Meeting Request: "+meetingTopic, requestBody(name, receiver.getURL())) >>
  receiver.get() >>
  Println("Received response from " + name + " at " + email) >>
  form.getValue().get("data").get("times")

def notify(time, invitees, responders) =
  each(invitees) >(name, email)>
  SendMailFrom(from, email, "Meeting Notification: "+meetingTopic, notificationBody(name, dateFormat.print(time), dateFormat.print(time.plusMinutes(duration))))

def formatResponses(responses) =
  def formatResponse((responder, times)) =
    def toString(x) =
      dateFormat.print(x.getStart())
      + " -- " + timeFormat.print(x.getEnd())
    responder + ":\n" + unlines(map(toString, times))
  unlines(map(formatResponse, responses))

def fail(message) =
  SendMail(from, "Meeting Request Failed: "+meetingTopic,
    "The invitees were unable to agree on a meeting time. "
    + "Their responses follow:\n\n" + message)

-- Main orchestration

def handleResponses((_:_) as responses) =
  formatResponses(responses) >msg>
  unzip(responses) >(responders,ranges)>
  afold(lambda (a,b) = a.intersect(b), ranges) >times>
  Let(
    pickMeetingTime(times) >time>
    Println("Chosen time: " + dateFormat.print(time)) >>
    notify(time, invitees, responders)
    ; Println("Request failed") >>
      fail(msg) )
  >> "DONE"
def handleResponses([]) =
  SendMail(from, "Meeting Request Failed: "+meetingTopic,
    "Nobody responded to the meeting request.")

handleResponses(inviteQuorum(invitees))
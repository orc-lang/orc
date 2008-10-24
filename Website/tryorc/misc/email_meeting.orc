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

val (requestor, span, invitees) =
  WebPrompt("Meeting Parameters", [
    Mandatory(Textbox("from", "Your Email Address")),
    Mandatory(DateField("start", "First Possible Meeting Date")),
    Mandatory(DateField("end", "Last Possible Meeting Date")),
    FormInstructions("inviteesi",
      "Invitees should be one per line: name and email address, separated by space.
       You may either upload the invitees or enter them in the text box below."),
    UploadField("inviteesUpload", "Upload Invitees"),
    Textarea("inviteesText", "Enter Invitees", false),
    Button("submit", "Submit") ])
  >data> (
    val inviteesText =
      if data.get("inviteesText") = Null()
      then data.get("inviteesUpload").getString()
      else data.get("inviteesUpload").getString()
           + "\n" + data.get("inviteesText")
    val invitees =
      parseInvitees(inviteesText) >(_:_) as x> x
      ; error("No invitees found. Please try again.")
    val span =
      DateRange(data.get("start"), data.get("end").plusDays(1)) >span>
      if span.isEmpty()
      then error("Empty date range. Please try again.")
      else span
    val from = data.get("from")
    (from, span, invitees)
  )

def requestBody(name, url) =
  "Greetings " + name + ",\n" +
  "Click on the below URL to choose time slots when you"
  +" are available for a 1-hour meeting.\n"
  +"After all invitees have responded, everyone will receive"
  +" an email with the chosen meeting time.\n"
  +"\n" + url + "\n\n"
  +"Thank you, and if you have any questions, contact "
  + requestor + " for more information.\n"

def notificationBody(name, time) =
  "Greetings " + name + ",\n" +
  "All invitees have responded and the chosen time slot was:\n" +
  time + "\n" +
  "Thank you, and if you have any questions, contact "
  + requestor + " for more information.\n"

def mergeRanges(ranges) =
  def f(accum, next) = accum.intersect(next) >> accum
  foldl1(f, ranges)

def pickMeetingTime(times) =
  times.getRanges() >ranges>
  if ranges.size() > 0 then ranges.first().getStart()

def buildForm() =
  Form() >form>
  FieldGroup("data", "Meeting Request") >group>
  group.addPart(DateRangesField("times",
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
  (name, form.getValue().get("data").get("times"))

def notify(time, invitees, responders) =
  each(invitees) >(name, email)>
  SendMail(email, "Meeting Notification", notificationBody(name, time))

def failureMessage(responses) =
  def formatResponse((responder, times)) =
    def toString(x) =
      dateFormat.print(x.getStart())
      + " -- " + timeFormat.print(x.getEnd())
    responder + ":\n" + unlines(map(toString, times.getRanges()))
  unlines(map(formatResponse, responses))

def fail(message) =
  SendMail(requestor, "Meeting Request Failed",
    "The invitees were unable to agree on a meeting time. "
    + "Their responses follow:\n\n" + message)

-- Main orchestration

val responses = map(invite, invitees)
failureMessage(responses) >msg>
unzip(responses) >(responders,ranges)>
mergeRanges(ranges) >times>
let(
  pickMeetingTime(times) >time>
  dateFormat.print(time) >time>
  println("Chosen time: " + time) >>
  notify(time, invitees, responders)
  ; println("Request failed") >>
    fail(msg) )
>> "DONE"

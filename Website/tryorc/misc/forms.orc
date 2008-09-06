include "forms.inc"

def WebPrompt(fields) =
  Form() >form>
  FieldGroup("main", "Form") >group>
  fork(map(
    lambda (field)() = group.addPart(field),
    fields)) >>
  form.addPart(group) >>
  SendForm(form) >receiver>
  Redirect(receiver.getURL()) >>
  receiver.get() >>
  form.getValue().get("main")

WebPrompt([
  Textbox("first", "First Name"),
  Textbox("last", "Last Name"),
  IntegerField("age", "Age"),
  Checkbox("brown", "Brown Hair?"),
  Button("submit", "Submit") ]) >data>
println("First Name: " + data.get("first")) >>
println("Last Name: " + data.get("last")) >>
println("Age: " + data.get("age")) >>
println("Brown Hair? " + data.get("brown")) >>
println("Submitted: " + data.get("submit")) >>
stop

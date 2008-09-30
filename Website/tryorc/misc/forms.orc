{-
Demonstrate collecting data from the user via a web form.
Asks you for some personal information and then prints
the information you entered.
-}
include "forms.inc"

{-
This function is part of forms.inc but
is included here so you can see how the
low-level API works.
-}
def WebPrompt(title, fields) =
  Form() >form>
  FieldGroup("main", title) >group>
  map(group.addPart, fields) >>
  form.addPart(group) >>
  SendForm(form) >receiver>
  Redirect(receiver.getURL()) >>
  receiver.get() >>
  form.getValue().get("main")

WebPrompt("Personal Information", [
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

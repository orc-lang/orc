include "date.inc"

site SendForm = orc.lib.orchard.forms.FormSenderSite
class Form = orc.lib.orchard.forms.Form
class Textbox = orc.lib.orchard.forms.Textbox
class Textarea = orc.lib.orchard.forms.Textarea
class Checkbox = orc.lib.orchard.forms.Checkbox
class Button = orc.lib.orchard.forms.Button
class IntegerField = orc.lib.orchard.forms.IntegerField
class UploadField = orc.lib.orchard.forms.UploadField
class FormInstructions = orc.lib.orchard.forms.FormInstructions
class PasswordField = orc.lib.orchard.forms.PasswordField
class Mandatory = orc.lib.orchard.forms.Mandatory
class FieldGroup = orc.lib.orchard.forms.FieldGroup
class DateTimeRangesField = orc.lib.orchard.forms.DateTimeRangesField
class DateField = orc.lib.orchard.forms.DateField
  
-- returns the form data
def WebPrompt(title, fields) =
  Form() >form>
  FieldGroup("main", title) >group>
  map(group.addPart, fields) >>
  form.addPart(group) >>
  SendForm(form) >receiver>
  Redirect(receiver.getURL()) >>
  receiver.get() >>
  form.getValue().get("main")

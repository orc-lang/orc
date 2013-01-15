-- forms.inc
-
-- $Id$
--

include "date.inc"

import site SendForm = "orc.lib.orchard.forms.FormSenderSite"
import class Form = "orc.lib.orchard.forms.Form"
import class Textbox = "orc.lib.orchard.forms.Textbox"
import class Textarea = "orc.lib.orchard.forms.Textarea"
import class Checkbox = "orc.lib.orchard.forms.Checkbox"
import class Button = "orc.lib.orchard.forms.Button"
import class IntegerField = "orc.lib.orchard.forms.IntegerField"
import class UploadField = "orc.lib.orchard.forms.UploadField"
import class FormInstructions = "orc.lib.orchard.forms.FormInstructions"
import class PasswordField = "orc.lib.orchard.forms.PasswordField"
import class Mandatory = "orc.lib.orchard.forms.Mandatory"
import class FieldGroup = "orc.lib.orchard.forms.FieldGroup"
import class DateTimeRangesField = "orc.lib.orchard.forms.DateTimeRangesField"
import class DateField = "orc.lib.orchard.forms.DateField"

-- returns the form data
def WebPrompt(title, fields) =
  Form() >form>
  FieldGroup("main", title) >group>
  map(group.addPart, fields) >>
  form.addPart(group) >>
  SendForm(form) >receiver>
  Browse(receiver.getURL()) >>
  receiver.get() >>
  form.getValue().get("main")

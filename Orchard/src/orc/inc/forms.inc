site SendForm = orc.lib.orchard.forms.FormSenderSite
class Form = orc.lib.orchard.forms.Form
class Textbox = orc.lib.orchard.forms.Textbox
class Checkbox = orc.lib.orchard.forms.Checkbox
class Button = orc.lib.orchard.forms.Button
class IntegerField = orc.lib.orchard.forms.IntegerField
class FieldGroup = orc.lib.orchard.forms.FieldGroup
class DateRangesField = orc.lib.orchard.forms.DateRangesField
class DateRange = orc.lib.orchard.calendar.DateRange

def WebPrompt(title, fields) =
  Form() >form>
  FieldGroup("main", title) >group>
  ForkJoin(map(
    lambda (field)() = group.addPart(field),
    fields)) >>
  form.addPart(group) >>
  SendForm(form) >receiver>
  Redirect(receiver.getURL()) >>
  receiver.get() >>
  form.getValue().get("main")
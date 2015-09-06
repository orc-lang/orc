{-
This program will send an email to you
and then print the body of your response.
-}
include "mail.inc"

Prompt("Your email address:") >to>
MailPrompt(to, "Orc Query", "Please reply.") >response>
"Your response: " + response

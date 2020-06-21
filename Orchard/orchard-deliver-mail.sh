#!/bin/sh

# This program should be placed somewhere like /usr/local/bin
# And then add the following to your /etc/aliases:
# orchard: |/usr/local/bin/orchard-deliver-mail.sh
FILE=$(tempfile)
cat > "$FILE"
wget -q --post-file="$FILE" 'http://orc.csres.utexas.edu/orchard/MailListenerServlet?'"$RECIPIENT"
rm "$FILE"

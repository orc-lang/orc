
{- Support for instant messaging using Jabber -}

val (orchardUID, orchardPass) = ("orchardserver", "ckyogack")

site XMPPConnection = orc.lib.xmpp.XMPPConnection

val chatConnection = 
  XMPPConnection("talk.google.com", 5222, "gmail.com") >c>
  c.connect() >>
  c.login(orchardUID, orchardPass) >> 
  c
 


def orchardChatQuery(recipient, query) =
  val chat = chatConnection.chat(recipient)
  chat.send(query) >> print(query,"??") >> chat.receive()


def Ben(q) = orchardChatQuery("bendelaware@gmail.com", q)
def Adrian(q) = orchardChatQuery("adrianquark@gmail.com", q)
def David(q) = orchardChatQuery("dkitchin@gmail.com", q)


def Email(address, payload) = ("email", address, payload)

val william = "wcook@cs.utexas.edu"
val davidphone = "5127366972@messaging.sprintpcs.com"

-- Generate a random number in the range [i,j] inclusive
def Random(i,j) = random(j - i) + i + 1


{-
def David(s) = Rtimer(random(5) * 500) >> "breakfast tacos"
def Adrian(s) = Rtimer(random(5) * 500) >> "polytetrafluoroethylene"
-}

-- net.inc
--
-- $Id$
--

import site localhost = "orc.lib.net.Localhost"
import site Webservice = "orc.lib.net.Webservice"
import site Ping = "orc.lib.net.Ping"
import site GoogleSearchFactory = "orc.lib.net.GoogleSearchFactory"
import site GoogleSpellUnofficial = "orc.lib.net.GoogleSpellUnofficial"
import site XMPPConnection = "orc.lib.net.XMPPConnection"
import site TrueRandom = "orc.lib.net.TrueRandom"
import site KeyedHTTP = "orc.lib.net.KeyedHTTP"
import site BingSearchFactoryPropertyFile = "orc.lib.net.BingSearchFactoryPropertyFile"
import site BingSearchFactoryUsernameKey = "orc.lib.net.BingSearchFactoryUsernameKey"

def BingWebSearchFactoryConverter(s :: lambda(String) :: List[Bot]) :: lambda(String) :: List[(String, String)] = 
	lambda(q :: String) = map(lambda (e :: Bot) = (e.Url :!: String, e.Title :!: String), s(q).d.results :!: List[Bot])
def BingWebSearchFactoryPropertyFile(file :: String) =
	  BingWebSearchFactoryConverter(BingSearchFactoryPropertyFile(file, "Web"))
def BingWebSearchFactoryUsernameKey(user :: String, key :: String) =
	  BingWebSearchFactoryConverter(BingSearchFactoryUsernameKey(user, key, "Web"))

def BingSpellFactoryConverter(s :: lambda(String) :: List[Bot]) :: lambda(String) :: List[String] = 
	lambda(q :: String) = map(lambda (e :: Bot) = e.Value :!: String, s(q).d.results :!: List[Bot])
def BingSpellFactoryPropertyFile(file :: String) =
	  BingSpellFactoryConverter(BingSearchFactoryPropertyFile(file, "SpellingSuggestions"))
def BingSpellFactoryUsernameKey(user :: String, key :: String) =
	  BingSpellFactoryConverter(BingSearchFactoryUsernameKey(user, key, "SpellingSuggestions"))

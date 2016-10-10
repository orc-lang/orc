-- net.inc
--
-- $Id$
--

import site localhost = "orc.lib.net.Localhost"
import site Webservice = "orc.lib.net.Webservice"
import site Ping = "orc.lib.net.Ping"
import site GoogleSearchFactory = "orc.lib.net.GoogleSearchFactory"
import site XMPPConnection = "orc.lib.net.XMPPConnection"
import site TrueRandom = "orc.lib.net.TrueRandom"
import site KeyedHTTP = "orc.lib.net.KeyedHTTP"

import site WrapTwitterStream = "orc.lib.net.WrapTwitterStream"
import site TwitterFactoryPropertyFile = "orc.lib.net.TwitterFactoryPropertyFile"
import site TwitterFactoryKeySecret = "orc.lib.net.TwitterFactoryKeySecret"
import site TwitterStreamFactoryPropertyFile = "orc.lib.net.TwitterStreamFactoryPropertyFile"
import site TwitterStreamFactoryKeySecret = "orc.lib.net.TwitterStreamFactoryKeySecret"
import class TwitterQuery = "twitter4j.Query"
import class TwitterFilterQuery = "twitter4j.FilterQuery"
import class TwitterAccessToken = "twitter4j.auth.AccessToken"

import site MicrosoftTranslatorFactoryPropertyFile = "orc.lib.net.MicrosoftTranslatorFactoryPropertyFile"
import site MicrosoftTranslatorFactoryUsernameKey = "orc.lib.net.MicrosoftTranslatorFactoryUsernameKey"

import site BingSearchFactoryPropertyFile = "orc.lib.net.BingSearchFactoryPropertyFile"
import site BingSearchFactoryUsernameKey = "orc.lib.net.BingSearchFactoryUsernameKey"

def BingWebSearchFactoryConverter(s :: lambda(String) :: List[Bot]) :: lambda(String) :: List[(String, String)] =
  { map({ val e = _ # (e.Url :!: String, e.Title :!: String) }, s(_).d.results :!: List[Bot]) }
def BingWebSearchFactoryPropertyFile(file :: String) =
  BingWebSearchFactoryConverter(BingSearchFactoryPropertyFile(file, "Web"))
def BingWebSearchFactoryUsernameKey(user :: String, key :: String) =
  BingWebSearchFactoryConverter(BingSearchFactoryUsernameKey(user, key, "Web"))

def BingSpellFactoryConverter(s :: lambda(String) :: List[Bot]) :: lambda(String) :: List[String] =
  { map( { _.Value :!: String }, s(_).d.results :!: List[Bot]) }
def BingSpellFactoryPropertyFile(file :: String) =
  BingSpellFactoryConverter(BingSearchFactoryPropertyFile(file, "SpellingSuggestions"))
def BingSpellFactoryUsernameKey(user :: String, key :: String) =
  BingSpellFactoryConverter(BingSearchFactoryUsernameKey(user, key, "SpellingSuggestions"))

def GoogleGeocoder(address :: String) = 
  val response = ReadJSON(HTTP("http://maps.googleapis.com/maps/api/geocode/json", {.address = address, sensor = false .}).get())
  if response.status = "OK"
  then
    response.results.head().formatted_address >returnedAddress> 
    response.results.head().geometry.location >{. lat = lattitude, lng = longitude .}> 
    (returnedAddress, lattitude, longitude)
  else
    Error("Google Maps Geocoding API failure: "+response.status)

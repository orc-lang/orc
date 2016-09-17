package orc.servlettests

import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class ChatServerGatling extends Simulation {

	val httpProtocol = http
		.baseURL("http://localhost:8080")
		.inferHtmlResources()
		.acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
		.acceptEncodingHeader("gzip, deflate, sdch")
		.acceptLanguageHeader("en-US,en;q=0.8")
		.doNotTrackHeader("1")
		.userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/51.0.2704.79 Chrome/51.0.2704.79 Safari/537.36")

	val headers_0 = Map(
		"Pragma" -> "no-cache",
		"Upgrade-Insecure-Requests" -> "1")

	val headers_1 = Map(
		"Accept" -> "*/*",
		"Pragma" -> "no-cache")

	val headers_2 = Map("Upgrade-Insecure-Requests" -> "1")

  def randomString(n: Int) = scala.util.Random.alphanumeric.take(n).mkString
  
  val roomNames = Seq("RickRoll", "LOL", "ChuckNorris", "DancyBaby", "Fail", "Icanhascheezburger", "LeeroyJenkins", "NumaNuma", "KeyboardCat", "PeanutButterJelly", "Jibjab")
  
  val msgs = Iterator.continually(
    Map("msg1" -> randomString(5),
        "msg2" -> randomString(20),
        "msg3" -> randomString(30),
        "msg4" -> randomString(17),
        "msg5" -> randomString(50),
        "roomname" -> roomNames(scala.util.Random.nextInt(roomNames.size))
        )
  )

	val scn = scenario("RecordedSimulation")
	  .feed(msgs)
		.exec(http("request_0")
			.get("/chat/test")
			.headers(headers_0))
		.pause(2)
		.exec(http("request_2")
			.get("/chat/test")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_3")
			.get("/chat/test")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_4")
			.get("/chat/test?msg=${msg1}")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_5")
			.get("/chat/test?msg=${msg2}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_6")
			.get("/chat/test")
			.headers(headers_2))
		.pause(1)
		.exec(http("request_7")
			.get("/chat/other/${roomname}?msg=test")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_8")
			.get("/chat/other/${roomname}?msg=${msg3}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_9")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_10")
			.get("/chat/other/${roomname}?msg=${msg4}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_11")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_12")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_13")
			.get("/chat/other/${roomname}?msg=${msg5}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_14")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(1)
		.exec(http("request_15")
			.get("/chat/test")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_16")
			.get("/chat/test?msg=${msg1}")
			.headers(headers_2))
		.pause(2)
		.exec(http("request_17")
			.get("/chat/test?msg=${msg3}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_18")
			.get("/chat/test")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_19")
			.get("/chat/test")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_20")
			.get("/chat/test")
			.headers(headers_2))
		.pause(1)
		.exec(http("request_21")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_22")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_23")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_24")
			.get("/chat/other/${roomname}")
			.headers(headers_2))
		.pause(3)
		.exec(http("request_25")
			.get("/chat/other/${roomname}")
			.headers(headers_2))

	normalPausesWithStdDevDuration(2)
			
	setUp(scn.inject(
	    atOnceUsers(20),
	    constantUsersPerSec(20) during(60 seconds)
	  )).protocols(httpProtocol)
}
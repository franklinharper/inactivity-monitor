package com.example.demo

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title


// TODO move the Transition data class, and the associate enums
//  into a module for sharing code between backend and frontend.

enum class ActivityType {
  IN_VEHICLE,
  ON_BICYCLE,
  ON_FOOT, // The device is on a user who is walking or running.
  STILL,
  WALKING, // The device is on a user who is walking.
  RUNNING, // The device is on a user who is running.
  OTHER,
  // The SENTINEL value is used to indicate the time at which the end of an Activity occurred.
  SENTINEL;
}

enum class TransitionType {
  ENTER, EXIT;
}

data class Transition(
  val activityType: ActivityType,
  val transitionType: TransitionType,
  val time: Long
)

// Entry Point of the application as defined in resources/application.conf.
// @see https://ktor.io/servers/configuration.html#hocon-file
fun Application.main() {
  install(DefaultHeaders)
  install(CallLogging)
  install(ContentNegotiation) {
    jackson {
    }
  }

  routing {
//    Next steps:
//    1) create a POST route for adding Transitions
//    2) decode parameters into a kotlin data class
//    3) store Transition in a DB (Postgres SQL + JDBI?)
//    4) Connect to backend in client app
//    5) On client upload Transitions from local DB to the backend
    post("/transition") {
      val transition = call.receive<Transition>()
      call.respondHtml {
        head { title { +"post transition"} }
        body { p { +"posted: $transition"} }
      }
    }
    get("/transition") {
      call.respondHtml {
        head {
          title { +"transition!" }
        }
        body {
          p {
            +"second transition"
          }
        }
      }
    }
    get("/") {
      call.respondHtml {
        head {
          title { +"Ktor on Google App Engine standard environment" }
        }
        body {
          p {
            +"Hello there! This is Ktor running on App Engine standard environment"
          }
        }
      }
    }
    get("/demo") {
      call.respondHtml {
        head {
          title { +"Ktor on App Engine standard environment" }
        }
        body {
          p {
            +"It's another route!"
          }
        }
      }
    }

  }
}


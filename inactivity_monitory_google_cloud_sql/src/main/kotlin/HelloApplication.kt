package com.example.demo

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.html.respondHtml
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.p
import kotlinx.html.title

// Entry Point of the application as defined in resources/application.conf.
// @see https://ktor.io/servers/configuration.html#hocon-file
fun Application.main() {
  // This adds Date and Server headers to each response, and allows custom additional headers
  install(DefaultHeaders)
  // This uses use the logger to log every call (request/response)
  install(CallLogging)

  // Registers routes
  routing {
    get()
    // Here we use a DSL for building HTML on the route "/"
    // @see https://github.com/Kotlin/kotlinx.html
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


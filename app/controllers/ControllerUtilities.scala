package controllers

import play.api.mvc._

trait ControllerUtilities { this: Controller =>
  protected val backendIsDownResponse = ServiceUnavailable(
    """ |The connection to the data aggregation application seems
        |to be down.  Please check the `backend.url` configuration
        |setting.
    """.stripMargin
  )

}

package views.helper

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}

package object transform {
  implicit class DateTimeTransformations(dt: DateTime) {
    def prettify(implicit formatter: DateTimeFormatter = defaultFormatter): String = {
      formatter.print(dt)
    }

    private val defaultFormatter = DateTimeFormat.mediumDateTime()
  }
}

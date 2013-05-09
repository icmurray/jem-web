package domain

import org.joda.time.DateTime

case class Recording(
    id: String,
    status: String,
    startTime: DateTime,
    endTime: Option[DateTime],
    configuredGateways: List[ConfiguredGateway])

case class RecordedRunConfiguration(
  selections: List[ConfiguredGateway]
)

case class ConfiguredGateway(
  host: String,
  port: Int,
  devices: List[ConfiguredDevice])

case class ConfiguredDevice(
    unit: Int,
    table1: Boolean = true,
    table2: Boolean = true,
    table3: Boolean = false,
    table4: Boolean = false,
    table5: Boolean = false,
    table6: Boolean = false) {

  val tableIds = List(
    table1, table2, table3, table4, table5, table6
  ).zipWithIndex.filter(_._1).map(_._2+1)

  // TODO: this should take device type into account
  def registers(tableId: Int): List[Register] = tableId match {
    case 1 => (50512 to 50572 by 2).map(Register(_, 0, 100)).toList
    case 2 => (50768 to 50817 by 2).map(Register(_, 0, 100)).toList
    case 3 => (51024 to 51086 by 2).map(Register(_, 0, 100)).toList
    case 4 => (51280 to 51313).map(Register(_, 0, 100)).toList
    case 5 => (51456 to 51463).map(Register(_, 0, 100)).toList
    case 6 => (51536 to 51858).map(Register(_, 0, 100)).toList
    case _ => List()
  }
}


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
    case 1 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case 2 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case 3 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case 4 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case 5 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case 6 => (15026 to 15036).map{i: Int => Register(i.asInstanceOf[Short], 0, 100)}.toList
    case _ => List()
  }
}


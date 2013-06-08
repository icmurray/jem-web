package domain

import org.joda.time.DateTime

case class Recording(
    id: String,
    status: String,
    startTime: DateTime,
    endTime: Option[DateTime],
    gateways: List[Gateway])

case class RecordedRunConfiguration(
  selections: List[ConfiguredGateway]
)

case class ConfiguredGateway(
  host: String,
  port: Int,
  devices: List[ConfiguredDevice],
  label: Option[String] = None)

case class ConfiguredDevice(
    unit: Int,
    table1: Boolean,
    table2: Boolean,
    table3: Boolean,
    table4: Boolean,
    table5: Boolean,
    table6: Boolean,
    label: Option[String] = None)

case class ConfiguredRegister(
    address: Int,
    minValue: Int,
    maxValue: Int,
    customLabel: Option[String] = None) {

  val label = customLabel getOrElse s"Register ${address}"

}

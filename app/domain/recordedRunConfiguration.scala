package domain

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
  table6: Boolean = false)


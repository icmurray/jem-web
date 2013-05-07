package domain

case class RecordedRunConfiguration(
  selections: List[ConfiguredDevice]
)

case class ConfiguredDevice(
  device: Device,
  table1: Boolean = true,
  table2: Boolean = true,
  table3: Boolean = true,
  table4: Boolean = true,
  table5: Boolean = true,
  table6: Boolean = true)


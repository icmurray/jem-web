package domain

case class Register(
    address: Int,
    label: Option[String],
    minValue: Int,
    maxValue: Option[Int],
    unitOfMeasurement: Option[String]) {

  val friendlyName = label getOrElse label.toString
}

case class Table(
    id: Int,
    label: Option[String],
    registers: List[Register]) {

  val friendlyName = label getOrElse s"Table ${id}"
}

case class Device(
    unit: Int,
    makeModel: String,
    label: Option[String],
    tables: List[Table]) {

  val friendlyName = label getOrElse s"Unit ${unit}"
}

case class DeviceConfig(
    unit: Int,
    makeModel: String,
    label: Option[String],
    maxCurrent: Int,
    maxVoltage: Int,
    maxPower: Int)

case class Gateway(
    host: String,
    port: Int,
    label: Option[String],
    devices: List[Device]) {

  val friendlyName = label getOrElse s"${host}:${port}"
}

case class GatewayConfig(
    host: String,
    port: Int,
    label: Option[String],
    devices: List[DeviceConfig])

case class SystemConfig(
    gateways: List[GatewayConfig])

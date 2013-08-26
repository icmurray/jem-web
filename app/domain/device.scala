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
    maxPower: Int) {

  import DeviceConfig._

  def updateDevice(device: Device): Device = {
    Device(
      unit = unit,
      makeModel = makeModel,
      label = label,
      tables = device.tables.map(updateTable))
  }

  private def updateTable(t: Table) = {
    Table(
      id = t.id,
      label = t.label,
      registers = t.registers.map(updateRegister))
  }

  private def updateRegister(r: Register) = {

    val (minValue, maxValue) = {
      if (voltageAddresses contains r.address) {
        (0, Some(maxVoltage))
      } else if (currentAddresses contains r.address) {
        (0, Some(maxCurrent))
      } else if (powerAddresses contains r.address) {
        (-maxPower, Some(maxPower))
      } else {
        (r.minValue, r.maxValue)
      }
    }

    Register(r.address, r.label, minValue, maxValue, r.unitOfMeasurement)
  }

}

object DeviceConfig {
  def apply(device: Device): DeviceConfig = {
    DeviceConfig(
      unit = device.unit,
      makeModel = device.makeModel,
      label = device.label,
      maxVoltage = guessVoltage(device),
      maxCurrent = guessCurrent(device),
      maxPower = guessPower(device))
  }

  private val voltageAddresses = (0xC552 to 0xC55C by 2).toSet
  private val currentAddresses = (0xC560 to 0xC566 by 2).toSet
  private val powerAddresses: Set[Int] = {
    (0xC568 to 0xC56C by 2).toSet ++
    (0xC570 to 0xC580 by 2).toSet
  }

  private def getRegisterValue(address: Int)(device: Device): Option[Int] = {
    for {
      table <- device.tables.filter(_.id == 1).headOption
      register <- table.registers.filter(_.address == address).headOption
      maxValue <- register.maxValue
    } yield maxValue
  }

  private def guessVoltage(device: Device): Int = {
    getRegisterValue(voltageAddresses.head)(device).getOrElse(50000)
  }

  private def guessCurrent(device: Device): Int = {
    getRegisterValue(currentAddresses.head)(device).getOrElse(1000)
  }

  private def guessPower(device: Device): Int = {
    getRegisterValue(powerAddresses.head)(device).getOrElse(100)
  }
}

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
    devices: List[DeviceConfig]) {

  def updateGateway(gateway: Gateway): Gateway = {
    val newDevices = devices.zip(gateway.devices)
                            .map { case (deviceConfig, device) =>
      deviceConfig.updateDevice(device)
    }
    Gateway(
      host = host,
      port = port,
      label = label,
      devices = newDevices)
  }

}

object GatewayConfig {
  def apply(gateway: Gateway): GatewayConfig = {
    GatewayConfig(
      gateway.host,
      gateway.port,
      gateway.label,
      gateway.devices.map(DeviceConfig.apply))
  }
}

case class SystemConfig(
    gateways: List[GatewayConfig])

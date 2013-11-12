package domain

case class Register(
    address: Int,
    label: Option[String],
    minValue: Int,
    maxValue: Option[Int],
    unitOfMeasurement: Option[String]) {

  val friendlyName = label getOrElse label.toString

  def scale = Register.scales.get(address).getOrElse(1.0)
  def scaledUnitOfMeasurement = {
    Register.scaledUnitOfMeasurement.get(address)
            .map(Some(_))
            .getOrElse(unitOfMeasurement)
  }
}

object Register {
  val scales = Map(
    // Voltages
    (0xC558 -> 1/100.0), (0xC552 -> 1/100.0), (0xC55A -> 1/100.0),
    (0xC554 -> 1/100.0), (0xC55C -> 1/100.0), (0xC556 -> 1/100.0),

    // Current
    (0xC560 -> 1/1000.0), (0xC562 -> 1/1000.0), (0xC564 -> 1/1000.0), (0xC566 -> 1/1000.0),

    // Power
    (0xC570 -> 1/100.0), (0xC57C -> 1/100.0), (0xC576 -> 1/100.0),
    (0xC572 -> 1/100.0), (0xC57E -> 1/100.0), (0xC578 -> 1/100.0),
    (0xC574 -> 1/100.0), (0xC580 -> 1/100.0), (0xC57A -> 1/100.0),
    (0xC568 -> 1/100.0), (0xC56A -> 1/100.0), (0xC56C -> 1/100.0),

    // Power factors
    (0xC582 -> 1/1000.0), (0xC584 -> 1/1000.0), (0xC586 -> 1/1000.0), (0xC56E -> 1/1000.0),
  
    // Frequency
    (0xC55E -> 1/100.0)
  )


  def scaledUnitOfMeasurement = Map(
    // Voltages
    (0xC558 -> "V"), (0xC552 -> "V"), (0xC55A -> "V"),
    (0xC554 -> "V"), (0xC55C -> "V"), (0xC556 -> "V"),

    // Current
    (0xC560 -> "A"), (0xC562 -> "A"), (0xC564 -> "A"), (0xC566 -> "A"),

    // Power
    (0xC570 -> "kW"), (0xC57C -> "kW"), (0xC576 -> "kW"),
    (0xC572 -> "kVA"), (0xC57E -> "kVA"), (0xC578 -> "kVA"),
    (0xC574 -> "kVAr"), (0xC580 -> "kVAr"), (0xC57A -> "kVAr"),
    (0xC568 -> "kW"), (0xC56A -> "kVA"), (0xC56C -> "kVAr"),

    // Power factors
    (0xC582 -> ""), (0xC584 -> ""), (0xC586 -> ""), (0xC56E -> ""),
  
    // Frequency
    (0xC55E -> "Hz")
  )
}



//0xC558, 0xC552, 0xC560, 0xC570, 0xC57C, 0xC576, 0xC582
//0xC55A, 0xC554, 0xC562, 0xC572, 0xC57E, 0xC578, 0xC584
//0xC55C, 0xC556, 0xC564, 0xC574, 0xC580, 0xC57A, 0xC586

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

  lazy val registers: Map[Int, Register] = {
    (for {
      table <- tables
      register <- table.registers
    } yield(register.address, register)).toMap
  }

  def register(address: Int): Option[Register] = registers.get(address)
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
      } else if (apparentPowerAddresses contains r.address) {
        (0, Some(maxPower))
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
  private val apparentPowerAddresses = Set(0xC56C, 0xC57C, 0xC57E, 0xC580)
  private val powerAddresses: Set[Int] = {
    (0xC568 to 0xC56C by 2).toSet ++
    (0xC570 to 0xC580 by 2).toSet
  }
  private val nonApparentPowerAddresses = powerAddresses -- apparentPowerAddresses

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

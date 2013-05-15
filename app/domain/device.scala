package domain

case class Register(
    address: Int,
    minValue: Int,
    maxValue: Int)

case class Device(
    unit: Int,
    makeModel: String,
    label: Option[String])

case class Gateway(
    host: String,
    port: Int,
    label: Option[String],
    devices: List[Device])

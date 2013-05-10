package domain

case class Register(
    address: Int,
    minValue: Int,
    maxValue: Int)

case class Device(unit: Int)

case class Gateway(
    host: String,
    port: Int,
    devices: List[Device])

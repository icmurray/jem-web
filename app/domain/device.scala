package domain

case class Device(unit: Int)
case class Gateway(
    host: String,
    port: Int,
    devices: List[Device])

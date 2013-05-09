package domain

case class Register(
    address: Int,
    minValue: Int,
    maxValue: Int) {

  val name: String = s"Register ${address}"
}

case class Device(unit: Int)

case class Gateway(
    host: String,
    port: Int,
    devices: List[Device])

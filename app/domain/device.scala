package domain

case class Register(
    address: Int,
    label: Option[String],
    minValue: Int,
    maxValue: Int)

case class Table(
    id: Int,
    label: Option[String],
    registers: List[Register])

case class Device(
    unit: Int,
    makeModel: String,
    label: Option[String],
    tables: List[Table])

case class Gateway(
    host: String,
    port: Int,
    label: Option[String],
    devices: List[Device])

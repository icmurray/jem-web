package domain

case class Gateway(host: String, port: Int)
case class Device(unit: Int, gateway: Gateway)

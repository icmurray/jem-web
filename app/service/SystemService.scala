package service

import domain.SystemStatus

trait SystemService {
  def status: SystemStatus
  def start(): Unit
  def stop(): Unit
}

object SystemService extends SystemService {
  private var running = false

  def status = SystemStatus(running)
  def start { running = true }
  def stop { running = false }
}

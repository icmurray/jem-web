package domain

case class SystemStatus(
    running: Boolean,
    activeRecordingIds: List[String]
)

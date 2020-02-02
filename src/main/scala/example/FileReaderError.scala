package example

sealed trait FileReaderError extends Throwable
object FileReaderError {
  case object CannotReadFile extends FileReaderError
  case object CannotParseJson extends FileReaderError
}

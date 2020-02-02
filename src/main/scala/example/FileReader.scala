package example

import java.nio.file.{Files, Path, Paths}

import cats.effect.IO

import scala.util.control.NonFatal
import io.circe.parser.parse
import io.circe.generic.semiauto._
import cats.implicits._
import io.circe.Decoder

final case class Article(name: String, text: String)

object FileReader {
  implicit val decodeArticle: Decoder[Article] = deriveDecoder[Article]

  def readArticlesEither(
      fileName: Path): Either[FileReaderError, List[Article]] = {
    val eitherFileContent = (try {
      Right(new String(Files.readAllBytes(fileName)))
    } catch {
      case NonFatal(_) => Left(FileReaderError.CannotReadFile)
    })
    for {
      fileContent <- eitherFileContent
      jsonLine <- fileContent
        .split("\n")
        .toList
        .traverse(line =>
          parse(line).leftMap(_ => FileReaderError.CannotParseJson))
    } yield jsonLine.mapFilter(_.as[Article].toOption)
  }

  def readArticles(fileName: Path): IO[List[Article]] =
    for {
      fileContent <- IO(new String(Files.readAllBytes(fileName)))
        .handleErrorWith(_ => IO.raiseError(FileReaderError.CannotReadFile))
      jsonLine <- fileContent
        .split("\n")
        .toList
        .traverse(line =>
          IO.fromEither(parse(line).leftMap(_ =>
            FileReaderError.CannotParseJson)))
    } yield jsonLine.mapFilter(_.as[Article].toOption)
}

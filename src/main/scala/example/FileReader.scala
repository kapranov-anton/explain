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
  def readArticles(fileName: Path): IO[String] =
    IO(new String(Files.readAllBytes(fileName)))
      .handleErrorWith(_ => IO.raiseError(FileReaderError.CannotReadFile))
  def parseJsonLines(
      fileContent: String): Either[FileReaderError, List[Article]] =
    fileContent
      .split("\n")
      .toList
      .traverse(line =>
        parse(line).leftMap(_ => FileReaderError.CannotParseJson))
      .map(_.mapFilter(_.as[Article].toOption))
  def readAndParse(fileName: Path): IO[List[Article]] =
    readArticles(fileName).flatMap(content =>
      IO.fromEither(parseJsonLines(content)))
}

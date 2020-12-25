package name.nikiforo

import scala.io.Source
import cats.effect.IOApp
import cats.effect.{ExitCode, IO}
import scodec.Decoder
import scodec.bits.ByteVector
import scodec.Encoder
import cats.syntax.monoid._
import cats.kernel.Monoid
import scodec.bits.BitVector
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object DeHexer {

  private implicit val bitsMonoid =
    new Monoid[BitVector] {
      def empty: BitVector = BitVector.empty
      def combine(x: BitVector, y: BitVector): BitVector = x ++ y
    }

  def main(args: Array[String]): Unit = {
    val string = ByteVector.fromHex(getMsg("alex4.hex")).get
    val msgs = Decoder.decodeCollect(Message.parser3, None)(string.bits).require.value
    val hidden = Monoid.combineAll(msgs.toList.map(msg => Message.parser3.encode(msg).require))
    Files.write(Paths.get("nulled.hex"), hidden.toHex.getBytes(StandardCharsets.UTF_8))
  }

  private def getMsg(filename: String): String = {
    val source = Source.fromURL(getClass.getResource(s"/$filename"))
    val lines = source.getLines().toList
    source.close()
    lines match {
      case List(line) => line
      case _ => ???
    }
  }
}

package name.nikiforo

import scodec.Codec
import scodec.codecs._

object Common {

  private val uint7 = uint(7)
  private val uint15 = uint(15)
  private val uint31 = uint(31)
  val ignore16 = ignore(16)

  val extensible1byte: Codec[(Boolean, Int)] = bool.flatZip(highBit => if (highBit) uint15 else uint7)

  val extensible2bytes: Codec[(Boolean, Int)] = bool.flatZip(highBit => if (highBit) uint31 else uint15)
}

package name.nikiforo

import scodec.Decoder
import scodec.codecs._
import scodec.Attempt
import scodec.bits._
import scodec.Attempt.Failure
import scodec.Err
import scodec.Codec

object Message {

  private val Head = constant(hex"2424")

  val parser3: scodec.Codec[(Unit, ((Boolean, Int), (Unit, Option[((Boolean, Int), Unit)])))] =
    Head ~
      Common.extensible1byte.flatZip { case(_, tpe) =>
        Common.ignore16 ~
          conditional(tpe != 2, Common.extensible2bytes.flatZip(a1 => ignore((a1._2+2)*8) ))
      }
}

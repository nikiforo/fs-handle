package name.nikiforo

import scodec.bits.ByteVector

sealed trait Messages
case object MessageStub extends Messages

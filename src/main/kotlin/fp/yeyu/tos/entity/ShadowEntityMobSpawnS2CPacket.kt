package fp.yeyu.tos.entity

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket
import java.io.IOException
import java.util.*
import kotlin.jvm.Throws

class ShadowEntityMobSpawnS2CPacket(shadowEntity: ShadowEntity?, var copyingUuid: UUID) : MobSpawnS2CPacket(shadowEntity) {

    @Throws(IOException::class)
    override fun read(buf: PacketByteBuf) {
        super.read(buf)
        copyingUuid = buf.readUuid()
    }

    @Throws(IOException::class)
    override fun write(buf: PacketByteBuf) {
        super.write(buf)
        buf.writeUuid(copyingUuid)
    }

}
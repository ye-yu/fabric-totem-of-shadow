package fp.yeyu.tos.entity

import fp.yeyu.tos.TotemOfShadowEntry
import fp.yeyu.tos.mixinutil.EntityPacketListener
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.PacketConsumer
import net.fabricmc.fabric.api.network.PacketContext
import net.fabricmc.fabric.impl.networking.ServerSidePacketRegistryImpl
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier

object SpiritEntityHeadingToS2CPacket : PacketConsumer {

    private const val packetName = "qewvmir"
    val identifier = Identifier(TotemOfShadowEntry.NAMESPACE, packetName)

    fun makePacket(entity: Entity, x: Double, y: Double, z: Double): Packet<*> {
        val packetByteBuf = PacketByteBuf(Unpooled.buffer())
        packetByteBuf.writeVarInt(entity.entityId)
        packetByteBuf.writeVarLong(x.toBits())
        packetByteBuf.writeVarLong(y.toBits())
        packetByteBuf.writeVarLong(z.toBits())
        return ServerSidePacketRegistryImpl.INSTANCE.toPacket(identifier, packetByteBuf)
    }

    override fun accept(context: PacketContext, buffer: PacketByteBuf) {
        val entityId = buffer.readVarInt()
        val x = Double.fromBits(buffer.readVarLong())
        val y = Double.fromBits(buffer.readVarLong())
        val z = Double.fromBits(buffer.readVarLong())
        val entity = MinecraftClient.getInstance().world?.getEntityById(entityId) as EntityPacketListener? ?: return
        entity.onHeadingToPacket(x, y, z)
    }
}
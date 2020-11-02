package fp.yeyu.tos;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;

import java.io.IOException;
import java.util.UUID;

public class ShadowEntityMobSpawnS2CPacket extends MobSpawnS2CPacket {

    private UUID playerUuid;

    public ShadowEntityMobSpawnS2CPacket(ShadowEntity shadowEntity, UUID uuid) {
        super(shadowEntity);
        playerUuid = uuid;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        super.read(buf);
        this.playerUuid = buf.readUuid();
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        super.write(buf);
        buf.writeUuid(playerUuid);
    }

    public UUID getCopyingUuid() {
        return playerUuid;
    }
}

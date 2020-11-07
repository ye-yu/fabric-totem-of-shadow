package fp.yeyu.tos.mixinutil;

public interface EntityPacketListener {
    void onLookAtPacket(double x, double y, double z);

    void onHeadingToPacket(double x, double y, double z);
}

package fp.yeyu.tos;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ShadowEntity extends LivingEntity {

    private ItemStack main;
    public final DefaultedList<ItemStack> armor;
    private ItemStack offHand;
    protected PlayerListEntry copyingClientEntry;
    protected static final TrackedData<Optional<UUID>> COPYING_UUID = DataTracker.registerData(ShadowEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    protected PlayerEntity copyingEntity;
    private static final int UPDATE_MOB_CALLING_TICK_DEF = 30;
    private int updateMobCallingTick = 5;

    public ShadowEntity(EntityType<? extends ShadowEntity> shadowEntityEntityType, World world) {
        super(shadowEntityEntityType, world);
        this.main = ItemStack.EMPTY;
        this.armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
        this.offHand = ItemStack.EMPTY;
        this.copyingClientEntry = null;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(COPYING_UUID, Optional.empty());
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return this.armor;
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:
            case LEGS:
            case CHEST:
            case FEET:
                return armor.get(slot.ordinal() - EquipmentSlot.FEET.ordinal());
            case OFFHAND:
                return offHand;
            case MAINHAND:
                return main;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        switch (slot) {
            case HEAD:
            case LEGS:
            case CHEST:
            case FEET:
                armor.set(slot.ordinal() - EquipmentSlot.FEET.ordinal(), stack);
            case OFFHAND: {
                offHand = stack;
                break;
            }
            case MAINHAND:
                main = stack;
        }
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    public void setCopyingUUID(UUID uuid) {
        dataTracker.set(COPYING_UUID, Optional.of(uuid));
    }

    @Environment(EnvType.CLIENT)
    public PlayerListEntry getPlayerListEntry() {
        if (copyingClientEntry == null && COPYING_UUID != null) {
            final MinecraftClient instance = MinecraftClient.getInstance();
            if (instance == null) return null;
            final ClientPlayNetworkHandler networkHandler = instance.getNetworkHandler();
            if (networkHandler == null) return null;
            final Optional<UUID> uuid = dataTracker.get(COPYING_UUID);
            copyingClientEntry = networkHandler.getPlayerListEntry(uuid.orElse(Optional.ofNullable(instance.player).map(Entity::getUuid).orElse(null)));
            if (copyingClientEntry == null) return null;
            copyingEntity = new OtherClientPlayerEntity((ClientWorld) world, copyingClientEntry.getProfile());
            this.setCustomName(copyingEntity.getDisplayName());
        }
        return copyingClientEntry;
    }

    @Environment(EnvType.CLIENT)
    public Identifier getSkinTexture() {
        final PlayerListEntry playerListEntry = getPlayerListEntry();
        return playerListEntry == null || !playerListEntry.hasSkinTexture() ? DefaultSkinHelper.getTexture() : playerListEntry.getSkinTexture();
    }

    @Environment(EnvType.CLIENT)
    public boolean isPartVisible(PlayerModelPart modelPart) {
        return copyingEntity == null || copyingEntity.isPartVisible(modelPart);
    }

    public Packet<?> createSpawnPacket(UUID uuid) {
        return new ShadowEntityMobSpawnS2CPacket(this, uuid);
    }

    public void sendSpawnPacket(UUID uuid) {
        if (world.isClient) return;
        ((ServerWorld) world).getServer().getPlayerManager().sendToAll(createSpawnPacket(uuid));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(world instanceof ServerWorld)) return;
        if (copyingEntity == null) return;
        updateMobCallingTick = --updateMobCallingTick % UPDATE_MOB_CALLING_TICK_DEF;
        if (updateMobCallingTick != 0) return;
        world.getEntitiesByClass(HostileEntity.class,
                new Box(getBlockPos()).expand(20),
                he -> Objects.equals(he.getTarget(), copyingEntity))
                .forEach(he -> he.setTarget(this));
    }

    @Override
    public void writeCustomDataToTag(CompoundTag tag) {
        super.writeCustomDataToTag(tag);
        tag.putUuid("copyingUuid", copyingEntity.getUuid());
        tag.putString("copyingName", copyingEntity.getEntityName());
    }

    @Override
    public void readCustomDataFromTag(CompoundTag tag) {
        super.readCustomDataFromTag(tag);
        if (tag.contains("copyingUuid")) {
            final UUID copyingUuid = tag.getUuid("copyingUuid");
            setCopyingUUID(copyingUuid);
            final Entity entity = ((ServerWorld) world).getEntity(copyingUuid);
            if (entity instanceof PlayerEntity) copyingEntity = (PlayerEntity) entity;
            world.sendEntityStatus(this, (byte) 80);
        }

        if (tag.contains("copyingName")) {
            final String copyingName = tag.getString("copyingName");
            setCustomName(copyingName);
        }
    }

    private void setCustomName(String copyingName) {
        setCustomName(new LiteralText(copyingName));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleStatus(byte status) {
        switch (status) {
            case 80:
                getPlayerListEntry();
                break;
            case 81:
                playSpawnParticles();
                break;
            default:
                super.handleStatus(status);
                break;
        }
    }

    private static final int[][] spread = new int[9][];

    static {
        spread[0] = new int[]{-1, 0, -1};
        spread[1] = new int[]{-1, 0, 0};
        spread[2] = new int[]{-1, 0, 1};
        spread[3] = new int[]{0, 0, -1};
        spread[4] = new int[]{0, 0, 0};
        spread[5] = new int[]{0, 0, 1};
        spread[6] = new int[]{1, 0, -1};
        spread[7] = new int[]{1, 0, 0};
        spread[8] = new int[]{1, 0, 1};
    }

    public void playSpawnParticles() {
        if (world.isClient) {
            for (int i = 0; i < 20; ++i) {
                for (int[] ints : spread) {
                    final BlockPos add = getBlockPos().add(ints[0], ints[1], ints[2]);
                    this.world.addParticle(ParticleTypes.POOF,
                            add.getX() + random.nextDouble(),
                            add.getY() + random.nextGaussian() * 0.02,
                            add.getZ() + random.nextDouble(),
                            random.nextGaussian() * 0.02, random.nextGaussian() * 0.35, random.nextGaussian() * 0.02
                    );
                    this.world.addParticle(ParticleTypes.POOF,
                            add.getX() + random.nextDouble(),
                            add.getY() + 1 + random.nextGaussian() * 0.02,
                            add.getZ() + random.nextDouble(),
                            random.nextGaussian() * 0.02, random.nextGaussian() * 0.35, random.nextGaussian() * 0.02
                    );
                }
            }
        } else world.sendEntityStatus(this, (byte) 81);
    }
}

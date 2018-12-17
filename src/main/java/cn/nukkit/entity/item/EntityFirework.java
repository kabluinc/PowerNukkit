package cn.nukkit.entity.item;

import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.entity.data.SlotEntityData;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemFirework;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacketV1;
import cn.nukkit.network.protocol.PlaySoundPacket;

import java.util.Random;

/**
 * @author CreeperFace
 */
public class EntityFirework extends Entity {

    public static final int NETWORK_ID = 72;

    private int lifetime;
    private Item firework;

    public EntityFirework(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public void initEntity() {
        super.initEntity();

        Random rand = new Random();
        this.lifetime = 30 + rand.nextInt(6) + rand.nextInt(7);

        this.motionX = rand.nextGaussian() * 0.001D;
        this.motionZ = rand.nextGaussian() * 0.001D;
        this.motionY = 0.05D;

        if (namedTag.contains("FireworkItem")) {
            firework = NBTIO.getItemHelper(namedTag.getCompound("FireworkItem"));
        } else {
            firework = new ItemFirework();
        }

        this.setDataProperty(new SlotEntityData(Entity.DATA_DISPLAY_ITEM, firework));
        this.setDataProperty(new IntEntityData(Entity.DATA_DISPLAY_OFFSET, 1));
        this.setDataProperty(new ByteEntityData(Entity.DATA_HAS_DISPLAY, 1));
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        }

        int tickDiff = currentTick - this.lastUpdate;

        if (tickDiff <= 0 && !this.justCreated) {
            return true;
        }

        this.lastUpdate = currentTick;

        this.timing.startTiming();


        boolean hasUpdate = this.entityBaseTick(tickDiff);

        if (this.isAlive()) {

            this.motionX *= 1.15D;
            this.motionZ *= 1.15D;
            this.motionY += 0.04D;
            this.move(this.motionX, this.motionY, this.motionZ);

            this.updateMovement();


            float f = (float) Math.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
            this.yaw = (float) (Math.atan2(this.motionX, this.motionZ) * (180D / Math.PI));

            this.pitch = (float) (Math.atan2(this.motionY, (double) f) * (180D / Math.PI));


            if (this.age == 0) {
                PlaySoundPacket pk = new PlaySoundPacket();
                pk.name = "firework.launch";
                pk.volume = 1;
                pk.pitch = 1;
                pk.x = getFloorX();
                pk.y = getFloorY();
                pk.z = getFloorZ();

                this.level.addChunkPacket(this.getFloorX() >> 4, this.getFloorZ() >> 4, pk);

                hasUpdate = true;
            }

            if (this.age >= this.lifetime) {
                EntityEventPacket pk = new EntityEventPacket();
                pk.event = EntityEventPacket.FIREWORK_EXPLOSION;
                pk.eid = this.getId();

                LevelSoundEventPacketV1 pk2 = new LevelSoundEventPacketV1();
                pk2.sound = LevelSoundEventPacket.SOUND_LARGE_BLAST;
                pk2.extraData = -1;
                pk2.pitch = -1;
                pk2.x = (float) getX();
                pk2.y = (float) getY();
                pk2.z = (float) getZ();

                this.level.addChunkPacket(this.getFloorX() >> 4, this.getFloorZ() >> 4, pk);
                this.level.addChunkPacket(this.getFloorX() >> 4, this.getFloorZ() >> 4, pk2);

                this.kill();

                hasUpdate = true;
            }
        }

        this.timing.stopTiming();

        return hasUpdate || !this.onGround || Math.abs(this.motionX) > 0.00001 || Math.abs(this.motionY) > 0.00001 || Math.abs(this.motionZ) > 0.00001;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        return (source.getCause() == DamageCause.VOID ||
                source.getCause() == DamageCause.FIRE_TICK ||
                source.getCause() == DamageCause.ENTITY_EXPLOSION ||
                source.getCause() == DamageCause.BLOCK_EXPLOSION)
                && super.attack(source);
    }

    public void setFirework(Item item) {
        this.firework = item;
        this.setDataProperty(new SlotEntityData(Entity.DATA_DISPLAY_ITEM, item));
    }

    @Override
    public float getWidth() {
        return 0.25f;
    }

    @Override
    public float getHeight() {
        return 0.25f;
    }
}
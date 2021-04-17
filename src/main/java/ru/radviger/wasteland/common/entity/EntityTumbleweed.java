package com.legacy.wasteland.common.entity;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.client.render.RenderTumbleweed;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.WeatherType;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

public class EntityTumbleweed extends Entity {
    public static final int FADE_TIME = 4 * 20;
    private static final int DESPAWN_RANGE = 110;
    private static final float BASE_SIZE = 0.75F;

    private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> CUSTOM_WIND_ENABLED = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> CUSTOM_WIND_X = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> CUSTOM_WIND_Z = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> FADING = EntityDataManager.createKey(EntityTumbleweed.class, DataSerializers.BOOLEAN);

    private int age;
    public int fadeAge;
    private boolean canDespawn;
    private int lifetime;

    private float distanceWalkedModified;
    private float distanceWalkedOnStepModified;
    private int nextStepDistance;
    private int groundTicks;

    @SideOnly(Side.CLIENT)
    public float rot1, rot2, rot3;
    @SideOnly(Side.CLIENT)
    public Quaternion prevQuat, quat;

    public EntityTumbleweed(World world) {
        super(world);

        this.entityCollisionReduction = 0.95f;
        this.preventEntitySpawning = true;
        this.canDespawn = true;

        if (this.world.isRemote) {
            this.rot1 = 360f * world.rand.nextFloat();
            this.rot2 = 360f * world.rand.nextFloat();
            this.rot3 = 360f * world.rand.nextFloat();

            this.quat = new Quaternion();
            this.prevQuat = new Quaternion();
        }
    }

    @Override
    protected void entityInit() {
        rand.setSeed(getEntityId());

        this.dataManager.register(SIZE, rand.nextInt(5) - 2);
        this.dataManager.register(CUSTOM_WIND_ENABLED, false);
        this.dataManager.register(CUSTOM_WIND_X, 0F);
        this.dataManager.register(CUSTOM_WIND_Z, 0F);
        this.dataManager.register(FADING, false);

        this.lifetime = 30 * 20 + rand.nextInt(200);

        updateSize();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setInteger("Size", getSize());
        compound.setBoolean("CustomWindEnabled", getCustomWindEnabled());
        compound.setDouble("CustomWindX", getCustomWindX());
        compound.setDouble("CustomWindZ", getCustomWindZ());
        compound.setBoolean("CanDespawn", canDespawn);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("Size"))
            this.dataManager.set(SIZE, compound.getInteger("Size"));

        if (compound.hasKey("CustomWindEnabled"))
            this.dataManager.set(CUSTOM_WIND_ENABLED, compound.getBoolean("CustomWindEnabled"));

        if (compound.hasKey("CustomWindX"))
            this.dataManager.set(CUSTOM_WIND_X, compound.getFloat("CustomWindX"));

        if (compound.hasKey("CustomWindZ"))
            this.dataManager.set(CUSTOM_WIND_Z, compound.getFloat("CustomWindZ"));

        if (compound.hasKey("CanDespawn"))
            canDespawn = compound.getBoolean("CanDespawn");
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);

        if (key == SIZE)
            updateSize();
    }

    private void updateSize() {
        float mcSize = BASE_SIZE + this.getSize() * (1 / 8f);
        this.setSize(mcSize, mcSize);
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entityIn) {
        return null;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canBePushed() {
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (this.getRidingEntity() != null) {
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
            return;
        }

        if (!this.isInWater())
            this.motionY -= 0.012;

        double x = this.motionX;
        double y = this.motionY;
        double z = this.motionZ;

        boolean ground = onGround;

        WastelandWorldData data = Wasteland.PROXY.getData(this.world);

        if (data != null) {
            BlockPos pos = new BlockPos(this);
            int skyLight = this.world.getLightFor(EnumSkyBlock.SKY, pos);
            if (data.getWeatherType() == WeatherType.SANDSTORM && skyLight >= 5) {

                this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

                float weatherStrength = data.getInterpolatedWeatherStrength(1F);

                float windMod = weatherStrength * (float)skyLight / 15F;

                double windX = data.getWindX() * windMod;
                double windZ = data.getWindZ() * windMod;

                if (this.isInWater()) {
                    this.motionY += 0.02;
                    this.motionX *= 0.95;
                    this.motionZ *= 0.95;
                } else if (windX != 0 || windZ != 0) {
                    this.motionX = windX;
                    this.motionZ = windZ;
                }

                // Rotate
                if (this.world.isRemote) {
                    groundTicks--;

                    if ((!ground && onGround) || isInWater())
                        groundTicks = 10;
                    else if (getCustomWindEnabled())
                        groundTicks = 5;

                    double div = 5d * width - groundTicks / 5d;
                    double rotX = 2d * Math.PI * this.motionX / div;
                    double rotZ = -2d * Math.PI * this.motionZ / div;

                    this.prevQuat = this.quat;
                    RenderTumbleweed.TEMP_QUAT.setFromAxisAngle((new Vector4f(1, 0, 0, (float) rotZ)));
                    Quaternion.mul(this.quat, RenderTumbleweed.TEMP_QUAT, this.quat);
                    RenderTumbleweed.TEMP_QUAT.setFromAxisAngle((new Vector4f(0, 0, 1, (float) rotX)));
                    Quaternion.mul(this.quat, RenderTumbleweed.TEMP_QUAT, this.quat);
                }

                // Bounce on ground
                if (this.onGround) {
                    this.motionY = windX * windX + windZ * windZ >= 0.05 * 0.05 ? Math.max(-y * 0.7, 0.24 - getSize() * 0.02) : -y * 0.7;
                }

                // Bounce on walls
                if (this.collidedHorizontally) {
                    this.motionX = -x * 0.4;
                    this.motionZ = -z * 0.4;
                }

                this.motionX *= 0.98;
                this.motionY *= 0.98;
                this.motionZ *= 0.98;
            }
        }

        if (Math.abs(this.motionX) < 0.005)
            this.motionX = 0.0;

        if (Math.abs(this.motionY) < 0.005)
            this.motionY = 0.0;

        if (Math.abs(this.motionZ) < 0.005)
            this.motionZ = 0.0;

        collideWithNearbyEntities();

        if (!this.world.isRemote) {
            this.age++;
            tryDespawn();
        }

        if (isFading()) {
            this.fadeAge++;

            if (this.fadeAge > FADE_TIME)
                setDead();
        }
    }

    private void tryDespawn() {
        if (!canDespawn) {
            this.age = 0;
            return;
        }

        Entity entity = this.world.getClosestPlayerToEntity(this, DESPAWN_RANGE);
        if (entity == null)
            this.setDead();

        if (this.age > this.lifetime && fadeAge == 0)
            this.dataManager.set(FADING, true);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.isEntityInvulnerable(source)) {
            return false;
        }

        if (!this.isDead && !this.world.isRemote) {
            this.setDead();
            this.markVelocityChanged();

            SoundType sound = SoundType.PLANT;
            this.playSound(sound.getBreakSound(), (sound.getVolume() + 1F) / 2F, sound.getPitch() * 0.8F);

            ItemStack stack = new ItemStack(Items.STICK, 1 + this.world.rand.nextInt(2));
            EntityItem item = new EntityItem(this.world, this.posX, this.posY, this.posZ, stack);
            item.motionX = 0;
            item.motionY = 0.2;
            item.motionZ = 0;
            item.setDefaultPickupDelay();
            this.world.spawnEntity(item);
        }

        return true;
    }

    @Override
    public boolean hitByEntity(Entity entity) {
        return entity instanceof EntityPlayer && this.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) entity), 0F);
    }

    @Override
    public void move(MoverType mover, double velX, double velY, double velZ) {
        if (this.noClip) {
            this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velX, velY, velZ));
            this.resetPositionToBB();
            return;
        }

        double startX = this.posX;
        double startY = this.posY;
        double startZ = this.posZ;

        if (this.isInWeb) {
            this.isInWeb = false;
            velX *= 0.25D;
            velY *= 0.05D;
            velZ *= 0.25D;
            this.motionX = 0;
            this.motionY = 0;
            this.motionZ = 0;
        }

        double startVelX = velX;
        double startVelY = velY;
        double startVelZ = velZ;

        List<AxisAlignedBB> boxes = this.world.getCollisionBoxes(this, this.getEntityBoundingBox().expand(velX, velY, velZ)); //FIXME maybe grow, not expand?

        for (AxisAlignedBB box : boxes)
            velY = box.calculateYOffset(this.getEntityBoundingBox(), velY);

        this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0, velY, 0));

        for (AxisAlignedBB box : boxes)
            velX = box.calculateXOffset(this.getEntityBoundingBox(), velX);

        this.setEntityBoundingBox(this.getEntityBoundingBox().offset(velX, 0, 0));

        for (AxisAlignedBB box : boxes)
            velZ = box.calculateZOffset(this.getEntityBoundingBox(), velZ);

        this.setEntityBoundingBox(this.getEntityBoundingBox().offset(0, 0, velZ));

        this.resetPositionToBB();
        this.collidedHorizontally = startVelX != velX || startVelZ != velZ;
        this.collidedVertically = startVelY != velY;
        this.onGround = this.collidedVertically && startVelY < 0;
        this.collided = this.collidedHorizontally || this.collidedVertically;
        int x = MathHelper.floor(this.posX);
        int y = MathHelper.floor(this.posY - 0.2D);
        int z = MathHelper.floor(this.posZ);
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = this.world.getBlockState(pos);
        Block block = state.getBlock();

        if (state.getBlock() == Blocks.AIR) {
            IBlockState downState = this.world.getBlockState(pos.down());
            Block downBlock = downState.getBlock();

            if (downBlock instanceof BlockFence || downBlock instanceof BlockWall || downBlock instanceof BlockFenceGate) {
                state = downState;
                pos = pos.down();
            }
        }

        this.updateFallState(velY, this.onGround, state, pos);

        if (startVelX != velX)
            this.motionX = 0;

        if (startVelZ != velZ)
            this.motionZ = 0;

        if (startVelY != velY) {
            block.onLanded(this.world, this);

            if (block == Blocks.FARMLAND) {
                if (!world.isRemote && world.rand.nextFloat() < 0.7F) {
                    if (!world.getGameRules().getBoolean("mobGriefing"))
                        return;

                    world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                }
            }
        }

        double d15 = this.posX - startX;
        double d16 = this.posY - startY;
        double d17 = this.posZ - startZ;

        if (block != Blocks.LADDER)
            d16 = 0;

        if (this.onGround)
            block.onEntityWalk(this.world, pos, this);

        this.distanceWalkedModified = (float) ((double) this.distanceWalkedModified + (double) MathHelper.sqrt(d15 * d15 + d17 * d17) * 0.6D);
        this.distanceWalkedOnStepModified = (float) ((double) this.distanceWalkedOnStepModified + (double) MathHelper.sqrt(d15 * d15 + d16 * d16 + d17 * d17) * 0.6D);

        if (this.distanceWalkedOnStepModified > (float) this.nextStepDistance && state.getMaterial() != Material.AIR) {
            this.nextStepDistance = (int) this.distanceWalkedOnStepModified + 1;

            if (this.isInWater()) {
                float f = MathHelper.sqrt(this.motionX * this.motionX * 0.2D + this.motionY * this.motionY + this.motionZ * this.motionZ * 0.2D) * 0.35F;

                if (f > 1) f = 1;

                this.playSound(this.getSwimSound(), f, 1 + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.4F);
            }

            if (!state.getMaterial().isLiquid()) {
                SoundType sound = SoundType.PLANT;
                this.playSound(sound.getStepSound(), sound.getVolume() * 0.15F, sound.getPitch());
            }
        }

        try {
            this.doBlockCollisions();
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
            CrashReportCategory category = report.makeCategory("Entity being checked for collision");
            this.addEntityCrashInfo(category);
            throw new ReportedException(report);
        }
    }

    private void collideWithNearbyEntities() {
        List<Entity> entities = this.world.getEntitiesInAABBexcluding(this, this.getEntityBoundingBox().expand(0.2, 0, 0.2), Entity::canBePushed);

        for (Entity e : entities) {
            collision(e);
        }
    }

    private void collision(Entity entity) {
        if (isPassenger(entity) || this.getRidingEntity() == entity)
            return;

        if (this.noClip || entity.noClip)
            return;

        if (!this.world.isRemote && entity instanceof EntityMinecart && ((EntityMinecart) entity).getType() == EntityMinecart.Type.RIDEABLE && entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.01D && entity.getPassengers().isEmpty() && this.getRidingEntity() == null) {
            this.startRiding(entity);
            this.motionY += 0.25;
            this.velocityChanged = true;
        } else {
            double dx = this.posX - entity.posX;
            double dz = this.posZ - entity.posZ;
            double dmax = MathHelper.absMax(dx, dz);

            if (dmax < 0.01D)
                return;

            dmax = (double) MathHelper.sqrt(dmax);
            dx /= dmax;
            dz /= dmax;
            double d3 = Math.min(1, 1.0 / dmax);
            dx *= d3;
            dz *= d3;
            dx *= 0.05D;
            dz *= 0.05D;
            dx *= (double) (1F - entity.entityCollisionReduction);
            dz *= (double) (1F - entity.entityCollisionReduction);

            if (entity.getPassengers().isEmpty()) {
                entity.motionX += -dx;
                entity.motionZ += -dz;
            }

            if (this.getPassengers().isEmpty()) {
                this.motionX += dx;
                this.motionZ += dz;
            }
        }
    }

    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this) && this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty() && !this.world.containsAnyLiquid(this.getEntityBoundingBox());
    }

    public int getSize() {
        return this.dataManager.get(SIZE);
    }

    public double getCustomWindX() {
        return this.dataManager.get(CUSTOM_WIND_X);
    }

    public double getCustomWindZ() {
        return this.dataManager.get(CUSTOM_WIND_Z);
    }

    public boolean getCustomWindEnabled() {
        return this.dataManager.get(CUSTOM_WIND_ENABLED);
    }

    public boolean isFading() {
        return this.dataManager.get(FADING);
    }
}

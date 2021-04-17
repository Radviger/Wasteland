package com.legacy.wasteland.common.block;

import com.legacy.wasteland.Wasteland;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;

import javax.annotation.Nullable;

public class BlockAcidWater extends BlockFluidClassic {
    public BlockAcidWater() {
        super(Wasteland.FLUID_ACID_WATER, Material.WATER);
        this.setUnlocalizedName("acid_water");
        this.setRegistryName("acid_water");
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (entity instanceof EntityLivingBase) {
            ((EntityLivingBase) entity).addPotionEffect(new PotionEffect(MobEffects.POISON, 20));
            entity.attackEntityFrom(Wasteland.DAMAGE_ACID_WATER, 2F);
        }
    }

    @Nullable
    public Boolean isEntityInsideMaterial(IBlockAccess blockAccessor, BlockPos pos, IBlockState state, Entity entity, double y, Material material, boolean p_isEntityInsideMaterial_8_) {
        return material == Material.WATER;
    }
}

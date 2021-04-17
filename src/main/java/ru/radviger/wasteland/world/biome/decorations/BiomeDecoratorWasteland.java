package com.legacy.wasteland.world.biome.decorations;

import com.legacy.wasteland.Wasteland;
import com.legacy.wasteland.config.WastelandConfig;
import com.legacy.wasteland.world.WastelandWorldData;
import com.legacy.wasteland.world.biome.decorations.template.WastelandStructure;
import com.legacy.wasteland.world.biome.decorations.template.WastelandStructure.SavedStructure;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStone.EnumType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.UserListOpsEntry;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.ChunkGeneratorSettings.Factory;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent.Decorate.EventType;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

public class BiomeDecoratorWasteland extends BiomeDecorator {
    public ChunkPos chunkPos;
    public boolean decorating;
    public ChunkGeneratorSettings chunkProviderSettings;
    public int firePerChunk;
    public WorldGenerator deadTreeGen = new WorldGenDeadBigTree(true, Blocks.LOG.getDefaultState());
    public WorldGenerator dirtGen;
    public WorldGenerator gravelOreGen;
    public WorldGenerator graniteGen;
    public WorldGenerator dioriteGen;
    public WorldGenerator andesiteGen;
    public WorldGenerator coalGen;
    public WorldGenerator ironGen;
    public WorldGenerator goldGen;
    public WorldGenerator redstoneGen;
    public WorldGenerator diamondGen;
    public WorldGenerator lapisGen;

    public BiomeDecoratorWasteland() {
        this.firePerChunk = WastelandConfig.worldgen.randomFirePerChunk;
        this.flowersPerChunk = -999;
        this.grassPerChunk = 5;
        this.deadBushPerChunk = 2;
        this.generateFalls = false;
        this.treesPerChunk = -999;
    }

    @Override
    public void decorate(World world, Random random, Biome biome, BlockPos pos) {
        if (this.decorating) {
            throw new RuntimeException("Already decorating");
        } else {
            this.chunkPos = new ChunkPos(pos);
            this.deadBushPerChunk = 5;
            this.chunkProviderSettings = Factory.jsonToFactory(world.getWorldInfo().getGeneratorOptions()).build();
            this.dirtGen = new WorldGenMinable(Blocks.DIRT.getDefaultState(), this.chunkProviderSettings.dirtSize);
            this.graniteGen = new WorldGenMinable(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, EnumType.GRANITE), this.chunkProviderSettings.graniteSize);
            this.dioriteGen = new WorldGenMinable(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, EnumType.DIORITE), this.chunkProviderSettings.dioriteSize);
            this.andesiteGen = new WorldGenMinable(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, EnumType.ANDESITE), this.chunkProviderSettings.andesiteSize);
            this.coalGen = new WorldGenMinable(Blocks.COAL_ORE.getDefaultState(), this.chunkProviderSettings.coalSize);
            this.ironGen = new WorldGenMinable(Blocks.IRON_ORE.getDefaultState(), this.chunkProviderSettings.ironSize);
            this.goldGen = new WorldGenMinable(Blocks.GOLD_ORE.getDefaultState(), this.chunkProviderSettings.goldSize);
            this.redstoneGen = new WorldGenMinable(Blocks.REDSTONE_ORE.getDefaultState(), this.chunkProviderSettings.redstoneSize);
            this.diamondGen = new WorldGenMinable(Blocks.DIAMOND_ORE.getDefaultState(), this.chunkProviderSettings.diamondSize);
            this.lapisGen = new WorldGenMinable(Blocks.LAPIS_ORE.getDefaultState(), this.chunkProviderSettings.lapisSize);
            this.genDecorations(biome, world, random);
            this.decorating = false;
        }
    }

    @Override
    protected void genDecorations(Biome biome, World world, Random rand) {
        this.generateOres(world, rand);

        this.generateRandomStructure(biome, world, rand);

        if (TerrainGen.decorate(world, rand, this.chunkPos.getBlock(0, 0, 0), EventType.DEAD_BUSH)) {
            for (int size = 0; size < this.deadBushPerChunk; ++size) {
                int x = rand.nextInt(16) + 8;
                int z = rand.nextInt(16) + 8;
                int y = this.getHeightAt(world, x, z) * 2;
                if (y > 0) {
                    int randomY = rand.nextInt(y);
                    (new WorldGenDeadBushOrSapling()).generate(world, rand, this.chunkPos.getBlock(x, randomY, z));
                }
            }

            for (int size = 0; size < this.grassPerChunk; ++size) {
                int x = rand.nextInt(16) + 8;
                int z = rand.nextInt(16) + 8;
                int y = this.getHeightAt(world, x, z) * 2;
                if (y > 0) {
                    int randomY = rand.nextInt(y);
                    (new WorldGenDeadGrass()).generate(world, rand, this.chunkPos.getBlock(x, randomY, z));
                }
            }
        }

        if (rand.nextInt(WastelandConfig.worldgen.wastelandTreeSpawnRate * 15) == 0) {
            this.deadTreeGen.generate(world, rand, world.getHeight(this.chunkPos.getBlock(rand.nextInt(16) + 8, 0, rand.nextInt(16) + 8)));
        }
    }

    private boolean generateRandomStructure(Biome biome, World world, Random rand) {
        WastelandWorldData data = Wasteland.PROXY.getData(world);
        if (data == null) {
            return false;
        }

        WastelandStructure structure = Wasteland.findRandomStructure(rand);

        if (structure == null) {
            return false;
        }

        if (rand.nextInt(structure.rarity) != 0
                && (structure.biomeFilter.isEmpty() || !structure.biomeFilter.contains(biome.getRegistryName()))) {

            int x = rand.nextInt(16);
            int z = rand.nextInt(16);
            int y = this.getHeightAt(world, x, z);
            if (y > 0) {
                BlockPos pos = this.chunkPos.getBlock(x, y, z);

                Tuple<BlockPos, WastelandStructure> currentGeneratingStructure = data.getCurrentGeneratingStructure();
                if (currentGeneratingStructure != null) {
                    BlockPos targetPos = currentGeneratingStructure.getFirst();
                    WastelandStructure targetStructure = currentGeneratingStructure.getSecond();
                    if (Math.sqrt(targetPos.distanceSq(pos)) < structure.minDistance + targetStructure.minDistance) {
                        return false;
                    }
                }

                data.setCurrentGeneratingStructure(new Tuple<>(pos, structure));

                SavedStructure nearestStructure = data.getNearestStructure(pos);
                if (nearestStructure != null && nearestStructure.distanceTo(pos) < structure.minDistance + nearestStructure.minDistance) {
                    return false;
                }
                Rotation rotation = structure.rotated ? Rotation.values()[rand.nextInt(Rotation.values().length)] : Rotation.NONE;
                Mirror mirror = structure.mirrored ? Mirror.values()[rand.nextInt(Mirror.values().length)] : Mirror.NONE;

                SavedStructure ss = structure.generate(world, rand, pos, rotation, mirror);
                if (ss != null) {
                    MinecraftServer server = world.getMinecraftServer();
                    PlayerList playerList = server.getPlayerList();
                    for (GameProfile profile : playerList.getOnlinePlayerProfiles()) {
                        UserListOpsEntry opsEntry = playerList.getOppedPlayers().getEntry(profile);
                        if (opsEntry != null && opsEntry.getPermissionLevel() >= 3) {
                            EntityPlayerMP player = playerList.getPlayerByUUID(profile.getId());
                            ITextComponent coord = Wasteland.formatCoordinates(pos);
                            ITextComponent message = new TextComponentString("Generated " + structure.id + " at ");
                            message.appendSibling(coord);
                            player.sendMessage(message);
                        }
                    }

                    data.addStructure(structure.id, ss);
                    data.setCurrentGeneratingStructure(null);

                    return true;
                }
            }
        }

        return false;
    }

    private int getHeightAt(World world, int localX, int localZ) {
        return world.getHeight((this.chunkPos.x << 4) + localX, (this.chunkPos.z << 4) + localZ);
    }

    @Override
    protected void generateOres(World world, Random random) {
        if (WastelandConfig.worldgen.shouldSpawnOres) {
            BlockPos pos = chunkPos.getBlock(0, 0, 0);
            MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Pre(world, random, pos));
            if (TerrainGen.generateOre(world, random, dirtGen, pos, OreGenEvent.GenerateMinable.EventType.DIRT))
                this.genStandardOre1(world, random, this.chunkProviderSettings.dirtCount, this.dirtGen, this.chunkProviderSettings.dirtMinHeight, this.chunkProviderSettings.dirtMaxHeight);
            if (TerrainGen.generateOre(world, random, dioriteGen, pos, OreGenEvent.GenerateMinable.EventType.DIORITE))
                this.genStandardOre1(world, random, this.chunkProviderSettings.dioriteCount, this.dioriteGen, this.chunkProviderSettings.dioriteMinHeight, this.chunkProviderSettings.dioriteMaxHeight);
            if (TerrainGen.generateOre(world, random, graniteGen, pos, OreGenEvent.GenerateMinable.EventType.GRANITE))
                this.genStandardOre1(world, random, this.chunkProviderSettings.graniteCount, this.graniteGen, this.chunkProviderSettings.graniteMinHeight, this.chunkProviderSettings.graniteMaxHeight);
            if (TerrainGen.generateOre(world, random, andesiteGen, pos, OreGenEvent.GenerateMinable.EventType.ANDESITE))
                this.genStandardOre1(world, random, this.chunkProviderSettings.andesiteCount, this.andesiteGen, this.chunkProviderSettings.andesiteMinHeight, this.chunkProviderSettings.andesiteMaxHeight);
            if (TerrainGen.generateOre(world, random, coalGen, pos, OreGenEvent.GenerateMinable.EventType.COAL))
                this.genStandardOre1(world, random, this.chunkProviderSettings.coalCount, this.coalGen, this.chunkProviderSettings.coalMinHeight, this.chunkProviderSettings.coalMaxHeight);
            if (TerrainGen.generateOre(world, random, ironGen, pos, OreGenEvent.GenerateMinable.EventType.IRON))
                this.genStandardOre1(world, random, this.chunkProviderSettings.ironCount, this.ironGen, this.chunkProviderSettings.ironMinHeight, this.chunkProviderSettings.ironMaxHeight);
            if (TerrainGen.generateOre(world, random, goldGen, pos, OreGenEvent.GenerateMinable.EventType.GOLD))
                this.genStandardOre1(world, random, this.chunkProviderSettings.goldCount, this.goldGen, this.chunkProviderSettings.goldMinHeight, this.chunkProviderSettings.goldMaxHeight);
            if (TerrainGen.generateOre(world, random, redstoneGen, pos, OreGenEvent.GenerateMinable.EventType.REDSTONE))
                this.genStandardOre1(world, random, this.chunkProviderSettings.redstoneCount, this.redstoneGen, this.chunkProviderSettings.redstoneMinHeight, this.chunkProviderSettings.redstoneMaxHeight);
            if (TerrainGen.generateOre(world, random, diamondGen, pos, OreGenEvent.GenerateMinable.EventType.DIAMOND))
                this.genStandardOre1(world, random, this.chunkProviderSettings.diamondCount, this.diamondGen, this.chunkProviderSettings.diamondMinHeight, this.chunkProviderSettings.diamondMaxHeight);
            if (TerrainGen.generateOre(world, random, lapisGen, pos, OreGenEvent.GenerateMinable.EventType.LAPIS))
                this.genStandardOre2(world, random, this.chunkProviderSettings.lapisCount, this.lapisGen, this.chunkProviderSettings.lapisCenterHeight, this.chunkProviderSettings.lapisSpread);
            MinecraftForge.ORE_GEN_BUS.post(new OreGenEvent.Post(world, random, pos));
        }
    }

    @Override
    protected void genStandardOre1(World world, Random random, int blockCount, WorldGenerator generator, int minHeight, int maxHeight) {
        if (maxHeight < minHeight) {
            int i = minHeight;
            minHeight = maxHeight;
            maxHeight = i;
        } else if (maxHeight == minHeight) {
            if (minHeight < 255) {
                ++maxHeight;
            } else {
                --minHeight;
            }
        }

        for (int j = 0; j < blockCount; ++j) {
            BlockPos pos = this.chunkPos.getBlock(random.nextInt(16), random.nextInt(maxHeight - minHeight) + minHeight, random.nextInt(16));
            generator.generate(world, random, pos);
        }
    }

    @Override
    protected void genStandardOre2(World world, Random random, int blockCount, WorldGenerator generator, int centerHeight, int spread) {
        for (int i = 0; i < blockCount; ++i) {
            BlockPos pos = this.chunkPos.getBlock(random.nextInt(16), random.nextInt(spread) + random.nextInt(spread) + centerHeight - spread, random.nextInt(16));
            generator.generate(world, random, pos);
        }
    }
}

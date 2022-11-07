package at.kara.geoworldgen;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class GeoScaleChunkGenerator extends ChunkGenerator {

    public static final int EXTREME_HEIGHT = 250;
    public static final int TREE_BOARDER = 190;
    public static final int MEADOW_BOARDER = 80;

    public static final int ROUGH_TERRAIN = 35;

    public static final int SEMI_ROUGH_TERRAIN = 27;

    private final HeightMapReader heightMapReader;
    private final MetaMapReader metaMapReader;

    public GeoScaleChunkGenerator(HeightMapReader heightMapReader, MetaMapReader metaMapReader) {
        this.heightMapReader = heightMapReader;
        this.metaMapReader = metaMapReader;
    }


    @Override
    public boolean shouldGenerateCaves() { return true; }

    public boolean shouldGenerateDecorations() { return true; }

    @Override
    public boolean shouldGenerateMobs() { return true; }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkGenerator.ChunkData chunkData) {

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int absoluteX = worldX + x;
                int absoluteZ = worldZ + z;

                //set ground layer bedrock
                chunkData.setBlock(x, -64, z, Material.BEDROCK);

                int heightForLocation = this.heightMapReader.getHeightForMcXZ(absoluteX, absoluteZ);
                if(heightForLocation == this.heightMapReader.noMapDataValue){
                    continue;
                }

                Material surfaceMaterial;
                float terrainRoughness = this.heightMapReader.getTerrainRoughness(absoluteX, absoluteZ);

                int stoneBoarder = heightForLocation - random.nextInt(10, 30);
                for (int y = -63; y < stoneBoarder; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }

                for (int y = stoneBoarder; y <= heightForLocation; y++) {

                    if(heightForLocation > EXTREME_HEIGHT){ //extreme height
                        surfaceMaterial = Material.STONE;

                    }else if(heightForLocation > TREE_BOARDER){ //tree boarder
                        if(terrainRoughness > ROUGH_TERRAIN){
                            surfaceMaterial = Material.STONE;
                        }else if(terrainRoughness > SEMI_ROUGH_TERRAIN){
                            surfaceMaterial = Material.GRAVEL;
                        }else {
                            surfaceMaterial = Material.DIRT;
                        }

                    }else if(heightForLocation > MEADOW_BOARDER){ //meadow
                       if(terrainRoughness > SEMI_ROUGH_TERRAIN){
                            surfaceMaterial = Material.GRAVEL;
                        }else {
                            surfaceMaterial = Material.DIRT;
                        }

                    }else {
                        surfaceMaterial = Material.DIRT;
                    }

                    if(y == heightForLocation && surfaceMaterial == Material.DIRT){
                        surfaceMaterial = Material.GRASS_BLOCK;
                    }
                    chunkData.setBlock(x, y, z, surfaceMaterial);
                }
            }
        }
    }


    @Override
    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkGenerator.ChunkData chunkData) {
        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int absoluteX = worldX + x;
                int absoluteZ = worldZ + z;
                int blockHeight = this.heightMapReader.getHeightForMcXZ(absoluteX, absoluteZ);

                Biome currentBiome = chunkData.getBiome(x, blockHeight, z);
                if(currentBiome == Biome.RIVER){
                    chunkData.setBlock(x, blockHeight, z, Material.WATER);
                }else if(currentBiome == Biome.FROZEN_RIVER){
                    chunkData.setBlock(x, blockHeight, z, blockHeight > EXTREME_HEIGHT ? Material.BLUE_ICE : Material.ICE);
                }
            }
        }


    }

    private static class CustomBiomesProvider extends BiomeProvider {

        private final GeoScaleChunkGenerator parent;

        private CustomBiomesProvider(GeoScaleChunkGenerator parent) {
            this.parent = parent;
        }

        @NotNull
        @Override
        public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            MetaMapReader.TerrainType terrainType = parent.metaMapReader == null ? MetaMapReader.TerrainType.NO_DATA : parent.metaMapReader.getTypeForLocation(x, z);
            int heightForLocation = parent.heightMapReader.getHeightForMcXZ(x, z);

            switch (terrainType){
                case FOREST -> {
                    return heightForLocation > TREE_BOARDER ? Biome.TAIGA : Biome.OLD_GROWTH_SPRUCE_TAIGA;
                }
                case WATER -> {
                    return heightForLocation > EXTREME_HEIGHT ? Biome.FROZEN_RIVER : Biome.RIVER;
                }
                case NO_DATA -> {
                    float terrainRoughness = parent.heightMapReader.getTerrainRoughness(x, z);

                    if(heightForLocation > EXTREME_HEIGHT){
                        if(terrainRoughness > ROUGH_TERRAIN){
                            return Biome.JAGGED_PEAKS;
                        }else if(terrainRoughness > SEMI_ROUGH_TERRAIN){
                            return Biome.SNOWY_SLOPES;
                        }else {
                            return Biome.SNOWY_PLAINS;
                        }
                    }else if(heightForLocation > TREE_BOARDER){
                        if(terrainRoughness > ROUGH_TERRAIN){
                            return Biome.WINDSWEPT_GRAVELLY_HILLS;
                        }else {
                            return Biome.WINDSWEPT_HILLS;
                        }
                    }else if(heightForLocation > MEADOW_BOARDER){
                        if(terrainRoughness > ROUGH_TERRAIN){
                            return Biome.WINDSWEPT_HILLS;
                        }else {
                            return Biome.MEADOW;
                        }
                    }else {
                        return Biome.PLAINS;
                    }
                }
            }

            return Biome.PLAINS;
        }

        @NotNull
        @Override
        public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return List.of(
                    Biome.JAGGED_PEAKS,
                    Biome.FROZEN_PEAKS,
                    Biome.SNOWY_SLOPES,
                    Biome.SNOWY_PLAINS,
                    Biome.WINDSWEPT_GRAVELLY_HILLS,
                    Biome.WINDSWEPT_HILLS,
                    Biome.WINDSWEPT_FOREST,
                    Biome.MEADOW,
                    Biome.PLAINS,
                    Biome.OLD_GROWTH_SPRUCE_TAIGA,
                    Biome.TAIGA,
                    Biome.FOREST,
                    Biome.FLOWER_FOREST,
                    Biome.BIRCH_FOREST,
                    Biome.FROZEN_RIVER,
                    Biome.RIVER
            );
        }

    }

    @Nullable
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new CustomBiomesProvider(this);
    }

    @Nullable
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        Location spawn = this.heightMapReader.getSpawn(world);
        spawn.setWorld(world);
        return spawn;
    }

}

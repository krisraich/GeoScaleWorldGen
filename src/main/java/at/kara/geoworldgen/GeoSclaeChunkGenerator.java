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

public class GeoSclaeChunkGenerator extends ChunkGenerator {

    private final GeoTiffReader geoTiffReader;
    private final GeoScaleWorldConfig geoScaleWorldConfig;

    private Location spawn;

    public GeoSclaeChunkGenerator(GeoTiffReader geoTiffReader, GeoScaleWorldConfig geoScaleWorldConfig) {
        this.geoScaleWorldConfig = geoScaleWorldConfig;
        this.geoTiffReader = geoTiffReader;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkGenerator.ChunkData chunkData) {

        int worldX = chunkX * 16;
        int worldZ = chunkZ * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {

                int heightForLocation = this.geoTiffReader.getHeightForLocation(worldX + x, worldZ + z);

                chunkData.setBlock(x, -64, z, Material.BEDROCK);
                for (int y = -63; y < heightForLocation; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }
            }
        }
    }


    //    @Override
//    public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull HeightMap heightMap) {
//        return super.getBaseHeight(worldInfo, random, x, z, heightMap);
//    }
//
//
//    @Override
//    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.ChunkData chunkData) {
//        super.generateSurface(worldInfo, random, x, z, chunkData);
//    }
//
//    @Override
//    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.ChunkData chunkData) {
//        super.generateCaves(worldInfo, random, x, z, chunkData);
//    }



    private static class CustomBiomesProvider extends BiomeProvider {

        private static CustomBiomesProvider instance;

        public static CustomBiomesProvider getInstance(GeoSclaeChunkGenerator geoSclaeChunkGenerator){
            if(instance == null){
                instance = new CustomBiomesProvider(geoSclaeChunkGenerator);
            }
            return instance;
        }

        private final GeoSclaeChunkGenerator parent;

        private CustomBiomesProvider(GeoSclaeChunkGenerator parent) {
            this.parent = parent;
        }

        @NotNull
        @Override
        public Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return parent.geoTiffReader.getHeightForLocation(x,z) > 190 ? Biome.FOREST : Biome.FROZEN_PEAKS; //todo: read biome from orhto
        }

        @NotNull
        @Override
        public List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return List.of(Biome.FOREST, Biome.ICE_SPIKES);
        }

    }

    @Nullable
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return CustomBiomesProvider.getInstance(this);
    }

    @Nullable
    @Override
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        if(this.spawn == null){
            Location location = this.geoTiffReader.posToMcLocation(
                    this.geoScaleWorldConfig.getMapSpawnLongitude(),
                    this.geoScaleWorldConfig.getMapSpawnLatitude(),
                    world
            );
            if(location.getBlockY() == this.geoTiffReader.noMapDataValue){
                location = new Location(world,
                        0,
                        this.geoTiffReader.getHeightForLocation(0,0),
                        0
                );
            }
            this.spawn = location;
        }
        return spawn;
    }

    @Override
    public boolean shouldGenerateBedrock() { return true; }

    @Override
    public boolean shouldGenerateCaves() { return true; }

    @Override
    public boolean shouldGenerateDecorations() { return true; }

    @Override
    public boolean shouldGenerateMobs() { return true; }

    @Override
    public boolean shouldGenerateStructures() { return true; }

}

package at.kara.geoworldgen;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;


public final class GeoScaleWorldGen extends JavaPlugin {

    private static GeoScaleWorldGen plugin;

    public static GeoScaleWorldGen getPlugin() {
        return plugin;
    }

    private GeoScaleChunkGenerator geoScaleChunkGenerator;

    private final PluginLogger logger = new PluginLogger(this);

    @Override
    public void onEnable() {
        saveDefaultConfig();
        GeoScaleWorldConfig geoScaleWorldConfig = GeoScaleWorldConfig.read(logger, getConfig());
        GeoTiffReader geoTiffReader = new GeoTiffReader(geoScaleWorldConfig);
        geoTiffReader.init();
        this.geoScaleChunkGenerator = new GeoScaleChunkGenerator(geoTiffReader, geoScaleWorldConfig);
        GeoCodingService geoCodingService = new GeoCodingService(this, geoScaleWorldConfig, geoTiffReader);

        this.getCommand("tpl").setExecutor(geoCodingService);
        this.getCommand("tpc").setExecutor(geoCodingService);
        this.getCommand("tpr").setExecutor(geoCodingService);

        logger.log(Level.INFO, "Finished loading plugin.");
    }

    @Override
    public void onDisable() {
        logger.log(Level.INFO, "Unloading plugin.");
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return this.geoScaleChunkGenerator;
    }

}

package at.kara.geoworldgen;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginLogger;

import java.util.logging.Level;

@Getter
@Setter
public class GeoScaleWorldConfig {

    private PluginLogger logger;

    protected double mapSpawnLatitude;

    protected double mapSpawnLongitude;

    protected String tifFileLocation;

    protected String geoCodingApiKey;

    protected String teleportationSuffix;


    public void info(String message){
        if(logger == null) {
            System.out.println("[INFO] " + message);
        }else {
            logger.log(Level.INFO, message);
        }
    }

    public static GeoScaleWorldConfig read(PluginLogger logger, FileConfiguration config) {

        GeoScaleWorldConfig geoScaleWorldConfig = new GeoScaleWorldConfig();
        geoScaleWorldConfig.setLogger(logger);
        geoScaleWorldConfig.info("Reading configs.");

        geoScaleWorldConfig.setMapSpawnLatitude(config.getDouble("spawnLocation.lat"));
        geoScaleWorldConfig.setMapSpawnLongitude(config.getDouble("spawnLocation.lng"));
        geoScaleWorldConfig.setTifFileLocation(config.getString("heightMap"));
        geoScaleWorldConfig.setGeoCodingApiKey(config.getString("geoCodingApiKey"));
        geoScaleWorldConfig.setTeleportationSuffix(config.getString("teleportationSuffix"));

        return geoScaleWorldConfig;
    }
}

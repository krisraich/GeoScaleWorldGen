package at.kara.geoworldgen;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
@Setter
public class PluginConfig {

    protected int mapScaleFactor;

    protected double mapSpawnLatitude;

    protected double mapSpawnLongitude;

    protected String heightMapPath;

    protected String metaMapPath;

    protected String geoCodingApiKey;

    protected String teleportationSuffix;


    public static PluginConfig read(FileConfiguration config) {

        PluginConfig pluginConfig = new PluginConfig();
        pluginConfig.setMapScaleFactor(config.getInt("scale"));
        pluginConfig.setMapSpawnLatitude(config.getDouble("spawnLocation.lat"));
        pluginConfig.setMapSpawnLongitude(config.getDouble("spawnLocation.lng"));
        pluginConfig.setHeightMapPath(config.getString("heightMap"));
        pluginConfig.setMetaMapPath(config.getString("metaMap"));
        pluginConfig.setGeoCodingApiKey(config.getString("geoCodingApiKey"));
        pluginConfig.setTeleportationSuffix(config.getString("teleportationSuffix"));

        return pluginConfig;
    }
}

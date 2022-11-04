import at.kara.geoworldgen.*;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@Log
public class Tests {

    public static double ACHENSEE_LAT = 47.436347885547754;
    public static double ACHENSEE_LNG = 11.71606722308175;

    public static double WILDSPITZE_LAT = 46.885278;
    public static double WILDSPITZE_LNG = 10.867222;

    public static double IBK_LAT = 47.271636;
    public static double IBK_LNG = 11.396928;


    public static double WALD_LAT = 46.7865249;
    public static double WALD_LNG = 12.8530762;

    private static HeightMapReader heightMapReader;

    private static MetaMapReader metaMapReader;

    private static GeoCodingService geoCodingService;

    private static final PluginConfig pluginConfig = new PluginConfig();

    @BeforeClass
    public static void init(){
        Yaml yaml = new Yaml();
        InputStream inputStream = Test.class
                .getClassLoader()
                .getResourceAsStream("config.yml");
        Map<String, Object> config = yaml.load(inputStream);



        pluginConfig.setGeoCodingApiKey((String) config.get("geoCodingApiKey"));
        pluginConfig.setMapScaleFactor((Integer) config.get("scale"));
        pluginConfig.setTeleportationSuffix((String) config.get("teleportationSuffix"));
        pluginConfig.setHeightMapPath((String) config.get("heightMap"));
        pluginConfig.setMetaMapPath((String) config.get("metaMap"));

        Map<String, Double> spawnLocation = (Map)config.get("spawnLocation");
        pluginConfig.setMapSpawnLatitude(spawnLocation.get("lat"));
        pluginConfig.setMapSpawnLongitude(spawnLocation.get("lng"));

        heightMapReader = new HeightMapReader(pluginConfig);
        geoCodingService = new GeoCodingService(null, pluginConfig, heightMapReader);
        heightMapReader.init();

        metaMapReader = new MetaMapReader(pluginConfig);
        metaMapReader.init();

    }

    @Test
    public void test_SpawnMapping(){
        assertLocation(
                heightMapReader.getMcLocationForLongLat(pluginConfig.getMapSpawnLongitude(), pluginConfig.getMapSpawnLatitude()),
                0,
                -4,
                0
        );
    }


    @Test
    public void test_wildspitz(){
        assertLocation(
                heightMapReader.getMcLocationForLongLat(WILDSPITZE_LNG, WILDSPITZE_LAT),
                -4038,
                315,
                4282
        );
    }

    @Test
    public void test_mappingAccuracy(){
        int[] metaPos = metaMapReader.lngLatToMcXZ(ACHENSEE_LNG, ACHENSEE_LAT);
        int[] heightMapPos = heightMapReader.lngLatToMcXZ(ACHENSEE_LNG, ACHENSEE_LAT);
        Assert.assertArrayEquals(metaPos, heightMapPos);

        metaPos = metaMapReader.lngLatToMcXZ(WILDSPITZE_LNG, WILDSPITZE_LAT);
        heightMapPos = heightMapReader.lngLatToMcXZ(WILDSPITZE_LNG, WILDSPITZE_LAT);
        Assert.assertArrayEquals(metaPos, heightMapPos);

        metaPos = metaMapReader.lngLatToMcXZ(IBK_LNG, IBK_LAT);
        heightMapPos = heightMapReader.lngLatToMcXZ(IBK_LNG, IBK_LAT);
        Assert.assertArrayEquals(metaPos, heightMapPos);

        metaPos = metaMapReader.lngLatToMcXZ(WALD_LNG, WALD_LAT);
        heightMapPos = heightMapReader.lngLatToMcXZ(WALD_LNG, WALD_LAT);
        Assert.assertArrayEquals(metaPos, heightMapPos);

    }



    @Test
    public void test_MetaMap_Water(){
        int[] achenSeeMcPos = metaMapReader.lngLatToMcXZ(ACHENSEE_LNG, ACHENSEE_LAT);

        MetaMapReader.TerrainType typeForLocation = metaMapReader.getTypeForLocation(achenSeeMcPos[0], achenSeeMcPos[1]);
        Assert.assertSame(typeForLocation, MetaMapReader.TerrainType.WATER);
    }

    @Test
    public void test_MetaMap_NoData(){
        int[] wildSpitzeMcPos = metaMapReader.lngLatToMcXZ(WILDSPITZE_LNG, WILDSPITZE_LAT);

        MetaMapReader.TerrainType typeForLocation = metaMapReader.getTypeForLocation(wildSpitzeMcPos[0], wildSpitzeMcPos[1]);
        Assert.assertSame(typeForLocation, MetaMapReader.TerrainType.NO_DATA);
    }

    @Test
    public void test_MetaMap_Forest(){
        int[] forestMcPos = metaMapReader.lngLatToMcXZ(WALD_LNG, WALD_LAT);

        MetaMapReader.TerrainType typeForLocation = metaMapReader.getTypeForLocation(forestMcPos[0], forestMcPos[1]);
        Assert.assertSame(typeForLocation, MetaMapReader.TerrainType.FOREST);
    }





    @Test
    public void test_HeightMap(){
        Assert.assertEquals(
                -4,
                heightMapReader.getHeightForMcXZ(0,0)
        );
    }

    @Test
    public void test_ibk(){
        assertLocation(
                heightMapReader.getMcLocationForLongLat(IBK_LNG, IBK_LAT),
                -1,
                -4,
                1
        );
    }

    @Test
    public void test_geoCodingName() {
        GeoCodingService.BaseTeleportationTask teleportToNameTask = geoCodingService.getTeleportToNameTask(null, new String[]{"Innsbruck"});
        teleportToNameTask.run();
        assertLocation(
                teleportToNameTask.getResult(),
                -123,
                -4,
                106
        );
    }

    @Test
    public void test_geoCodingLocation() {
        GeoCodingService.BaseTeleportationTask teleportToCoordinatesTask = geoCodingService.getTeleportToCoordinatesTask(null, new String[]{"" + IBK_LAT, "" + IBK_LNG});
        teleportToCoordinatesTask.run();
        assertLocation(
                teleportToCoordinatesTask.getResult(),
                -1,
                -4,
                1
        );
    }

    @Test
    public void test_Incline() {
        Location location = heightMapReader.getMcLocationForLongLat(WILDSPITZE_LNG, WILDSPITZE_LAT);
        float surfaceIncline = heightMapReader.getTerrainRoughness(location);
        Assert.assertEquals(17.36, surfaceIncline, 0.01);
    }


    @Test
    public void test_RandomTeleport() {
        GeoCodingService.RandomTeleportTask randomTeleportTask = geoCodingService.getRandomTeleportTask(null);
        randomTeleportTask.run();
        Assert.assertNotEquals(
                heightMapReader.noMapDataValue,
                randomTeleportTask.getResult().getBlockY()
        );
    }



    private void assertLocation(Location location, int x, int y, int z){
        if(location == null){
            Assert.fail();
        }
        Assert.assertEquals(x, location.getBlockX());
        Assert.assertEquals(y, location.getBlockY());
        Assert.assertEquals(z, location.getBlockZ());
    }

}

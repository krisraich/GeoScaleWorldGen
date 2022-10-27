import at.kara.geoworldgen.GeoCodingService;
import at.kara.geoworldgen.GeoScaleWorldConfig;
import at.kara.geoworldgen.GeoTiffReader;
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

    public static double WILDSPITZE_LAT = 46.885278;
    public static double WILDSPITZE_LNG = 10.867222;

    public static double IBK_LAT = 47.271636;
    public static double IBK_LNG = 11.396928;

    private static GeoScaleWorldConfig geoScaleWorldConfig;
    private static GeoTiffReader geoTiffReader;
    private static GeoCodingService geoCodingService;

    @BeforeClass
    public static void init(){
        Yaml yaml = new Yaml();
        InputStream inputStream = Test.class
                .getClassLoader()
                .getResourceAsStream("config.yml");
        Map<String, Object> config = yaml.load(inputStream);


        GeoScaleWorldConfig geoScaleWorldConfig = new GeoScaleWorldConfig();
        geoScaleWorldConfig.setTeleportationSuffix((String) config.get("teleportationSuffix"));
        geoScaleWorldConfig.setTifFileLocation((String) config.get("heightMap"));
        geoScaleWorldConfig.setGeoCodingApiKey((String) config.get("geoCodingApiKey"));

        Map<String, Double> spawnLocation = (Map)config.get("spawnLocation");
        geoScaleWorldConfig.setMapSpawnLatitude(spawnLocation.get("lat"));
        geoScaleWorldConfig.setMapSpawnLongitude(spawnLocation.get("lng"));

        geoTiffReader = new GeoTiffReader(geoScaleWorldConfig);
        geoCodingService = new GeoCodingService(null, geoScaleWorldConfig, geoTiffReader);


        geoTiffReader.init();
    }

    @Test
    public void test_HeightMap(){
        Assert.assertEquals(
                180,
                geoTiffReader.getHeightForLocation(0,0)
        );
    }

    @Test
    public void test_ibk(){
        assertLocation(
                geoTiffReader.posToMcLocation(IBK_LNG, IBK_LAT),
                -1336,
                -4,
                -887
        );
    }

    @Test
    public void test_wildspitz(){
        assertLocation(
                geoTiffReader.posToMcLocation(WILDSPITZE_LNG, WILDSPITZE_LAT),
                -5289,
                315,
                3478
        );
    }

    @Test
    public void test_geoCodingName() {
        GeoCodingService.BaseTeleportationTask teleportToNameTask = geoCodingService.getTeleportToNameTask(null, new String[]{"Innsbruck"});
        teleportToNameTask.run();
        assertLocation(
                teleportToNameTask.getResult(),
                -1457,
                -3,
                -781
        );
    }

    @Test
    public void test_geoCodingName2() {
        GeoCodingService.BaseTeleportationTask teleportToNameTask = geoCodingService.getTeleportToNameTask(null, new String[]{"Imst"});
        teleportToNameTask.run();
        assertLocation(
                teleportToNameTask.getResult(),
                -6233,
                17,
                -509
        );
    }

    @Test
    public void test_geoCodingLocation() {
        GeoCodingService.BaseTeleportationTask teleportToCoordinatesTask = geoCodingService.getTeleportToCoordinatesTask(null, new String[]{"" + IBK_LAT, "" + IBK_LNG});
        teleportToCoordinatesTask.run();
        assertLocation(
                teleportToCoordinatesTask.getResult(),
                -1336,
                -4,
                -887
        );
    }

    @Test
    public void test_Incline() {
        Location location = geoTiffReader.posToMcLocation(WILDSPITZE_LNG, WILDSPITZE_LAT);
        float surfaceIncline = geoTiffReader.getTerrainRoughness(location);
        Assert.assertEquals(20.5599, surfaceIncline, 0.001);
    }


    @Test
    public void test_RandomTeleport() {
        GeoCodingService.RandomTeleportTask randomTeleportTask = geoCodingService.getRandomTeleportTask(null);
        randomTeleportTask.run();
        Assert.assertNotEquals(
                geoTiffReader.noMapDataValue,
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

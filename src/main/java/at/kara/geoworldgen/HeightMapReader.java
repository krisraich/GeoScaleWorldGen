package at.kara.geoworldgen;

import com.twelvemonkeys.imageio.metadata.Entry;
import org.bukkit.Location;
import org.bukkit.World;

import java.awt.image.DataBuffer;
import java.util.Locale;


/**
 * LAT = Y(Z) = Height = NORTH / SOUT (47)
 * LONG = X = Width = EAST / WEST (12)
 * Elevation = Z
 *
 * Tirol
 * 3770m höchster punkt
 * 465m  tiefster punkt
 * 3305m höhenunterschied
 *
 * mc
 * 319 max height
 * -64 min height
 * 383m total height
 *
 * tirol 1:10
 * 377m -> 319 = -58 Offset
 * 47m - 58 = -11
 *
 * mc 319 -> tirol 377  3770
 * mc 192 -> tirol 250 2500
 * mc -11 -> tirol 47    470
 * mc -58 -> tirol 0       0
 * mc -64 -> tirol -6    -60
 *
 */
public class HeightMapReader extends BaseTiffReader {


    public static final int MC_BUILD_LIMIT = 319;

    public static final int MC_DIG_LIMIT = -64;

    public static final int MC_TOTAL_HEIGHT = 383;

    protected int heightScale;

    protected int heightOffset;

    public int noMapDataValue;

    public HeightMapReader(PluginConfig pluginConfig) {
        super(
                pluginConfig.getHeightMapPath(),
                pluginConfig.getMapScaleFactor(),
                pluginConfig.getMapSpawnLatitude(),
                pluginConfig.getMapSpawnLongitude()
        );
    }

    @Override
    public void init() {
        super.init(); // init tiff

        Util.log("Reading height information...");
        //read no map value
        Entry noDataValue = this.imageMetadata.getTIFFField(GDAL_NODATA_TAG);
        this.noMapDataValue = Integer.parseInt((String)noDataValue.getValue());

        float minHeight = Float.MAX_VALUE;
        float maxHeight = Float.MIN_VALUE;
        DataBuffer dataBuffer = this.raster.getDataBuffer();
        int size = dataBuffer.getSize();
        for(int i = 0; i< size; i++){
            float currentElevation = dataBuffer.getElemFloat(i);
            if(currentElevation != this.noMapDataValue){
                minHeight = Math.min(minHeight, currentElevation);
                maxHeight = Math.max(maxHeight, currentElevation);
            }
        }
        float diff = maxHeight - minHeight;

        Util.log(String.format(Locale.ENGLISH, "Map height min, max, difference[m]: %.1f, %.1f, %.1f ", minHeight, maxHeight, diff));
        int heightScale = (int)Math.ceil(diff / MC_TOTAL_HEIGHT);

        if(heightScale % 10 == 9){
            heightScale++;
        }else if(heightScale % 10 == 1){
            heightScale--;
        }
        this.heightScale = heightScale;
        Util.log(String.format(Locale.ENGLISH, "Using height scale 1:%d ", this.heightScale));

        int mapHighestBlock = Math.round(maxHeight / heightScale);
        if(mapHighestBlock > MC_BUILD_LIMIT) {
            this.heightOffset = mapHighestBlock - MC_BUILD_LIMIT;
            Util.log(String.format(Locale.ENGLISH, "Ortho map is too high for Minecraft. Setting offset to -%d blocks ", this.heightOffset));
        }

    }


    public Location getSpawn(World world){
        return new Location(
                world,
                0,
                this.getHeightForMcXZ(0, 0),
                0
        );
    }


    private int translateHeightToMc(float realHeight){
        if(realHeight == this.noMapDataValue){
            return this.noMapDataValue;
        }

        realHeight /= this.heightScale;

        realHeight -= this.heightOffset;

        return Math.round(realHeight);
    }


    public Location getMcLocationForLongLat(double longitude, double latitude, World world){
        int[] mcXZ = this.lngLatToMcXZ(longitude, latitude);

        return new Location(
                world,
                mcXZ[0],
                this.getHeightForMcXZ(mcXZ[0], mcXZ[1]),
                mcXZ[1]
        );
    }
    public Location getMcLocationForLongLat(double longitude, double latitude){
        return getMcLocationForLongLat(longitude, latitude, null);
    }

    private float getHeightFromMap(int x, int y){
        try {
            return this.raster.getSampleFloat(x, y, 0);
        }catch (ArrayIndexOutOfBoundsException ignored){
            return this.noMapDataValue;
        }
    }


    private int getHeightForRasterXY(int x, int y){
        return translateHeightToMc(
                this.getHeightFromMap(x, y)
        );
    }


    public int getHeightForMcXZ(int x, int z){
        int[] rasterXY = this.mcXZtoRasterXY(x, z);
        if(rasterXY == null){
            return this.noMapDataValue;
        }
        return getHeightForRasterXY(rasterXY[0], rasterXY[1]);
    }
    
    public int getHeightForMcLocation(Location location){
        return this.getHeightForMcXZ(location.getBlockX(), location.getBlockZ());
    }


    public float getTerrainRoughness(int xOrigin, int zOrigin){
        int[] heights = new int[25];
        float sum = 0;
        int i = 0;
        for(int z = 0; z < 5; z++){
            for(int x = 0; x < 5; x++){
                int currentHeight = getHeightForMcXZ(
                        xOrigin - 2 + x,
                        zOrigin - 2 + z
                );
                heights[i++] = currentHeight;
                sum += currentHeight;
            }
        }
        float average = sum / 25;
        float deviation = 0;
        for(i = 0; i < 25; i++){
            deviation += Math.abs(average - heights[i]);
        }
        return deviation;
    }

    public float getTerrainRoughness(Location location){
        return this.getTerrainRoughness(location.getBlockX(), location.getBlockZ());
    }


    public Location randomLocationOnMap(){

        double startLat = this.rasterOriginLatitude - this.pixelYScale * this.rasterHeight;
        double endLong = this.rasterOriginLongitude + this.pixelXScale * this.rasterWidth;

        Location location;
        do {

            double currentLat = Util.SECURE_RANDOM.nextDouble(startLat, this.rasterOriginLatitude);
            double currentLong = Util.SECURE_RANDOM.nextDouble(this.rasterOriginLongitude, endLong);
            location = this.getMcLocationForLongLat(currentLong, currentLat);
        }while (location.getY() == this.noMapDataValue);

        return location;
    }
    
}

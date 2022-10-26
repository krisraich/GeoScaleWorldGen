package at.kara.geoworldgen;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.plugins.tiff.CustomTIFFImageReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.World;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;

import javax.imageio.ImageIO;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * LAT = Y = Height = NORTH / SOUT (47)
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
@Log
@Getter
public class GeoTiffReader {

    /**
     * https://www.awaresystems.be/imaging/tiff/tifftags.html
     */
    public static final int MODEL_PIXEL_SCALE_TAG = 33550;
    public static final int MODEL_TIEPOINT_TAG = 33922;
    public static final int GDAL_NODATA_TAG = 42113;
    public static final int GEO_DOUBLE_PARAMS_TAG = 34736;

    public static final int MC_BUILD_LIMIT = 319;

    public static final int MC_DIG_LIMIT = -64;

    public static final int MC_TOTAL_HEIGHT = 383;


    private final GeodeticCalculator geoCalc = new GeodeticCalculator();

    private final GeoScaleWorldConfig geoScaleWorldConfig;

    private BufferedImage bufferedImage;


    /**
     * in MC Y -> Z
     */
    private int xOffset;
    private int zOffset;

    private int heightScale;

    private int heightOffset;

    private Raster raster;

    private Ellipsoid referenceEllipsoid;

    private int rasterWidth;

    private int rasterHeight;

    private double rasterLatitude;

    private double rasterLongitude;

    private GlobalPosition rasterOriginPosition;

    private double pixelXSize;

    private double pixelYSize;

    private double pixelXScale;

    private double pixelYScale;

    public float noMapDataValue;


    public GeoTiffReader(GeoScaleWorldConfig geoScaleWorldConfig) {
        this.geoScaleWorldConfig = geoScaleWorldConfig;
    }

    @SneakyThrows
    public void init() {
        this.geoScaleWorldConfig.info("Start initializing GeoTiffReader");
        File file = new File(this.geoScaleWorldConfig.getTifFileLocation());
        this.geoScaleWorldConfig.info("Opening Tiff file from location: " + file.getAbsolutePath());
        if(!file.exists()){
            throw new IOException("Can't find file!");
        }
        if(!file.canRead()){
            throw new IOException("Can't read file");
        }
        this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Tiff size: %.2f MiB. Start reading...", file.length() / 1048576.0));

        CustomTIFFImageReader imageReader = new CustomTIFFImageReader();
        try(ImageInputStream input = ImageIO.createImageInputStream(file)) {
            imageReader.setInput(input);
            this.bufferedImage = imageReader.read(0, new TIFFImageReadParam());
        }

        this.raster = bufferedImage.getRaster();
        this.rasterWidth = this.raster.getWidth();
        this.rasterHeight = this.raster.getHeight();

        this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Done: map width/height[px]: %d / %d", this.rasterWidth, this.rasterHeight));

        this.xOffset = this.rasterWidth / 2;
        this.zOffset = this.rasterHeight / 2;

        //read geo info
        TIFFImageMetadata imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(0);

        //get tie point
        Entry longLat = imageMetadata.getTIFFField(MODEL_TIEPOINT_TAG);
        double[] val = (double[])longLat.getValue();
        this.rasterLongitude = val[3];
        this.rasterLatitude = val[4];
        this.rasterOriginPosition = new GlobalPosition(this.rasterLatitude, this.rasterLongitude, 0);
        this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Map corner pos[lat,lon]: %f, %f ", this.rasterLatitude, this.rasterLongitude));

        //try read Ellipsoid
        try{
            Entry wsgType = imageMetadata.getTIFFField(GEO_DOUBLE_PARAMS_TAG);
            val= (double[]) wsgType.getValue();
            this.referenceEllipsoid = Ellipsoid.fromAAndInverseF(val[1], val[0]);
            this.geoScaleWorldConfig.info("Creating reference ellipsoid from meta data");
        }catch (Exception e){
            this.referenceEllipsoid = Ellipsoid.WGS84;
            this.geoScaleWorldConfig.info("Cant read reference Ellipsoid.. using WGS84");
        }

        this.geoScaleWorldConfig.info("Reading height information...");
        //read no map value
        Entry noDataValue = imageMetadata.getTIFFField(GDAL_NODATA_TAG);
        this.noMapDataValue = Float.parseFloat((String)noDataValue.getValue());

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

        this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Map height min, max, difference[m]: %.1f, %.1f, %.1f ", minHeight, maxHeight, diff));
        int heightScale = (int)Math.ceil(diff / MC_TOTAL_HEIGHT);

        if(heightScale % 10 == 9){
            heightScale++;
        }else if(heightScale % 10 == 1){
            heightScale--;
        }
        this.heightScale = heightScale;
        this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Using height scale 1:%d ", this.heightScale));

        int mapHighestBlock = Math.round(maxHeight / heightScale);
        if(mapHighestBlock > MC_BUILD_LIMIT) {
            this.heightOffset = mapHighestBlock - MC_BUILD_LIMIT;
            this.geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "Ortho map is too high for Minecraft. Setting offset to -%d blocks ", this.heightOffset));
        }

        //read pixel Scale
        Entry pixelScale = imageMetadata.getTIFFField(MODEL_PIXEL_SCALE_TAG);
        val= (double[]) pixelScale.getValue();
        this.pixelXScale = val[0];
        this.pixelYScale = val[1];

        //this is not constant...
        this.pixelXSize = this.geoCalc.calculateGeodeticMeasurement(
                this.referenceEllipsoid,
                rasterOriginPosition,
                new GlobalPosition(this.rasterLatitude, this.rasterLongitude + this.pixelXScale, 0)
        ).getPointToPointDistance();

        this.pixelYSize = this.geoCalc.calculateGeodeticMeasurement(
                this.referenceEllipsoid,
                rasterOriginPosition,
                new GlobalPosition(this.rasterLatitude + this.pixelYScale, this.rasterLongitude, 0)
        ).getPointToPointDistance();

        long areaScale = Math.round((this.pixelXSize + this.pixelYSize) / 2);
        geoScaleWorldConfig.info(String.format(
                Locale.ENGLISH,
                "Pixel size length/width[m]: %.3f / %.3f. Rounded size scale 1:%d", this.pixelXSize, this.pixelYSize, areaScale
        ));

        if(areaScale == this.heightScale){
            geoScaleWorldConfig.info("Height and size scale are identical. Nice! Creating perfect 1:" + areaScale + " model");
        }
        geoScaleWorldConfig.info("GeoTIFF successfully read");
    }

    public Location posToMcLocation(double longitude, double latitude){
        return this.posToMcLocation(longitude, latitude, null);
    }

    public Location posToMcLocation(double longitude, double latitude, World world){
        //get x
        double xDistance = this.geoCalc.calculateGeodeticMeasurement(
                referenceEllipsoid,
                rasterOriginPosition,
                new GlobalPosition(this.rasterLatitude, longitude, 0)
        ).getPointToPointDistance();

        //get y
        double yDistance = this.geoCalc.calculateGeodeticMeasurement(
                referenceEllipsoid,
                rasterOriginPosition,
                new GlobalPosition(latitude, this.rasterLongitude, 0)
        ).getPointToPointDistance();


        //scaleDistance
        xDistance /= this.pixelXSize;
        yDistance /= this.pixelYSize;

        //subtract mc offset
        int xCoord = (int) xDistance - this.xOffset;
        int zCoord = (int) yDistance - this.zOffset;

        return new Location(
                world,
                xCoord,
                this.getHeightForLocation(xCoord, zCoord),
                zCoord
        );
    }


    private double getHeightFromMap(int x, int y){
        try {
            return this.raster.getSampleFloat(x, y, 0);
        }catch (ArrayIndexOutOfBoundsException ignored){
            return this.noMapDataValue;
        }
    }

    public int getHeightForLocation(int x, int z){
        double height = this.getHeightFromMap(
                this.xOffset + x,
                this.zOffset + z
        );

        if(height == this.noMapDataValue){
            return (int)this.noMapDataValue;
        }

        height /= this.heightScale;

        height -= this.heightOffset;

        return (int)Math.round(height);
    }

    public int getHeightForLocation(Location location){
        return this.getHeightForLocation(location.getBlockX(), location.getBlockZ());
    }

}

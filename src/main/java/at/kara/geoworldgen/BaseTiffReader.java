package at.kara.geoworldgen;

import com.twelvemonkeys.imageio.metadata.Entry;
import com.twelvemonkeys.imageio.plugins.tiff.CustomTIFFImageReader;
import com.twelvemonkeys.imageio.plugins.tiff.TIFFImageMetadata;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.gavaghan.geodesy.*;

import javax.imageio.ImageIO;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * LAT = Y(Z) = Height = NORTH / SOUT (47)
 * LONG = X = Width = EAST / WEST (12)
 * Elevation = Z
 */
@Log
@Getter
public abstract class BaseTiffReader {

    /**
     * https://www.awaresystems.be/imaging/tiff/tifftags.html
     */
    public static final int MODEL_PIXEL_SCALE_TAG = 33550;
    public static final int MODEL_TIEPOINT_TAG = 33922;
    public static final int GDAL_NODATA_TAG = 42113;
    public static final int GEO_DOUBLE_PARAMS_TAG = 34736;


    protected final GeodeticCalculator geoCalc = new GeodeticCalculator();

    protected final String tiffLocation;
    protected final double centerLongitude;
    protected final double centerLatitude;
    protected final GlobalPosition centerPosition;
    protected final int mapScaleFactor;


    protected double rasterOriginLatitude;
    protected double rasterOriginLongitude;
    protected GlobalPosition rasterOriginPosition;

    protected BufferedImage bufferedImage;

    protected Raster raster;

    protected Ellipsoid referenceEllipsoid;

    protected TIFFImageMetadata imageMetadata;

    protected int rasterWidth;

    protected int rasterHeight;

    protected double pixelXScale;

    protected double pixelYScale;

    public BaseTiffReader(String tiffLocation, int mapScaleFactor, double centerLatitude, double centerLongitude) {
        this.tiffLocation = tiffLocation;
        this.mapScaleFactor = mapScaleFactor;
        this.centerLongitude = centerLongitude;
        this.centerLatitude = centerLatitude;
        this.centerPosition = new GlobalPosition(this.centerLatitude, this.centerLongitude , 0);
    }

    @SneakyThrows
    public void init() {
        Util.log("Start initializing GeoTiffReader");
        File file = new File(this.tiffLocation);
        Util.log("Opening Tiff file from location: " + file.getAbsolutePath());
        if(!file.exists()){
            throw new IOException("Can't find file!");
        }
        if(!file.canRead()){
            throw new IOException("Can't read file!");
        }
        Util.log(String.format(Locale.ENGLISH, "Tiff size: %.2f MiB. Start reading...", file.length() / 1048576.0));

        CustomTIFFImageReader imageReader = new CustomTIFFImageReader();
        try(ImageInputStream input = ImageIO.createImageInputStream(file)) {
            imageReader.setInput(input);
            this.bufferedImage = imageReader.read(0, new TIFFImageReadParam());
        }

        this.raster = bufferedImage.getRaster();
        this.rasterWidth = this.raster.getWidth();
        this.rasterHeight = this.raster.getHeight();


        Util.log(String.format(Locale.ENGLISH, "Done: map width/height[px]: %d / %d", this.raster.getWidth(), this.raster.getHeight()));

        //read geo info
        this.imageMetadata = (TIFFImageMetadata) imageReader.getImageMetadata(0);

        //get tie point
        Entry longLat = this.imageMetadata.getTIFFField(MODEL_TIEPOINT_TAG);
        double[] val = (double[])longLat.getValue();
        this.rasterOriginLongitude = val[3];
        this.rasterOriginLatitude = val[4];
        this.rasterOriginPosition = new GlobalPosition(this.rasterOriginLatitude, this.rasterOriginLongitude, 0);
        Util.log(String.format(Locale.ENGLISH, "Map corner pos[lat,lon]: %f, %f ", this.rasterOriginLatitude, this.rasterOriginLongitude));

        //try read Ellipsoid
        try{
            Entry wsgType = this.imageMetadata.getTIFFField(GEO_DOUBLE_PARAMS_TAG);
            val= (double[]) wsgType.getValue();
            this.referenceEllipsoid = Ellipsoid.fromAAndInverseF(val[1], val[0]);
            Util.log("Creating reference ellipsoid from meta data");
        }catch (Exception e){
            this.referenceEllipsoid = Ellipsoid.WGS84;
            Util.log("Cant read reference Ellipsoid.. using WGS84");
        }

        //read pixel Scale
        Entry pixelScale = this.imageMetadata.getTIFFField(MODEL_PIXEL_SCALE_TAG);
        val= (double[]) pixelScale.getValue();
        this.pixelXScale = val[0];
        this.pixelYScale = val[1];

        Util.log("GeoTIFF successfully read");
    }


    protected int[] lngLatToRasterXY(double longitude, double latitude){
        //if out of map, return null
        if(latitude > this.rasterOriginLatitude || longitude < this.rasterOriginLongitude){
            return null;
        }

        return new int[]{
                (int) ((longitude - this.rasterOriginLongitude) / this.pixelXScale),
                (int) ((this.rasterOriginLatitude - latitude) / this.pixelYScale)
        };
    }



    public int[] lngLatToMcXZ(double longitude, double latitude){
        GeodeticCurve geodeticCurve = this.geoCalc.calculateGeodeticCurve(
                this.referenceEllipsoid,
                this.centerPosition,
                new GlobalPosition(latitude, longitude, 0)
        );

        double distance = geodeticCurve.getEllipsoidalDistance() / this.mapScaleFactor;
        double azimuth =  geodeticCurve.getAzimuth();

        //convert azimuth to +/- Pi
        if(azimuth > 180){
            azimuth -= 360;
        }
        azimuth = Math.toRadians(azimuth);

        //convert to XZ
        return new int[]{
                (int) (Math.sin(azimuth) * distance),
                (int) (Math.cos(azimuth) * -distance)
        };
    }


    /**
     * nort: z- -> lat+
     * sout: z+ -> lat-
     * east: x+ -> lng+
     * west: x- -> lng-
     *
     * @param x
     * @param z
     * @return
     */
    public double[] mcXZtoLngLat(int x, int z){
        double distanceFromCenterMeter = Math.sqrt(x*x + z*z) * this.mapScaleFactor;

        //convert to bearing
        double bearing = Math.toDegrees(Math.atan2(x, -z));
        if(bearing < 0.0) {
            bearing += 360.0;
        }
        GlobalCoordinates coordinates = this.geoCalc.calculateEndingGlobalCoordinates(
                this.referenceEllipsoid,
                this.centerPosition,
                bearing,
                distanceFromCenterMeter
        );
        return new double[]{
                coordinates.getLongitude(),
                coordinates.getLatitude()
        };

    }

    public int[] mcXZtoRasterXY(int x, int z){
        double[] lngLat = mcXZtoLngLat(x, z);
        if(lngLat == null){
            return null;
        }
        return lngLatToRasterXY(lngLat[0], lngLat[1]);

    }


}

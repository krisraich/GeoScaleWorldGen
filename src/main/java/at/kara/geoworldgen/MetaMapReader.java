package at.kara.geoworldgen;

public class MetaMapReader extends BaseTiffReader{


    public enum TerrainType{
        NO_DATA,
        WATER,
        FOREST
    }

    private static int[] NO_DATA_PIXEL = new int[]{
            255,
            255,
            255
    };

    public MetaMapReader(PluginConfig pluginConfig) {
        super(
                pluginConfig.getMetaMapPath(),
                pluginConfig.getMapScaleFactor(),
                pluginConfig.getMapSpawnLatitude(),
                pluginConfig.getMapSpawnLongitude()
        );
    }

    @Override
    public void init() {
        super.init(); // init tiff

        Util.log("Reading meta information...");
    }

    private int[] getDataFromMap(int x, int y){
        try {
            return this.raster.getPixel(x, y, (int[])null);
        }catch (ArrayIndexOutOfBoundsException ignored){
            return NO_DATA_PIXEL;
        }
    }

    public TerrainType getTypeForLocation(int x, int z){

        int[] xy = this.mcXZtoRasterXY(x, z);

        int[] pixel = this.getDataFromMap(
                xy[0],
                xy[1]
        );

        //pixel is R G B

        if( (pixel[0] & pixel[1] & pixel[2]) == 255){
            return TerrainType.NO_DATA;
        }

        //check if pixel is mostly green
        if(pixel[1] > pixel[0] && pixel[1] > pixel[2]){
            return TerrainType.FOREST;
        }
        //check if pixel is mostly blue
        if(pixel[2] > pixel[0] && pixel[2] > pixel[1]){
            return TerrainType.WATER;
        }

        //red is not used currently, so return no data
        return TerrainType.NO_DATA;
    }

    /**
     * Applies image smoothing to terrain, for better water
     * todo: implement a Custom Kernel (maybe Gauss)
     * @param mcX
     * @param mcZ
     * @param gridSize
     * @return
     */
    public float applyFilterToWaterMap(int mcX, int mcZ, int gridSize){

        if(gridSize % 2 != 1){
            throw new RuntimeException("grid is not of uneven length");
        }

        int kernelSize = gridSize * gridSize;

        double smoothFactor = 1.0 / kernelSize;

        int centerOffset = gridSize / 2;

        float sum = 0;
        for(int z = 0; z < gridSize; z++){
            for(int x = 0; x < gridSize; x++){
                if(getTypeForLocation(
                        mcX - centerOffset + x,
                        mcZ - centerOffset + z
                ) == TerrainType.WATER){
                    sum += smoothFactor;
                }
            }
        }

        return sum;
    }


}

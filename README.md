# GeoScaleWorldGen

## Installation
1. Download the `DGM 10m Tirol (EPSG:31254)` height model from [https://www.data.gv.at/katalog/dataset/0454f5f3-1d8c-464e-847d-541901eb021a](https://www.data.gv.at/katalog/dataset/0454f5f3-1d8c-464e-847d-541901eb021a)

2. Convert the GeoTiff from `EPSG:31254` to `EPSG4326 WSG84` using [QGIS](https://www.qgis.org/de/site/)

3. Add an API Key from [geoapify.com](https://www.geoapify.com/get-started-with-maps-api) to `config.yml`

4. Build with `mvn clean package`. It should automatically run the unit tests. If tests are successful:

5. Run plugin in a `spigot-1.18.2.jar` server

## Example
![MC vs RL](img.png?raw=true "MC vs RL")
<sup><sub>Image by Google Earth</sub></sup>

## ToDo:
- Implement water (from GeoTif)
- Implement forest (from GeoTif)
- Generate Biomes (from GeoTif?)
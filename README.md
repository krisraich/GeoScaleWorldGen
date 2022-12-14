# GeoScaleWorldGen

## Installation
1. Download `DGM 10m Tirol (EPSG:31254)` height model from [https://www.data.gv.at/katalog/dataset/0454f5f3-1d8c-464e-847d-541901eb021a](https://www.data.gv.at/katalog/dataset/0454f5f3-1d8c-464e-847d-541901eb021a)

2. Convert GeoTiff from `EPSG:31254` to `EPSG:4326 WSG84` using [QGIS](https://www.qgis.org/de/site/)

3. Add API Key from [geoapify.com](https://www.geoapify.com/get-started-with-maps-api) to `config.yml`

4. Build with `mvn clean package`. It should automatically run the unit tests. If tests are successful:

5. Run plugin with a `spigot-1.18.2.jar` server

## Example
![MC vs RL](img.png?raw=true "MC vs RL")
<sup><sub>Image by Google Earth</sub></sup>
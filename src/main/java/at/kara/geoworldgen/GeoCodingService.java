package at.kara.geoworldgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Log
public class GeoCodingService implements CommandExecutor {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String API_URL = "https://api.geoapify.com/v1/geocode/search?apiKey=%s&text=%s";


    public abstract class BaseTeleportationTask extends BukkitRunnable {

        protected final Player player;
        protected final String[] args;

        @Getter
        protected Location result;

        private BaseTeleportationTask(Player player, String[] args) {
            this.player = player;
            this.args = args;
        }

        protected void teleportPlayer(double latitude, double longitude){
            geoScaleWorldConfig.info(String.format(Locale.ENGLISH, "resolved request to: %f, %f", latitude, longitude));

            //check if inside of map
            result = geoTiffReader.posToMcLocation(longitude, latitude);

            geoScaleWorldConfig.info(String.format("mc pos: %d %d %d", (int)result.getX(), (int)result.getY(), (int)result.getZ()));

            if(result.getY() == geoTiffReader.noMapDataValue){
                geoScaleWorldConfig.info("cant teleport, outside of map");
                if(player != null){
                    player.sendMessage("Location outside of map!");
                }
            }else {
                teleportPlayer();
            }
        }

        protected void teleportPlayer(){
            geoScaleWorldConfig.info("start teleporting");
            if(player != null){
                //set player heading north
                result.setPitch(0);
                result.setYaw(180);
                result.setWorld(player.getWorld());
                player.sendMessage("Teleporting... Energize!");
                player.teleport(result);
            }
        }
    }

    public class TeleportToCoordinatesTask extends BaseTeleportationTask {

        private TeleportToCoordinatesTask(Player player, String[] args) {
            super(player, args);
        }

        @SneakyThrows
        @Override
        public void run() {
            double latitude;
            double longitude;

            if(args.length < 2){
                geoScaleWorldConfig.info("no args given");
                if(player != null){
                    player.sendMessage("Use /tpc 0.00000 0.00000");
                }
                return;
            }

            //remove comma
            String latStr = args[0];
            if(',' == latStr.charAt(latStr.length()-1)){
                latStr = latStr.substring(0, latStr.length()-2);
            }

            try{
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(args[1]);
            }catch (NumberFormatException e){
                geoScaleWorldConfig.info("cant parse latitude or longitude");
                if(player != null){
                    player.sendMessage("Invalid latitude and/or longitude");
                }
                return;
            }

            teleportPlayer(latitude, longitude);
        }
    }

    public class TeleportToNameTask extends BaseTeleportationTask {

        private TeleportToNameTask(Player player, String[] args) {
            super(player, args);
        }

        @SneakyThrows
        @Override
        public void run() {

            String targetLocation;

            if(args.length < 1){
                geoScaleWorldConfig.info("no args given");
                if(player != null){
                    player.sendMessage("Use /tpc location Name");
                }
                return;
            }
            targetLocation = String.join(" ", args);

            if(player != null){
                player.sendMessage("Processing teleportation to: " + targetLocation);
            }
            geoScaleWorldConfig.info("start resolving name query: " + targetLocation);
            URL url = new URL(String.format(
                    API_URL,
                    geoScaleWorldConfig.getGeoCodingApiKey(),
                    URLEncoder.encode(targetLocation + geoScaleWorldConfig.teleportationSuffix, StandardCharsets.UTF_8)
            ));

            geoScaleWorldConfig.info("sending request: " + url);

            HttpURLConnection http = (HttpURLConnection)url.openConnection();
            http.setRequestProperty("Accept", "application/json");

            if(http.getResponseCode() != 200){
                throw new Exception(http.getResponseMessage());
            }

            InputStream httpInputStream = http.getInputStream();

            //todo: create proper model
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Map<String, ArrayList<Double>>>>> map = OBJECT_MAPPER.readValue(httpInputStream, Map.class);
            List<Map<String, Map<String, ArrayList<Double>>>> features = map.get("features");

            double minBoxSize = Double.MAX_VALUE;
            Map<String, Map<String, ArrayList<Double>>> currentSmallestFeature = null;
            for(Map<String, Map<String, ArrayList<Double>>> feature : features){
                ArrayList<Double> bboxList = (ArrayList)feature.get("bbox");
                if(bboxList == null){ // location is precise
                    currentSmallestFeature = feature;
                    break;
                }
                double boxSize = (Math.abs(bboxList.get(0) - bboxList.get(2)) * Math.abs(bboxList.get(1) - bboxList.get(3))) / 2;
                if(boxSize < minBoxSize){
                    currentSmallestFeature = feature;
                    minBoxSize = boxSize;
                }
            }

            ArrayList<Double> coordinates = currentSmallestFeature.get("geometry").get("coordinates");

            httpInputStream.close();
            http.disconnect();

            double longitude = coordinates.get(0);
            double latitude = coordinates.get(1);

            teleportPlayer(latitude, longitude);

        }
    }

    public class RandomTeleportTask extends BaseTeleportationTask {

        private RandomTeleportTask(Player player) {
            super(player, null);
        }

        @Override
        public void run() {
           this.result =  geoTiffReader.randomLocationOnMap();
           this.teleportPlayer();
        }
    }

    private final Plugin plugin;

    private final GeoScaleWorldConfig geoScaleWorldConfig;

    private final GeoTiffReader geoTiffReader;


    public GeoCodingService(Plugin plugin, GeoScaleWorldConfig geoScaleWorldConfig, GeoTiffReader geoTiffReader) {
        this.plugin = plugin;
        this.geoScaleWorldConfig = geoScaleWorldConfig;
        this.geoTiffReader = geoTiffReader;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player player){
            BukkitRunnable bukkitRunnable;
            switch (command.getName()){
                case "tpl":
                    bukkitRunnable = getTeleportToNameTask(player, args);
                    break;
                case "tpc":
                    bukkitRunnable = getTeleportToCoordinatesTask(player, args);
                    break;
                case "tpr":
                    bukkitRunnable = getRandomTeleportTask(player);
                    break;
                default:
                    return false;
            }
            bukkitRunnable.runTask(plugin);
        }
        return true;
    }

    public TeleportToNameTask getTeleportToNameTask(Player player, String[] args){
        return new TeleportToNameTask(player, args);
    }

    public TeleportToCoordinatesTask getTeleportToCoordinatesTask(Player player, String[] args){
        return new TeleportToCoordinatesTask(player, args);
    }

    public RandomTeleportTask getRandomTeleportTask(Player player){
        return new RandomTeleportTask(player);
    }


}

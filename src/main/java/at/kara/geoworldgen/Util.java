package at.kara.geoworldgen;

import org.bukkit.plugin.PluginLogger;

import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;

public class Util {

    public static PluginLogger logger;

    public static final Random SECURE_RANDOM = new SecureRandom();


    public static void log(String message){
        if(logger == null) {
            System.out.println("[INFO] " + message);
        }else {
            logger.log(Level.INFO, message);
        }
    }


}

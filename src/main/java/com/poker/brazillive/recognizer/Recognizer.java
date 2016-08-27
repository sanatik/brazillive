/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.recognizer;

import com.poker.brazillive.config.ConfigParser;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author boson
 */
public class Recognizer {

    private static final Logger LOGGER = Logger.getLogger(Recognizer.class.getName());

    public static List<Integer> recognizeFreePlaces(ConfigParser config, BufferedImage img) {
        try {
            List<Integer> freePlaces = new ArrayList<>();
            RecognizeBoolean rb = new RecognizeBoolean(
                    config.getCommonConfigs().get("table.freePlaceProps"));
            boolean[] free = rb.recognize(img, null);
            for (int i = 0; i < free.length; i++) {
                if (free[i]) {
                    freePlaces.add(i);
                }
            }
            return freePlaces;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return new ArrayList<>();
    }

    public static boolean isMyTurn(ConfigParser config, BufferedImage img) {
        if (img == null) {
            img = testImage();
        }
        try {
            boolean myTurn = true;
            RecognizeBoolean rb = new RecognizeBoolean(
                    config.getCommonConfigs().get("table.myTurnProps"));
            boolean[] buttons = rb.recognize(img, null);
            for (int i = 0; i < buttons.length; i++) {
                if (!buttons[i]) {
                    myTurn = false;
                }
            }
            return myTurn;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }

        return false;
    }

    public static boolean[] doPlayersHaveCards(ConfigParser config, BufferedImage img) {
        if (img == null) {
            img = testImage();
        }
        try {
            RecognizeBoolean rb = new RecognizeBoolean(
                    config.getCommonConfigs().get("table.playersHaveCardsProps"));
            List<Double> ratios = new ArrayList<>();
            boolean[] doPlayersHaveCards = rb.recognize(img, ratios);
            printTestInfo(ratios, doPlayersHaveCards);
            return doPlayersHaveCards;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        return new boolean[0];
    }

    private static BufferedImage testImage() {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File("test/imgs/20160730/132851_0x008B08D2.bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    private static void printTestInfo(List<Double> ratios, boolean[] result) {
        LOGGER.log(Level.INFO, Arrays.toString(ratios.toArray()));
        LOGGER.log(Level.INFO, Arrays.toString(result));
    }
}

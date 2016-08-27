/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.capture;

import com.poker.brazillive.config.ConfigParser;
import com.poker.brazillive.shell.Shell;
import com.poker.brazillive.windows.WindowInfo;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author boson
 */
public class CaptureMaker {

    private static final Logger LOGGER = Logger.getLogger(CaptureMaker.class.getName());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConfigParser config;
    private final Shell shell;
    private final List<WindowInfo> clients;

    public CaptureMaker(ConfigParser config, Shell shell, List<WindowInfo> client) {
        this.config = config;
        this.shell = shell;
        this.clients = client;
    }

    public void startMakeCaptures() {

        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (WindowInfo client : clients) {
                    BufferedImage bi = Capture.capture(client, shell);
                    if (config.getCaptureConfig().isSave() && !client.isLobby()) {
                        Capture.saveCapture("bmp", client, bi, config.getCaptureConfig().getFolder());
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void shutdown() {
        System.out.println("shutdown...");
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}

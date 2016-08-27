/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive;

import com.poker.brazillive.capture.CaptureMaker;
import com.poker.brazillive.config.ConfigParser;
import com.poker.brazillive.recognizer.Recognizer;
import com.poker.brazillive.shell.Shell;
import com.poker.brazillive.shell.rec.RecText;
import com.poker.brazillive.windows.ClientLauncher;
import com.poker.brazillive.windows.WindowInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author boson
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        try {
            ConfigParser config = new ConfigParser("project.conf", args);
            Shell shell = new Shell(config.getAhkConfig());
            List<WindowInfo> tables = new ClientLauncher(config, shell).launch();
            if (tables != null && tables.size() > 0) {
                CaptureMaker captureMaker = new CaptureMaker(config, shell, tables);
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        captureMaker.shutdown();
                    }
                });
                captureMaker.startMakeCaptures();
                LOGGER.info("Capture making started");
            }
//            RecText.test();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}

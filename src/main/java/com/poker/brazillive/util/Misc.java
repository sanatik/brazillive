/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author boson
 */
public class Misc {

    private static final Logger LOGGER = Logger.getLogger(Misc.class.getName());

    public static void sysCall(String cmd) throws Exception {
        long st = getTime();
        Runtime run = Runtime.getRuntime();
        File dir = new File(new java.io.File(".").getCanonicalPath());
        Process pp = run.exec(cmd, null, dir);
        BufferedReader err;
        try (BufferedReader out = new BufferedReader(new InputStreamReader(pp.getInputStream()))) {
            err = new BufferedReader(new InputStreamReader(pp.getErrorStream()));
            String line;
            while ((line = out.readLine()) != null) {
                LOGGER.log(Level.INFO, line);
            }

            while ((line = err.readLine()) != null) {
                LOGGER.log(Level.WARNING, line);
            }

            int exitVal = pp.waitFor();

            if (exitVal != 0) {
                LOGGER.log(Level.WARNING, "Process end with exit number {0}", exitVal);
            }
            long t = getTime() - st;
            LOGGER.log(Level.INFO, "Time to run ahk script " + cmd + " is {0}", t);
        }
        err.close();
    }

    public static long getTime() {
        return System.currentTimeMillis() /*- 1000*60*60*/;
    }
}

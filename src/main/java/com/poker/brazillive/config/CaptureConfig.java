/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.config;

/**
 *
 * @author boson
 */
public class CaptureConfig {

    private boolean save;
    private String folder;

    public CaptureConfig(boolean save, String folder) {
        this.save = save;
        this.folder = folder;
    }

    public boolean isSave() {
        return save;
    }

    public void setSave(boolean save) {
        this.save = save;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

}

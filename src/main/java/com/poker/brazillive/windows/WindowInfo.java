/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.windows;

import java.awt.Rectangle;

/**
 *
 * @author boson
 */
public class WindowInfo {
    /**
     * There is hack with client window size
     * MIN_WIDTH - client width cannot be less than this value
     * MIN_HEIGHT - client height cannot be less than this value
     * WIDTH_DIFF - when capture taken, the width somehow is less than width from config for this value
     * HEIGHT_DIFF - when capture taken, the height somehow is less than height from config for this value
     */
    public static final int MIN_WIDTH = 800;
    public static final int MIN_HEIGHT = 600;
    public static final int WIDTH_DIFF = 16;
    public static final int HEIGHT_DIFF = 59;
    
    private final Rectangle rectangle;
    
    private int hwnd;
    private Rect rect;
    private String title;
    private boolean lobby;

    public WindowInfo(int hwnd, Rect rect, String title, Rectangle rectangle, boolean isLobby) {
        this.hwnd = hwnd;
        this.rect = rect;
        this.title = title;
        this.rectangle = rectangle;
        this.lobby = isLobby;
    }

    public int getHwnd() {
        return hwnd;
    }

    public void setHwnd(int hwnd) {
        this.hwnd = hwnd;
    }

    public Rect getRect() {
        return rect;
    }

    public void setRect(Rect rect) {
        this.rect = rect;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getWidth() {
        return rectangle.getWidth();
    }

    public double getHeight() {
        return rectangle.getHeight();
    }

    public Rectangle getRectangle() {
        return this.rectangle;
    }

    public boolean isLobby() {
        return lobby;
    }

    public void setLobby(boolean lobby) {
        this.lobby = lobby;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)-(%d,%d) : \"%s\"(%s) ",
                rect.left, rect.top, rect.right, rect.bottom, title, hwnd);
    }
}

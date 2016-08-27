/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.windows;

import com.sun.jna.win32.StdCallLibrary;

/**
 *
 * @author boson
 */
public interface WndEnumProc extends StdCallLibrary.StdCallCallback {

    boolean callback(int hWnd, int lParam);

}

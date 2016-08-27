/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.windows;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

/**
 *
 * @author boson
 */
public interface User32 extends StdCallLibrary {

    final User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

    boolean EnumWindows(WndEnumProc wndenumproc, int lParam);

    boolean IsWindowVisible(int hWnd);

    int GetWindowRect(int hWnd, Rect r);

    void GetWindowTextA(int hWnd, byte[] buffer, int buflen);

    int GetTopWindow(int hWnd);

    int GetWindow(int hWnd, int flag);
    final int GW_HWNDNEXT = 2;
}

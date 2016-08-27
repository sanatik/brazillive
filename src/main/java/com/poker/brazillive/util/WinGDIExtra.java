/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.util;

import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinGDI;

public interface WinGDIExtra extends WinGDI {

    public DWORD SRCCOPY = new DWORD(0x00CC0020);

}

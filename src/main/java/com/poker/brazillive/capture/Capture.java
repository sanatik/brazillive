/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.capture;

import com.poker.brazillive.shell.Shell;
import com.poker.brazillive.util.GDI32Extra;
import com.poker.brazillive.util.User32Extra;
import com.poker.brazillive.util.WinGDIExtra;
import com.poker.brazillive.windows.WindowInfo;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author boson
 */
public class Capture {

    private static final Logger LOGGER = Logger.getLogger(Capture.class.getName());

    public static BufferedImage capture(WindowInfo client, Shell shell) throws Exception {
        WinDef.HWND hWnd = handleToHwnd(Integer.toHexString(client.getHwnd()));
        WinDef.HDC hdcWindow = com.sun.jna.platform.win32.User32.INSTANCE.GetDC(hWnd);

        WinDef.HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

        WinDef.RECT bounds = new WinDef.RECT();
        User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        if (width == 0 || height == 0) {
            shell.activate(client.getHwnd());
            LOGGER.log(Level.FINE, "ACTIVATED");
            bounds = new WinDef.RECT();
            User32Extra.INSTANCE.GetClientRect(hWnd, bounds);
            width = bounds.right - bounds.left;
            height = bounds.bottom - bounds.top;
        }
        WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);

        WinNT.HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
        GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY);

        GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
        GDI32.INSTANCE.DeleteDC(hdcMemDC);

        WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
        bmi.bmiHeader.biWidth = width;
        bmi.bmiHeader.biHeight = -height;
        bmi.bmiHeader.biPlanes = 1;
        bmi.bmiHeader.biBitCount = 32;
        bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

        Memory buffer = new Memory(width * height * 4);
        GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);

        GDI32.INSTANCE.DeleteObject(hBitmap);
        com.sun.jna.platform.win32.User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

        return image;

    }

    private static WinDef.HWND handleToHwnd(String handle) {
        WinDef.HWND hWnd = null;
        try {
            if (handle != null) {
                if (handle.startsWith("0x")) {
                    handle = handle.substring(2);
                }
                hWnd = new WinDef.HWND(Pointer.createConstant(Long.parseLong(handle,
                        16)));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        return hWnd;
    }

    private WinDef.HWND getHwndByTitle(String title) {
        WinDef.HWND hWnd = com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, title);
        return hWnd;
    }

    public static void saveCapture(String format, WindowInfo pokerClient,
            BufferedImage bi, String folder) throws Exception {

        File imgFolder = new File(String.format("%s", folder));
        imgFolder.mkdirs();

        long wndTime = new Date().getTime();
        String df = new SimpleDateFormat("yyyyMMdd").format(new Date(wndTime));
        File wndFolder = new File(imgFolder.getAbsolutePath() + "/" + df);
        wndFolder.mkdirs();

        String timeCreatedFile = new SimpleDateFormat("HHmmss").format(new Date(wndTime));
        File imgFile = new File(wndFolder.getAbsolutePath() + "/"
                + String.format("%s_0x%08X.%s", timeCreatedFile, pokerClient.getHwnd(), format));
        ImageIO.write(bi, format, imgFile);
    }
}

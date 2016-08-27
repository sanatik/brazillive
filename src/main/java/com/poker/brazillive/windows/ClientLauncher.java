/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.windows;

import com.poker.brazillive.capture.Capture;
import com.poker.brazillive.config.ButtonConfig;
import com.poker.brazillive.config.ConfigParser;
import com.poker.brazillive.shell.Shell;
import com.poker.brazillive.util.GDI32Extra;
import com.poker.brazillive.util.User32Extra;
import com.poker.brazillive.util.WinGDIExtra;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author boson
 */
public class ClientLauncher {

    private static final Logger LOGGER = Logger.getLogger(ClientLauncher.class.getName());

    private final ConfigParser config;
    private final Shell shell;

    public ClientLauncher(ConfigParser config, Shell shell) {
        this.config = config;
        this.shell = shell;
    }

    public List<WindowInfo> launch() {
        try {
            WindowInfo lobby = recognizeOpenedWindow(config.getCommonConfigs().get("launcher.lobbyWindowTitle"), true).get(0);
            if (lobby == null) {
                lobby = openClient();
            }
            resizeWindowIfNeeded(lobby);
            login(lobby);
            setFilters(lobby);
            //TODO Open Table from Lobby
            List<WindowInfo> tables = recognizeOpenedWindow(
                    config.getCommonConfigs().get("launcher.tableWindowTitle"), false);
            for (WindowInfo table : tables) {
                resizeWindowIfNeeded(table);
            }
            tables.add(lobby);
            return tables;
        } catch (IOException | InterruptedException | NullPointerException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        return null;
    }

    private boolean isLoggedIn(WindowInfo client) {

        return client.getTitle().contains("Nickname:");
    }

    private WindowInfo openClient() throws IOException, InterruptedException, NullPointerException {

        int i = 0;
        WindowInfo pokerClient = null;
        /**
         * Trying to run client 3 times and recognize it. if not return null If
         * not return null and throw NPE to stop program
         */
        while (pokerClient == null && i < 3) {
            runClient();
            Thread.sleep(30000);
            pokerClient = recognizeOpenedWindow(config.getCommonConfigs().get("launcher.lobbyWindowTitle"), true).get(0);
            i++;
        }
        if (pokerClient == null) {
            throw new NullPointerException("Not possible to run client");
        }
        return pokerClient;
    }

    private List<WindowInfo> recognizeOpenedWindow(String windowTitle, boolean isLobby) {

        final List<WindowInfo> clients = new ArrayList<>();
        final List<Integer> order = new ArrayList<>();
        int top = User32.INSTANCE.GetTopWindow(0);
        while (top != 0) {
            order.add(top);
            top = User32.INSTANCE.GetWindow(top, User32.GW_HWNDNEXT);
        }
        User32.INSTANCE.EnumWindows((int hWnd, int lParam) -> {
            Rect r = new Rect();
            User32.INSTANCE.GetWindowRect(hWnd, r);
            byte[] buffer = new byte[1024];
            User32.INSTANCE.GetWindowTextA(hWnd, buffer, buffer.length);
            String title = Native.toString(buffer);
            if (title.contains(windowTitle)) {
                clients.add(new WindowInfo(hWnd, r, title,
                        new Rectangle(r.left, r.top,
                                Math.abs(r.right - r.left),
                                Math.abs(r.bottom - r.top)), isLobby));
            }
            return true;
        }, 0);
        Collections.sort(clients, (WindowInfo o1, WindowInfo o2)
                -> order.indexOf(o1.getHwnd()) - order.indexOf(o2.getHwnd()));
        return clients;
    }

    private void runClient() throws IOException {

        new ProcessBuilder(config.getCommonConfigs().get("launcher.path")).start();
        LOGGER.log(Level.FINE, "Client run");

    }

    private void login(WindowInfo pokerClient) throws Exception {
        if (!isLoggedIn(pokerClient)) {
            ButtonConfig loginButton = config.getButtonConfig("login");
            ButtonConfig loginNamePosition = config.getButtonConfig("login_name");
            ButtonConfig loginPwdPosition = config.getButtonConfig("login_pwd");
            shell.click(pokerClient.getHwnd(),
                    loginNamePosition.getX(), loginNamePosition.getY(),
                    loginNamePosition.getCount(), 1);
            shell.raise(pokerClient.getHwnd(),
                    loginNamePosition.getX(), loginNamePosition.getY(),
                    config.getCommonConfigs().get("account.name"));
            Thread.sleep(500);
            shell.click(pokerClient.getHwnd(),
                    loginPwdPosition.getX(), loginPwdPosition.getY(),
                    loginPwdPosition.getCount(), 1);
            shell.raise(pokerClient.getHwnd(),
                    loginPwdPosition.getX(), loginPwdPosition.getY(),
                    config.getCommonConfigs().get("account.password"));
            Thread.sleep(500);
            shell.click(pokerClient.getHwnd(),
                    loginButton.getX(), loginButton.getY(),
                    loginButton.getCount(), 1);
            Thread.sleep(10000);
        }
    }

    private void resizeWindowIfNeeded(WindowInfo pokerClient) throws Exception {
        if (checkResizeNeeded(Capture.capture(pokerClient, shell))) {
            shell.resize(pokerClient.getHwnd(),
                    config.getClientSizeConfig().getWidth(),
                    config.getClientSizeConfig().getHeight());
            LOGGER.log(Level.FINE, "RESIZED");
        }

    }

    private boolean checkResizeNeeded(BufferedImage bi) {
        if (config.getClientSizeConfig().getWidth() <= WindowInfo.MIN_WIDTH) {
            if (bi.getWidth() != WindowInfo.MIN_WIDTH) {
                return true;
            }
        } else if ((config.getClientSizeConfig().getWidth() - WindowInfo.WIDTH_DIFF)
                != bi.getWidth()) { //hack description in WindowInfo constant comment
            return true;
        }
        if (config.getClientSizeConfig().getHeight() <= WindowInfo.MIN_HEIGHT) {
            if (bi.getHeight() != WindowInfo.MIN_HEIGHT) {
                return true;
            }
        } else if ((config.getClientSizeConfig().getHeight() - WindowInfo.HEIGHT_DIFF)
                != bi.getHeight()) { //hack description in WindowInfo constant comment
            return true;
        }
        return false;
    }

    private void setFilters(WindowInfo client) throws Exception {
        setFilterByName(client, "filter_no_limit");
        setFilterByName(client, "filter_low");
        setFilterByName(client, "filter_currency");
        setFilterByName(client, "filter_currency_" + config.getCommonConfigs().get("account.currency"));
    }

    private void setFilterByName(WindowInfo client, String filterName) throws Exception {

        ButtonConfig filter = config.getButtonConfig(filterName);
        shell.click(client.getHwnd(), filter.getX(), filter.getY(), filter.getCount(), 1);
    }
}

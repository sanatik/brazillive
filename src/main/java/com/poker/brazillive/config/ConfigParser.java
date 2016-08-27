/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.poker.brazillive.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author boson
 */
public class ConfigParser {

    private static final Logger LOGGER = Logger.getLogger(ConfigParser.class.getName());

    private final Config config;
    private final List<String> configNames = Arrays.asList(
            "launcher.path",
            "launcher.lobbyWindowTitle",
            "launcher.tableWindowTitle",
            "account.name",
            "account.password",
            "account.currency",
            "table.freePlaceProps",
            "table.myTurnProps",
            "table.playersHaveCardsProps"
    );
    private final List<String> buttonNames = Arrays.asList(
            "login",
            "filter_no_limit",
            "filter_low",
            "login_name",
            "login_pwd",
            "filter_currency",
            "filter_currency_all",
            "filter_currency_chp",
            "filter_currency_fbr",
            "filter_currency_fus"
    );
    private final Map<String, String> commonConfigs;
    private final Map<String, ButtonConfig> buttonConfigs;
    private AhkConfig ahkConfig;
    private ClientSizeConfig clientSizeConfig;
    private TakeConfig takeConfig;
    private CaptureConfig captureConfig;

    public ConfigParser(String path, String[] args) {
        config = ConfigFactory.parseFile(new File(getConfigPath(path, args))).resolve();
        commonConfigs = new HashMap<>();
        buttonConfigs = new HashMap<>();
        parseAhkConfig();
        parseCommonConfig();
        parseClientSizeConfig();
        parseTakeConfig();
        parseButtonConfig();
        parseCaptureConfig();
    }

    public Map<String, String> getCommonConfigs() {
        return this.commonConfigs;
    }

    public AhkConfig getAhkConfig() {
        return this.ahkConfig;
    }

    public ClientSizeConfig getClientSizeConfig() {
        return this.clientSizeConfig;
    }

    public TakeConfig getTakeConfig() {
        return this.takeConfig;
    }

    public ButtonConfig getButtonConfig(String buttonName) {
        return this.buttonConfigs.get(buttonName);
    }

    public CaptureConfig getCaptureConfig() {
        return this.captureConfig;
    }

    private void parseCommonConfig() {

        configNames.stream().forEach((_item) -> {
            commonConfigs.put(_item,
                    config.getString("project." + _item));
        });

    }

    private void parseAhkConfig() {
        this.ahkConfig = new AhkConfig(config.getString("project.ahk.path"),
                config.getString("project.ahk.control"));
    }

    private void parseClientSizeConfig() {
        this.clientSizeConfig = new ClientSizeConfig(
                config.getInt("project.client.width"),
                config.getInt("project.client.height"));
    }

    private void parseTakeConfig() {
        this.takeConfig = new TakeConfig(
                config.getString("project.take.host"),
                config.getInt("project.take.port"));
    }

    private void parseButtonConfig() {

        buttonNames.stream().forEach((s) -> {
            String sb = "project.buttons." + s + ".";
            buttonConfigs.put(s,
                    new ButtonConfig(config.getInt(sb + "x"),
                            config.getInt(sb + "y"),
                            config.getInt(sb + "count")));
        });
    }

    private void parseCaptureConfig() {
        captureConfig = new CaptureConfig(
                config.getBoolean("project.capture.save"),
                config.getString("project.capture.folder"));
    }

    private String getConfigPath(String configFilePath, String[] args) {

        Option optionConf = Option.builder("f").longOpt("conf")
                .argName("configuration").hasArg()
                .desc("Path to the configuration file").build();

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();
        options.addOption(optionConf);
        try {
            CommandLine commandLine = parser.parse(options, args);
            if (commandLine.hasOption("f")) {
                configFilePath = commandLine.getOptionValue("f");
            } else if (commandLine.hasOption("conf")) {
                configFilePath = commandLine.getOptionValue("conf");
            }
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        return configFilePath;
    }

}

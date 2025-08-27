package com.study.simplechat.configs;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.themes.FlatMacLightLaf;

public class ThemeConfig {
    public ThemeConfig() throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new FlatMacLightLaf());
        UIManager.put("TextComponent.arc", 15);
        UIManager.put("Button.arc", 16);
        UIManager.put("List.arc", 15);
        UIManager.put("ScrollPane.arc", 15);

        UIManager.put("Panel.arc", 20);
    }
}

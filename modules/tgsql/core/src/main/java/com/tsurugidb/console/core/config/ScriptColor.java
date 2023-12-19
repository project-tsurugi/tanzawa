package com.tsurugidb.console.core.config;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * color
 */
public final class ScriptColor {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptColor.class);

    /**
     * parse color
     *
     * @param s string
     * @return color
     */
    public static ScriptColor parse(String s) {
        try {
            if (Integer.parseInt(s) < 0) {
                return null;
            }
        } catch (NumberFormatException ignore) {
            // ignore
        }

        String t = s.replaceAll("[\\s-_/]", "");
        if (t.length() < 6) {
            throw new RuntimeException("enter in 'rrggbb' format");
        }
        try {
            int r = Integer.parseInt(t.substring(0, 2), 16);
            int g = Integer.parseInt(t.substring(2, 4), 16);
            int b = Integer.parseInt(t.substring(4, 6), 16);
            return new ScriptColor(r, g, b);
        } catch (Exception e) {
            LOG.debug("parse error. [{}]", t, e);
            String message = MessageFormat.format("color parse error. [{0}] {1}", s, e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    private final int red;
    private final int green;
    private final int blue;

    private ScriptColor(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    /**
     * get red
     *
     * @return red
     */
    public int red() {
        return this.red;
    }

    /**
     * get green
     *
     * @return green
     */
    public int green() {
        return this.green;
    }

    /**
     * get blue
     *
     * @return blue
     */
    public int blue() {
        return this.blue;
    }

    /**
     * get rgb
     *
     * @return rgb
     */
    public int rgb() {
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public String toString() {
        return String.format("%02x%02x%02x", red, green, blue);
    }
}

package edu.olivet.harvester.fulfill.service;

import javax.inject.Singleton;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/14/17 6:54 PM
 */
@Singleton
public class PSEventListener {
    public static boolean stop = false;
    public static boolean pause = false;

    public static void reset() {
        stop = false;
        pause = false;
    }

    public static void stop() {
        stop = true;
        pause = false;
    }

    public static void pause() {
        pause = true;
        stop = false;
    }
}

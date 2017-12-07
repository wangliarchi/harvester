package edu.olivet.harvester.fulfill.service;

import javax.inject.Singleton;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/14/17 6:54 PM
 */
@Singleton
public class PSEventListener {
    public enum Status {
        NotRunning,
        Running,
        Paused,
        Stopped,
        Ended,
    }

    public static Status status = Status.NotRunning;


    public static void reset() {
        status = Status.NotRunning;
    }

    public static void stop() {
        status = Status.Stopped;
    }

    public static void pause() {
        status = Status.Paused;
    }

    public static void start() {
        status = Status.Running;
    }

    public static void resume() {
        status = Status.Running;
    }

    public static boolean stopped() {
        return status == Status.Stopped;
    }

    public static boolean paused() {
        return status == Status.Paused;
    }

    public static boolean ended() {
        return status == Status.Ended;
    }

    public static void end() {
        status = Status.Ended;
    }


}

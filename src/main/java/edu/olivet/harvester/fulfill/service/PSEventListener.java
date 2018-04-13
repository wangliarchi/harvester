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

    private static Status status = Status.NotRunning;

    private static PSEventHandler eventHandler;

    public static void reset(PSEventHandler eventHandler) {
        PSEventListener.eventHandler = eventHandler;
        status = Status.NotRunning;
        new Thread(() -> {
            PSEventListener.eventHandler.hidePauseBtn();
            PSEventListener.eventHandler.disableStartButton();
        }).start();
    }

    public static void start() {
        status = Status.Running;
        if (eventHandler != null) {
            eventHandler.showPauseBtn();
            eventHandler.disableStartButton();
        }
    }

    public static void pause() {
        status = Status.Paused;
        eventHandler.paused();
    }

    public static void resume() {
        status = Status.Running;
        new Thread(() -> eventHandler.resetPauseBtn()).start();
    }

    public static void stop() {
        status = Status.Stopped;
        eventHandler.disableStopButton();
        //eventHandler.enableStartButton();
    }


    public static void end() {
        status = Status.Ended;
        if (eventHandler != null) {
            eventHandler.hidePauseBtn();
            eventHandler.enableStartButton();
        }

    }

    public static boolean isRunning() {
        return status == Status.Running;
    }

    public static boolean stopped() {
        return status == Status.Stopped;
    }

    public static boolean paused() {
        return status == Status.Paused;
    }


}

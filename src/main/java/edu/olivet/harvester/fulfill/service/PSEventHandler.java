package edu.olivet.harvester.fulfill.service;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 6:39 AM
 */
public interface PSEventHandler {
    void showPauseBtn();

    void hidePauseBtn();

    void paused();

    void resetPauseBtn();

    void disableStartButton();

    void enableStartButton();

    void disableStopButton();
}

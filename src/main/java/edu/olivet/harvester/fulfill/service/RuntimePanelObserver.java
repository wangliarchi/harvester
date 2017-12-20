package edu.olivet.harvester.fulfill.service;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/16/2017 1:20 PM
 */
public interface RuntimePanelObserver {
    void updateSpending(String spending);
    void updateBudget(String budget);
}

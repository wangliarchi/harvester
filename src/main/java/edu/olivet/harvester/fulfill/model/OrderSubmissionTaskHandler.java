package edu.olivet.harvester.fulfill.model;

import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/14/17 2:04 PM
 */
public interface OrderSubmissionTaskHandler {
    void saveTasks(List<OrderSubmissionTask> tasks);
}

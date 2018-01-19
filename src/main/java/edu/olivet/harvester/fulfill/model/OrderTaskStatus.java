package edu.olivet.harvester.fulfill.model;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 12/12/17 9:40 AM
 */
public enum OrderTaskStatus {
    Scheduled,
    Queued,
    Processing,
    Completed,
    Error,
    Stopped,
    Deleted,
    Retried
}

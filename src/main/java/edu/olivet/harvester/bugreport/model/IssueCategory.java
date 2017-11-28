package edu.olivet.harvester.bugreport.model;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/28/17 9:58 AM
 */
public enum IssueCategory {
    OrderConfirmation("Confirm Order"),
    FindSupplier("Find Supplier"),
    OrderSubmission("Submit Order"),
    Settings("Settings"),
    Misc("Misc");

    private final String desc;

    IssueCategory(String desc) {
        this.desc = desc;
    }

    public String desc() {
        return this.desc;
    }

    public String label() {
        return this.desc();
    }

    @Override
    public String toString() {
        return this.label();
    }
}
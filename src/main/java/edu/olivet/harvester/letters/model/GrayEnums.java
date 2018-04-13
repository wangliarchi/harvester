package edu.olivet.harvester.letters.model;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 3/10/2018 10:09 AM
 */
public class GrayEnums {
    public enum GrayLetterType {
        SG(false, true),
        NG(true, true),
        WC(true, false),
        HP(true, true),
        PH(true, true),
        LW(true, true),
        DN(true, true);

        private boolean shouldBeShipped;
        private boolean sendImmediately;

        GrayLetterType(boolean sendImmediately, boolean shouldBeShipped) {
            this.sendImmediately = sendImmediately;
            this.shouldBeShipped = shouldBeShipped;
        }

        public static GrayLetterType getTypeFromStatus(String status) {
            for (GrayLetterType type : GrayLetterType.values()) {
                if (type.name().equalsIgnoreCase(status.trim())) {
                    return type;
                }
            }

            return null;
        }

        public boolean handleImmediately() {
            return sendImmediately;
        }

        public boolean isShouldBeShipped() {
            return shouldBeShipped;
        }

    }
    //"ng", "hp", "ph", "wc", "lw", "dn"
}

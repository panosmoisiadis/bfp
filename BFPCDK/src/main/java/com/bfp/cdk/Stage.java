package com.bfp.cdk;

public enum Stage {
    Gamma("Gamma"),
    Prod("Prod"),
    Dev("Dev");

    private final String stage;

    Stage(String stage) {
        this.stage = stage;
    }

    Stage(String stage, String environment) {
        this.stage = stage + "-" + environment;
    }

    @Override
    public String toString() {
        return getStage();
    }

    public String getStage() {
        return stage.toLowerCase();
    }

    public static Stage fromString(String text) {
        for (Stage stage : Stage.values()) {
            if (stage.stage.equalsIgnoreCase(text)) {
                return stage;
            }
        }

        // Need a default for cdk bootstrap
        return Dev;
    }
}

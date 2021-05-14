package edu.harvard.cs50.wikisteps.path;

import java.io.Serializable;

public class Step implements Serializable {

    private String stepTitle;

    public Step(String stepTitle) {
        this.stepTitle = stepTitle;
    }

    public String getStepTitle() {
        return stepTitle;
    }

}

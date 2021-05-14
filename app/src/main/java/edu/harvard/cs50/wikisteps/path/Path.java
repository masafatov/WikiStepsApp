package edu.harvard.cs50.wikisteps.path;

import java.io.Serializable;
import java.util.ArrayList;

public class Path implements Serializable {

    private ArrayList<Step> stepList;

    public Path(ArrayList<Step> stepList) {
        this.stepList = stepList;
    }

    public ArrayList<Step> getStepList() {
        return stepList;
    }

}

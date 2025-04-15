package com.example.eco;

public class devicegetsetble {
    private String nameble;
    private boolean statusble;
    public devicegetsetble (String nameble, boolean statusble){
        this.nameble = nameble;
        this.statusble = statusble;
    }

    public String getNameble() {
        return nameble;
    }
    public void setNameble(String nameble) {
        this.nameble = nameble;
    }
    public boolean getStatusble() {
        return statusble;
    }
    public void setStatusble(boolean statusble) {
        this.statusble = statusble;
    }

}

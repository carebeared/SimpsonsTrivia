package com.simpsonstrial2.models;

import java.io.Serializable;

public class HighScoreItem implements Serializable
{
    private String rank;
    private String score;
    private String name;
    private boolean isUser;

    public HighScoreItem(String rank, String score, String name, boolean isUser)
    {
        this.rank = rank;
        this.score = score;
        this.name = name;
        this.isUser = isUser;
    }

    public String getRank() {
        return rank;
    }
    public void setRank (String newRank) { this.rank = newRank; }

    public String getScore() {
        return score;
    }
    public int getIntScore() { return Integer.parseInt(score); }

    public String getName() {
        return name;
    }

    public boolean getIsUser()
    {
        return isUser;
    }
}

package com.simpsonstrial2.interfaces;

public interface BonusRoundListener
{
    void onBonusRoundTriggered();
    void onBonusRoundInstructionsLaunch();
    void onBonusRoundInstructionsHide();
    void onBonusRoundStart();
    void onBonusRoundNewQuestion();
    void onBonusRoundTimeExpired();
    void onBonusRoundResultsShow();
    void onBonusRoundHidden();
    void onBonusRoundFinished();
}
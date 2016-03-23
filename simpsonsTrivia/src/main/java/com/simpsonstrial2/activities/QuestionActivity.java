package com.simpsonstrial2.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

import com.simpsonstrial2.utils.IntentManager;
import com.simpsonstrial2.utils.QuestionHandler;
import com.simpsonstrial2.presenters.QuestionPresenter;
import com.simpsonstrial2.R;
import com.simpsonstrial2.models.ScoreModel;
import com.simpsonstrial2.presenters.ScorePresenter;
import com.simpsonstrial2.views.Timer;
import com.simpsonstrial2.presenters.TimerPresenter;
import com.simpsonstrial2.enums.AnswerResult;
import com.simpsonstrial2.enums.GamePlayType;
import com.simpsonstrial2.interfaces.AnswerResultListener;
import com.simpsonstrial2.interfaces.FiftyFiftyListener;
import com.simpsonstrial2.interfaces.GameStateListener;
import com.simpsonstrial2.interfaces.QuestionListener;
import com.simpsonstrial2.interfaces.TimerListener;
import com.simpsonstrial2.models.GameMode;
import com.simpsonstrial2.models.Question;

public class QuestionActivity extends Activity implements GameStateListener, QuestionListener, AnswerResultListener, TimerListener, FiftyFiftyListener
{
    private GameMode gameMode;

    private QuestionHandler questionHandler;
    private QuestionPresenter questionPresenter;

    private ScoreModel scoreModel;
    private ScorePresenter scorePresenter;

    private Timer timer;
    private Timer bonusRoundTimer;
    private TimerPresenter timerPresenter;

    private boolean gameStarted = false;
    private boolean bonusTimerExpired = false;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_question);

        loadGameMode();
        loadScoreModels();
        loadQuestionModels();
        loadTimer();

        //onGameStart(); -- to be called on window focus
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (questionPresenter.isAnimationsRunning())
        {
            questionPresenter.stopAnimations();
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setTitle("Exit game?")
            .setMessage("Are you sure you want to exit this game? Your progress will be lost.")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    removeListeners();
                    goHome();
                }
            })
            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    private void goHome()
    {
        startActivity(IntentManager.getMainMenuIntent(this));
    }

    private void loadGameMode()
    {
        gameMode = GameMode.getGameMode();
    }

    private void loadQuestionModels()
    {
        questionHandler = new QuestionHandler(this.gameMode, 0, this, this);
        questionHandler.loadQuestions();
        questionPresenter = new QuestionPresenter(this, this, this, this, scorePresenter);
    }

    private void loadScoreModels()
    {
        scorePresenter = new ScorePresenter(this, this.gameMode.getGamePlayType() == GamePlayType.SPEED);
        scoreModel = new ScoreModel(scorePresenter, this.gameMode.getGamePlayType() == GamePlayType.SPEED, this.gameMode.BonusRoundEnabled());
        if (!gameMode.BonusRoundEnabled())
        {
            scorePresenter.hideMultiplier();
        }
    }

    private void loadTimer()
    {
        timerPresenter = new TimerPresenter(this);

        if (gameMode.TimerEnabled())
        {
            timer = new Timer(30000, 1000, this, false, timerPresenter);
        }
        else
        {
            timerPresenter.hideTime();
        }

        if (gameMode.BonusRoundEnabled())
        {
            bonusRoundTimer = new Timer(30000, 1000, this, true, timerPresenter);
        }
    }


    /* TimerListener */
    @Override
    public void onTimeExpired()
    {
        onStartNewQuestion();
    }

    @Override
    public void onBonusRoundTimeExpired()
    {
        Log.d("bonus round", "time expired");
        bonusTimerExpired = true;
        onBonusRoundResultsShow();
    }
    /* End TimerListener */



    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus && !gameStarted)
        {
            onGameStart();
            gameStarted = true;
        }
    }

    /* GameStateListener */
    public void onGameStart()
    {
        questionPresenter.presentIntroTransition();
    }

    /* Question presenter lets us know when intro animations are done */
    public void onReadyForNextQuestion()
    {
        onStartNewQuestion();
    }

    public void onStartNewQuestion()
    {
        questionHandler.GetNextQuestion();
    }
    public void onBonusRoundTriggered()
    {
        bonusRoundTimer.reset();
        timerPresenter.reset();
        questionHandler.loadBonusQuestions();
        onBonusRoundInstructionsLaunch();
    }
    public void onBonusRoundInstructionsLaunch()
    {
        questionPresenter.onBonusRoundInstructionsLaunch();
        scorePresenter.collapseMultiplier();
    }
    public void onBonusRoundInstructionsHide()
    {
        questionPresenter.onBonusRoundInstructionsHide(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onBonusRoundStart();
            }
        });
    }
    public void onBonusRoundStart() {
        scorePresenter.onBonusScoreUpdated(0, 0);
        scoreModel.onBonusRoundStart();
        onBonusRoundNewQuestion();
    }

    public void onBonusRoundNewQuestion()
    {
        questionHandler.GetNextBonusQuestion();
    }

    public void onBonusRoundResultsShow() {
        questionPresenter.onShowBonusRoundResults();
        scorePresenter.onShowBonusRoundResults(scoreModel.getCurrentBonusScore(), scoreModel.getCurrentGameScore());
    }

    public void onBonusRoundHidden()
    {
        questionPresenter.presentQuestionNumber();
        scoreModel.onBonusRoundHidden();
        scorePresenter.presentCurrentScore();
        scorePresenter.startupMultiplier();
        bonusRoundTimer.reset();
        bonusTimerExpired = false;
        onBonusRoundFinished();
    }

    public void onBonusRoundFinished()
    {
        onStartNewQuestion();
    }
    /* End GameStateListener */





    /* QuestionListener */
    public void onQuestionNumChanged(int newQuestionNumber)
    {
        questionPresenter.SetQuestionNumber(newQuestionNumber, this.gameMode.getQuizLength());
    }

    public void onQuestionReturned(Question nextQuestion)
    {
        questionPresenter.presentQuestion(nextQuestion, false);
    }

    public void onBonusQuestionReturned(Question nextQuestion)
    {
        questionPresenter.presentBonusQuestion(nextQuestion);
    }

    public void onQuestionLive()
    {
        if (gameMode.TimerEnabled())
        {
            timer.onRestartTimer();
        }
    }

    public void onBonusQuestionLive()
    {
        if (!bonusRoundTimer.isRunning() && bonusRoundTimer.isLoaded())
            bonusRoundTimer.onRestartTimer();
    }

    public void removeListeners()
    {
        questionHandler.removeListeners();
        questionPresenter.removeListeners();
        scoreModel.removeListeners();
        scorePresenter.removeListeners();
        if (timer != null)
            timer.removeListeners();
        if (bonusRoundTimer != null)
            bonusRoundTimer.removeListeners();
        timerPresenter.removeListeners();
    }

    public void onGameOver()
    {
        if (questionPresenter.isAnimationsRunning())
        {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    onGameOver();
                }
            }, 500);

            return;
        }

        removeListeners();


        startActivity(IntentManager.getResultsIntent(this, scoreModel.getScoreDataModel()));
        overridePendingTransition(R.anim.slide_in_activity_right, R.anim.slide_out_activity_left);

        finish();
    }

    public void onQuestionHistoryUpdated(AnswerResult answerResult) {}
    /* End QuestionListener */





    /* AnswerResultListener */
    @Override
    public void onCorrectAnswer()
    {
        questionHandler.onCorrectAnswer();

        if (gameMode.TimerEnabled() && gameMode.getGamePlayType() == GamePlayType.SPEED)
        {
            int speedBonus = this.timerPresenter.getIntTime();
            scoreModel.SetSpeedBonus(speedBonus);
        }

        final boolean bonusRoundTriggered = scoreModel.isBonusRoundTriggered();
        scoreModel.onCorrectAnswer();

        scorePresenter.presentCorrectScoreAnimations(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (gameMode.TimerEnabled())
                {
                    timer.onCorrectAnswer();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                if (bonusRoundTriggered)
                {
                    onBonusRoundTriggered();
                }
                else
                {
                    onStartNewQuestion();
                }
            }
        });
    }





    @Override
    public void onWrongAnswer()
    {
        questionHandler.onWrongAnswer();
        scoreModel.onWrongAnswer();
        if (gameMode.TimerEnabled()) {
            timer.onWrongAnswer();
        }
        scorePresenter.presentCurrentMultiplier();
        onStartNewQuestion();
    }

    @Override
    public void onCorrectBonusAnswer()
    {
        questionHandler.onCorrectBonusAnswer();
        scoreModel.onCorrectBonusAnswer();
        bonusRoundTimer.onCorrectBonusAnswer();

        scorePresenter.presentCorrectScoreBonusAnimations(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (bonusRoundTimer.isTimeExpired() == false)
                    onBonusRoundNewQuestion();
            }
        });
    }

    @Override
    public void onWrongBonusAnswer()
    {
        questionHandler.onWrongBonusAnswer();
        scoreModel.onWrongBonusAnswer();
        bonusRoundTimer.onWrongBonusAnswer();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                if (bonusRoundTimer.isTimeExpired() == false)
                    onBonusRoundNewQuestion();
            }
        }, 2000);
    }
    /* End AnswerResultListener */



    /* FiftyFiftyListener */
    @Override
    public void onFiftyFifty()
    {
        // TODO: hide fifty fifty button
        questionHandler.onFiftyFifty(questionPresenter);
    }
    /* End FiftyFiftyListener */

}


/* Copyright (C) 2010 by Alex Kompel  */
/* This file is part of DroidZebra.

	DroidZebra is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	DroidZebra is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with DroidZebra.  If not, see <http://www.gnu.org/licenses/>
*/

package win.regin.reversi;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import org.jetbrains.annotations.NotNull;
import win.regin.reversi.ReversiEngine.CandidateMove;
import win.regin.reversi.ReversiEngine.Move;
import win.regin.reversi.ReversiEngine.PlayerInfo;

/**
 * @author :Reginer in  2019/3/18 17:41.
 * 联系方式:QQ:282921012
 * 功能描述:黑白棋
 */
public class ReversiActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int
            FUNCTION_HUMAN_VS_HUMAN = 0,
            FUNCTION_ZEBRA_WHITE = 1,
            FUNCTION_ZEBRA_BLACK = 2,
            FUNCTION_ZEBRA_VS_ZEBRA = 3;
    private static final int
            RANDOMNESS_NONE = 0,
            RANDOMNESS_SMALL = 1,
            RANDOMNESS_MEDIUM = 2,
            RANDOMNESS_LARGE = 3,
            RANDOMNESS_HUGE = 4;
    final public static int BOARD_SIZE = 8;
    public static final int DEFAULT_SETTING_FUNCTION = FUNCTION_ZEBRA_WHITE;
    public static final String DEFAULT_SETTING_STRENGTH = "1|1|1";
    public static final boolean DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES = false;
    public static final int DEFAULT_SETTING_RANDOMNESS = RANDOMNESS_LARGE;
    public static final String DEFAULT_SETTING_FORCE_OPENING = "None";
    public static final boolean DEFAULT_SETTING_HUMAN_OPENINGS = false;
    public static final boolean DEFAULT_SETTING_PRACTICE_MODE = false;
    public static final boolean DEFAULT_SETTING_USE_BOOK = true;
    public static final boolean DEFAULT_SETTING_DISPLAY_PV = true;
    public static final boolean DEFAULT_SETTING_DISPLAY_MOVES = true;
    public static final boolean DEFAULT_SETTING_DISPLAY_LAST_MOVE = true;
    public static final boolean DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS = false;
    public static final String
            SETTINGS_KEY_FUNCTION = "settings_engine_function";
    public static final String SETTINGS_KEY_STRENGTH = "settings_engine_strength";
    public static final String SETTINGS_KEY_AUTO_MAKE_FORCED_MOVES = "settings_engine_auto_make_moves";
    public static final String SETTINGS_KEY_RANDOMNESS = "settings_engine_randomness";
    public static final String SETTINGS_KEY_FORCE_OPENING = "settings_engine_force_opening";
    public static final String SETTINGS_KEY_HUMAN_OPENINGS = "settings_engine_human_openings";
    public static final String SETTINGS_KEY_PRACTICE_MODE = "settings_engine_practice_mode";
    public static final String SETTINGS_KEY_USE_BOOK = "settings_engine_use_book";
    public static final String SETTINGS_KEY_DISPLAY_PV = "settings_ui_display_pv";
    public static final String SETTINGS_KEY_DISPLAY_MOVES = "settings_ui_display_moves";
    public static final String SETTINGS_KEY_DISPLAY_LAST_MOVE = "settings_ui_display_last_move";
    public static final String SETTINGS_KEY_DISPLAY_ENABLE_ANIMATIONS = "settings_ui_display_enable_animations";
    public ReversiEngine mZebraThread;
    public int mSettingFunction = DEFAULT_SETTING_FUNCTION;

    public boolean mSettingAutoMakeForcedMoves = DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES;
    public int mSettingZebraRandomness = DEFAULT_SETTING_RANDOMNESS;
    public String mSettingZebraForceOpening = DEFAULT_SETTING_FORCE_OPENING;
    public boolean mSettingZebraHumanOpenings = DEFAULT_SETTING_HUMAN_OPENINGS;
    public boolean mSettingZebraPracticeMode = DEFAULT_SETTING_PRACTICE_MODE;
    public boolean mSettingZebraUseBook = DEFAULT_SETTING_USE_BOOK;
    public boolean mSettingDisplayPV = DEFAULT_SETTING_DISPLAY_PV;
    public boolean mSettingDisplayMoves = DEFAULT_SETTING_DISPLAY_MOVES;
    public boolean mSettingDisplayLastMove = DEFAULT_SETTING_DISPLAY_LAST_MOVE;
    public boolean mSettingDisplayEnableAnimations = DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS;
    public int mSettingAnimationDelay = 1000;
    private BoardState[][] mBoard = new BoardState[BOARD_SIZE][BOARD_SIZE];
    private CandidateMove[] mCandidateMoves = null;
    private Move mLastMove = null;
    private int mWhiteScore = 0;
    private int mBlackScore = 0;
    private BoardView mBoardView;
    private StatusView mStatusView;
    private boolean mBusyDialogUp = false;
    private boolean mHintIsUp = false;
    private boolean mIsInitCompleted = false;
    private boolean mActivityActive = false;
    private int mSettingZebraDepth = 1;
    private int mSettingZebraDepthExact = 1;
    private int mSettingZebraDepthWLD = 1;
    private DroidZebraHandler mDroidZebraHandler = null;
    private ActionBar mActionBar;

    public ReversiActivity() {
        initBoard();
    }

    private void newCompletionPort(final Runnable completion) {
        new Thread() {
            @Override
            public void run() {
                mZebraThread.waitForEngineState(ReversiEngine.ES_READY2PLAY);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        completion.run();
                    }
                });
            }
        }.start();
    }

    public BoardState[][] getBoard() {
        return mBoard;
    }

    public void setBoard(byte[] board) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                mBoard[i][j].set(board[i * BOARD_SIZE + j]);
            }
        }
    }

    public void initBoard() {
        mCandidateMoves = null;
        mLastMove = null;
        mWhiteScore = mBlackScore = 0;
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                mBoard[i][j] = new BoardState(ReversiEngine.PLAYER_EMPTY);
            }
        }
        if (mStatusView != null) {
            mStatusView.clear();
        }
    }

    public CandidateMove[] getCandidateMoves() {
        return mCandidateMoves;
    }

    public void setCandidateMoves(CandidateMove[] cmoves) {
        mCandidateMoves = cmoves;
        mBoardView.invalidate();
    }

    public Move getLastMove() {
        return mLastMove;
    }

    public void setLastMove(Move move) {
        mLastMove = move;
    }

    public boolean isValidMove(Move move) {
        if (mCandidateMoves == null) {
            return false;
        }
        for (CandidateMove m : mCandidateMoves) {
            if (m.mMove.getX() == move.getX() && m.mMove.getY() == move.getY()) {
                return true;
            }
        }
        return false;
    }

    public boolean evalsDisplayEnabled() {
        return mSettingZebraPracticeMode || mHintIsUp;
    }

    public void newGame() {
        if (mZebraThread.getEngineState() != ReversiEngine.ES_READY2PLAY) {
            mZebraThread.stopGame();
        }
        newCompletionPort(
                new Runnable() {
                    @Override
                    public void run() {
                        ReversiActivity.this.initBoard();
                        ReversiActivity.this.loadSettings();
                        ReversiActivity.this.mZebraThread.setEngineState(ReversiEngine.ES_PLAY);
                    }
                }
        );
    }

    /* Creates the menu items */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mIsInitCompleted) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.menu_new_game:
                newGame();
                return true;
            case R.id.menu_quit:
                finish();
                return true;
            case R.id.menu_take_back:
                mZebraThread.undoMove();
                return true;
            case R.id.menu_take_redo:
                mZebraThread.redoMove();
                return true;
            case R.id.menu_settings: {
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }
            return true;
            case R.id.menu_switch_sides: {
                switchSides();
            }
            break;
            case R.id.menu_hint: {
                showHint();
            }
            return true;

            default:
                break;
        }

        return false;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = getSupportActionBar();
        setContentView(R.layout.spash_layout);
        if (mActionBar != null) {
            mActionBar.hide();
        }

        // start your engines
        mDroidZebraHandler = new DroidZebraHandler();
        mZebraThread = new ReversiEngine(this, mDroidZebraHandler);

        // preferences
        SharedPreferences mSettings = getSharedPreferences(Constants.SHARED_PREFS_NAME, 0);
        mSettings.registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState != null && savedInstanceState.containsKey("moves_played_count")) {
            mZebraThread.setInitialGameState(savedInstanceState.getInt("moves_played_count"),
                    savedInstanceState.getByteArray("moves_played"));
        }

        mZebraThread.start();

        newCompletionPort(
                new Runnable() {
                    @Override
                    public void run() {
                        ReversiActivity.this.setContentView(R.layout.board_layout);
                        if (mActionBar != null) {
                            mActionBar.show();
                        }
                        ReversiActivity.this.mBoardView = ReversiActivity.this.findViewById(R.id.board);
                        ReversiActivity.this.mStatusView = ReversiActivity.this.findViewById(R.id.status_panel);
                        ReversiActivity.this.mBoardView.setDroidZebra(ReversiActivity.this);
                        ReversiActivity.this.mBoardView.requestFocus();
                        ReversiActivity.this.initBoard();
                        ReversiActivity.this.loadSettings();
                        ReversiActivity.this.mZebraThread.setEngineState(ReversiEngine.ES_PLAY);
                        ReversiActivity.this.mIsInitCompleted = true;
                    }
                }
        );
    }

    private void loadSettings() {
        int settingsFunction, settingZebraDepth, settingZebraDepthExact, settingZebraDepthWLD;
        int settingRandomness;
        boolean settingAutoMakeForcedMoves;
        String settingZebraForceOpening;
        boolean settingZebraHumanOpenings;
        boolean settingZebraPracticeMode;
        boolean settingZebraUseBook;

        SharedPreferences settings = getSharedPreferences(Constants.SHARED_PREFS_NAME, 0);

        settingsFunction = Integer.parseInt(settings.getString(SETTINGS_KEY_FUNCTION, String.valueOf(DEFAULT_SETTING_FUNCTION)));
        String[] strength = settings.getString(SETTINGS_KEY_STRENGTH, DEFAULT_SETTING_STRENGTH).split("\\|");

        settingZebraDepth = Integer.parseInt(strength[0]);
        settingZebraDepthExact = Integer.parseInt(strength[1]);
        settingZebraDepthWLD = Integer.parseInt(strength[2]);

        settingAutoMakeForcedMoves = settings.getBoolean(SETTINGS_KEY_AUTO_MAKE_FORCED_MOVES, DEFAULT_SETTING_AUTO_MAKE_FORCED_MOVES);
        settingRandomness = Integer.parseInt(settings.getString(SETTINGS_KEY_RANDOMNESS, String.valueOf(DEFAULT_SETTING_RANDOMNESS)));
        settingZebraForceOpening = settings.getString(SETTINGS_KEY_FORCE_OPENING, DEFAULT_SETTING_FORCE_OPENING);
        settingZebraHumanOpenings = settings.getBoolean(SETTINGS_KEY_HUMAN_OPENINGS, DEFAULT_SETTING_HUMAN_OPENINGS);
        settingZebraPracticeMode = settings.getBoolean(SETTINGS_KEY_PRACTICE_MODE, DEFAULT_SETTING_PRACTICE_MODE);
        settingZebraUseBook = settings.getBoolean(SETTINGS_KEY_USE_BOOK, DEFAULT_SETTING_USE_BOOK);

        boolean bZebraSettingChanged = (
                mSettingFunction != settingsFunction
                        || mSettingZebraDepth != settingZebraDepth
                        || mSettingZebraDepthExact != settingZebraDepthExact
                        || mSettingZebraDepthWLD != settingZebraDepthWLD
                        || mSettingAutoMakeForcedMoves != settingAutoMakeForcedMoves
                        || mSettingZebraRandomness != settingRandomness
                        || mSettingZebraForceOpening != settingZebraForceOpening
                        || mSettingZebraHumanOpenings != settingZebraHumanOpenings
                        || mSettingZebraPracticeMode != settingZebraPracticeMode
                        || mSettingZebraUseBook != settingZebraUseBook
        );

        mSettingFunction = settingsFunction;
        mSettingZebraDepth = settingZebraDepth;
        mSettingZebraDepthExact = settingZebraDepthExact;
        mSettingZebraDepthWLD = settingZebraDepthWLD;
        mSettingAutoMakeForcedMoves = settingAutoMakeForcedMoves;
        mSettingZebraRandomness = settingRandomness;
        mSettingZebraForceOpening = settingZebraForceOpening;
        mSettingZebraHumanOpenings = settingZebraHumanOpenings;
        mSettingZebraPracticeMode = settingZebraPracticeMode;
        mSettingZebraUseBook = settingZebraUseBook;

        try {
            mZebraThread.setAutoMakeMoves(mSettingAutoMakeForcedMoves);
            mZebraThread.setForcedOpening(mSettingZebraForceOpening);
            mZebraThread.setHumanOpenings(mSettingZebraHumanOpenings);
            mZebraThread.setPracticeMode(mSettingZebraPracticeMode);
            mZebraThread.setUseBook(mSettingZebraUseBook);

            switch (mSettingFunction) {
                case FUNCTION_HUMAN_VS_HUMAN:
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_BLACK, 0, 0, 0, ReversiEngine.INFINIT_TIME, 0));
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_WHITE, 0, 0, 0, ReversiEngine.INFINIT_TIME, 0));
                    break;
                case FUNCTION_ZEBRA_BLACK:
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_BLACK, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ReversiEngine.INFINIT_TIME, 0));
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_WHITE, 0, 0, 0, ReversiEngine.INFINIT_TIME, 0));
                    break;
                case FUNCTION_ZEBRA_VS_ZEBRA:
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_BLACK, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ReversiEngine.INFINIT_TIME, 0));
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_WHITE, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ReversiEngine.INFINIT_TIME, 0));
                    break;
                case FUNCTION_ZEBRA_WHITE:
                default:
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_BLACK, 0, 0, 0, ReversiEngine.INFINIT_TIME, 0));
                    mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_WHITE, mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD, ReversiEngine.INFINIT_TIME, 0));
                    break;
            }
            mZebraThread.setPlayerInfo(new PlayerInfo(ReversiEngine.PLAYER_ZEBRA, mSettingZebraDepth + 1, mSettingZebraDepthExact + 1, mSettingZebraDepthWLD + 1, ReversiEngine.INFINIT_TIME, 0));

            switch (mSettingZebraRandomness) {
                case RANDOMNESS_SMALL:
                    mZebraThread.setSlack(1.5f);
                    mZebraThread.setPerturbation(1.0f);
                    break;
                case RANDOMNESS_MEDIUM:
                    mZebraThread.setSlack(4.0f);
                    mZebraThread.setPerturbation(2.5f);
                    break;
                case RANDOMNESS_LARGE:
                    mZebraThread.setSlack(6.0f);
                    mZebraThread.setPerturbation(6.0f);
                    break;
                case RANDOMNESS_HUGE:
                    mZebraThread.setSlack(10.0f);
                    mZebraThread.setPerturbation(16.0f);
                    break;
                case RANDOMNESS_NONE:
                default:
                    mZebraThread.setSlack(0.0f);
                    mZebraThread.setPerturbation(0.0f);
                    break;
            }
        } catch (EngineError e) {
            FatalError(e.msg);
        }

        mStatusView.setTextForID(
                StatusView.ID_SCORE_SKILL,
                String.format(getString(R.string.display_depth), mSettingZebraDepth, mSettingZebraDepthExact, mSettingZebraDepthWLD)
        );

        mSettingDisplayPV = settings.getBoolean(SETTINGS_KEY_DISPLAY_PV, DEFAULT_SETTING_DISPLAY_PV);
        if (!mSettingDisplayPV) {
            mStatusView.setTextForID(StatusView.ID_STATUS_PV, "");
            mStatusView.setTextForID(StatusView.ID_STATUS_EVAL, "");
        }

        mSettingDisplayMoves = settings.getBoolean(SETTINGS_KEY_DISPLAY_MOVES, DEFAULT_SETTING_DISPLAY_MOVES);
        mSettingDisplayLastMove = settings.getBoolean(SETTINGS_KEY_DISPLAY_LAST_MOVE, DEFAULT_SETTING_DISPLAY_LAST_MOVE);

        mSettingDisplayEnableAnimations = settings.getBoolean(SETTINGS_KEY_DISPLAY_ENABLE_ANIMATIONS, DEFAULT_SETTING_DISPLAY_ENABLE_ANIMATIONS);
        mZebraThread.setMoveDelay(mSettingDisplayEnableAnimations ? mSettingAnimationDelay + 1000 : 0);

        if (bZebraSettingChanged) {
            mZebraThread.sendSettingsChanged();
        }
    }


    private void switchSides() {
        int newFunction = -1;

        if (mSettingFunction == FUNCTION_ZEBRA_WHITE) {
            newFunction = FUNCTION_ZEBRA_BLACK;
        } else if (mSettingFunction == FUNCTION_ZEBRA_BLACK) {
            newFunction = FUNCTION_ZEBRA_WHITE;
        }

        if (newFunction > 0) {
            SharedPreferences settings = getSharedPreferences(Constants.SHARED_PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(SETTINGS_KEY_FUNCTION, String.valueOf(newFunction));
            editor.apply();
        }

        loadSettings();

        // start a new game if not playing
        if (!mZebraThread.gameInProgress()) {
            newGame();
        }
    }

    private void showHint() {
        if (!mSettingZebraPracticeMode) {
            mHintIsUp = true;
            mZebraThread.setPracticeMode(true);
            mZebraThread.sendSettingsChanged();
        }
    }

    @Override
    protected void onDestroy() {
        boolean retry = true;
        mZebraThread.setRunning(false);
        mZebraThread.interrupt(); // if waiting
        while (retry) {
            try {
                mZebraThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mDroidZebraHandler = null;
        super.onDestroy();
    }

    void showDialog(DialogFragment dialog, String tag) {
        if (mActivityActive) {
            dialog.show(getSupportFragmentManager(), tag);
        }
    }


    public void showPassDialog() {
        DialogFragment newFragment = DialogPass.newInstance();
        showDialog(newFragment, "dialog_pass");
    }

    public void showGameOverDialog() {
        DialogFragment newFragment = DialogGameOver.newInstance();
        showDialog(newFragment, "dialog_gameover");
    }

    public void showQuitDialog() {
        DialogFragment newFragment = DialogQuit.newInstance();
        showDialog(newFragment, "dialog_quit");
    }

    public void showBusyDialog() {
        if (!mBusyDialogUp && mZebraThread.isThinking()) {
            DialogFragment newFragment = DialogBusy.newInstance();
            mBusyDialogUp = true;
            showDialog(newFragment, "dialog_busy");
        }
    }

    public void dismissBusyDialog() {
        if (mBusyDialogUp) {
            Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog_busy");
            if (prev != null) {
                DialogFragment df = (DialogFragment) prev;
                df.dismiss();
            }
            mBusyDialogUp = false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (mZebraThread != null) {
            loadSettings();
        }
    }

    public void FatalError(String msg) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Zebra Error");
        alertDialog.setMessage(msg);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ReversiActivity.this.finish();
                    }
                }
        );
        alertDialog.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mActivityActive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActivityActive = true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            /*try {
                mZebraThread.undoMove();
            } catch (EngineError e) {
                FatalError(e.msg);
            }*/
            showQuitDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ReversiEngine.GameState gs = mZebraThread.getGameState();
        if (gs != null) {
            outState.putByteArray("moves_played", gs.mMoveSequence);
            outState.putInt("moves_played_count", gs.mDisksPlayed);
            outState.putInt("version", 1);
        }
    }

    public static class BoardState {
        public final static byte ST_FLIPPED = 0x01;
        public byte mState;
        public byte mFlags;

        public BoardState(byte state) {
            mState = state;
            mFlags = 0;
        }

        public void set(byte newState) {
            if (newState != ReversiEngine.PLAYER_EMPTY && mState != ReversiEngine.PLAYER_EMPTY && mState != newState) {
                mFlags |= ST_FLIPPED;
            } else {
                mFlags &= ~ST_FLIPPED;
            }
            mState = newState;
        }

        public byte getState() {
            return mState;
        }

        public boolean isFlipped() {
            return (mFlags & ST_FLIPPED) > 0;
        }
    }


    //-------------------------------------------------------------------------
    // Pass Dialog
    public static class DialogPass extends DialogFragment {

        public static DialogPass newInstance() {
            return new DialogPass();
        }

        public ReversiActivity getDroidZebra() {
            return (ReversiActivity) getActivity();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.dialog_pass_text)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getDroidZebra().mZebraThread.setEngineState(ReversiEngine.ES_PLAY);
                                }
                            }
                    )
                    .create();
        }
    }

    //-------------------------------------------------------------------------
    // Game Over Dialog
    public static class DialogGameOver extends DialogFragment {

        public static DialogGameOver newInstance() {
            return new DialogGameOver();
        }

        public ReversiActivity getDroidZebra() {
            return (ReversiActivity) getActivity();
        }

        @SuppressLint("DefaultLocale")
        public void refreshContent(View dialog) {
            int winner;
            int blackScore = getDroidZebra().mBlackScore;
            int whiteScore = getDroidZebra().mWhiteScore;
            if (whiteScore > blackScore) {
                winner = R.string.gameover_text_white_wins;
            } else if (whiteScore < blackScore) {
                winner = R.string.gameover_text_black_wins;
            } else {
                winner = R.string.gameover_text_draw;
            }
            ((TextView) dialog.findViewById(R.id.gameover_text)).setText(winner);
            ((TextView) dialog.findViewById(R.id.gameover_score))
                    .setText(String.format("%d : %d", blackScore, whiteScore));
        }

        @Override
        public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().setTitle(R.string.gameover_title);
            View v = inflater.inflate(R.layout.gameover, container, false);
            Button button;
            button = (Button) v.findViewById(R.id.gameover_choice_new_game);
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();
                            getDroidZebra().newGame();
                        }
                    });

            button = (Button) v.findViewById(R.id.gameover_choice_switch);
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();
                            getDroidZebra().switchSides();
                        }
                    });

            button = (Button) v.findViewById(R.id.gameover_choice_cancel);
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();
                        }
                    });

            button = (Button) v.findViewById(R.id.gameover_choice_options);
            button.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dismiss();

                            // start settings
                            Intent i = new Intent(getDroidZebra(), SettingsActivity.class);
                            startActivity(i);
                        }
                    });

            refreshContent(v);
            return v;
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshContent(getView());
        }
    }

    //-------------------------------------------------------------------------
    // Pass Dialog
    public static class DialogQuit extends DialogFragment {

        public static DialogQuit newInstance() {
            return new DialogQuit();
        }

        public ReversiActivity getDroidZebra() {
            return (ReversiActivity) getActivity();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.dialog_quit_title)
                    .setPositiveButton(R.string.dialog_quit_button_quit, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    getDroidZebra().finish();
                                }
                            }
                    )
                    .setNegativeButton(R.string.dialog_quit_button_cancel, null)
                    .create();
        }
    }

    //-------------------------------------------------------------------------
    // Pass Dialog
    public static class DialogBusy extends DialogFragment {

        public static DialogBusy newInstance() {
            return new DialogBusy();
        }

        public ReversiActivity getDroidZebra() {
            return (ReversiActivity) getActivity();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog pd = new ProgressDialog(getActivity()) {
                @Override
                public boolean onKeyDown(int keyCode, KeyEvent event) {
                    if (getDroidZebra().mZebraThread.isThinking()) {
                        getDroidZebra().mZebraThread.stopMove();
                    }
                    getDroidZebra().mBusyDialogUp = false;
                    cancel();
                    return super.onKeyDown(keyCode, event);
                }

                @Override
                public boolean onTouchEvent(MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        if (getDroidZebra().mZebraThread.isThinking()) {
                            getDroidZebra().mZebraThread.stopMove();
                        }
                        getDroidZebra().mBusyDialogUp = false;
                        cancel();
                        return true;
                    }
                    return super.onTouchEvent(event);
                }
            };
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setMessage(getResources().getString(R.string.dialog_busy_message));
            return pd;
        }
    }

    @SuppressLint("HandlerLeak")
    class DroidZebraHandler extends Handler {
        @Override
        public void handleMessage(Message m) {
            // block messages if waiting for something
            switch (m.what) {
                case ReversiEngine.MSG_ERROR: {
                    FatalError(m.getData().getString("error"));
                }
                break;
                case ReversiEngine.MSG_MOVE_START: {
                    // noop
                }
                break;
                case ReversiEngine.MSG_BOARD: {
                    String score;
                    int sideToMove = m.getData().getInt("side_to_move");

                    setBoard(m.getData().getByteArray("board"));

                    mBlackScore = m.getData().getBundle("black").getInt("disc_count");
                    mWhiteScore = m.getData().getBundle("white").getInt("disc_count");

                    if (sideToMove == ReversiEngine.PLAYER_BLACK) {
                        score = String.format("•%d", mBlackScore);
                    } else {
                        score = String.format("%d", mBlackScore);
                    }
                    mStatusView.setTextForID(
                            StatusView.ID_SCORE_BLACK,
                            score
                    );

                    if (sideToMove == ReversiEngine.PLAYER_WHITE) {
                        score = String.format("%d•", mWhiteScore);
                    } else {
                        score = String.format("%d", mWhiteScore);
                    }
                    mStatusView.setTextForID(
                            StatusView.ID_SCORE_WHITE,
                            score
                    );

                    int iStart, iEnd;
                    byte[] black_moves = m.getData().getBundle("black").getByteArray("moves");
                    byte[] white_moves = m.getData().getBundle("white").getByteArray("moves");

                    iEnd = black_moves.length;
                    iStart = Math.max(0, iEnd - 4);
                    for (int i = 0; i < 4; i++) {
                        String num_text = String.format("%d", i + iStart + 1);
                        String move_text;
                        if (i + iStart < iEnd) {
                            Move move = new Move(black_moves[i + iStart]);
                            move_text = move.getText();
                        } else {
                            move_text = "";
                        }
                        mStatusView.setTextForID(
                                StatusView.ID_SCORELINE_NUM_1 + i,
                                num_text
                        );
                        mStatusView.setTextForID(
                                StatusView.ID_SCORELINE_BLACK_1 + i,
                                move_text
                        );
                    }

                    iEnd = white_moves.length;
                    iStart = Math.max(0, iEnd - 4);
                    for (int i = 0; i < 4; i++) {
                        String move_text;
                        if (i + iStart < iEnd) {
                            Move move = new Move(white_moves[i + iStart]);
                            move_text = move.getText();
                        } else {
                            move_text = "";
                        }
                        mStatusView.setTextForID(
                                StatusView.ID_SCORELINE_WHITE_1 + i,
                                move_text
                        );
                    }
                    mBoardView.onBoardStateChanged();
                }
                break;

                case ReversiEngine.MSG_CANDIDATE_MOVES: {
                    setCandidateMoves((CandidateMove[]) m.obj);
                }
                break;

                case ReversiEngine.MSG_PASS: {
                    showPassDialog();
                }
                break;

                case ReversiEngine.MSG_OPENING_NAME: {
                    String mOpeningName = m.getData().getString("opening");
                    mStatusView.setTextForID(
                            StatusView.ID_STATUS_OPENING,
                            mOpeningName
                    );
                }
                break;

                case ReversiEngine.MSG_LAST_MOVE: {
                    byte move = (byte) m.getData().getInt("move");
                    setLastMove(move == Move.PASS ? null : new Move(move));
                }
                break;

                case ReversiEngine.MSG_GAME_OVER: {
                    showGameOverDialog();
                }
                break;

                case ReversiEngine.MSG_EVAL_TEXT: {
                    if (mSettingDisplayPV) {
                        mStatusView.setTextForID(
                                StatusView.ID_STATUS_EVAL,
                                m.getData().getString("eval")
                        );
                    }
                }
                break;

                case ReversiEngine.MSG_PV: {
                    if (mSettingDisplayPV) {
                        byte[] pv = m.getData().getByteArray("pv");
                        String pvText = "";
                        for (byte move : pv) {
                            pvText += new Move(move).getText();
                            pvText += " ";
                        }
                        mStatusView.setTextForID(
                                StatusView.ID_STATUS_PV,
                                pvText
                        );
                    }
                }
                break;

                case ReversiEngine.MSG_MOVE_END: {
                    dismissBusyDialog();
                    if (mHintIsUp) {
                        mHintIsUp = false;
                        mZebraThread.setPracticeMode(mSettingZebraPracticeMode);
                        mZebraThread.sendSettingsChanged();
                    }
                }
                break;

                case ReversiEngine.MSG_CANDIDATE_EVALS: {
                    CandidateMove[] evals = (CandidateMove[]) m.obj;
                    for (CandidateMove eval : evals) {
                        for (int i = 0; i < mCandidateMoves.length; i++) {
                            if (mCandidateMoves[i].mMove.mMove == eval.mMove.mMove) {
                                mCandidateMoves[i] = eval;
                                break;
                            }
                        }
                    }
                    mBoardView.invalidate();
                }
                break;

                case ReversiEngine.MSG_DEBUG: {
                    Log.d("ReversiActivity", m.getData().getString("message"));
                }
                break;
            }
        }
    }
}

package com.chanakyabharwaj.whistle.Game;

import java.util.ArrayList;

public class GameState {
    int gameLevel;
    long levelRunningFor;
    public int gameScore = 0;
    ArrayList<EnemyCircleState> enemiesState;
    WhistleStickState stickState;
}
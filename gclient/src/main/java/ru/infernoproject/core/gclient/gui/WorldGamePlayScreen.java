package ru.infernoproject.core.gclient.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import ru.infernoproject.core.gclient.gui.base.BaseConsoleScreen;

public class WorldGamePlayScreen extends BaseConsoleScreen {

    @Override
    protected void onBind(Nifty nifty, Screen screen) {

    }

    @Override
    public void onStartScreen() {
        switch (screen.getScreenId()) {
            case "gamePlayStart":
                break;
        }
    }

    @Override
    public void onEndScreen() {

    }
}

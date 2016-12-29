package ru.infernoproject.core.client.gui.base;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.Console;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.input.NiftyInputEvent;
import de.lessvoid.nifty.input.NiftyStandardInputEvent;
import de.lessvoid.nifty.input.mapping.DefaultInputMapping;
import de.lessvoid.nifty.screen.KeyInputHandler;
import de.lessvoid.nifty.screen.Screen;

import javax.annotation.Nonnull;

public abstract class BaseConsoleScreen extends BaseScreen implements KeyInputHandler {

    protected Element consolePopup;

    protected Console console;

    private boolean consoleVisible = false;
    private boolean allowConsoleToggle = true;

    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {
        consolePopup = nifty.createPopup("consolePopup");

        screen.addKeyboardInputHandler(new DefaultInputMapping(), this);

        super.bind(nifty, screen);
    }

    @Override
    public boolean keyEvent(@Nonnull NiftyInputEvent inputEvent) {
        switch ((NiftyStandardInputEvent) inputEvent) {
            case ConsoleToggle:
                toggleConsole();
                return true;
            default:
                break;
        }

        return false;
    }

    private void toggleConsole() {
        if (allowConsoleToggle) {
            allowConsoleToggle = false;

            if (consoleVisible) {
                closeConsole();
            } else {
                openConsole();
            }
        }
    }

    private void openConsole() {
        String id = consolePopup != null ? consolePopup.getId() : null;

        if (id == null) {
            return;
        }

        nifty.showPopup(screen, id, consolePopup.findElementById("console#textInput"));
        screen.processAddAndRemoveLayerElements();

        if (console == null) {
            console = screen.findNiftyControl("console", Console.class);
        }

        consoleVisible = true;
        allowConsoleToggle = true;
    }

    private void closeConsole() {
        String id = consolePopup != null ? consolePopup.getId() : null;

        if (id == null) {
            return;
        }

        nifty.closePopup(id, () -> {
            consoleVisible = false;
            allowConsoleToggle = true;
        });
    }
}

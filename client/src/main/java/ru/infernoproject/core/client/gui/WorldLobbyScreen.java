package ru.infernoproject.core.client.gui;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.ConsoleExecuteCommandEvent;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;

import ru.infernoproject.core.client.gui.base.BaseConsoleScreen;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.GenderInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;
import ru.infernoproject.core.common.utils.Result;

import javax.annotation.Nonnull;
import java.util.List;

public class WorldLobbyScreen extends BaseConsoleScreen {

    private TextField firstName;
    private TextField lastName;

    private ListBox characterList;

    private ListBox raceSelector;
    private ListBox classSelector;
    private DropDown genderSelector;

    @Override
    @SuppressWarnings("unchecked")
    public void onBind(Nifty nifty, Screen screen) {
        firstName = screen.findNiftyControl("firstName", TextField.class);
        lastName = screen.findNiftyControl("lastName", TextField.class);

        characterList = screen.findNiftyControl("characterSelector", ListBox.class);
        if (characterList != null) characterList.changeSelectionMode(ListBox.SelectionMode.Single, true);

        raceSelector = screen.findNiftyControl("raceSelector", ListBox.class);
        if (raceSelector != null) raceSelector.changeSelectionMode(ListBox.SelectionMode.Single, true);

        classSelector = screen.findNiftyControl("classSelector", ListBox.class);
        if (classSelector != null) classSelector.changeSelectionMode(ListBox.SelectionMode.Single, true);

        genderSelector = screen.findNiftyControl("genderSelector", DropDown.class);
        if (genderSelector != null) {
            genderSelector.addItem(GenderInfo.MALE);
            genderSelector.addItem(GenderInfo.FEMALE);
        }
    }

    @Override
    public void onStartScreen() {
        switch (screen.getScreenId()) {
            case "connect":
                worldClient = realmClient.serverConnect();

                mainClient.worldClientSet(worldClient);

                if (worldClient.isConnected()) {
                    realmClient.sessionTokenGet(this::sessionTokenCallBack);
                } else {
                    nifty.fromXml("Interface/realmLogInScreen.xml", "realmList");
                }
                break;
            case "characterList":
                worldClient.characterListGet(this::characterListCallBack);
                break;
            case "characterCreate":
                worldClient.raceListGet(this::raceListCallBack);
                worldClient.classListGet(this::classListCallBack);
                break;
        }
    }

    @Override
    public void onEndScreen() {

    }


    @NiftyEventSubscriber(id = "console")
    public void onConsoleCommand(final String id, @Nonnull ConsoleExecuteCommandEvent command) {
        if (console != null) {
            worldClient.commandExecute(
                command.getCommand(), command.getArguments(), this::commandExecuteCallBack
            );
        }
    }

    private void commandExecuteCallBack(Result result) {
        if (console != null) {
            if (result.isSuccess()) {
                for (String outputLine: result.attr(String[].class, "output")) {
                    console.output(outputLine);
                }
            } else {
                for (String outputLine: result.attr(String[].class, "output")) {
                    console.outputError(outputLine);
                }
            }
        }
    }

    private void sessionTokenCallBack(Result result) {
        if (result.isSuccess()) {
            worldClient.authorize(
                result.attr(byte[].class, "sessionToken"),
                this::authorizeCallBack
            );
        } else {
            showError(result.attr("message"));
        }
    }

    private void authorizeCallBack(Result authorization) {
        if (authorization.isSuccess()) {
            realmClient.disconnect();

            nifty.fromXml("Interface/worldLobbyScreen.xml", "characterList");
        } else {
            showError(authorization.attr("message"));

            nifty.fromXml("Interface/realmLogInScreen.xml", "realmList");
        }
    }

    @SuppressWarnings("unchecked")
    private void characterListCallBack(Result characters) {
        if (characters.isSuccess()) {
            characterList.addAllItems(characters.attr(List.class, "characters"));
        } else {
            showError(characters.attr("message"));
        }
    }

    public void characterSelect() {
        CharacterInfo character = (CharacterInfo) characterList.getSelection().get(0);

        if (character != null) {
            worldClient.characterSelect(character, this::characterSelectCallBack);
        } else {
            showError("No character selected");
        }
    }

    private void characterSelectCallBack(Result result) {
        if (result.isSuccess()) {
            CharacterInfo characterInfo = result.attr(CharacterInfo.class, "characterInfo");
            if (characterInfo != null) {
                showError("Character selected. Game play is not implemented yet.");
            } else {
                showError("No character data found.");
            }
        } else {
            showError(result.attr("message"));
        }
    }

    public void characterCreateScreen() {
        nifty.fromXml("Interface/worldLobbyScreen.xml", "characterCreate");
    }

    @SuppressWarnings("unchecked")
    private void raceListCallBack(Result raceList) {
        if (raceList.isSuccess()) {
            raceSelector.addAllItems(raceList.attr(List.class, "raceList"));
        } else {
            showError("Unable to retrieve race list");
        }
    }

    @SuppressWarnings("unchecked")
    private void classListCallBack(Result classList) {
        if (classList.isSuccess()) {
            classSelector.addAllItems(classList.attr(List.class, "classList"));
        } else {
            showError("Unable to retrieve class list");
        }
    }

    public void characterCreate() {
        if ((firstName != null)&&(lastName != null)) {
            RaceInfo raceInfo = ((RaceInfo) raceSelector.getSelection().get(0));
            GenderInfo genderInfo = ((GenderInfo) genderSelector.getSelection());
            ClassInfo classInfo = ((ClassInfo) classSelector.getSelection().get(0));

            if (raceInfo == null) {
                showError("Race should be selected");
                return;
            }

            if (genderInfo == null) {
                showError("Gender should be selected");
                return;
            }

            if (classInfo == null) {
                showError("Class should be selected");
                return;
            }

            worldClient.characterCreate(
                new CharacterInfo(
                    firstName.getRealText(), lastName.getRealText(),
                    raceInfo, genderInfo, classInfo
                ),
                this::characterCreateCallBack
            );
        } else {
            showError("Internal application error");
        }
    }

    public void characterCreateCallBack(Result result) {
        if (result.isSuccess()) {
            nifty.fromXml("Interface/worldLobbyScreen.xml", "characterList");
        } else {
            showError(result.attr("message"));
        }
    }

    public void characterListScreen() {
        nifty.fromXml("Interface/worldLobbyScreen.xml", "characterList");
    }

    private void logOutCallBack(Result result) {
        worldClient.disconnect();

        if (result.isSuccess()) {
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    public void exit() {
        worldClient.logOut(this::logOutCallBack);
    }
}

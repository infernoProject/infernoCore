package ru.infernoproject.core.client.gui;

import com.jme3.system.AppSettings;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.controls.ListBox;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.screen.Screen;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import ru.infernoproject.core.client.gui.base.BaseScreen;
import ru.infernoproject.core.client.realm.RealmClient;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.utils.Result;

import java.util.Arrays;
import java.util.List;

public class RealmLogInScreen extends BaseScreen {

    private TextField logIn;
    private TextField passWord;
    private TextField passWordConfirmation;

    private DropDown resolutionList;
    private CheckBox fullScreenSwitch;

    private ListBox realmList;

    @Override
    public void onBind(Nifty nifty, Screen screen) {
        logIn = screen.findNiftyControl("login", TextField.class);
        passWord = screen.findNiftyControl("password", TextField.class);
        passWordConfirmation = screen.findNiftyControl("password_confirmation", TextField.class);

        resolutionList = screen.findNiftyControl("resolutionList", DropDown.class);
        fullScreenSwitch = screen.findNiftyControl("fullScreenSwitch", CheckBox.class);

        realmList = screen.findNiftyControl("realmSelector", ListBox.class);

        if (realmList != null) {
            realmList.changeSelectionMode(ListBox.SelectionMode.Single, true);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onStartScreen() {
        switch (screen.getScreenId()) {
            case "startUp":
                realmClient = new RealmClient(
                    System.getProperty("serverHost", "realm.inferno-project.ru"),
                    Integer.parseInt(System.getProperty("serverPort", "3274"))
                );

                mainClient.realmClientSet(realmClient);

                try {
                    realmClient.srp6ConfigGet();
                } catch (InterruptedException e) {
                    showError("Unable to establish security session");
                }

                nifty.fromXml("Interface/realmLogInScreen.xml", "logIn");
                break;
            case "logIn":
                break;
            case "signUp":
                break;
            case "realmList":
                realmClient.realmListGet(this::realmListCallBack);
                break;
            case "settings":
                try {
                    List<DisplayMode> displayModes = Arrays.asList(Display.getAvailableDisplayModes());
                    String currentDisplayMode = String.format(
                        "%dx%d",
                        mainClient.settingsGet().getWidth(),
                        mainClient.settingsGet().getHeight()
                    );

                    resolutionList.addItem(currentDisplayMode);
                    resolutionList.selectItem(currentDisplayMode);

                    for (DisplayMode displayMode : displayModes) {
                        if (displayMode.isFullscreenCapable()) {
                            resolutionList.addItem(String.format(
                                "%dx%d", displayMode.getWidth(), displayMode.getHeight()
                            ));
                        }
                    }

                    fullScreenSwitch.setChecked(
                        mainClient.settingsGet().isFullscreen()
                    );
                } catch (LWJGLException e) {
                    showError("Unable to list available resolutions");
                }
                break;
        }
    }

    @Override
    public void onEndScreen() {

    }

    public void logIn() {
        if ((logIn != null)&&(passWord != null)) {
            realmClient.logIn(
                logIn.getRealText(), passWord.getRealText(), this::logInCallBack
            );
        } else {
            showError("Internal application error");
        }
    }

    private void logInCallBack(Result result) {
        if (result.isSuccess()) {
            nifty.fromXml("Interface/realmLogInScreen.xml", "realmList");
        } else {
            showError(result.attr("message"));
        }
    }

    public void logInScreen() {
        nifty.fromXml("Interface/realmLogInScreen.xml", "logIn");
    }

    public void signUp() {
        if ((logIn != null)&&(passWord != null)&&(passWordConfirmation != null)) {
            if (passWord.getRealText().equals(passWordConfirmation.getRealText())) {
                try {
                    realmClient.signUp(
                        logIn.getRealText(), passWord.getRealText(), this::signUpCallBack
                    );
                } catch (InterruptedException e) {
                    showError("Unable to perform request");
                }
            } else {
                showError("Password confirmation doesn't match password");
            }
        } else {
            showError("Internal application error");
        }
    }

    private void signUpCallBack(Result result) {
        if (result.isSuccess()) {
            nifty.fromXml("Interface/realmLogInScreen.xml", "logIn");
        } else {
            showError(result.attr("message"));
        }
    }

    public void signUpScreen() {
        nifty.fromXml("Interface/realmLogInScreen.xml", "signUp");
    }

    @SuppressWarnings("unchecked")
    private void realmListCallBack(Result result) {
        if (result.isSuccess()) {
            realmList.addAllItems(result.attr(List.class, "realmList"));
        } else {
            showError(result.attr("message"));
        }
    }

    public void realmSelect() {
        RealmServerInfo server = (RealmServerInfo) realmList.getSelection().get(0);

        realmClient.serverSelect(server);

        nifty.fromXml("Interface/worldLobbyScreen.xml", "connect");
    }

    public void settingsSave() {
        AppSettings settings = new AppSettings(true);

        if (resolutionList.getSelection() != null) {
            String[] resolution = ((String) resolutionList.getSelection()).split("x");

            int width = Integer.parseInt(resolution[0]);
            int height = Integer.parseInt(resolution[1]);

            settings.setResolution(width, height);
            settings.setFullscreen(fullScreenSwitch.isChecked());

            mainClient.setSettings(settings);
            mainClient.reshape(width, height);
            mainClient.restart();
        }

        nifty.fromXml("Interface/realmLogInScreen.xml", "logIn");
    }

    public void settingsScreen() {
        nifty.fromXml("Interface/realmLogInScreen.xml", "settings");
    }

    public void exit() {
        realmClient.disconnect();
        System.exit(0);
    }

}

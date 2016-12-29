package ru.infernoproject.core.client;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.niftygui.NiftyJmeDisplay;

import com.jme3.system.AppSettings;
import ru.infernoproject.core.client.realm.RealmClient;
import ru.infernoproject.core.client.world.WorldClient;

public class Client extends SimpleApplication {

    private RealmClient realmClient;
    private WorldClient worldClient;

    public static Client APP = new Client();

    public static void main(String[] args) {
        APP.main();
    }

    public void main() {
        setPauseOnLostFocus(false);
        setDisplayStatView(false);
        setDisplayFps(false);
        setShowSettings(false);

        AppSettings settings = new AppSettings(true);

        settings.setFullscreen(false);
        settings.setResolution(1280, 720);

        setSettings(settings);

        start();
    }

    @Override
    public void simpleInitApp() {
        NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(
            assetManager, inputManager,
            audioRenderer, guiViewPort
        );

        inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);

        inputManager.addMapping(
            "Pause",
            new KeyTrigger(KeyInput.KEY_ESCAPE),
            new KeyTrigger(KeyInput.KEY_P),
            new KeyTrigger(KeyInput.KEY_PAUSE)
        );

        // inputManager.addListener(this, "Pause");

        niftyDisplay.getNifty()
            .fromXml("Interface/realmLogInScreen.xml", "startUp");

        guiViewPort.addProcessor(niftyDisplay);
        flyCam.setEnabled(false);
    }

    public void realmClientSet(RealmClient realmClient) {
        this.realmClient = realmClient;
    }

    public RealmClient realmClientGet() {
        return realmClient;
    }

    public void worldClientSet(WorldClient worldClient) {
        this.worldClient = worldClient;
    }

    public WorldClient worldClientGet() {
        return worldClient;
    }

    public AppSettings settingsGet() {
        return settings;
    }
}

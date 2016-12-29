package ru.infernoproject.core.client.gui.base;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import ru.infernoproject.core.client.Client;
import ru.infernoproject.core.client.realm.RealmClient;
import ru.infernoproject.core.client.world.WorldClient;

import javax.annotation.Nonnull;

public abstract class BaseScreen implements ScreenController {

    protected Nifty nifty;
    protected Screen screen;

    protected Element errorPopup;

    protected Client mainClient = Client.APP;
    protected RealmClient realmClient;
    protected WorldClient worldClient;

    public void showError(String message) {
        String id = errorPopup != null ? errorPopup.getId() : null;

        if (id == null) {
            return;
        }

        nifty.showPopup(screen, id, errorPopup.findElementById("errorConfirm"));

        Element errorMessage = errorPopup.findElementById("errorMessage");
        if (errorMessage != null) {
            errorMessage.getChildren()
                    .forEach(Element::markForRemoval);

            final LabelBuilder labelBuilder = new LabelBuilder();

            labelBuilder.label(message);

            labelBuilder.valignCenter();
            labelBuilder.textVAlignTop();

            labelBuilder.build(nifty, screen, errorMessage);
        }

        screen.processAddAndRemoveLayerElements();
    }

    public void errorConfirm() {
        String id = errorPopup != null ? errorPopup.getId() : null;

        if (id == null) {
            return;
        }

        nifty.closePopup(id);
    }

    @Override
    public void bind(@Nonnull Nifty nifty, @Nonnull Screen screen) {
        this.nifty = nifty;
        this.screen = screen;

        errorPopup = nifty.createPopup("errorPopup");

        realmClient = Client.APP.realmClientGet();
        worldClient = Client.APP.worldClientGet();

        onBind(nifty, screen);
    }

    protected abstract void onBind(Nifty nifty, Screen screen);
}

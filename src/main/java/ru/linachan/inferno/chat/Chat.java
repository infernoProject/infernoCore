package ru.linachan.inferno.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.linachan.inferno.InfernoServer;
import ru.linachan.inferno.common.codec.Message;
import ru.linachan.inferno.world.Player;
import ru.linachan.inferno.world.WorldHandler;
import ru.linachan.inferno.world.interest.InterestEvent;
import ru.linachan.inferno.world.interest.InterestEventType;

import java.nio.ByteBuffer;

public class Chat {

    private static Logger logger = LoggerFactory.getLogger(Chat.class);

    public void sendMessage(Player sender, Message message) {
        String messageText = new String(message.data().array());

        if (!messageText.startsWith(".")) {
            ((WorldHandler) InfernoServer.HANDLERS.get("world")).getWorld().getPlayerData().stream()
                .filter(player -> !player.equals(sender)).forEach(player -> {
                    InterestEvent messageEvent = new InterestEvent(InterestEventType.MESSAGE, sender);
                    messageEvent.setEventData(message.data());
                    player.getInterestArea().onEvent(messageEvent);
                });
        } else {
            handleSystemMessage(sender, messageText);
        }
    }

    private void handleSystemMessage(Player sender, String message) {
        logger.info("Got system message from {}: '{}'", sender.getName(), message);

        InterestEvent messageEvent = new InterestEvent(InterestEventType.MESSAGE, null);
        messageEvent.setEventData(ByteBuffer.allocate(0));
        sender.getInterestArea().onEvent(messageEvent);
    }
}

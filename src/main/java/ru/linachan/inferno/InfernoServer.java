package ru.linachan.inferno;

import com.google.common.base.Joiner;
import io.netty.channel.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.linachan.inferno.common.InfernoBasicServer;
import ru.linachan.inferno.common.InfernoBasicServerBuilder;
import ru.linachan.inferno.common.InfernoConfig;
import ru.linachan.inferno.common.auth.AuthManager;
import ru.linachan.inferno.common.codec.session.SessionDecoder;
import ru.linachan.inferno.common.codec.xor.XORDecoder;
import ru.linachan.inferno.common.codec.xor.XOREncoder;
import ru.linachan.inferno.common.console.CommandLineUtils;
import ru.linachan.inferno.common.session.SessionKiller;
import ru.linachan.inferno.common.session.SessionManager;
import ru.linachan.inferno.realm.RealmHandler;
import ru.linachan.inferno.world.WorldHandler;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfernoServer {
    private SessionManager sessionManager;
    private SessionKiller sessionKiller;
    private AuthManager authManager;

    private InfernoBasicServer infernoServer;
    private ExecutorService serverPool = Executors.newFixedThreadPool(3);

    public static InfernoServer INSTANCE;
    public static InfernoConfig CONFIG;

    private static Logger logger = LoggerFactory.getLogger(InfernoServer.class);

    private InfernoServer(CommandLineUtils.CommandLine command) throws IOException {
        String configFile = command.getKeywordArgs().getOrDefault("config", "inferno.ini");
        CONFIG = InfernoConfig.readConfig(new File(configFile));

        String role = command.getKeywordArgs().get("role");

        if (role == null) {
            logger.error("No InfernoServer role provided. Please specify role with --role parameter. Available roles: realmd, world");
            System.exit(1);
        }

        sessionManager = new SessionManager();
        sessionKiller = new SessionKiller(sessionManager);
        authManager = new AuthManager();

        ChannelHandler mainHandler = null;
        int serverPort = 0;

        switch (role) {
            case "world":
                mainHandler = new WorldHandler();
                serverPort = CONFIG.getInt("world.port", 41597);
                break;
            case "realmd":
                mainHandler = new RealmHandler();
                serverPort = CONFIG.getInt("realm.port", 41596);
                break;
            default:
                logger.error("InfernoServer doesn't support role '{}'. Available roles: realmd, world.", role);
                System.exit(1);
                break;
        }

        infernoServer = new InfernoBasicServerBuilder(serverPort)
            .addHandler(XOREncoder.class).addHandler(XORDecoder.class)
            .addHandler(SessionDecoder.class)
            .addHandler(mainHandler)
            .build();

        serverPool.submit(infernoServer);
        serverPool.submit(sessionKiller);
    }

    private void shutdown() {
        infernoServer.stop();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public static void main(String[] args) {
        CommandLineUtils.CommandLine command = CommandLineUtils.parse(String.format(
            "inferno %s", Joiner.on(" ").join(args)
        ));

        try {
            InfernoServer.INSTANCE = new InfernoServer(command);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                InfernoServer.INSTANCE.shutdown();
            }));
        } catch (IOException e) {
            logger.error("Unable to open configuration file: [{}]: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }
}

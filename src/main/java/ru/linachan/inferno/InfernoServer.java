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
import ru.linachan.inferno.common.db.InfernoDataBase;
import ru.linachan.inferno.common.session.SessionKiller;
import ru.linachan.inferno.common.session.SessionManager;
import ru.linachan.inferno.realm.RealmHandler;
import ru.linachan.inferno.world.WorldHandler;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfernoServer {
    private SessionKiller sessionKiller = null;

    public static InfernoServer INSTANCE;
    public static InfernoConfig CONFIG;
    public static InfernoDataBase DB;

    public static SessionManager SESSION_MANAGER;
    public static AuthManager AUTH_MANAGER;

    private List<InfernoBasicServer> SERVERS = new ArrayList<>();
    public static Map<String, ChannelHandler> HANDLERS = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(InfernoServer.class);

    private InfernoServer(CommandLineUtils.CommandLine command) throws IOException {
        String configFile = command.getKeywordArgs().getOrDefault("config", "inferno.ini");
        CONFIG = InfernoConfig.readConfig(new File(configFile));

        String[] roles = command.getKeywordArgs().get("roles").split(",");

        if (roles.length == 0) {
            logger.error("No InfernoServer role provided. Please specify role with --role parameter. Available roles: realmd, world");
            System.exit(1);
        }

        DB = new InfernoDataBase();
        SESSION_MANAGER = new SessionManager();
        AUTH_MANAGER = new AuthManager();

        AUTH_MANAGER.register("velovec", "qwerty");

        for (String role: roles) {
            ChannelHandler handler = null;
            int serverPort = 0;

            switch (role) {
                case "world":
                    handler = new WorldHandler();
                    serverPort = CONFIG.getInt("world.port", 41597);
                    break;
                case "realmd":
                    handler = new RealmHandler();
                    serverPort = CONFIG.getInt("realm.port", 41596);
                    break;
                default:
                    logger.error("InfernoServer doesn't support role '{}'. Available roles: realmd, world.", role);
                    break;
            }

            if (handler != null) {
                InfernoBasicServer infernoServer = new InfernoBasicServerBuilder(serverPort)
                    .addHandler(XOREncoder.class).addHandler(XORDecoder.class)
                    .addHandler(SessionDecoder.class)
                    .addHandler(handler)
                    .build();

                HANDLERS.put(role, handler);
                SERVERS.add(infernoServer);
            }
        }

        if (SERVERS.size() == 0) {
            logger.error("no roles configured. Shutting down...");
            System.exit(1);
        }

        ExecutorService serverPool = Executors.newFixedThreadPool(10);
        SERVERS.forEach(serverPool::submit);

        if (HANDLERS.containsKey("realmd")) {
            sessionKiller = new SessionKiller(SESSION_MANAGER);
            serverPool.submit(sessionKiller);
        }
    }

    private void shutdown() {
        SERVERS.forEach(InfernoBasicServer::stop);

        if (sessionKiller != null) {
            sessionKiller.stop();
        }
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

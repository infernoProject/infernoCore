package ru.infernoproject.core.client.world;

import ru.infernoproject.core.client.common.EventListener;
import ru.infernoproject.core.common.constants.ErrorCodes;
import ru.infernoproject.core.common.net.client.Client;
import ru.infernoproject.core.common.net.client.ClientCallBack;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;
import ru.infernoproject.core.common.utils.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.WorldOperations.*;

public class WorldClient extends Client {

    private static final String AUTHORIZE_CALLBACK = "authorizeCallBack";
    private static final String EXECUTE_CALLBACK = "commandExecuteCallBack";
    private static final String CHARACTER_LIST_CALLBACK = "characterListCallBack";
    private static final String CHARACTER_CREATE_CALLBACK = "characterCreateCallBack";
    private static final String CHARACTER_SELECT_CALLBACK = "characterSelectCallBack";
    private static final String RACE_LIST_CALLBACK = "raceListCallBack";
    private static final String CLASS_LIST_CALLBACK = "classListCallBack";
    private static final String SPELL_CAST_CALLBACK = "spellCaseCallBack";
    private static final String LOG_OUT_CALLBACK = "logOutCallBack";

    private boolean authorized = false;
    private Long latency = 0L;

    private CharacterInfo character;
    private EventListener eventListener;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public WorldClient(RealmServerInfo server) {
        super(server.getHost(), server.getPort());

        scheduler.scheduleAtFixedRate(
            this::heartBeat, 5, 5, TimeUnit.SECONDS
        );
    }

    public void authorize(byte[] sessionToken, Callback callback) {
        registerCallBack(AUTHORIZE_CALLBACK, callback);
        send(new ByteArray()
            .put(AUTHORIZE).put(sessionToken)
        );
    }

    @ClientCallBack(opCode = AUTHORIZE)
    public void authorizeCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(AUTHORIZE_CALLBACK).callBack(Result.success());
                authorized = true;
                break;
            case AUTH_ERROR:
                getCallBack(AUTHORIZE_CALLBACK).callBack(
                    Result.failed().message("Authorization error")
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(AUTHORIZE_CALLBACK).callBack(
                    Result.failed().message("Server failure")
                );
                break;
        }
    }

    public void commandExecute(String command, String[] args, Callback callback) {
        registerCallBack(EXECUTE_CALLBACK, callback);
        send(new ByteArray()
            .put(EXECUTE).put(command).put(args)
        );
    }

    @ClientCallBack(opCode = EXECUTE)
    public void commandExecuteCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                Result result;

                switch (response.getInt()) {
                    case 0x00:
                        result = Result.success();
                        break;
                    case 0x01:
                        result = Result.failed();
                        break;
                    default:
                        result = Result.failed();
                        break;
                }

                String[] output = response.getStrings();

                getCallBack(EXECUTE_CALLBACK).callBack(
                    result.attr("output", output)
                );
                break;
            case UNKNOWN_COMMAND:
                getCallBack(EXECUTE_CALLBACK).callBack(
                    Result.failed().attr("output", new String[] { "Unknown command" })
                );
                break;
            default:
                getCallBack(EXECUTE_CALLBACK).callBack(
                    Result.failed().attr("output", new String[] { "Server failure" })
                );
        }
    }

    public void characterListGet(Callback callback) {
        registerCallBack(CHARACTER_LIST_CALLBACK, callback);
        send(CHARACTER_LIST);
    }

    @ClientCallBack(opCode = CHARACTER_LIST)
    public void characterListCallBack(ByteWrapper characterListData) {
        switch (characterListData.getByte()) {
            case SUCCESS:
                List<CharacterInfo> characterInfoList = characterListData.getList().stream()
                    .map(CharacterInfo::new)
                    .collect(Collectors.toList());

                getCallBack(CHARACTER_LIST_CALLBACK).callBack(
                    Result.success()
                        .attr("characters", characterInfoList)
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(CHARACTER_LIST_CALLBACK).callBack(
                    Result.failed().message("Server failure")
                );
                break;
        }
    }

    public void characterCreate(CharacterInfo characterInfo, Callback callback) {
        registerCallBack(CHARACTER_CREATE_CALLBACK, callback);
        send(new ByteArray()
            .put(CHARACTER_CREATE)
            .put(characterInfo)
        );
    }

    @ClientCallBack(opCode = CHARACTER_CREATE)
    public void characterCreateCallBack(ByteWrapper result) {
        switch (result.getByte()) {
            case SUCCESS:
                CharacterInfo characterInfo = new CharacterInfo(result.getWrapper());

                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.success().attr("characterInfo", characterInfo)
                );
                break;
            case ALREADY_EXISTS:
                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.failed().message("Already exists")
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.failed().message("Server failure")
                );
                break;
        }
    }

    public void characterSelect(CharacterInfo characterInfo, Callback callback) {
        registerCallBack(CHARACTER_SELECT_CALLBACK, callback);
        send(new ByteArray()
            .put(CHARACTER_SELECT).put(characterInfo.getId())
        );
    }

    @ClientCallBack(opCode = CHARACTER_SELECT)
    public void characterSelectCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                character = new CharacterInfo(response.getWrapper());

                getCallBack(CHARACTER_SELECT_CALLBACK).callBack(
                    Result.success().attr("characterInfo", character)
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(CHARACTER_SELECT_CALLBACK).callBack(
                    Result.failed().message("Server failure")
                );
                break;
        }
    }

    public void raceListGet(Callback callback) {
        registerCallBack(RACE_LIST_CALLBACK, callback);
        send(RACE_LIST);
    }

    @ClientCallBack(opCode = RACE_LIST)
    public void raceListCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                List<RaceInfo> raceList = response.getList().stream()
                    .map(RaceInfo::new)
                    .collect(Collectors.toList());

                getCallBack(RACE_LIST_CALLBACK).callBack(
                    Result.success().attr("raceList", raceList)
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(RACE_LIST_CALLBACK).callBack(
                    Result.success().message("Server failure")
                );
                break;
        }
    }

    public void classListGet(Callback callback) {
        registerCallBack(CLASS_LIST_CALLBACK, callback);
        send(CLASS_LIST);
    }

    @ClientCallBack(opCode = CLASS_LIST)
    public void classListCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                List<ClassInfo> classList = response.getList().stream()
                    .map(ClassInfo::new)
                    .collect(Collectors.toList());

                getCallBack(CLASS_LIST_CALLBACK).callBack(
                    Result.success().attr("classList", classList)
                );
                break;
            case ErrorCodes.SERVER_ERROR:
                getCallBack(CLASS_LIST_CALLBACK).callBack(
                    Result.success().message("Server failure")
                );
                break;
        }
    }

    public void spellCast(int spellId, Callback callback) {
        registerCallBack(SPELL_CAST_CALLBACK, callback);
        send(new ByteArray().put(SPELL_CAST).put(spellId));
    }

    @ClientCallBack(opCode = SPELL_CAST)
    public void spellCastCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(SPELL_CAST_CALLBACK).callBack(Result.success());
                break;
            case NOT_IN_GAME:
                getCallBack(SPELL_CAST_CALLBACK).callBack(
                    Result.failed().message("Not in game.")
                );
                break;
            case PLAYER_DEAD:
                getCallBack(SPELL_CAST_CALLBACK).callBack(
                    Result.failed().message("You are dead.")
                );
                break;
            case SPELL_COOL_DOWN:
                getCallBack(SPELL_CAST_CALLBACK).callBack(
                        Result.failed().message("Spell on CoolDown.")
                );
                break;
            case SPELL_NOT_LEARNED:
                getCallBack(SPELL_CAST_CALLBACK).callBack(
                    Result.failed().message("Spell not learned.")
                );
                break;
            default:
                getCallBack(SPELL_CAST_CALLBACK).callBack(
                    Result.failed().message("Server failure")
                );
                break;
        }
    }

    public void logOut(Callback callback) {
        registerCallBack(LOG_OUT_CALLBACK, callback);
        send(LOG_OUT);
    }

    @ClientCallBack(opCode = LOG_OUT)
    public void logOutCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(LOG_OUT_CALLBACK).callBack(Result.success());
                break;
            default:
                getCallBack(LOG_OUT_CALLBACK).callBack(
                    Result.failed().message("Server Failure.")
                );
                break;
        }
    }

    public void heartBeat() {
        send(new ByteArray().put(HEART_BEAT).put(System.currentTimeMillis()));
    }

    @ClientCallBack(opCode = HEART_BEAT)
    public void heartBeatCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                latency = System.currentTimeMillis() - response.getLong();
                logger.info("Latency: {} ms", latency);
                break;
        }
    }

    @ClientCallBack(opCode = EVENT)
    public void eventCallBack(ByteWrapper response) {
        if (eventListener != null) {
            eventListener.onEvent(
                response.getByte(), response.getInt(), response.getInt(),
                response.getInt(), response.getInt()
            );
        }
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    @Override
    public void disconnect() {
        scheduler.shutdown();

        super.disconnect();
    }

    public Long getLatency() {
        return latency;
    }

    public boolean isAuthorized() {
        return authorized;
    }
}

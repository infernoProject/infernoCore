package ru.infernoproject.core.client.world;

import io.netty.channel.ChannelHandlerContext;

import ru.infernoproject.core.client.common.BasicClient;
import ru.infernoproject.core.common.types.realm.RealmServerInfo;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.common.types.world.ClassInfo;
import ru.infernoproject.core.common.types.world.RaceInfo;
import ru.infernoproject.core.common.utils.*;

import java.util.List;
import java.util.stream.Collectors;

import static ru.infernoproject.core.common.constants.ErrorCodes.*;
import static ru.infernoproject.core.common.constants.WorldOperations.*;

public class WorldClient extends BasicClient {

    private static final String AUTHORIZE_CALLBACK = "authorizeCallBack";
    private static final String EXECUTE_CALLBACK = "commandExecuteCallBack";
    private static final String CHARACTER_LIST_CALLBACK = "characterListCallBack";
    private static final String CHARACTER_CREATE_CALLBACK = "characterCreateCallBack";
    private static final String CHARACTER_SELECT_CALLBACK = "characterSelectCallBack";
    private static final String RACE_LIST_CALLBACK = "raceListCallBack";
    private static final String CLASS_LIST_CALLBACK = "classListCallBack";
    private static final String LOG_OUT_CALLBACK = "logOutCallBack";

    private boolean authorized = false;

    private CharacterInfo character;

    public WorldClient(RealmServerInfo server) {
        super(server.getHost(), server.getPort());
    }

    public void authorize(byte[] sessionToken, Callback callback) {
        registerCallBack(AUTHORIZE_CALLBACK, callback);
        send(new ByteArray()
            .put(AUTHORIZE).put(sessionToken)
        );
    }

    private void authorizeCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(AUTHORIZE_CALLBACK).callBack(
                    Result.success()
                );

                authorized = true;
                break;
            case AUTH_ERROR:
                getCallBack(AUTHORIZE_CALLBACK).callBack(
                    Result.failed().attr("message", "Authorization error")
                );
                break;
            case SQL_ERROR:
                getCallBack(AUTHORIZE_CALLBACK).callBack(
                    Result.failed().attr("message", "Server failure")
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

    private void commandExecuteCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                Result result;

                switch (response.getByte()) {
                    case (byte) 0x00:
                        result = Result.success();
                        break;
                    case (byte) 0x01:
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
        }
    }

    public void characterListGet(Callback callback) {
        registerCallBack(CHARACTER_LIST_CALLBACK, callback);
        send(CHARACTER_LIST);
    }

    private void characterListCallBack(ByteWrapper characterListData) {
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
            case SQL_ERROR:
                getCallBack(CHARACTER_LIST_CALLBACK).callBack(
                    Result.failed()
                        .attr("message", "Server failure")
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

    private void characterCreateCallBack(ByteWrapper result) {
        switch (result.getByte()) {
            case SUCCESS:
                CharacterInfo characterInfo = new CharacterInfo(result.getWrapper());

                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.success().attr("characterInfo", characterInfo)
                );
                break;
            case ALREADY_EXISTS:
                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.failed().attr("message","Already exists")
                );
                break;
            case SQL_ERROR:
                getCallBack(CHARACTER_CREATE_CALLBACK).callBack(
                    Result.failed().attr("message","Server failure")
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

    private void characterSelectCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                character = new CharacterInfo(response.getWrapper());

                getCallBack(CHARACTER_SELECT_CALLBACK).callBack(
                    Result.success().attr("characterInfo", character)
                );
                break;
            case SQL_ERROR:
                getCallBack(CHARACTER_SELECT_CALLBACK).callBack(
                    Result.failed().attr("message", "Server failure")
                );
                break;
        }
    }

    public void raceListGet(Callback callback) {
        registerCallBack(RACE_LIST_CALLBACK, callback);
        send(RACE_LIST);
    }

    private void raceListCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                List<RaceInfo> raceList = response.getList().stream()
                    .map(RaceInfo::new)
                    .collect(Collectors.toList());

                getCallBack(RACE_LIST_CALLBACK).callBack(
                    Result.success().attr("raceList", raceList)
                );
                break;
            case SQL_ERROR:
                getCallBack(RACE_LIST_CALLBACK).callBack(
                    Result.success().attr("message", "Server failure")
                );
                break;
        }
    }

    public void classListGet(Callback callback) {
        registerCallBack(CLASS_LIST_CALLBACK, callback);
        send(CLASS_LIST);
    }

    private void classListCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                List<ClassInfo> classList = response.getList().stream()
                    .map(ClassInfo::new)
                    .collect(Collectors.toList());

                getCallBack(CLASS_LIST_CALLBACK).callBack(
                    Result.success().attr("classList", classList)
                );
                break;
            case SQL_ERROR:
                getCallBack(CLASS_LIST_CALLBACK).callBack(
                    Result.success().attr("message", "Server failure")
                );
                break;
        }
    }

    public void logOut(Callback callback) {
        registerCallBack(LOG_OUT_CALLBACK, callback);
        send(LOG_OUT);
    }

    private void logOutCallBack(ByteWrapper response) {
        switch (response.getByte()) {
            case SUCCESS:
                getCallBack(LOG_OUT_CALLBACK).callBack(Result.success());
                break;
            default:
                getCallBack(LOG_OUT_CALLBACK).callBack(Result.failed());
                break;
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteWrapper in) throws Exception {
        switch (in.getByte()) {
            case AUTHORIZE:
                authorizeCallBack(in.getWrapper());
                break;
            case EXECUTE:
                commandExecuteCallBack(in.getWrapper());
                break;
            case CHARACTER_LIST:
                characterListCallBack(in.getWrapper());
                break;
            case CHARACTER_CREATE:
                characterCreateCallBack(in.getWrapper());
                break;
            case CHARACTER_SELECT:
                characterSelectCallBack(in.getWrapper());
                break;
            case RACE_LIST:
                raceListCallBack(in.getWrapper());
                break;
            case CLASS_LIST:
                classListCallBack(in.getWrapper());
                break;
            case LOG_OUT:
                logOutCallBack(in.getWrapper());
                break;
        }
    }

    public boolean isAuthorized() {
        return authorized;
    }
}

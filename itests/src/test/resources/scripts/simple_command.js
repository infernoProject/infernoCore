var Base = Java.type('ru.infernoproject.worldd.script.impl.CommandBase');
var ByteArray = Java.type('ru.infernoproject.common.utils.ByteArray');

var Command = Java.extend(Base, {
  execute: function (dataSourceManager, session, args) {
    return new ByteArray().put(args);
  }
});

var sObject = new Command();
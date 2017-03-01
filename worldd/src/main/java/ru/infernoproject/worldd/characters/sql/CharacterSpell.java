package ru.infernoproject.worldd.characters.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.worldd.scripts.sql.SpellInfo;

@SQLObject(table = "character_spells", database = "characters")
public class CharacterSpell implements SQLObjectWrapper {

    @SQLField(column = "id", type = Integer.class)
    public int id;

    @SQLField(column = "character_id", type = CharacterInfo.class)
    public CharacterInfo character;

    @SQLField(column = "spell_id", type = SpellInfo.class)
    public SpellInfo spell;

    @SQLField(column = "cooldown", type = Integer.class)
    public int coolDown = 0;

    public CharacterSpell() {

    }

    public CharacterSpell(CharacterInfo character, SpellInfo spell) {
        this.character = character;
        this.spell = spell;
    }
}

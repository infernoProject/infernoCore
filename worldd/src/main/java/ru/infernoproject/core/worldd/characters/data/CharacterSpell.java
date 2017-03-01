package ru.infernoproject.core.worldd.characters.data;

import ru.infernoproject.core.common.db.sql.SQLField;
import ru.infernoproject.core.common.db.sql.SQLObject;
import ru.infernoproject.core.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.core.common.types.world.CharacterInfo;
import ru.infernoproject.core.worldd.scripts.data.SpellInfo;

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

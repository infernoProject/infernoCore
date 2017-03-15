package ru.infernoproject.worldd.characters.sql;

import ru.infernoproject.common.db.sql.SQLField;
import ru.infernoproject.common.db.sql.SQLObject;
import ru.infernoproject.common.db.sql.SQLObjectWrapper;
import ru.infernoproject.worldd.scripts.sql.SpellInfo;

@SQLObject(table = "character_spells", database = "characters")
public class CharacterSpell implements SQLObjectWrapper {

    @SQLField(column = "id")
    public int id;

    @SQLField(column = "character_id")
    public CharacterInfo character;

    @SQLField(column = "spell_id")
    public SpellInfo spell;

    @SQLField(column = "cooldown")
    public int coolDown = 0;

    public CharacterSpell() {
        // Default constructor for SQLObjectWrapper
    }

    public CharacterSpell(CharacterInfo character, SpellInfo spell) {
        this.character = character;
        this.spell = spell;
    }
}

package chronoMods.coop.relics;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.core.*;
import com.megacrit.cardcrawl.cards.*;
import com.megacrit.cardcrawl.blights.*;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.*;

import chronoMods.*;
import chronoMods.network.steam.*;
import chronoMods.network.*;
import chronoMods.ui.deathScreen.*;
import chronoMods.ui.hud.*;
import chronoMods.ui.lobby.*;
import chronoMods.ui.mainMenu.*;
import chronoMods.utilities.*;

public class StringOfFate extends AbstractBlight {
    // When one player dies, they are 'sustained' by their ally's health.
    //  If either player takes damage during this time, both their health drops.
    //  Extra 'overkill' damage will also be applied to both players
    //  If both players have 0 hp, they die

    public static final String ID = "StringOfFate";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString(ID);
    public static final String NAME = blightStrings.NAME;
    public static final String[] DESCRIPTIONS = blightStrings.DESCRIPTION;

    public StringOfFate() {
        super(ID, NAME, "", "spear.png", true);
        this.blightID = ID;
        this.name = NAME;
        updateDescription();
        this.unique = true;
        this.img = ImageMaster.loadImage("chrono/images/blights/" + ID + ".png");
        this.outlineImg = ImageMaster.loadImage("chrono/images/blights/outline/" + ID + ".png");


        this.increment = 0;
        this.tips.clear();
        this.tips.add(new PowerTip(name, description));
        this.counter = TogetherManager.players.size() - 1;
        if (this.counter >= 4) {
            this.increment = 1;
            this.counter--;
        }
    }

    @Override
    public void updateDescription() {
        if (TogetherManager.players.size() <= 4)
            this.description = this.DESCRIPTIONS[0];
        if (TogetherManager.players.size() > 4)
            this.description = this.DESCRIPTIONS[1];
    }

    @Override
    public void effect() {
        if (counter > 0) {
            flash();
            AbstractDungeon.player.isDead = false;
            AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth, true);
        }

        if (this.increment > 0)
            this.increment--;
        else
            this.counter--;

        NetworkHelper.sendData(NetworkHelper.dataType.Hp);
        NetworkHelper.sendData(NetworkHelper.dataType.LoseLife);
    }

    @Override
    public void renderInTopPanel(SpriteBatch sb) {
        if (this.counter <= 0)
          ShaderHelper.setShader(sb, ShaderHelper.Shader.GRAYSCALE); 
        
        super.renderInTopPanel(sb);
        sb.setColor(Color.WHITE);

        int i = 0;
        for (; i < this.counter; i++) {
            sb.draw(ImageMaster.TP_HP, this.currentX - (56.0F * Settings.scale) + (10.0F * Settings.scale * i), this.currentY - 64.0F * Settings.scale, 32.0F, 32.0F, 64.0F, 64.0F, this.scale/1.5f, this.scale/1.5f, 0, 0, 0, 64, 64, false, false);
        }        

        for (; i < this.counter + this.increment; i++) {
            sb.setColor(Color.GOLD);
            sb.draw(TogetherManager.TP_WhiteHeart, this.currentX - (56.0F * Settings.scale) + (10.0F * Settings.scale * i), this.currentY - 64.0F * Settings.scale, 32.0F, 32.0F, 64.0F, 64.0F, this.scale/1.5f, this.scale/1.5f, 0, 0, 0, 64, 64, false, false);
        }
        sb.setColor(Color.WHITE);

        if (this.counter <= 0)
          ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
    }

    @Override
    public void renderCounter(SpriteBatch sb, boolean inTopPanel) {}

    // This patch prevents death
    @SpirePatch(clz = AbstractPlayer.class, method="damage")
    public static class RevivePlayer {
        @SpireInsertPatch(rloc=1875-1725, localvars={})
        public static SpireReturn Insert(AbstractPlayer player, DamageInfo info) {
            if (player.isDead) {
                if (player.hasBlight("StringOfFate")) {
                    player.currentHealth = 0;
                    player.getBlight("StringOfFate").effect();
                    return SpireReturn.Return(null);
                } 
            }

            return SpireReturn.Continue();
        }
    }
}

package com.example.chaosmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class ChaosEvents {

    /** Шанс, что при ломании блока что-то случится. 0.2 = 20%. */
    private static final float CHANCE = 0.2f;

    @FunctionalInterface
    private interface Chaos {
        void run(ServerPlayer p, ServerLevel level, RandomSource rnd);
    }

    private static final String[] GOD_PHRASES = {
            "Ты чё там копаешь, пассажир?",
            "Я вижу, что ты делаешь.",
            "Мне скучно, поэтому пусть будет весело.",
            "Это не баг, это фича.",
            "Передай привет Левитану.",
            "Блок был не твой."
    };

    private static final List<Chaos> EFFECTS = List.of(

            // 1. Мобы вокруг взлетают и танцуют
            (p, level, rnd) -> {
                AABB box = p.getBoundingBox().inflate(20);
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box);
                for (Mob m : mobs) {
                    m.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 80, 1));
                    m.setYRot(m.getYRot() + 180);
                }
                p.sendSystemMessage(Component.literal("§dВсе мобы пошли танцевать! (" + mobs.size() + ")"));
            },

            // 2. Один предмет в инвентаре превращается в картошку
            (p, level, rnd) -> {
                int size = p.getInventory().getContainerSize();
                for (int tries = 0; tries < 20; tries++) {
                    int slot = rnd.nextInt(size);
                    if (!p.getInventory().getItem(slot).isEmpty()) {
                        p.getInventory().setItem(slot, new ItemStack(Items.POISONOUS_POTATO, 1));
                        p.sendSystemMessage(Component.literal("§6Один из твоих предметов стал картошкой. Ядовитой."));
                        return;
                    }
                }
                p.sendSystemMessage(Component.literal("§7У тебя нечего превращать в картошку. Бомж."));
            },

            // 3. Космонавт: гравитация сломалась
            (p, level, rnd) -> {
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200, 2));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
                p.sendSystemMessage(Component.literal("§bГравитация отдыхает 10 секунд."));
            },

            // 4. Крипер Левитан
            (p, level, rnd) -> {
                Creeper c = EntityType.CREEPER.create(level);
                if (c == null) return;
                c.moveTo(p.getX() + 3, p.getY(), p.getZ(), 0, 0);
                c.setCustomName(Component.literal("Левитан"));
                c.setCustomNameVisible(true);
                level.addFreshEntity(c);
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§c<Левитан> ВНИМАНИЕ. ГОВОРИТ ЛЕВИТАН. ЗА ТОБОЙ КРИПЕР."), false);
            },

            // 5. Куриный дождь
            (p, level, rnd) -> {
                for (int i = 0; i < 10; i++) {
                    Chicken ch = EntityType.CHICKEN.create(level);
                    if (ch == null) continue;
                    ch.moveTo(p.getX() + rnd.nextInt(7) - 3, p.getY() + 12 + rnd.nextInt(5),
                            p.getZ() + rnd.nextInt(7) - 3, rnd.nextFloat() * 360f, 0);
                    level.addFreshEntity(ch);
                }
                p.sendSystemMessage(Component.literal("§eС неба посыпались курицы."));
            },

            // 6. Внезапные алмазы
            (p, level, rnd) -> {
                ItemEntity item = new ItemEntity(level, p.getX(), p.getY() + 1, p.getZ(),
                        new ItemStack(Items.DIAMOND, 1 + rnd.nextInt(5)));
                level.addFreshEntity(item);
                p.sendSystemMessage(Component.literal("§aПодарок от вселенной. Пока."));
            },

            // 7. Молния просто для красоты
            (p, level, rnd) -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt == null) return;
                bolt.moveTo(p.getX(), p.getY(), p.getZ());
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
                p.sendSystemMessage(Component.literal("§fТебя ударило. Но не больно."));
            },

            // 8. ТНТ под ногами
            (p, level, rnd) -> {
                PrimedTnt tnt = new PrimedTnt(level, p.getX(), p.getY(), p.getZ(), null);
                tnt.setFuse(80);
                level.addFreshEntity(tnt);
                p.sendSystemMessage(Component.literal("§4Бежим. У тебя 4 секунды."));
            },

            // 9. Бог в чате
            (p, level, rnd) -> {
                String phrase = GOD_PHRASES[rnd.nextInt(GOD_PHRASES.length)];
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("§5<Бог> " + phrase), false);
            }
    );

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        RandomSource rnd = level.getRandom();
        if (rnd.nextFloat() > CHANCE) return;

        EFFECTS.get(rnd.nextInt(EFFECTS.size())).run(player, level, rnd);
    }
}

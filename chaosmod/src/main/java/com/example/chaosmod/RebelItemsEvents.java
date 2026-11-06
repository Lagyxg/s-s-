package com.example.chaosmod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** «Бунт предметов»: у вещей появляется характер. */
public class RebelItemsEvents {

    // --- настройки ---
    /** Базовый шанс, что меч откажется бить (растёт с износом). */
    private static final float SWORD_BASE_REFUSE = 0.10f;
    /** Насколько добавляется шанс при полностью убитом мече. */
    private static final float SWORD_WEAR_BONUS = 0.30f;
    /** Шанс, что кирка выскажется при ломании блока. */
    private static final float PICKAXE_RANT_CHANCE = 0.12f;
    /** Шанс, что яблоко сбежит вместо того, чтобы быть съеденным. */
    private static final float APPLE_ESCAPE_CHANCE = 0.30f;
    /** Шанс, что съеденная еда выскажет претензию. */
    private static final float FOOD_WHINE_CHANCE = 0.20f;

    private static final String[] SWORD_MOODS = {
            "Не в настроении.",
            "Сегодня без драк, у меня давление.",
            "Я пацифист с понедельника.",
            "Бей кулаками, они у тебя всё равно есть.",
            "Я вообще-то в отпуске."
    };

    private static final String[] SWORD_TIRED = {
            "Я устал. Почини меня, изверг.",
            "У меня трещины на душе и на лезвии.",
            "Ещё один удар, и я напишу заявление."
    };

    private static final String[] PICKAXE_WRONG = {
            "Ты что делаешь?! Я для этого не создана!",
            "Этим блоком надо лопатой, а не мной!",
            "Ты меня неправильно используешь!!!",
            "Мне стыдно за нас обоих."
    };

    private static final String[] PICKAXE_RANTS = {
            "Опять копать?! А спасибо где?",
            "Я столько блоков сломала, а ты даже не поблагодарил.",
            "Ещё чуть-чуть, и я уйду к другому игроку.",
            "Знаешь, у меня тоже есть чувства. Каменные, но есть.",
            "Давай хоть чай сделаем перерыв."
    };

    private static final String[] APPLE_LINES = {
            "Не ешь меня!!!",
            "Я хочу жить! Я только что созрело!",
            "Прощай, жестокий мир!",
            "Лови меня, если сможешь."
    };

    private static final String[] FOOD_WHINES = {
            "%s: Ты меня сожрал. Я всё помню.",
            "%s: Это было больно, между прочим.",
            "%s: Передай моей семье, что я был вкусным.",
            "%s: Я хотел стать кем-то большим."
    };

    private static String pick(String[] arr, RandomSource rnd) {
        return arr[rnd.nextInt(arr.length)];
    }

    private static void say(ServerPlayer p, String color, String who, String msg) {
        p.sendSystemMessage(Component.literal(color + "<" + who + "> " + msg));
    }

    // 1. Меч отказывается бить, и чем он потрёпаннее, тем чаще
    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SwordItem)) return;

        RandomSource rnd = player.getRandom();
        float wear = stack.getMaxDamage() > 0
                ? stack.getDamageValue() / (float) stack.getMaxDamage()
                : 0f;
        float chance = SWORD_BASE_REFUSE + SWORD_WEAR_BONUS * wear;

        if (rnd.nextFloat() < chance) {
            event.setCanceled(true);
            String line = wear > 0.5f ? pick(SWORD_TIRED, rnd) : pick(SWORD_MOODS, rnd);
            say(player, "§7", stack.getHoverName().getString(), line);
        }
    }

    // 2. Кирка орёт в чат
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PickaxeItem)) return;

        RandomSource rnd = player.getRandom();
        boolean wrongTool = !player.hasCorrectToolForDrops(event.getState());

        // если блок не для кирки, возмущается почти всегда
        if (wrongTool && rnd.nextFloat() < 0.5f) {
            say(player, "§c", stack.getHoverName().getString(), pick(PICKAXE_WRONG, rnd));
        } else if (rnd.nextFloat() < PICKAXE_RANT_CHANCE) {
            say(player, "§e", stack.getHoverName().getString(), pick(PICKAXE_RANTS, rnd));
        }
    }

    // 3. Яблоко убегает из руки
    @SubscribeEvent
    public void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.APPLE)) return;

        RandomSource rnd = player.getRandom();
        if (rnd.nextFloat() >= APPLE_ESCAPE_CHANCE) return;

        ItemStack held = player.getMainHandItem().is(Items.APPLE)
                ? player.getMainHandItem()
                : player.getOffhandItem();
        if (!held.is(Items.APPLE)) return;

        event.setCanceled(true);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        ItemEntity apple = new ItemEntity(player.level(),
                player.getX(), player.getEyeY() - 0.3, player.getZ(),
                new ItemStack(Items.APPLE, 1));
        apple.setPickUpDelay(40);
        Vec3 look = player.getLookAngle();
        apple.setDeltaMovement(
                look.x * 0.6 + (rnd.nextDouble() - 0.5) * 0.4,
                0.35,
                look.z * 0.6 + (rnd.nextDouble() - 0.5) * 0.4);
        player.level().addFreshEntity(apple);

        say(player, "§a", "Яблоко", pick(APPLE_LINES, rnd));
    }

    // 4. Еда жалуется после того, как её съели
    @SubscribeEvent
    public void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!stack.isEdible()) return;

        RandomSource rnd = player.getRandom();
        if (rnd.nextFloat() >= FOOD_WHINE_CHANCE) return;

        String name = stack.getHoverName().getString();
        player.sendSystemMessage(Component.literal(
                "§6" + String.format(pick(FOOD_WHINES, rnd), name)));
    }
}

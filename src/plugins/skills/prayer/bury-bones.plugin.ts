import { itemInteractionActionHandler } from '@engine/action';
import { findItem, widgets } from '@engine/config';
import { Achievements, Skill, giveAchievement } from '@engine/world/actor';
import { animationIds, soundIds } from '@engine/world/config';

const action: itemInteractionActionHandler = (details) => {
    const { player, option } = details;

    if (option !== 'bury') return;

    if (!player.canMove()) return;

    // bones can be buried only if prayerBuryXp is defined, but they can also
    // grant zero xp - this checks for that edge case
    if (!details.itemDetails.metadata.prayerBuryXp && details.itemDetails.metadata.prayerBuryXp !== 0) {
        return;
    }

    player.sendMessage(`You bury the ${details.itemDetails.name}.`);

    player.playAnimation(animationIds.buryBones);
    player.removeItem(details.itemSlot);
    player.playSound(soundIds.buryBones);
    player.skills.addExp(Skill.PRAYER, details.itemDetails.metadata.prayerBuryXp);

    giveAchievement(Achievements.BURY_BONES, player);
};

const allBones: number[] = [
    findItem('rs:bones')?.gameId,
    findItem('rs:bones_burnt')?.gameId,
    findItem('rs:bones_wolf')?.gameId,
    findItem('rs:bones_bat')?.gameId,
    findItem('rs:bones_big')?.gameId,
    findItem('rs:bones_dagannoth')?.gameId,
    findItem('rs:bones_babydragon')?.gameId,
    findItem('rs:bones_dragon')?.gameId,
    findItem('rs:bones_wyvern')?.gameId,
    findItem('rs:bones_monkey_normal')?.gameId,
    findItem('rs:bones_monkey_small_zombie')?.gameId,
    findItem('rs:bones_monkey_large_zombie')?.gameId,
    findItem('rs:bones_monkey_gorilla')?.gameId,
    findItem('rs:bones_monkey_bearded_gorilla')?.gameId,
    findItem('rs:bones_monkey_small_ninja')?.gameId,
    findItem('rs:bones_monkey_medium_ninja')?.gameId,
    findItem('rs:bones_monkey_skeleton_gorilla')?.gameId,
    findItem('rs:bones_jogre')?.gameId,
    findItem('rs:bones_zogre')?.gameId,
    findItem('rs:bones_fayrg')?.gameId,
    findItem('rs:bones_raurg')?.gameId,
    findItem('rs:bones_ourg')?.gameId,
].filter(id => typeof id === 'number') as number[];

export default {
    pluginId: 'rs:prayer_bury_bones',
    hooks: [
        {
            type: 'item_interaction',
            widgets: widgets.inventory,
            options: 'bury',
            itemIds: allBones,
            handler: action,
            cancelOtherActions: true
        }
    ]
};

import { ActorTask } from '@engine/task/impl';
import { Actor, Player } from '@engine/world/actor';
import { ObjectInteractionAction } from '../object-interaction.action';
import { ItemOnObjectAction } from '../item-on-object.action';
import { ActionHook } from '@engine/action/hook';
import { Task } from '@engine/task';

/**
 * The result of running a callback function will be recorded here so that the
 * `QueueableTask` can make a choice to continue looping and/or to enqueue
 * the desired task.
 */
export interface QueueableTaskEval {
    callbackResult: boolean;
    shouldContinueLooping: boolean;
}

/**
 * All actions supported by this plugin task.
 */
type ObjectAction = ObjectInteractionAction | ItemOnObjectAction;

/**
 * An ActionHook for a supported ObjectAction.
 */
type ObjectActionHook<TAction extends ObjectAction> = ActionHook<TAction, (data: TAction) => void>;

/**
 * The data unique to the action being executed (i.e. excluding shared data)
 */
type ObjectActionData<TAction extends ObjectAction> = Omit<TAction, 'player' | 'actor' | 'object' | 'position'>;

/**
 * Processes the provided callback function on every single tick, and also allows any
 * arbitrary base task to be queued at the next tick. Useful for queuing random
 * movements, as well as other things.
 *
 * Can loop infinitely based on the result of the passed in `callback` function.
*/
export class QueueableTask<TAction extends ObjectAction> extends ActorTask <Player | Actor> {
    /**
     * The plugins to execute on each tick. These will not get called if the
     * `callback` indicates a halting condition.
     */
    private plugins: ObjectActionHook<TAction>[];

    private data: ObjectActionData<TAction> | null;

    /**
     * Optionally provided base task to enqueue on each tick. Can be `null`.
     */
    private task: Task | null;

    /**
     * This callback function will be executed on every tick. It must return a
     * `QueueableTaskEval` so that this loop can determine if it should
     * terminate or continue looping.
     */
    private callback: () => QueueableTaskEval;

    constructor(plugins: ObjectActionHook<TAction>[], actor: Player | Actor, callback: () => QueueableTaskEval, task: Task | null, data: ObjectActionData<TAction> | null) {
        super(
            actor,
        );

        this.plugins = plugins;
        this.data = data;
        this.task = task;
        this.callback = callback;
    }

    /**
     * Executed every tick. Depending on the callback value, this task can stop
     * future executions.
     */
    public execute(): void {
        const ev = this.callback();
        if (!ev.callbackResult) {
            if (!ev.shouldContinueLooping) {
                this.stop()
            }
            return;
        }

        if (this.task) { // only gets executed if the callback returns true for its result
            this.actor.enqueueBaseTask(this.task)
        }

        // call the relevant plugins on each tick, if provided
        this.plugins.forEach(plugin => {
            if (!plugin || !plugin.handler) {
                return;
            }

            const action = {
                player: this.actor,
                ...this.data
            } as TAction;

            plugin.handler(action);
        });


        if (!ev.shouldContinueLooping) {
            this.stop();
            return
        }
    }
}

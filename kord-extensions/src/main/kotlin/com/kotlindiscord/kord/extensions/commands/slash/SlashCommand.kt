package com.kotlindiscord.kord.extensions.commands.slash

import com.kotlindiscord.kord.extensions.InvalidCommandException
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.commands.slash.parser.SlashCommandParser
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.event.interaction.InteractionCreateEvent

/**
 * Class representing a slash command.
 *
 * You shouldn't need to use this class directly - instead, create an [Extension] and use the
 * [slash command function][Extension.slashCommand] to register your command, by overriding the [Extension.setup]
 * function.
 *
 * @param extension The [Extension] that registered this command.
 */
public open class SlashCommand<T : Arguments>(extension: Extension) : Command(extension) {
    /** Arguments object builder for this command, if it has arguments. **/
    public open var arguments: (() -> T)? = null

    /** Command description, as displayed on Discord. **/
    public open lateinit var description: String

    /** @suppress **/
    public open lateinit var body: suspend SlashCommandContext<out T>.() -> Unit

    /** Guild ID this slash command is to be registered for, if any. **/
    public open var guild: Snowflake? = null

    /** Whether to send a message on discord showing the command invocation. **/
    public open var showSource: Boolean = false

    /** @suppress **/
    public open val checkList: MutableList<suspend (InteractionCreateEvent) -> Boolean> = mutableListOf()

    public override val parser: SlashCommandParser = SlashCommandParser(extension.bot)

    /**
     * An internal function used to ensure that all of a command's required properties are present.
     *
     * @throws InvalidCommandException Thrown when a required property hasn't been set.
     */
    @Throws(InvalidCommandException::class)
    public override fun validate() {
        super.validate()

        if (!::description.isInitialized) {
            throw InvalidCommandException(name, "No command description given.")
        }

        if (!::body.isInitialized) {
            throw InvalidCommandException(name, "No command action given.")
        }
    }

    // region: DSL functions

    /** Specify a specific guild for this slash command. **/
    public open fun guild(guild: Snowflake) {
        this.guild = guild
    }

    /** Specify a specific guild for this slash command. **/
    public open fun guild(guild: Long) {
        this.guild = Snowflake(guild)
    }

    /** Specify a specific guild for this slash command. **/
    public open fun guild(guild: GuildBehavior) {
        this.guild = guild.id
    }

    /** Specify the arguments builder for this slash command. **/
    public open fun arguments(args: () -> T) {
        this.arguments = args
    }

    /**
     * Define what will happen when your command is invoked.
     *
     * @param action The body of your command, which will be executed when your command is invoked.
     */
    public open fun action(action: suspend SlashCommandContext<out T>.() -> Unit) {
        this.body = action
    }

    /**
     * Define a check which must pass for the command to be executed.
     *
     * A command may have multiple checks - all checks must pass for the command to be executed.
     * Checks will be run in the order that they're defined.
     *
     * This function can be used DSL-style with a given body, or it can be passed one or more
     * predefined functions. See the samples for more information.
     *
     * @param checks Checks to apply to this command.
     */
    public open fun check(vararg checks: suspend (InteractionCreateEvent) -> Boolean) {
        checks.forEach { checkList.add(it) }
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    public open fun check(check: suspend (InteractionCreateEvent) -> Boolean) {
        checkList.add(check)
    }

    // endregion

    /** Run checks with the provided [InteractionCreateEvent]. Return false if any failed, true otherwise. **/
    public open suspend fun runChecks(event: InteractionCreateEvent): Boolean {
        for (check in checkList) {
            if (!check.invoke(event)) {
                return false
            }
        }
        return true
    }
}
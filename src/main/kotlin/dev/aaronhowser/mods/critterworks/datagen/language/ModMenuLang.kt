package dev.aaronhowser.mods.critterworks.datagen.language

object ModMenuLang {

	const val CONTAINER_EMPTY = "tooltip.critterworks.container.empty"
	const val CONTAINER_STACK = "tooltip.critterworks.container.stack"
	const val CONTAINER_STACKS = "tooltip.critterworks.container.stacks"
	const val WHITELIST = "tooltip.critterworks.filter.whitelist"
	const val BLACKLIST = "tooltip.critterworks.filter.blacklist"
	const val FILTER_ITEM = "tooltip.critterworks.filter.item"
	const val FILTER_ITEM_TAG = "tooltip.critterworks.filter.item_tag"
	const val FILTER_FLUID = "tooltip.critterworks.filter.fluid"
	const val FILTER_FLUID_TAG = "tooltip.critterworks.filter.fluid_tag"
	const val FILTER_IGNORE_COMPONENTS = "tooltip.critterworks.filter.ignore_components"
	const val FILTER_IGNORE_DAMAGE = "tooltip.critterworks.filter.ignore_damage"
	const val FILTER_REQUIRE_COMPONENTS = "tooltip.critterworks.filter.require_components"
	const val FILTER_LIST_POINT = "tooltip.critterworks.filter.list_point"
	const val WEB_PORT_COLOR = "menu.critterworks.web_port.color"
	const val WEB_PORT_INPUT = "menu.critterworks.web_port.input"
	const val WEB_PORT_OUTPUT = "menu.critterworks.web_port.output"
	const val WEB_PORT_PRIORITY = "menu.critterworks.web_port.priority"
	const val WEB_PORT_COLOR_TOOLTIP = "tooltip.critterworks.web_port.color"
	const val WEB_PORT_INPUT_TOOLTIP = "tooltip.critterworks.web_port.input"
	const val WEB_PORT_OUTPUT_TOOLTIP = "tooltip.critterworks.web_port.output"
	const val WEB_PORT_PRIORITY_TOOLTIP = "tooltip.critterworks.web_port.priority"
	const val SPIDER_NEST_TITLE = "menu.critterworks.spider_nest.title"
	const val SPIDER_NEST_SPIDER = "menu.critterworks.spider_nest.spider"
	const val SPIDER_NEST_POSITION = "menu.critterworks.spider_nest.position"
	const val SPIDER_NEST_IDLE = "menu.critterworks.spider_nest.idle"
	const val SPIDER_NEST_WANDERING = "menu.critterworks.spider_nest.wandering"
	const val SPIDER_NEST_COLLECTING = "menu.critterworks.spider_nest.collecting"
	const val SPIDER_NEST_DELIVERING = "menu.critterworks.spider_nest.delivering"
	const val SPIDER_NEST_WAITING = "menu.critterworks.spider_nest.waiting"
	const val SPIDER_NEST_RETURNING_ITEM = "menu.critterworks.spider_nest.returning_item"
	const val SPIDER_NEST_RETURNING = "menu.critterworks.spider_nest.returning"
	const val SPIDER_NEST_FAILURE = "menu.critterworks.spider_nest.failure"
	const val WEB_FLUID = "tooltip.critterworks.artificial_spinnerets.web_fluid"
	const val WEB_FLUID_INSTRUCTIONS = "tooltip.critterworks.artificial_spinnerets.web_fluid_instructions"
	const val SHIFT_FOR_INFO = "tooltip.critterworks.shift_for_info"

	fun add(provider: ModLanguageProvider) {
		fun add(key: String, value: String) = provider.add(key, value)

		add(CONTAINER_EMPTY, "Empty")
		add(CONTAINER_STACK, "Contains %s stack")
		add(CONTAINER_STACKS, "Contains %s stacks")
		add(WHITELIST, "Whitelist")
		add(BLACKLIST, "Blacklist")
		add(FILTER_ITEM, "Specific Item")
		add(FILTER_ITEM_TAG, "Item Tag")
		add(FILTER_FLUID, "Specific Fluid")
		add(FILTER_FLUID_TAG, "Fluid Tag")
		add(FILTER_IGNORE_COMPONENTS, "Ignore Components")
		add(FILTER_IGNORE_DAMAGE, "Ignore Damage")
		add(FILTER_REQUIRE_COMPONENTS, "Require Matching Components")
		add(FILTER_LIST_POINT, "• %s")
		add(WEB_PORT_COLOR, "Color: %s")
		add(WEB_PORT_INPUT, "Input")
		add(WEB_PORT_OUTPUT, "Output")
		add(WEB_PORT_PRIORITY, "Priority")
		add(SPIDER_NEST_TITLE, "Hopping Spiders")
		add(SPIDER_NEST_SPIDER, "Spider %s")
		add(SPIDER_NEST_POSITION, "Position: %s, %s, %s")
		add(SPIDER_NEST_IDLE, "Job: Idle")
		add(SPIDER_NEST_WANDERING, "Job: Wandering")
		add(SPIDER_NEST_COLLECTING, "Job: Collecting %s items")
		add(SPIDER_NEST_DELIVERING, "Job: Delivering %s")
		add(SPIDER_NEST_WAITING, "Job: Waiting (%s)")
		add(SPIDER_NEST_RETURNING_ITEM, "Job: Returning item (%s)")
		add(SPIDER_NEST_RETURNING, "Job: Returning to nest")
		add("$SPIDER_NEST_FAILURE.destination_missing", "destination missing")
		add("$SPIDER_NEST_FAILURE.source_missing", "source missing")
		add("$SPIDER_NEST_FAILURE.destination_not_output", "output disabled")
		add("$SPIDER_NEST_FAILURE.channel_changed", "channel changed")
		add("$SPIDER_NEST_FAILURE.filter_changed", "filter changed")
		add("$SPIDER_NEST_FAILURE.destination_unavailable", "inventory unavailable")
		add("$SPIDER_NEST_FAILURE.destination_full", "destination full")
		add(WEB_PORT_COLOR_TOOLTIP, "Color: %s")
		add(WEB_PORT_INPUT_TOOLTIP, "Input")
		add(WEB_PORT_OUTPUT_TOOLTIP, "Output")
		add(WEB_PORT_PRIORITY_TOOLTIP, "Priority: %s")
		add(WEB_FLUID, "Web Fluid: %s / %s")
		add(WEB_FLUID_INSTRUCTIONS, "Click String onto the Spinnerets to refill them")
		add(SHIFT_FOR_INFO, "[Shift for Info]")
	}
}
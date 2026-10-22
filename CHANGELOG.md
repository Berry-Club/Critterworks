# 1.5.0

### Added

- Snail Shell
	- Makes you immortal while active
	- Activates while sneaking with it in either hand
	- Reduces movement speed by 90% while active
	- After standing still for 5 seconds, most mobs forget and cannot target you
	- Also cannot left or right click during this
- Punchprawns
	- New mob that spawns around Ocean Monuments
	- Defaults to being hostile but can be tamed with Pearls
		- Follow you around while tamed, in and out of water
		- Sneak right-click your tamed Punchprawn to put them on your head
			- When riding your head, bare-handed melee attacks become much stronger
            - Sneak right-click the ground with an empty hand to place it back down again
- Pearl
	- Small chance of being dropped when giving Sand to Filterfeeders
- Several advancements

### Changed

- Hopping Spiders can now be worn in your head slot

### Fixed

- Lockbox items were opening when one attached to a Scoochworm was also open (#2)

# 1.4.0

### Added

- Item/Fluid Filterfeeders
	- New clam mobs found in ocean and river biomes
	- Click to collect as an item, works like an Item/Fluid Filter
	- Drop the item in water to turn it back into a mob
	- Also removes water fog within a radius around them

### Changed

- Tardigrades drop from 1% of Moss Blocks, increasing with Fortune
- Schoochstem vines now generate along Lush Cave surfaces
- The Lockbox can now take an Item Filterfeeder to limit what can go into it
- Updated the Lockbox's screen texture

### Fixed

- Fixed the config for the Pollen Point radius not being respected

### Removed

- Item Filter
	- Aliased it to the new item though

# 1.3.0

### Added

- Sparkpollen Pouch
- Sparkbug Bush
	- New block used to transfer FE wirelessly, can be found rarely in forests and swamps
	- Bind the Pouch to a Bush, and then mark nearby block faces as inputs or outputs
	- Every tick it tries to transfer energy from inputs to outputs
		- Inputs spread their FE equally across all outputs that accept
		- Firefly particles spawn near Pollen Points that are doing work
- Tardigrade
	- Craft with any arbitrary itemstack to make it unkillable while dropped
		- Technically only applies to damage sources that use the tag `#critterworks:tardigrade_protects_from`
			- Only includes `#c:is_environment` by default
		- Config that makes it also disable expiring after 5 minutes
	- Craft a Tardigraded item with a Potion of Poison to remove it

### Changed

- When riding a Scoochworm, your view is rotated to match its rotation
	- Aka, when you're on the ceiling, the world looks upside down
	- This can be disabled in the client config
- Added recipes to everything that was missing one

# 1.2.0

### Added

- Debug client config that renders the movement path of every Scoochworm
- Debug server config that sends a clickable message in chat to tp to structures as they spawn
- Hopping Spider animations
	- 4 animations for each combination of moving/still and holding/not holding an item

### Changed

- Using a Critter Cage while not sneaking will place the Scoochworm instead of placing the block
- New texture for the Scoochworm Depot and Stem-Encased Comparator
- New texture and model for the Hopping Spider Nest
- If Spiders have no path to their nest, they reset
- Natural Hopping Spider Nests spawn with fewer spiders, especially when they have smaller webs
- Hopping Spider nest and Scoochworm Apple worldgen rarity has been adjusted
- You can now configure the scale that Hopping Spiders render at on Web Lines

### Fixed

- Fixed Lockbox contents being lost after reloading a world
- Fixed Scoochworms that are on a wall when the world loads always moving downwards
- Hopefully fixed the crash when Scoochworm Apples spawn
- Fixed Critter Cage missing localization

# 1.1.0

### Added

- Scoochworm GPS
	- Can be used on a Scoochworm Segment to attach a Chunkloader
	- Configurable max of 15 per player, loads in a plus shape centered on the head

### Changed

- Changed the layout of the Web Port menu
- Web Line and Node interactions are now their own events

### Fixed

- Fixed Web Lines only rendering if Iris was installed (????)

# 1.0.0

- Initial alpha release
- Everything is broken

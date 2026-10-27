# 1.5.0

### Added

- Punchprawns
	- New mob that spawns around Ocean Monuments
	- Defaults to being hostile but can be tamed with Pearls
		- Follow you around while tamed, in and out of water
		- Sneak right-click your tamed Punchprawn to put them on your head
			- When riding your head, bare-handed melee attacks become much stronger
			- Right-click a block to place it back down again
- Sproutsnail
	- New mob
	- Acts as a flower pot - you can plant certain plant items in their shell
		- This isn't AI I just like em-dashes and couldn't figure out a different punctuation :(
	- Has a configurable 20% chance to Bone Meal applicable blocks it travels through
	- Prefers moving toward farther bonemealable blocks nearby
	- Adults passively drop Snail Slime every 5–10 minutes
	- Has a 10% chance to drop a Snail Shell Fragment when killed by a player
- Snail Shield
	- Repaired with Snail Shell Fragments
	- Makes you immortal while active
	- Activates while sneaking with it in either hand
		- Greatly reduces movement speed
		- Also cannot left or right click
		- After standing still for 5 seconds, most mobs forget and cannot target you
- The Snail That Kills You
	- Has a 1% chance of appearing when a Sproutsnail is killed by the player
		- Chance is configurable
		- Does not spawn if you already have one chasing you
	- Constantly moves in a straight line towards you
		- Goes through blocks
		- Moves 0.05 blocks per tick by default (configurable)
		- Only moves while the player is online and in the same dimension
	- Can be caught in a Critter Cage maybe possibly 👉👈
	- When The Snail That Kills You reaches you, it kills you and ends the chase
		- You can permanently end the chase by tanking the hit 5 times with an active Snail Shield (configurable)
	- Has commands under `/critterworks snail-chase`
- Pearl
	- Has a 10% chance of being produced when giving Sand to Filterfeeders
- Snail Slime
	- Acts like Bone Meal
- Snail Shell Fragment
- Several advancements

### Changed

- Hopping Spiders can now be worn in your head slot
- Scoochworm parts have been given the `#c:capturing_not_supported` and `#c:teleporting_not_supported` tags
- The Critter Cage can now capture most serializable living entities lacking the `#c:capturing_not_supported` tag
- Configured Filterfeeders are set as persistent
- Holding Sand will make a Filterfeeder stop moving, so you can more easily feed it

### Fixed

- Lockbox items were opening when one attached to a Scoochworm was also open (#2)

### Removed

- Web Pathfinder
	- This was a debug item that serves no purpose any more

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
	- Craft with almost any item stack to protect it while dropped
		- Technically only applies to damage sources that use the tag `#critterworks:tardigrade_protects_from`
			- Only includes `#neoforge:is_environment` by default
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
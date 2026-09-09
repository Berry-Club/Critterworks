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
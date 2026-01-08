# BoxServer

A Minecraft Paper 1.21.8+ plugin for managing a "Box Server" - a cube arena with spawn and PvP regions with specific protection rules.

## Requirements

- **Minecraft Version:** 1.21.8+
- **Java Version:** 21
- **Server Software:** Paper

## Building

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory.

## Installation

1. Build the plugin using Maven
2. Copy the JAR file to your server's `plugins/` directory
3. Restart your server

## Region Types

### Spawn Region (Center Rectangle)
- Block Breaking: DISABLED (except for whitelisted blocks)
- Block Placing: DISABLED
- Block Interaction: ENABLED (doors, buttons, levers, etc.)
- Wind Charges: DISABLED
- PvP/Player Damage: DISABLED
- Player Pushing: DISABLED

**Default Whitelisted Blocks:**
- Stone
- Spruce Logs
- Diamond Ore (and Deepslate variant)
- Emerald Ore (and Deepslate variant)
- Iron Ore (and Deepslate variant)
- Lapis Ore (and Deepslate variant)
- Gold Ore (and Deepslate variant)
- Mob Spawner

### PvP Area
- Block Breaking: ENABLED
- Block Placing: ENABLED (height restricted to 6 blocks above ceiling)
- Wind Charges: ENABLED
- PvP/Player Damage: ENABLED
- Auto-Reset: Every 10 minutes (configurable), all player-placed blocks are removed

### Protected Sub-Regions
- Block Breaking: DISABLED
- Block Placing: DISABLED
- PvP: ENABLED

### Boundary Region
- Used for walls, floor, and ceiling of the arena
- Cannot be broken or placed upon

## Commands

### Region Management
```
/boxserver region create <name> <type> - Create a new region (types: spawn, pvp, protected, boundary)
/boxserver region delete <name> - Delete a region
/boxserver region pos1 - Set first corner position
/boxserver region pos2 - Set second corner position
/boxserver region list - List all regions
/boxserver region info <name> - Get region info
/boxserver region priority <name> <priority> - Set region priority (higher = takes precedence)
```

### Block Whitelist (for spawn area)
```
/boxserver blocks add <region> <material> - Add a block that can be mined
/boxserver blocks remove <region> <material> - Remove a block from the whitelist
/boxserver blocks list <region> - List all whitelisted blocks
/boxserver blocks clear <region> - Clear all whitelisted blocks
```

### Configuration
```
/boxserver reload - Reload configuration
/boxserver reset <region> - Manually reset all placed blocks in a region
/boxserver setresettime <minutes> - Set the auto-reset interval
```

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `boxserver.admin` | Bypass all protection rules | op |
| `boxserver.command.region` | Manage regions | op |
| `boxserver.command.blocks` | Manage block whitelists | op |
| `boxserver.command.reload` | Reload configuration | op |
| `boxserver.command.reset` | Manual reset command | op |
| `boxserver.bypass.build` | Bypass build restrictions | false |
| `boxserver.bypass.pvp` | Bypass PvP restrictions | false |

## Configuration (config.yml)

```yaml
reset-interval-minutes: 10
messages:
  no-break: "&cYou cannot break blocks here!"
  no-place: "&cYou cannot place blocks here!"
  no-pvp: "&cPvP is disabled in this area!"
  no-windcharge: "&cWind charges are disabled in this area!"
  reset-warning: "&eBlock reset in %time% seconds!"
  reset-complete: "&aAll placed blocks have been reset!"
```

## Features

- Tab completion for all commands
- Colored messages with & color code support
- Efficient chunk-based block tracking
- Warning messages before block reset (60s, 30s, 10s, 5s)
- Protection against pistons, explosions, water/lava flow
- Ender pearl protection in spawn areas
- Persistent region and block data storage

## Data Storage

- Regions are stored in `plugins/BoxServer/regions.yml`
- Placed blocks are tracked in `plugins/BoxServer/placed-blocks.yml`

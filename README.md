# Bedwars Item Tracker

Bedwars Item Tracker is an utility Fabric mod, which can be used to track item
spawns in forges on bedwars servers, to measure the the rate and count of items
spawned over time. The collected data can be saved to a csv file, and then
analyzed using a python script included in this repository.

# Usage

## Collecting data

- Download the mod from the releases page and put in in your `mods` folder,
  along with Fabric API, for 1.21.8.
- Join a bedwars game on some server.
- When the game countdown reaches 0 and the actual game starts run the
  `/bwtracker reset` command to start collecting data with correct timing.
- The mod is going to start counting items that will spawn in your forge. Do
  note, that if you will go too far away from your base the items will not be
  counted, because the server will not send updates. Even going to a neighbor
  island or dying will stop items from being counted, until you return/respawn
  back at the base.
- At any point you can run `/bwtracker status` to see how many items have been
  counted.
- Once the game ends, or when you wish to save the data run
  `/bwtracker save [name]`, with `name` being an optional prefix appended to the
  csv file name.
- A csv file named `[prefix]_drops_YYYY-MM-DD_HH-MM.csv` will be created in your
  `.minecraft` directory.

## Commands

`/bwtracker status` - Shows current status of counters
`/bwtracker save [name]` - Saves the data to files `/bwtracker reset` - Resets
the counters, and internal state, which is needed to be reset every game to
correctly gather data. `/bwtracker mode <spawn/pickup>` - Changes item tracking
mode. By default spawn mode is used, which tracks item entities being spawned
within 0.5 block distance from your forge. Pickup mode tracks items being added
to your hotbar, which is worse, becaus it means you need to sit in the forge all
the time.

## Analyzing the data

- Install [Python](https://www.python.org/)
- Install [uv](https://docs.astral.sh/uv/)
- Clone / download this repository somewhere on your pc and open a terminal in
  that directory
- Run `uv run main.py <path_to_the_csv_file> [group_spawns]` The `group_spawns`
  paramter is an optional true/false argument which specifies whether many items
  spawned within 100ms time window should be treated as one spawn with the count
  added up. By default (when no argument is specified) this is true, because
  Hypixel spawns each item spearately leading to many items being spawned at
  once.
- A chart will show up for every item type, showing the time elapsed in seconds
  from previous item spawn plotted against time. Spawns will automatically be
  clustered into groups, and for each group an average duration between spawns
  will be calculated and printed in the terminal.

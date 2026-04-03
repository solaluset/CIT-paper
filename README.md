# CIT-paper
This plugin aims to replace usage of CIT Resewn and OptiFine for custom item textures and models.

It includes automatic resourcepack conversion to ensure seamless transition (some features are still in development).

The plugin is available on [Modrinth](https://modrinth.com/plugin/cit-paper).

## How to use?
1. Drop the plugin into the plugins folder
2. Start the server
3. Put your resource pack(s) into input directory
4. Set the output directory where converted resource packs will go
5. Restart the server - converted resource pack(s) will be put into output directory and config(s) will be generated
6. Enable the converted resource pack(s), for example by uploading to https://mc-packs.net/ - now you can rename an item and it will work just like with CIT Resewn (hopefully)

## Compatibility 
Mainly tested on Paper and Bukkit. Should also work on Spigot and Folia.

## Reporting issues / asking questions 
For now everything is concentrated in the Discord server. Please join to communicate!

## Technical details
This works by setting item_model component when items acquire corresponding state (it can be renaming, enchanting, damaging etc). Resource pack structure is changed accordingly to work with vanilla clients.

## CLI converter
In case you run GitHub actions or any other CI/CD service, you can use a standalone [CLI jar](https://github.com/solaluset/CIT-paper/releases) for resource pack conversion. Please note it's still necessary to install CIT-paper on the server.

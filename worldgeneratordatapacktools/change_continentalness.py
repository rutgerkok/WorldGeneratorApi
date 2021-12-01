import os.path
import json
import shutil

# USER SETTINGS
NOISE_SETTINGS_PATH = "data/minecraft/worldgen/noise_settings/overworld.json"
DIMENSION_SETTINGS_PATH = "data/minecraft/dimension/overworld.json"
def FORMULA(continentalness: float):
    return continentalness + 0.5  # This increases the amount of ocean


# SOME HELPER FUNCTIONS
def modify_terrain_shaper(terrain_shaper):
    modify_location = terrain_shaper["coordinate"] == "continents"
    for point in terrain_shaper["points"]:
        if modify_location:
            point["location"] = FORMULA(point["location"])
        if isinstance(point["value"], dict):
            modify_terrain_shaper(point["value"])



# SCRIPT STARTS

# Get data pack folder
data_pack_folder = input("Please enter the path of your datapack folder\n")
if not os.path.exists(os.path.join(data_pack_folder, "pack.mcmeta")):
    print("Error: " + data_pack_folder + " does not contain a pack.mcmeta file")
    exit(1)

# Check files
noise_settings_file = os.path.join(data_pack_folder, NOISE_SETTINGS_PATH)
if not os.path.exists(noise_settings_file):
    print(f"Error: {os.path.abspath(noise_settings_file)} file not found")
    exit(1)

dimension_setting_file = os.path.join(data_pack_folder, DIMENSION_SETTINGS_PATH)
if not os.path.exists(dimension_setting_file):
    print(f"Error: {os.path.abspath(dimension_setting_file)} file not found")
    exit(1)

# Convert biome generator
with open(dimension_setting_file, "r") as handle:
    contents = json.load(handle)
    if not isinstance(contents["generator"]["biome_source"], dict):
        print(f"Error: biome_source in {DIMENSION_SETTINGS_PATH} must be a dictionary, not a preset")
        exit(1)
    if contents["generator"]["biome_source"]["type"] != "minecraft:multi_noise":
        print(f"Warning: biome_source in {DIMENSION_SETTINGS_PATH} is not of type minecraft:multi_noise, cannot modify it")
    else:
        for biome in contents["generator"]["biome_source"]["biomes"]:
            if "continentalness" not in biome["parameters"]:
                continue
            if isinstance(biome["parameters"]["continentalness"], float):
                biome["parameters"]["continentalness"] = FORMULA(biome["parameters"]["continentalness"])
            else:
                for i in range(len(biome["parameters"]["continentalness"])):
                    biome["parameters"]["continentalness"][i] = FORMULA(biome["parameters"]["continentalness"][i])
shutil.copy2(dimension_setting_file, dimension_setting_file + "_BACKUP")
with open(dimension_setting_file, "w") as handle:
    json.dump(contents, handle, indent="  ")

# Convert noise settings file
with open(noise_settings_file, "r") as handle:
    contents = json.load(handle)
    terrain_shaper = contents["noise"]["terrain_shaper"]
    modify_terrain_shaper(terrain_shaper["offset"])
    modify_terrain_shaper(terrain_shaper["factor"])
    modify_terrain_shaper(terrain_shaper["jaggedness"])
shutil.copy2(noise_settings_file, noise_settings_file + "_BACKUP")
with open(noise_settings_file, "w") as handle:
    json.dump(contents, handle, indent="  ")

print("Done!")


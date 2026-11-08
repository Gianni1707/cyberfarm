# The stations need their chunks loaded, so the building waits for that
execute in minecraft:overworld run forceload add 1998 1988 2150 2044
execute in minecraft:overworld run kill @e[type=cyberfarm:drone,x=1998,y=95,z=1988,dx=154,dy=40,dz=58]
execute in minecraft:overworld run tp @s 2008.5 102 1992.5 0 15
gamemode creative @s
give @s cyberfarm:survey_tool
give @s cyberfarm:dock_tank
give @s cyberfarm:hologram_table 9
give @s cyberfarm:sponge_module 4
give @s cyberfarm:tank_module 4
schedule function cyberfarm:test_setup/build 3s

setblock 2014 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["2 Sponge + Tank","+ Dock Tank: lava","stored in it, bar","in dock screen"]}}
fill 2015 100 2003 2021 100 2009 minecraft:stone_bricks
fill 2016 100 2004 2020 100 2008 minecraft:lava
setblock 2016 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:24576,drone:[I;1000,2,0,0],work_area:[I;2016,99,2004,2020,102,2008]}
setblock 2017 100 2001 cyberfarm:dock_tank[facing=west]
summon cyberfarm:drone 2016.5 100.4 2001.5 {UUID:[I;1000,2,0,0],modules:["cyberfarm:advanced_frame","cyberfarm:sponge_module","cyberfarm:tank_module"],energy:60000,dock:[I;2016,100,2001]}

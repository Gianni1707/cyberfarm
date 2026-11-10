setblock 2028 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["3 Sponge + Tank","no Dock Tank:","fills up to 32 B","then it stops"]}}
fill 2029 100 2003 2037 100 2011 minecraft:stone_bricks
fill 2030 100 2004 2036 100 2010 minecraft:water
setblock 2030 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:24576,drone:[I;1000,3,0,0],work_area:[I;2030,99,2004,2036,102,2010]}
summon cyberfarm:drone 2030.5 100.4 2001.5 {UUID:[I;1000,3,0,0],modules:["cyberfarm:advanced_frame","cyberfarm:sponge_module","cyberfarm:tank_module"],energy:60000,dock:[I;2030,100,2001]}

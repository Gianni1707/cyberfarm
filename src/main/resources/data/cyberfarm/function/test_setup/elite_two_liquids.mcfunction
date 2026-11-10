setblock 2042 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["4 Elite: Sponge","Tank, Transport","water + lava:","one kind a trip"]}}
fill 2043 100 2003 2047 100 2007 minecraft:stone_bricks
fill 2044 100 2004 2046 100 2006 minecraft:water
fill 2047 100 2003 2051 100 2007 minecraft:stone_bricks
fill 2048 100 2004 2050 100 2006 minecraft:lava
setblock 2044 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:24580,drone:[I;1000,4,0,0],work_area:[I;2044,99,2004,2050,102,2006]}
setblock 2045 100 2001 cyberfarm:dock_tank[facing=west]
summon cyberfarm:drone 2044.5 100.4 2001.5 {UUID:[I;1000,4,0,0],modules:["cyberfarm:elite_frame","cyberfarm:sponge_module","cyberfarm:tank_module","cyberfarm:transport_module"],energy:150000,dock:[I;2044,100,2001]}

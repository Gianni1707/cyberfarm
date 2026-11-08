setblock 2098 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["8 Elite: Harvester","Planter, Tank:","cuts, then plants","the seeds it got"]}}
fill 2100 100 2004 2106 100 2010 minecraft:farmland[moisture=7]
fill 2100 101 2004 2106 101 2010 minecraft:wheat[age=7]
setblock 2100 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:16393,drone:[I;1000,8,0,0],work_area:[I;2100,99,2004,2106,102,2010]}
summon cyberfarm:drone 2100.5 100.4 2001.5 {UUID:[I;1000,8,0,0],modules:["cyberfarm:elite_frame","cyberfarm:harvester_module","cyberfarm:planter_module","cyberfarm:tank_module"],energy:150000,dock:[I;2100,100,2001]}

setblock 2000 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["1 Sponge alone:","liquid thrown","away on the spot",""]}}
fill 2001 100 2003 2007 100 2009 minecraft:stone_bricks
fill 2002 100 2004 2006 100 2008 minecraft:water
setblock 2002 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:8192,drone:[I;1000,1,0,0],work_area:[I;2002,99,2004,2006,102,2008]}
summon cyberfarm:drone 2002.5 100.4 2001.5 {UUID:[I;1000,1,0,0],modules:["cyberfarm:basic_frame","cyberfarm:sponge_module"],energy:20000,dock:[I;2002,100,2001]}

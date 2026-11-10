setblock 2084 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["7 Transport:","from its own dock","to the chest: put","items in the dock"]}}
setblock 2088 100 2005 minecraft:chest
setblock 2086 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:4,drone:[I;1000,7,0,0],transport_source:[I;2086,100,2001],transport_destination:[I;2088,100,2005]}
summon cyberfarm:drone 2086.5 100.4 2001.5 {UUID:[I;1000,7,0,0],modules:["cyberfarm:basic_frame","cyberfarm:transport_module"],energy:20000,dock:[I;2086,100,2001]}

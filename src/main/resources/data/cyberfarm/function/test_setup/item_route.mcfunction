setblock 2070 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["6 Transport:","left chest to","right chest",""]}}
setblock 2072 100 2005 minecraft:chest{Items:[{Slot:0b,id:"minecraft:wheat",count:64},{Slot:1b,id:"minecraft:iron_ingot",count:64},{Slot:2b,id:"minecraft:oak_log",count:64}]}
setblock 2076 100 2005 minecraft:chest
setblock 2074 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:4,drone:[I;1000,6,0,0],transport_source:[I;2072,100,2005],transport_destination:[I;2076,100,2005]}
summon cyberfarm:drone 2074.5 100.4 2001.5 {UUID:[I;1000,6,0,0],modules:["cyberfarm:basic_frame","cyberfarm:transport_module"],energy:20000,dock:[I;2074,100,2001]}

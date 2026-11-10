setblock 2056 100 2000 minecraft:oak_sign[rotation=8]{front_text:{messages:["5 Transport +","Tank: water from","left tank to","right tank"]}}
setblock 2058 100 2005 cyberfarm:liquid_tank{tank:{stacks:[{id:"minecraft:water",amount:32000}]}}
setblock 2062 100 2005 cyberfarm:liquid_tank
setblock 2060 100 2001 cyberfarm:drone_dock{energy:{energy:100000},drone_modules:16388,drone:[I;1000,5,0,0],transport_source:[I;2058,100,2005],transport_destination:[I;2062,100,2005]}
summon cyberfarm:drone 2060.5 100.4 2001.5 {UUID:[I;1000,5,0,0],modules:["cyberfarm:advanced_frame","cyberfarm:transport_module","cyberfarm:tank_module"],energy:60000,dock:[I;2060,100,2001]}

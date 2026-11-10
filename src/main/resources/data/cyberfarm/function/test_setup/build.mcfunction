kill @e[type=item,x=1998,y=95,z=1988,dx=154,dy=40,dz=58]
fill 1998 100 1988 2013 112 2044 minecraft:air
fill 1998 99 1988 2013 99 2044 minecraft:smooth_stone
fill 2014 100 1988 2029 112 2044 minecraft:air
fill 2014 99 1988 2029 99 2044 minecraft:smooth_stone
fill 2030 100 1988 2045 112 2044 minecraft:air
fill 2030 99 1988 2045 99 2044 minecraft:smooth_stone
fill 2046 100 1988 2061 112 2044 minecraft:air
fill 2046 99 1988 2061 99 2044 minecraft:smooth_stone
fill 2062 100 1988 2077 112 2044 minecraft:air
fill 2062 99 1988 2077 99 2044 minecraft:smooth_stone
fill 2078 100 1988 2093 112 2044 minecraft:air
fill 2078 99 1988 2093 99 2044 minecraft:smooth_stone
fill 2094 100 1988 2109 112 2044 minecraft:air
fill 2094 99 1988 2109 99 2044 minecraft:smooth_stone
fill 2110 100 1988 2125 112 2044 minecraft:air
fill 2110 99 1988 2125 99 2044 minecraft:smooth_stone
fill 2126 100 1988 2141 112 2044 minecraft:air
fill 2126 99 1988 2141 99 2044 minecraft:smooth_stone
fill 2142 100 1988 2150 112 2044 minecraft:air
fill 2142 99 1988 2150 99 2044 minecraft:smooth_stone
function cyberfarm:test_setup/sponge_alone
function cyberfarm:test_setup/sponge_dock_tank
function cyberfarm:test_setup/sponge_tank_full
function cyberfarm:test_setup/elite_two_liquids
function cyberfarm:test_setup/liquid_route
function cyberfarm:test_setup/item_route
function cyberfarm:test_setup/dock_route
function cyberfarm:test_setup/harvest
function cyberfarm:test_setup/parked_combinations
function cyberfarm:test_setup/holograms
function cyberfarm:test_setup/machines
forceload remove 1998 1988 2150 2044
tellraw @a {"text":"CyberFarm test setup ready: the signs say what each station shows","color":"green"}

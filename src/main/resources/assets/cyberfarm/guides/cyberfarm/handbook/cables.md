---
navigation:
  title: Cables
  position: 3
  icon: cyberfarm:energy_conduit
---

# Cables

Cables carry power and gas between your machines. Both kinds **connect by themselves** to cables of
the same kind and to any block that accepts what they carry, from CyberFarm or any other mod.

<ItemGrid>
  <ItemIcon id="energy_conduit" />
  <ItemIcon id="gas_pipe" />
</ItemGrid>

| Cable | Carries | Rate |
| --- | --- | --- |
| [Energy Conduit](energy_conduit.md) | FE | 1,000 FE/t per source |
| [Gas Pipe](gas_pipe.md) | Biogas, Methane, water, coolants, Steam and any other fluid | 1,000 mB/t per source |

Cables **store nothing**: whatever a machine pushes in goes straight to the machines on the other
end, taking turns between them so none is left without. A network can be up to 1,024 cables long.

The two kinds never connect to each other, so an Energy Conduit and a Gas Pipe can run side by side.

## Pages

<SubPages icons={true} />

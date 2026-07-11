# Implementation notes

This version is aligned with the NeoForge 1.21.1 MDK and fixes the earlier API mismatch by:

- using `DeferredHolder` instead of the removed `RegistryObject` import path
- adding `codec()` implementations required by 1.21 block classes
- keeping build metadata simple and explicit
- keeping the Create bridge as a placeholder until the real dependency wiring is added

The gameplay slice is intentionally conservative:
- wind varies by chunk cell
- rain and thunder boost wind behavior
- the wind sensor emits redstone on only one horizontal side
- the sail hub estimates output, but does not yet talk to Create directly

# Operator Guide

## First Deployment

1. Define the operator's precinct/unit scope and equipment/patrol-
   roster process.
2. Define consent and purpose categories for officer/equipment data
   handling.
3. Run synthetic operating scenarios (no real officer, equipment,
   patrol, incident, or supply data in this repository).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical`
   actions — including every `:flag-precinct-concern` and every
   above-threshold `:coordinate-supply-order`.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path
- provenance for all operating records
- human review for high-risk cases
- audit export for all gated actions

## No Use-of-Force, Weapon, Arrest, Detention, Search, or Pursuit Authority

This actor is a precinct/equipment/patrol-scheduling documentation/
administrative-logistics coordination robot ONLY. Operators MUST NOT
configure, extend or fork this actor to add an op that uses force (of
any kind, from physical restraint to lethal force), deploys a weapon,
makes an arrest, detains a person, authorizes a search or seizure, or
pursues/engages a suspect, or that otherwise exercises any tactical or
enforcement authority. Any such change removes the structural guarantee
this repository is built around and voids certification (see
[`GOVERNANCE.md`](../GOVERNANCE.md)). Every administrative/equipment/
training observation must route through `:flag-precinct-concern` to a
human officer/supervisor — the robot's role ends at "here is the
equipment log / the proposed roster / the flagged concern," never "here
is what enforcement action to take."

Operators MUST NOT configure this actor's `:coordinate-supply-order`
path to procure a weapon, ammunition, or other lethal/less-lethal-
equipment item under any circumstance — this is a hard, permanent block
independent of cost, not a threshold to be tuned.

## Certification

Certified operators must prove that the governor gates every
safety-critical robot action, that safety-critical risks escalate to
humans, and that no build of this actor has ever added an op resembling
a use-of-force decision, a weapon deployment, an arrest, a detention, a
search/seizure authorization, or a suspect pursuit/engagement to the
closed allowlist.

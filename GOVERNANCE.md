# Governance

`cloud-itonami-isco-5412` is an OSS open-occupation blueprint. Governance covers
both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions or disclose records.
- PrecinctOps Governor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval.
- the closed proposal-op allowlist NEVER gains an op that uses force (of
  any kind, from physical restraint to lethal force), deploys a weapon,
  makes an arrest, detains a person, authorizes a search or seizure, or
  pursues/engages a suspect, or that otherwise exercises any tactical or
  enforcement authority — this is a permanent scope boundary of the
  project, not subject to normal maintainer discretion.
- `:coordinate-supply-order` NEVER gains a path that can procure a
  weapon, ammunition, or other lethal/less-lethal-equipment item.
- every commit, hold and approval path is auditable.
- real officer/equipment/patrol/incident/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and data-flow
review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling officer/equipment/patrol/incident/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- adding, or attempting to add, any capability that uses force, deploys a
  weapon, makes an arrest, detains a person, authorizes a search or
  seizure, or pursues/engages a suspect

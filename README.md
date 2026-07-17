# cloud-itonami-isco-5412

Open Occupation Blueprint for **ISCO-08 5412**: Police Officers.

This repository designs a forkable OSS business for a police precinct
equipment and patrol-scheduling administrative-logistics coordination
practice: a precinct/equipment/patrol-scheduling coordination robot
manages equipment-readiness data entry, shift/patrol-route rostering,
and non-weapon supply-order coordination under a governor-gated actor —
and structurally **never** uses force, deploys a weapon, makes an
arrest, detains a person, authorizes a search or seizure, or
pursues/engages a suspect itself.

## This actor has NO use-of-force, weapon, arrest, detention, search, or pursuit authority

Police Officers have direct legal authority to use force (up to and
including lethal force in extreme circumstances), carry and use
weapons, make arrests, and physically detain individuals — a domain
where wrong autonomous action could kill or seriously injure someone,
or violate their liberty and due-process rights. **This actor is a
precinct/equipment/patrol-scheduling documentation/logistics
coordination robot ONLY.** It has NO op, anywhere in its allowlist,
that resembles a use-of-force decision (of any kind, from physical
restraint to lethal force), a weapon-deployment decision, an arrest, a
detention, a search/seizure authorization, or a decision to
pursue/engage a suspect. These are **structurally absent from the
closed op-allowlist entirely**, not merely gated behind escalation —
under any circumstance, at any confidence level, in any phase. Any
observation the robot logs that suggests a precinct/equipment/training
concern needs human attention is surfaced ONLY via an always-escalating
`:flag-precinct-concern` op that a human officer/supervisor reviews and
acts on entirely themselves. This mirrors the Wave4 person-facing-
service safety guardrail (ADR-2607152500) and the same structural-
exclusion pattern proven in the sibling ISCO-3355 (Police Inspectors
and Detectives) build: decisions directly touching a person's
liberty/due-process/bodily-safety rights always exclude the closed op
allowlist and always escalate. The robot's role ends at "here is the
equipment log / the proposed roster / the flagged concern" — never
"here is what enforcement action to take."

**Maturity: `:implemented`.** `src/precinctops/` implements the
`PrecinctOpsActor` as a `langgraph.graph/state-graph`
(`precinctops.actor`) wired to a `Precinct Coordination Advisor`
(`precinctops.advisor`) and an independent `PrecinctOpsGovernor`
(`precinctops.governor`), following the itonami actor pattern
(ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 54 tests / 203 assertions green (`clojure -M:test`).

HARD invariants (always hold, never overridable): officer provenance (a
proposal must resolve to an independently registered AND verified
officer record), a closed four-op proposal allowlist (any op outside
it — including anything that would use force, deploy a weapon, make an
arrest, detain a person, authorize a search/seizure, or pursue/engage a
suspect — is a permanent HARD block, because no such op exists in the
allowlist to begin with), no-actuation (`:effect` must be `:propose`),
a registered-and-verified equipment-unit basis (for the three ops that
reference one), a tactical-assessment-forbidden check
(`:log-equipment-record` may only carry equipment/vehicle condition
metadata, never a tactical or incident assessment), a
tactical-dispatch-forbidden check (`:schedule-patrol-operation` may
only carry administrative shift/patrol-route rostering logistics,
never a real-time tactical dispatch during an active incident), a
dedicated weapon-procurement-forbidden check (`:coordinate-supply-order`
may never name a weapon, ammunition, or other lethal/less-lethal-
equipment item, regardless of cost — this is a hard block, not merely a
cost-threshold gate), and a content-based scope-exclusion check: any
proposal whose free text names a finalization/execution action for a
use-of-force decision, a weapon deployment, an arrest, a detention, a
search/seizure authorization, or a pursuit/engagement of a suspect is a
permanent HARD block, independent of and in addition to the
op-allowlist check. This actor **never** exercises, simulates
exercising, or proposes exercising any use-of-force, weapon-deployment,
arrest, detention, search/seizure-authorization, or
suspect-pursuit/engagement authority — it only documents precinct
equipment/patrol logistics and coordinates administrative operations.

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-precinct-concern` (surfacing an administrative/equipment/
training concern — always requires human officer/supervisor review;
never auto-resolved, never in any phase's auto-commit set — this is the
ONLY channel by which such an observation may be surfaced) and any
`:coordinate-supply-order` above the registered per-equipment-unit cost
threshold (for allowed, non-weapon items only — a weapon/ammunition
item is a hard block regardless of cost, see above).

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical/administrative domain work**. Here a
precinct/equipment/patrol-scheduling coordination robot performs
equipment-readiness data entry, shift/patrol-route rostering, and
non-weapon supply-order coordination under an actor that proposes
actions and an independent **PrecinctOpsGovernor** that gates them. The
governor never dispatches hardware itself; `:high`/`:safety-critical`
actions (such as flagging a precinct concern, or an above-threshold
supply order) require human sign-off — and no action in this actor's
closed op allowlist can ever use force, deploy a weapon, make an
arrest, detain a person, authorize a search/seizure, or pursue/engage a
suspect.

## Core Contract

```text
officer equipment roster + patrol schedule + supply policy
        |
        v
Precinct Coordination Advisor -> PrecinctOpsGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
use force, deploy a weapon, make an arrest, detain a person, authorize
a search or seizure, pursue or engage a suspect, procure a weapon or
ammunition item, suppress an operating record, or disclose sensitive
data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `5412`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.

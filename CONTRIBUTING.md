# Contributing

`cloud-itonami-isco-5412` accepts contributions to the OSS actor, policy tests,
documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:dev:test
clojure -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real officer, equipment, patrol, incident, supply, or
  operator data, credentials or operating documents.
- Keep production writes and disclosures behind PrecinctOps Governor.
- **Never add an op that uses force (of any kind, from physical restraint
  to lethal force), deploys a weapon, makes an arrest, detains a person,
  authorizes a search or seizure, or pursues/engages a suspect, or that
  otherwise exercises any tactical or enforcement authority.** This
  actor's closed proposal-op allowlist is a hard scope boundary, not a
  starting point to extend. Any PR that proposes such an op will be
  rejected.
- **Never add a `:coordinate-supply-order` path that can procure a
  weapon, ammunition, or other lethal/less-lethal-equipment item.**
  `precinctops.governor/weapon-item-terms` is a hard, permanent block,
  not a tunable allowlist.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates

# Security Policy

This project handles police officer operating workflows, directly
touching a person's liberty, bodily safety, and due-process rights, and
indirectly touching officer safety. Treat vulnerabilities as potentially
high impact even when the demo data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real officer, equipment, patrol, incident, supply, or operator data
  exposure
- authorization bypass
- PrecinctOps Governor bypass
- any path by which the actor could use force, deploy a weapon, make an
  arrest, detain a person, authorize a search or seizure, or
  pursue/engage a suspect
- any path by which `:coordinate-supply-order` could procure a weapon,
  ammunition, or other lethal/less-lethal-equipment item
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on officer/equipment data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real officer/equipment/patrol/incident/operator data outside this
  repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.

(ns precinctops.store
  "SSoT for the ISCO-08 5412 police officers precinct/equipment/patrol-
  scheduling DOCUMENTATION/LOGISTICS COORDINATION actor (itonami actor
  pattern, ADR-2607121000 / CLAUDE.md Actors section; README's
  'Robotics premise' — a precinct/equipment/patrol-scheduling
  coordination robot performs equipment-readiness data entry,
  shift/patrol-route rostering and non-weapon supply-order coordination
  under this advisor/governor pair, which never dispatches hardware
  itself and NEVER exercises, simulates exercising, or proposes
  exercising ANY use-of-force decision (of any kind, from physical
  restraint to lethal force), weapon-deployment decision, arrest,
  detention, search/seizure authorization, or decision to pursue/engage
  a suspect — every one of those capabilities is a permanently
  out-of-scope, structurally absent op; this actor cannot use force,
  deploy a weapon, make an arrest, detain a person, authorize a search
  or seizure, or pursue/engage a suspect, no matter how confident the
  advisor is or how a human resumes an interrupted run). Modeled on
  cloud-itonami-isco-3355's caseadmin.store (closed op allowlist +
  independently-registered-AND-verified provenance for both the
  proposing officer and the referenced equipment unit), itself modeled
  on cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    officer   — a registered police officer {:officer-id :name
                :precinct-id :verified? boolean}. Independently
                registered/verified identity, never trusted from the
                proposal alone ('officer/precinct record must be
                independently verified/registered before any action').
                This actor never determines this officer's tactical
                decisions — it only logs, schedules and flags
                administrative/logistics records on the officer's
                behalf.
    equipment — a registered equipment/vehicle/patrol unit {:equipment-id
                :precinct-id :max-supply-cost number :verified?
                boolean}. Independently registered/verified, never
                trusted from the proposal alone. `:max-supply-cost` is
                the registered per-unit ceiling a proposed
                `:coordinate-supply-order` cost above which always
                escalates to a human — NOT a hard block, a supply order
                over budget just needs sign-off, it is not itself
                unsafe. Weapon/ammunition/lethal-equipment items are a
                HARD, permanent block regardless of cost (see
                `precinctops.governor/weapon-item?`).
    record    — a committed operating record (equipment-readiness log
                entry, patrol-operation scheduling proposal,
                precinct-concern flag, or supply-order coordination
                proposal) — written ONLY via commit-record!. A
                committed record is NEVER a use-of-force decision, a
                weapon deployment, an arrest, a detention, a
                search/seizure authorization, or a pursuit/engagement of
                a suspect — this actor documents and coordinates
                logistics, it never polices or enforces.
    ledger    — append-only audit trail, commit or hold.")

(defprotocol Store
  (officer [s officer-id])
  (equipment [s equipment-id])
  (records-of [s equipment-id])
  (ledger [s])
  (register-officer! [s o])
  (register-equipment! [s e])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (officer [_ officer-id] (get-in @a [:officers officer-id]))
  (equipment [_ equipment-id] (get-in @a [:equipment equipment-id]))
  (records-of [_ equipment-id] (filter #(= equipment-id (:equipment-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-officer! [s o]
    (swap! a assoc-in [:officers (:officer-id o)] o) s)
  (register-equipment! [s e]
    (swap! a assoc-in [:equipment (:equipment-id e)] e) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:officers {} :equipment {} :records [] :ledger []}
                                    seed)))))

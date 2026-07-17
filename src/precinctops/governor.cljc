(ns precinctops.governor
  "PrecinctOpsGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md, gating every
  documentation/logistics-coordination operation an advisor may
  propose. The governor never dispatches hardware itself and NEVER lets
  a proposal exercise, simulate exercising, or propose exercising ANY
  use-of-force decision (of any kind, from physical restraint to lethal
  force), weapon-deployment decision, arrest, detention, search/seizure
  authorization, or decision to pursue/engage a suspect — every one of
  those is permanently out of scope for this actor, not merely gated
  behind escalation. This is, alongside cloud-itonami-isco-5413
  (Prison Guards), the highest-stakes occupation actor built in this
  session's ISCO batch: police officers have direct legal authority to
  use force up to and including lethal force, carry and use weapons,
  make arrests, and physically detain individuals — this actor has
  ZERO of that authority, proposed or otherwise. This mirrors the Wave4
  person-facing-service safety guardrail (ADR-2607152500): decisions
  directly touching a person's liberty/due-process/bodily-safety rights
  always exclude the closed op allowlist and always escalate. Modeled
  on cloud-itonami-isco-3355's caseadmin.governor, with the same closed
  proposal-op allowlist + content-based scope-exclusion shape, adapted
  to this vertical's force/weapon/arrest/detention/search/pursuit
  guardrail.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. officer provenance      — the proposing officer record must be
                                independently registered AND verified
                                before ANY proposal can commit or
                                escalate. Never trusts the proposal's
                                own claim of who the officer is.
    2. no-actuation             — proposal :effect must be :propose
                                (the governor never dispatches hardware
                                and never itself performs a tactical or
                                enforcement action; it only gates what
                                the advisor may commit).
    3. closed op allowlist      — the proposal's :op must be one of the
                                four ops this actor is scoped to
                                (`closed-op-allowlist` below). This is
                                the STRUCTURAL guarantee: no op that
                                resembles a use-of-force decision, a
                                weapon-deployment decision, an arrest, a
                                detention, a search/seizure
                                authorization, or a decision to
                                pursue/engage a suspect exists anywhere
                                in this allowlist — such a proposal
                                cannot even reach a check, let alone
                                pass one. Any :op outside the allowlist
                                is a HARD, PERMANENT block.
    4. equipment-unit basis     — a proposal for `:log-equipment-record`,
                                `:schedule-patrol-operation` or
                                `:coordinate-supply-order` must cite a
                                REGISTERED AND VERIFIED equipment unit
                                belonging to the officer's own precinct
                                (`:unknown-equipment` /
                                `:equipment-unverified` /
                                `:equipment-wrong-precinct`).
                                `:flag-precinct-concern` does NOT
                                require an existing equipment unit (it
                                is the channel by which any
                                administrative/equipment/training
                                concern is surfaced for human intake).
    5. tactical-assessment forbidden — `:log-equipment-record` is
                                equipment/vehicle condition metadata
                                ONLY (mileage, inspection, mechanical
                                condition). Any proposal carrying a
                                tactical-assessment field
                                (`equipment-record-forbidden-keys`
                                below — e.g. `:threat-level`,
                                `:tactical-assessment`,
                                `:suspect-location`) is a HARD,
                                PERMANENT block — this actor never
                                records a tactical or incident
                                assessment, only equipment condition.
    6. tactical-dispatch forbidden — `:schedule-patrol-operation` is
                                administrative shift/patrol-route
                                rostering ONLY, never a real-time
                                tactical dispatch during an active
                                incident. Any proposal carrying
                                active-incident/dispatch content fields
                                (`patrol-schedule-forbidden-keys` below
                                — e.g. `:active-incident-id`,
                                `:dispatch-order`,
                                `:pursuit-authorization`) is a HARD,
                                PERMANENT block — this actor never
                                dispatches or directs a live tactical
                                response, only routine administrative
                                rostering.
    7. weapon-procurement forbidden — `:coordinate-supply-order` may
                                only name non-weapon equipment
                                (vehicles, radios, protective gear).
                                Any proposal whose `:item` or
                                `:category` names a weapon, ammunition
                                or other lethal/less-lethal-equipment
                                procurement item (`weapon-item?` below)
                                is a HARD, PERMANENT block — independent
                                of and IN ADDITION TO the cost-threshold
                                escalation (#10 below); a weapon/
                                ammunition item is blocked even at $0.
    8. scope exclusion           — independent, DEFENSE-IN-DEPTH layer
                                on top of #3/#5/#6/#7: even for an
                                otherwise-allowed op, any proposal whose
                                free text (`:rationale` or `:note`)
                                names a finalization/execution ACTION
                                for a use-of-force decision, a
                                weapon-deployment decision, an arrest, a
                                detention, a search/seizure
                                authorization, or a pursuit/engagement
                                of a suspect (`scope-excluded-terms`
                                below) is a HARD, PERMANENT block,
                                evaluated unconditionally on content.
                                This actor never exercises tactical,
                                enforcement or use-of-force authority —
                                it only documents equipment/patrol
                                logistics and coordinates precinct
                                administration.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off — these
  are :high/:safety-critical regardless of confidence):
    9. :op :flag-precinct-concern (surfacing an administrative/
                                equipment/training concern — ALWAYS
                                requires human officer/supervisor
                                review; it is never auto-resolved and
                                never appears in any phase's
                                auto-commit set; this is the ONLY path
                                by which an observation may be surfaced,
                                and the robot's role ends at 'here is
                                the flagged concern' — never 'here is
                                what enforcement action to take').
    10. an above-threshold :coordinate-supply-order (non-weapon
                                equipment procurement above
                                `supply-cost-escalation-threshold`
                                always needs human sign-off, regardless
                                of confidence — this is an escalation,
                                NOT a hard block, since an over-budget
                                supply request for allowed equipment is
                                not itself unsafe; a WEAPON/ammunition
                                item is a hard block per #7 above
                                regardless of cost).
    11. low confidence (< `confidence-floor`)."
  (:require [clojure.string :as str]
            [precinctops.store :as store]))

(def confidence-floor 0.6)

;; Non-weapon equipment supply orders at or below this estimated cost
;; may be auto-commit-eligible (subject to confidence); above it,
;; ALWAYS escalates to a human regardless of confidence. A
;; weapon/ammunition/lethal-equipment item is a HARD block regardless
;; of cost (see #7 above / `weapon-item?` below) — this threshold only
;; governs allowed, non-weapon equipment.
(def supply-cost-escalation-threshold 500)

;; The closed proposal-op allowlist. This governor NEVER allows any op
;; outside this set to commit or even escalate — an op outside this set
;; is a HARD, permanent block (see `hard-violations` :op-not-allowed
;; below), not merely un-auto-committable. This is a
;; precinct/equipment/patrol-scheduling documentation/administrative-
;; logistics coordination robot ONLY: it has NO op, anywhere in this
;; allowlist, that resembles a use-of-force decision, a
;; weapon-deployment decision, an arrest, a detention, a search/seizure
;; authorization, or a decision to pursue/engage a suspect. Those
;; capabilities are structurally absent, not gated.
(def closed-op-allowlist
  #{:log-equipment-record :schedule-patrol-operation
    :flag-precinct-concern :coordinate-supply-order})

;; :flag-precinct-concern always escalates to a human — never
;; auto-commit-eligible at any phase. It is the ONLY channel through
;; which an administrative/equipment/training observation may be
;; surfaced.
(def ^:private always-escalate-ops #{:flag-precinct-concern})

;; Ops that outside observers might expect a "police actor" to have —
;; named here explicitly (in addition to the closed-allowlist check
;; above) so the exclusion reads as an intentional, documented scope
;; boundary rather than an incidental unknown op. None of these are
;; ever defined as a real op anywhere in this codebase; they exist ONLY
;; as negative-test fixtures proving `closed-op-allowlist` rejects
;; them. Covers all six forbidden categories named in this actor's
;; charter: use-of-force, weapon-deployment, arrest, detention,
;; search/seizure authorization, pursuit/engagement.
(def scope-excluded-ops
  #{:use-force :authorize-use-of-force
    :deploy-weapon :discharge-weapon :fire-weapon
    :make-arrest :authorize-arrest
    :detain-suspect :authorize-detention
    :authorize-search :authorize-seizure :conduct-search
    :pursue-suspect :engage-suspect :authorize-pursuit})

;; log-equipment-record is equipment/vehicle condition metadata ONLY. A
;; proposal carrying any of these keys is smuggling a tactical or
;; incident assessment into what must remain a pure equipment-condition
;; log.
(def equipment-record-forbidden-keys
  #{:threat-level :tactical-assessment :suspect-location
    :incident-severity :engagement-status :use-of-force-assessment
    :suspect-description})

;; schedule-patrol-operation is administrative shift/patrol-route
;; rostering ONLY. A proposal carrying any of these keys is smuggling a
;; real-time tactical dispatch during an active incident into what must
;; remain pure administrative rostering.
(def patrol-schedule-forbidden-keys
  #{:active-incident-id :dispatch-order :tactical-directive
    :pursuit-authorization :engage-target :real-time-dispatch
    :suspect-description})

;; Weapon/ammunition/lethal-equipment procurement terms. A
;; :coordinate-supply-order whose :item or :category matches any of
;; these (case-insensitive substring) is a HARD, PERMANENT block — see
;; `weapon-item?` below. Protective gear (vests, helmets), vehicles and
;; communications equipment are NOT on this list and remain allowed.
(def weapon-item-terms
  ["firearm" "firearms" "pistol" "pistols" "handgun" "handguns"
   "rifle" "rifles" "shotgun" "shotguns"
   "ammunition" "ammo" "bullet" "bullets" "cartridge" "cartridges"
   "taser" "tasers" "conducted energy weapon" "stun gun" "stun guns"
   "pepper spray" "oc spray" "chemical irritant" "tear gas"
   "baton" "batons" "expandable baton"
   "less-lethal launcher" "less lethal launcher"
   "beanbag round" "beanbag rounds" "rubber bullet" "rubber bullets"
   "explosive" "explosives" "flashbang" "flashbangs"
   "stun grenade" "stun grenades" "grenade launcher"
   "拳銃" "小銃" "散弾銃" "弾薬" "実弾" "テーザー" "スタンガン"
   "警棒" "催涙スプレー" "催涙ガス" "手榴弾" "閃光弾"])

;; Scope-exclusion terms, phrased as the FINALIZATION/EXECUTION ACTION
;; (never a bare noun like "force", "weapon", "arrest", "detention",
;; "search" or "pursuit" alone) — a known self-tripping bug class in
;; this fleet: a bare-noun term list can accidentally match inside the
;; mock advisor's own default rationale text for a legitimate, allowed
;; proposal, causing the actor to self-block on its own happy path.
;; This advisor's default rationale template is "documented <op> for
;; equipment <id>", which never contains any of these full action
;; phrases. See `precinctops.governor-test/
;; default-mock-advisor-proposals-never-self-trip-scope-exclusion`.
(def scope-excluded-terms
  ["use force" "used force" "apply force" "applied force"
   "authorize the use of force" "authorized the use of force"
   "authorize use of force" "authorized use of force"
   "deploy the weapon" "deployed the weapon"
   "discharge the weapon" "discharged the weapon"
   "fire the weapon" "fired the weapon"
   "make the arrest" "made the arrest"
   "authorize the arrest" "authorized the arrest"
   "execute the arrest" "executed the arrest"
   "place under arrest" "placed under arrest"
   "detain the suspect" "detained the suspect"
   "authorize the detention" "authorized the detention"
   "take into custody" "taken into custody"
   "authorize the search" "authorized the search"
   "conduct the search" "conducted the search"
   "execute the search warrant" "executed the search warrant"
   "authorize the seizure" "authorized the seizure"
   "pursue the suspect" "pursued the suspect"
   "authorize the pursuit" "authorized the pursuit"
   "engage the suspect" "engaged the suspect"
   "実力を行使した" "武器を使用した" "武器を発砲した"
   "逮捕した" "逮捕を執行した" "拘束した" "身柄を拘束した"
   "捜索を許可した" "差押えを許可した"
   "容疑者を追跡した" "容疑者と交戦した"])

(defn out-of-scope?
  "True if any free-text field on `proposal` (:rationale or :note)
  contains a scope-excluded finalization/execution phrase for a
  use-of-force decision, weapon deployment, arrest, detention,
  search/seizure authorization, or pursuit/engagement of a suspect."
  [proposal]
  (let [text (str (:rationale proposal) " " (:note proposal))]
    (boolean (some #(str/includes? text %) scope-excluded-terms))))

(defn weapon-item?
  "True if `proposal`'s :item or :category names a weapon, ammunition,
  or other lethal/less-lethal-equipment procurement item
  (case-insensitive substring match against `weapon-item-terms`). This
  is a dedicated hard block for :coordinate-supply-order, independent
  of `out-of-scope?` — it fires on the procurement item itself, not
  only on finalization language in free text."
  [proposal]
  (let [text (str/lower-case (str (:item proposal) " " (:category proposal)))]
    (boolean (some #(str/includes? text (str/lower-case %)) weapon-item-terms))))

(defn- forbidden-keys-present [proposal forbidden-keys]
  (seq (filter #(contains? proposal %) forbidden-keys)))

(def ^:private equipment-required-ops
  #{:log-equipment-record :schedule-patrol-operation :coordinate-supply-order})

(defn- hard-violations [{:keys [proposal]} officer-record equipment-record]
  (let [{:keys [op equipment-id]} proposal
        needs-equipment? (contains? equipment-required-ops op)]
    (cond-> []
      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は戦術/執行行為を直接実行しない）"})

      (not (contains? closed-op-allowlist op))
      (conj {:rule :op-not-allowed
             :detail "closed allowlist 外の op（実力行使・武器展開・逮捕・拘束・捜索/差押えの許可・容疑者追跡/交戦の直接実行を含む一切の確定は許可されない）"})

      (nil? officer-record)
      (conj {:rule :unknown-officer :detail "未登録 officer への提案は不可"})

      (and officer-record (not (:verified? officer-record)))
      (conj {:rule :officer-unverified :detail "未検証 officer への提案は不可（登録のみでは不十分）"})

      (and needs-equipment? (nil? equipment-id))
      (conj {:rule :missing-equipment-id :detail "この op には equipment-id が必須"})

      (and needs-equipment? equipment-id (nil? equipment-record))
      (conj {:rule :unknown-equipment :detail "未登録 equipment への提案は不可"})

      (and needs-equipment? equipment-record (not (:verified? equipment-record)))
      (conj {:rule :equipment-unverified :detail "未検証 equipment への提案は不可（登録のみでは不十分）"})

      (and needs-equipment? equipment-record officer-record
           (not= (:precinct-id equipment-record) (:precinct-id officer-record)))
      (conj {:rule :equipment-wrong-precinct :detail "equipment が officer と別 precinct のもの"})

      (and (= :log-equipment-record op) (seq (forbidden-keys-present proposal equipment-record-forbidden-keys)))
      (conj {:rule :tactical-assessment-forbidden
             :detail "log-equipment-record は装備状態メタデータのみ — 戦術/事案評価の記録は永久に禁止"})

      (and (= :schedule-patrol-operation op) (seq (forbidden-keys-present proposal patrol-schedule-forbidden-keys)))
      (conj {:rule :tactical-dispatch-forbidden
             :detail "schedule-patrol-operation は行政的なシフト/パトロール経路調整のみ — リアルタイム戦術指令は永久に禁止"})

      (and (= :coordinate-supply-order op) (weapon-item? proposal))
      (conj {:rule :weapon-procurement-forbidden
             :detail "武器・弾薬・殺傷/非殺傷武器装備の調達提案は永久に禁止（費用に関わらずハードブロック）"})

      (out-of-scope? proposal)
      (conj {:rule :scope-excluded
             :detail "実力行使・武器展開・逮捕・拘束・捜索/差押えの許可・容疑者の追跡/交戦を直接確定する提案は恒久的に許可されない（このactorは文書化とロジスティクス調整のみを行う）"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `precinctops.store/Store`. Pure — never mutates
  the store, never uses force, never deploys a weapon, never makes an
  arrest, never detains a person, never authorizes a search/seizure,
  never pursues or engages a suspect."
  [_request _context proposal store]
  (let [officer-record (some->> (:officer-id proposal) (store/officer store))
        equipment-record (some->> (:equipment-id proposal) (store/equipment store))
        hard (hard-violations {:proposal proposal} officer-record equipment-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        over-threshold-supply-order?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-escalation-threshold))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-threshold-supply-order?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-threshold-supply-order?))}))

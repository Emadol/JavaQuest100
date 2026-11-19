#!/usr/bin/env python3
"""Unisce data-src/part1..4.json in src/main/resources/data/curriculum.json.

Uso:  python data-src/merge.py [--force] [--out PATH]
  --force  scrive il file anche se la validazione fallisce (es. fasi mancanti)
Exit code 0 = curriculum valido con 100 fasi, 1 = errori di validazione, 2 = file mancanti.
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_OUT = os.path.normpath(os.path.join(HERE, "..", "src", "main", "resources", "data", "curriculum.json"))
EXPECTED = 100
TEXT_FIELDS = ("category", "title", "lesson", "example", "help")


def load_parts():
    phases, missing = [], []
    for n in range(1, 5):
        path = os.path.join(HERE, f"part{n}.json")
        if not os.path.exists(path):
            missing.append(path)
            continue
        with open(path, encoding="utf-8-sig") as f:
            try:
                data = json.load(f)
            except json.JSONDecodeError as e:
                sys.exit(f"ERRORE: {path} non e' JSON valido: {e}")
        if isinstance(data, dict):
            data = data.get("phases", [])
        if not isinstance(data, list):
            sys.exit(f"ERRORE: {path} deve contenere un array di fasi")
        print(f"  {os.path.basename(path)}: {len(data)} fasi")
        phases.extend(data)
    return phases, missing


def as_text(v):
    if isinstance(v, list):
        return "\n".join(str(x) for x in v)
    return "" if v is None else str(v)


def normalize_answer(q):
    """answer -> indice 0..3 (accetta indice, lettera A-D o testo dell'opzione)."""
    a, opts = q.get("answer"), q.get("options", [])
    if isinstance(a, bool):
        return None
    if isinstance(a, (int, float)):
        a = int(a)
    elif isinstance(a, str):
        s = a.strip()
        if s in opts:
            a = opts.index(s)
        elif len(s) == 1 and s.upper() in "ABCD":
            a = "ABCD".index(s.upper())
        elif s.isdigit():
            a = int(s)
        else:
            return None
    else:
        return None
    return a if 0 <= a < len(opts) else None


def validate(phases):
    errors = []
    seen = {}
    for idx, p in enumerate(phases):
        where = f"fase #{idx} (id={p.get('id')})"
        if not isinstance(p, dict):
            errors.append(f"{where}: non e' un oggetto")
            continue
        try:
            p["id"] = int(p.get("id"))
        except (TypeError, ValueError):
            errors.append(f"{where}: id mancante o non numerico")
            continue
        if p["id"] in seen:
            errors.append(f"{where}: id duplicato")
        seen[p["id"]] = p
        for k in TEXT_FIELDS:
            p[k] = as_text(p.get(k))
            if not p[k].strip():
                errors.append(f"{where}: campo '{k}' vuoto")
        ex = p.get("exercise")
        if not isinstance(ex, dict):
            errors.append(f"{where}: exercise mancante")
        else:
            ex["prompt"] = as_text(ex.get("prompt"))
            if not ex["prompt"].strip():
                errors.append(f"{where}: exercise.prompt vuoto")
            if not isinstance(ex.get("lines"), list) or len(ex["lines"]) < 2:
                errors.append(f"{where}: exercise.lines deve avere almeno 2 righe")
            if not isinstance(ex.get("distractors"), list):
                ex["distractors"] = []
        quiz = p.get("quiz")
        if not isinstance(quiz, list) or len(quiz) != 3:
            errors.append(f"{where}: quiz deve avere 3 domande")
        else:
            for qi, q in enumerate(quiz):
                if not isinstance(q, dict) or not as_text(q.get("q")).strip():
                    errors.append(f"{where}: quiz[{qi}] senza testo")
                    continue
                if not isinstance(q.get("options"), list) or len(q["options"]) != 4:
                    errors.append(f"{where}: quiz[{qi}] deve avere 4 opzioni")
                    continue
                q["options"] = [as_text(o) for o in q["options"]]
                a = normalize_answer(q)
                if a is None:
                    errors.append(f"{where}: quiz[{qi}] answer non valida: {q.get('answer')!r}")
                else:
                    q["answer"] = a
                q["explain"] = as_text(q.get("explain"))
    ids = sorted(seen)
    missing_ids = [i for i in range(1, EXPECTED + 1) if i not in seen]
    if len(seen) != EXPECTED or missing_ids:
        errors.append(f"attese {EXPECTED} fasi con id 1..{EXPECTED}: trovate {len(seen)}; id mancanti: {missing_ids[:20]}"
                      + (" ..." if len(missing_ids) > 20 else ""))
    extra = [i for i in ids if i < 1 or i > EXPECTED]
    if extra:
        errors.append(f"id fuori intervallo: {extra}")
    return [seen[i] for i in ids], errors


def main():
    force = "--force" in sys.argv
    out = DEFAULT_OUT
    if "--out" in sys.argv:
        out = os.path.abspath(sys.argv[sys.argv.index("--out") + 1])
    print("Merge del curriculum:")
    phases, missing = load_parts()
    if missing:
        print("File mancanti:\n  " + "\n  ".join(missing))
        if not force:
            return 2
    ordered, errors = validate(phases)
    for e in errors:
        print("  ERRORE: " + e)
    if errors and not force:
        print(f"Validazione fallita ({len(errors)} errori): {out} NON aggiornato (usa --force per scrivere comunque).")
        return 1
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(ordered, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print(f"Scritto {out}: {len(ordered)} fasi" + (" (con errori)" if errors else " - OK"))
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())

"""Join completed P360/Mongo evidence and summarize offline proposals. No network writes."""
import csv
import gzip
import hashlib
import json
import os
import sys
from collections import Counter

def empty(value):
    return value is None or value == '' or value == {} or value == []

def filled(before, after, path=''):
    result = []
    if isinstance(after, dict):
        before = before if isinstance(before, dict) else {}
        for key, value in after.items():
            result.extend(filled(before.get(key), value, path + '/' + key))
    elif empty(before) and not empty(after):
        result.append({'path': path, 'proposedValue': after})
    return result

def load(path):
    with gzip.open(path, 'rt', encoding='utf-8') as source:
        return json.load(source)

def write(path, value):
    with gzip.open(path + '.partial', 'wt', encoding='utf-8') as target:
        json.dump(value, target, ensure_ascii=False)
    os.rename(path + '.partial', path)

def main(db, mongo, out):
    for path in [db, mongo]:
        if not os.path.isfile(os.path.join(path, 'complete.json')):
            raise RuntimeError('Incomplete input: ' + path)
    def digest(path):
        with open(os.path.join(path, 'input.csv'), 'rb') as source:
            return hashlib.sha256(source.read()).hexdigest()
    if digest(db) != digest(mongo):
        raise RuntimeError('Input CSV fingerprints differ')
    with open(os.path.join(db, 'complete.json')) as source:
        expected = json.load(source)['groups']
    with open(os.path.join(mongo, 'complete.json')) as source:
        if json.load(source)['groups'] != expected:
            raise RuntimeError('Evidence group counts differ')
    seen = set()
    os.mkdir(out)
    totals = Counter()
    examples = []
    columns = ['sku', 'identifiers', 'status', 'entities', 'mongoProducts', 'mongoSkus', 'baseIdentifier', 'productFieldsFilled', 'articleFieldsFilled', 'proposedArticleLinks', 'conflicts', 'ambiguousArticles']
    with open(os.path.join(db, 'groups.jsonl'), encoding='utf-8') as rows, open(os.path.join(out, 'review-index.csv'), 'w', newline='', encoding='utf-8') as output:
        index = csv.DictWriter(output, fieldnames=columns)
        index.writeheader()
        for line in rows:
            if not line.strip():
                continue
            row = json.loads(line)
            sku = row['sku']
            if sku in seen:
                raise RuntimeError('Repeated group in summary')
            seen.add(sku)
            source_file = os.path.join(db, sku + '.json.gz')
            mongo_file = os.path.join(mongo, sku + '.json.gz')
            p360, evidence = load(source_file), load(mongo_file)
            if p360['sku'] != sku or evidence['sku'] != sku:
                raise RuntimeError('SKU evidence mismatch')
            bases = {b['product']['identifier']: b for b in p360['normalizedBundles']}
            alternatives = []
            for proposal in p360['alternativeBasePlans']:
                base = bases[proposal['comparisonBase']]
                pf = filled(base['product'].get('dbFields'), proposal['product'].get('dbFields'), '/product/dbFields')
                af = []
                for i, article in enumerate(base['articles']):
                    af.extend(filled(article.get('dbFields'), proposal['articles'][i].get('dbFields'), '/articles/' + str(i) + '/dbFields'))
                links = [c for c in proposal['changes'] if c['action'] == 'propose-link']
                ambiguous = [c for c in proposal['changes'] if c['action'].startswith('review-')]
                alternative = {'baseIdentifier': proposal['comparisonBase'], 'productFieldsFilled': pf, 'articleFieldsFilled': af, 'proposedArticleLinks': links, 'ambiguousArticles': ambiguous, 'conflicts': proposal['conflicts']}
                alternatives.append(alternative)
                index.writerow(dict(sku=sku, identifiers='|'.join(p360['identifiers']), status=p360['status'], entities=row['entities'], mongoProducts=len(evidence['products']), mongoSkus=len(evidence['skus']), baseIdentifier=alternative['baseIdentifier'], productFieldsFilled=len(pf), articleFieldsFilled=len(af), proposedArticleLinks=len(links), conflicts=len(proposal['conflicts']), ambiguousArticles=len(ambiguous)))
            if not alternatives:
                index.writerow(dict(sku=sku, identifiers='|'.join(p360['identifiers']), status=p360['status'], entities=row['entities'], mongoProducts=len(evidence['products']), mongoSkus=len(evidence['skus'])))
            report = {'sku': sku, 'identifiers': p360['identifiers'], 'status': p360['status'], 'issue': p360['issue'], 'p360Evidence': source_file, 'mongoEvidence': mongo_file, 'alternatives': alternatives, 'mode': 'REVIEW_ONLY', 'note': 'No golden record selected. Legacy complement proposals; Mongo evidence remains separate until identity, metadata and timestamp rules are validated. No payload in this report is ready for persistence.'}
            write(os.path.join(out, sku + '.json.gz'), report)
            totals['groups'] += 1
            totals['productIdentifiers'] += len(p360['identifiers'])
            totals['alternativePlans'] += len(alternatives)
            totals[p360['status']] += 1
            totals['groupsWithMongoProductCandidates'] += bool(evidence['products'])
            totals['groupsWithMongoSkuCandidates'] += bool(evidence['skus'])
            if len(examples) < 10:
                examples.append((sku, p360['identifiers'], len(evidence['products']), len(evidence['skus']), p360['status']))
    if len(seen) != expected:
        raise RuntimeError('Incomplete group summary')
    with open(os.path.join(out, 'RESUMEN.md'), 'w', encoding='utf-8') as target:
        target.write('# Análisis de duplicados — solo revisión\n\n')
        for key, value in sorted(totals.items()):
            target.write('- {}: {}\n'.format(key, value))
        target.write('\nCada fila de review-index.csv compara una posible base. Las alternativas son excluyentes: no deben sumarse ni ejecutarse juntas. Los JSON comprimidos muestran campos complementables, artículos que se podrían asociar y conflictos. Los archivos originales conservan las fechas de los registros.\n\nMongo contiene candidatos por identidad, no una correspondencia aprobada. Aún no se aplicó la prioridad Mongo > P360 ni el desempate por fecha. No se creó ningún golden record, ni se quitaron SKU/EAN, ni se ejecutaron merges.\n\n| SKU | Identifiers | Productos Mongo candidatos | SKUs Mongo candidatos | Estado |\n|---|---|---:|---:|---|\n')
        for sku, ids, p, s, status in examples:
            target.write('| {} | {} | {} | {} | {} |\n'.format(sku, ', '.join(ids), p, s, status))
    totals['inputSHA256'] = digest(db)
    with open(os.path.join(out, 'complete.json'), 'w', encoding='utf-8') as target:
        json.dump(dict(totals), target, indent=2)
    print(json.dumps(dict(totals)))

if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise SystemExit('p360-output mongo-output NEW-review-output')
    main(*sys.argv[1:])

"""Turn durable P360 events into qualified changes, retaining unsupported scopes."""
import json, xml.etree.ElementTree as ET

ROOT = {'currentStatus':('CurrentStatus','CurrentStatus'), 'previousStatus':('PreviousStatus','Res_Int_03'),
        'externalStatus':('ExternalStatus','Res_Int_04'), 'business':('Business','Res_Int_01'),
        'ean':('EAN','EAN'), 'sku':('SKU','Res_Int_02')}
def value(n):
 if n is None: return None
 if len(n)==0:return dict(code=None,value=n.text or '')
 code=n.findtext('_code');label=n.findtext('_label')
 if code is not None and label is not None:return dict(code=code,value=label)
 return None

def parse(payload):
 o=json.loads(payload) if isinstance(payload,str) else payload
 c=o.get('entityItemChange') or {};entity=c.get('_entity');identifier=c.get('_identifier')
 if entity not in ('Product2G','Article') or not isinstance(identifier,str):return None
 result=dict(identifier=identifier,entity=1100 if entity=='Product2G' else 1000,at=c.get('_eventTimestamp'),patches=[],fallback=[],userId=(c.get('_user') or {}).get('_internalId'))
 if c.get('_changeType')!='CHANGED' or c.get('_revision',{}).get('_internalId')!='1':
  result['fallback']=['FULL'];return result
 xml=c.get('_changeSummary') or ''
 if '<!DOCTYPE' in xml or '<!ENTITY' in xml:raise ValueError('XML declarations forbidden')
 try: root=ET.fromstring(xml)
 except ET.ParseError:result['fallback']=['FULL'];return result
 fields=c.get('_changedField') or []
 if not fields:result['fallback']=['FULL'];return result
 for f in fields:
  section=f.split('.')[0]
  if 'CharacteristicValue' in section:continue
  if section in ('ArticleLang','Product2GLang'):result['fallback'].append('TEXT')
  elif section in ('ArticleDomain','Product2GDomain','ArticleExtraData','Product2GExtraData'):result['fallback'].append('DOMAIN')
  elif 'StructureMap' in section:result['fallback'].append('CLASS')
  elif 'Reference' in section:result['fallback'].append('REL')
  elif section in ('Article','Product2G','ArticleDetail','Product2GDetail'):
   known={'CurrentStatus','PrevStatus','ExternalStatus','Business','EAN','SKU','SupplierAltAID','FirstDateApproved','LastDateApproved','Res_DateTime_01','Res_DateTime_02','EmbeddedCodeWEB','EmbeddedCodeWAP'}
   result['fallback'].append('DETAIL' if f.split('.')[-1] in known else 'ROOT')
  else:result['fallback'].append('FULL')
 # Nested records retain their own characteristic/record/parent qualifiers.
 records=[n for n in root.iter() if n.tag in ('_characteristicRecords','_children')]
 for rec in records:
  name=rec.findtext('_qualification/characteristic/_code')
  if not name:result['fallback'].append('CHAR');continue
  record=rec.findtext('_qualification/recordKey');parent=rec.findtext('_qualification/parentRecordKey')
  deleted=rec.findtext('_changeType')=='DELETED'
  if rec.find('unit') is not None or deleted and rec.findtext('_datatype')=='NONE':
   result['fallback'].append('CHAR');continue # Includes cascading children; reconcile current section.
  for lang in rec.findall('_recordLang'):
   language=lang.findtext('_qualification/language/_key')
   if language=='-1':language=None
   if deleted or lang.findtext('_changeType')=='DELETED':
    result['patches'].append(dict(name=name,language=language,record=record,parent=parent,delete=True));continue
   values=lang.findall('values')
   if len(values)!=1:result['fallback'].append('CHAR');continue
   v=value(values[0].find('_current'))
   if v is None:result['fallback'].append('CHAR');continue
   result['patches'].append(dict(name=name,language=language,record=record,parent=parent,**v))
  if deleted and not rec.findall('_recordLang'):result['fallback'].append('CHAR')
 if any('CharacteristicValue' in f for f in fields) and not records:result['fallback'].append('CHAR')
 result['fallback']=sorted(set(result['fallback']))
 return result

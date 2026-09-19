import { env } from 'cloudflare:workers';
import { json,failure } from '@/lib/server-health';
import { hashSecret,syncSchema,windowAllowed } from '@/lib/device-sync';
export async function POST(req:Request){try{
 const token=req.headers.get('authorization')?.match(/^Bearer ([a-f0-9]{64})$/)?.[1];if(!token)return json({error:'휴대폰을 다시 연결해주세요.'},401);
 if(!env.DB)throw new Error('DB unavailable');const db=env.DB;const tokenHash=await hashSecret(token);
 const device=await db.prepare('SELECT user_id,last_observed_at FROM health_devices WHERE token_hash=?').bind(tokenHash).first<{user_id:string,last_observed_at:number}>();if(!device)return json({error:'연결이 해제되었습니다. 다시 연결해주세요.'},401);
 const raw=await req.text();if(raw.length>12000)return json({error:'요청이 너무 큽니다.'},413);const parsed=syncSchema.safeParse(JSON.parse(raw));if(!parsed.success||!windowAllowed(parsed.data))return json({error:'동기화 날짜와 휴대폰 시각을 확인해주세요.'},400);
 const payload=parsed.data;const now=new Date().toISOString();if(device.last_observed_at>payload.observedAt)return json({ok:true,ignored:true});
 const statements=payload.days.map(day=>day.steps===null
 ?db.prepare("DELETE FROM health_records WHERE user_id=? AND date=? AND metric='steps' AND source='health_connect' AND EXISTS(SELECT 1 FROM health_devices WHERE token_hash=? AND last_observed_at<=?)").bind(device.user_id,day.date,tokenHash,payload.observedAt)
 :db.prepare("INSERT INTO health_records(user_id,date,metric,value,note,kind,abv,source,updated_at) SELECT ?,?,'steps',?,'','',0,'health_connect',? WHERE EXISTS(SELECT 1 FROM health_devices WHERE token_hash=? AND last_observed_at<=?) ON CONFLICT(user_id,date,metric) DO UPDATE SET value=excluded.value,updated_at=excluded.updated_at WHERE health_records.source='health_connect'").bind(device.user_id,day.date,day.steps,now,tokenHash,payload.observedAt));
 statements.push(db.prepare('UPDATE health_devices SET last_sync_at=?,last_observed_at=?,background=?,timezone=? WHERE token_hash=? AND last_observed_at<=?').bind(now,payload.observedAt,payload.background?1:0,payload.timezone,tokenHash,payload.observedAt));
 // D1 batch is transactional: revoked devices and older snapshots cannot overwrite a newer snapshot.
 await db.batch(statements);return json({ok:true,syncedAt:now,days:payload.days.length});
 }catch(e){if(e instanceof SyntaxError)return json({error:'동기화 요청을 확인해주세요.'},400);return failure(e)}}

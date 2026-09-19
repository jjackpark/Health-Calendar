import { env } from 'cloudflare:workers';
import { json,failure } from '@/lib/server-health';
import { pairSchema,randomSecret,hashSecret } from '@/lib/device-sync';
export async function POST(req:Request){try{
 if(Number(req.headers.get('content-length')??0)>2048)return json({error:'요청이 너무 큽니다.'},413);
 const raw=await req.text();if(raw.length>2048)return json({error:'요청이 너무 큽니다.'},413);
 const parsed=pairSchema.safeParse(JSON.parse(raw));if(!parsed.success)return json({error:'연결 코드를 확인해주세요.'},400);
 if(!env.DB)throw new Error('DB unavailable');
 // DELETE RETURNING consumes a one-time code atomically. Only one concurrent caller can claim it.
 const owner=await env.DB.prepare('DELETE FROM health_pairing_codes WHERE code_hash=? AND expires_at>? RETURNING user_id').bind(await hashSecret(parsed.data.code),Date.now()).first<{user_id:string}>();
 if(!owner)return json({error:'연결 코드가 만료되었거나 이미 사용되었습니다. 웹에서 다시 발급해주세요.'},401);
 const token=randomSecret();await env.DB.prepare('INSERT INTO health_devices(user_id,token_hash,name,created_at,last_observed_at,background) VALUES (?,?,?,?,0,0) ON CONFLICT(user_id) DO UPDATE SET token_hash=excluded.token_hash,name=excluded.name,created_at=excluded.created_at,last_sync_at=NULL,last_observed_at=0,background=0').bind(owner.user_id,await hashSecret(token),parsed.data.name,new Date().toISOString()).run();
 return json({token,scope:'steps:write',deviceName:parsed.data.name});
 }catch(e){if(e instanceof SyntaxError)return json({error:'연결 요청을 확인해주세요.'},400);return failure(e)}}

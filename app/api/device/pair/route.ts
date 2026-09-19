import { env } from 'cloudflare:workers';
import { json,failure } from '@/lib/server-health';
import { pairSchema,randomSecret,hashSecret } from '@/lib/device-sync';
export async function POST(req:Request){try{
 if(Number(req.headers.get('content-length')??0)>2048)return json({error:'요청이 너무 큽니다.'},413);
 const raw=await req.text();if(raw.length>2048)return json({error:'요청이 너무 큽니다.'},413);
 const parsed=pairSchema.safeParse(JSON.parse(raw));if(!parsed.success)return json({error:'연결 코드를 확인해주세요.'},400);
 if(!env.DB)throw new Error('DB unavailable');
 // Claim, rotate and consume in one transaction; a simultaneous disconnect cannot be undone by a late insert.
 const codeHash=await hashSecret(parsed.data.code);const token=randomSecret();
 const results=await env.DB.batch([
  env.DB.prepare('INSERT INTO health_devices(user_id,token_hash,name,created_at,last_observed_at,background) SELECT user_id,?,?,?,0,0 FROM health_pairing_codes WHERE code_hash=? AND expires_at>? ON CONFLICT(user_id) DO UPDATE SET token_hash=excluded.token_hash,name=excluded.name,created_at=excluded.created_at,last_sync_at=NULL,last_observed_at=0,background=0 RETURNING user_id').bind(await hashSecret(token),parsed.data.name,new Date().toISOString(),codeHash,Date.now()),
  env.DB.prepare('DELETE FROM health_pairing_codes WHERE code_hash=?').bind(codeHash)
 ]);
 if(!results[0].results.length)return json({error:'연결 코드가 만료되었거나 이미 사용되었습니다. 웹에서 다시 발급해주세요.'},401);
 return json({token,scope:'steps:write',deviceName:parsed.data.name});
 }catch(e){if(e instanceof SyntaxError)return json({error:'연결 요청을 확인해주세요.'},400);return failure(e)}}

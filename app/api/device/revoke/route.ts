import { env } from 'cloudflare:workers';
import { json,failure } from '@/lib/server-health';
import { hashSecret } from '@/lib/device-sync';
export async function POST(req:Request){try{const token=req.headers.get('authorization')?.match(/^Bearer ([a-f0-9]{64})$/)?.[1];if(!token)return json({error:'인증이 필요합니다.'},401);if(!env.DB)throw new Error('DB unavailable');await env.DB.prepare('DELETE FROM health_devices WHERE token_hash=?').bind(await hashSecret(token)).run();return json({ok:true});}catch(e){return failure(e)}}

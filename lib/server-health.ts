import { env } from 'cloudflare:workers';
import { getChatGPTUser } from '@/app/chatgpt-auth';
export async function context(request:Request,write=false){const user=await getChatGPTUser();if(!user)throw new Response('로그인이 필요합니다.',{status:401});if(write){const origin=request.headers.get('origin');if(origin!==new URL(request.url).origin)throw new Response('허용되지 않은 요청입니다.',{status:403});}if(!env.DB)throw new Error('DB unavailable');return {db:env.DB,userId:user.userId};}
export const json=(value:unknown,status=200)=>Response.json(value,{status,headers:{'Cache-Control':'no-store'}});
export function failure(e:unknown){if(e instanceof Response)return e;console.error('Health request failed',e instanceof Error?e.message:'unknown');return json({error:'저장소에 연결하지 못했어요. 잠시 후 다시 시도해주세요.'},503);}
